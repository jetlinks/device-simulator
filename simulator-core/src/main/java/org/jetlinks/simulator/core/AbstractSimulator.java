package org.jetlinks.simulator.core;

import org.apache.commons.collections.CollectionUtils;
import org.hswebframework.web.utils.ExpressionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class AbstractSimulator implements Simulator {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    protected final List<SimulatorListener> listeners = new CopyOnWriteArrayList<>();

    protected final Map<String, Session> sessions = new ConcurrentHashMap<>();

    protected final SimulatorConfig config;

    protected final AddressPool addressPool;

    protected final SimulatorListenerBuilder listenerBuilder;

    private final EmitterProcessor<String> logProcessor = EmitterProcessor.create(false);
    private final FluxSink<String> logSink = logProcessor.sink(FluxSink.OverflowStrategy.BUFFER);

    private final AtomicLong connection = new AtomicLong();
    private final AtomicLong total = new AtomicLong();
    private final AtomicLong success = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();
    private final AtomicLong maxTime = new AtomicLong();
    private final AtomicLong minTime = new AtomicLong(99999);
    private final AtomicLong avgTime = new AtomicLong();
    private final AtomicLong totalTime = new AtomicLong();
    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean complete = new AtomicBoolean();

    private final Map<String, AtomicLong> errorCounter = new ConcurrentHashMap<>();

    private final Map<Integer, AtomicLong> dist = new ConcurrentHashMap<>();

    private final List<Runnable> completeListener = new CopyOnWriteArrayList<>();

    private final Disposable.Composite disposable = Disposables.composite();

    //时间分布
    int[] distArray = {5000, 1000, 500, 100, 20};


    public AbstractSimulator(SimulatorConfig config, SimulatorListenerBuilder builder, AddressPool pool) {
        this.addressPool = pool;
        this.config = config;
        for (int i : distArray) {
            dist.put(i, new AtomicLong());
        }
        this.listenerBuilder = builder;
    }

    protected String processExpression(String expression, Map<String, Object> context) {
        return ExpressionUtils.analytical(expression, context, "spel");
    }

    protected abstract Mono<? extends Session> createSession(int index, String bind);

    @Override
    public Mono<SimulatorListener> getListener(String id) {
        return Flux.fromIterable(listeners)
                .filter(s -> s.getId().equals(id))
                .singleOrEmpty();
    }

    @Override
    public void registerListener(SimulatorListener listener) {
        if (!listener.supported(this)) {
            return;
        }
        listener.init(this);
        listeners.add(listener);
        listeners.sort(Comparator.comparing(SimulatorListener::order));
    }


    protected void fireListener(Consumer<SimulatorListener> listenerConsumer) {
        listeners.forEach(listenerConsumer);
    }

    protected void before(Session session) {
        connection.incrementAndGet();
        fireListener(listener -> listener.before(session));
    }

    protected void after(Session session) {
        connection.decrementAndGet();
        total.incrementAndGet();
        if (session.isConnected()) {
            success.incrementAndGet();
        } else {
            session.lastError()
                    .ifPresent(this::error);
        }
        fireListener(listener -> listener.after(session));
        sessions.put(session.getId(), session);

        long time = session.getConnectUseTime();
        totalTime.addAndGet(time);

        maxTime.updateAndGet(v -> Math.max(time, v));
        minTime.updateAndGet(v -> Math.min(time, v));
        avgTime.set(totalTime.get() / total.get());
        for (int i : distArray) {
            if (time > i) {
                dist.computeIfAbsent(i, ignore -> new AtomicLong(i)).incrementAndGet();
            }
        }
    }

    protected void error(Throwable error) {
        log(error.getMessage(), error);
        failed.incrementAndGet();
        errorCounter
                .computeIfAbsent(error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage(), i -> new AtomicLong())
                .incrementAndGet();

    }

    private Mono<Void> createBath(int index, int total) {
        return Flux
                .range(index, total)
                .parallel()
                .runOn(Schedulers.parallel())
                .flatMap(i -> {

                    String bind = addressPool
                            .take(config.getRunner().getBinds())
                            .orElseThrow(() -> new UnsupportedOperationException("网络地址资源不够!"));

                    return this
                            .createSession(i, bind)
                            .doOnNext(session -> session.onDisconnected(() -> addressPool.release(bind)));

                })
                .doOnNext(this::before)
                .flatMap(session -> session.connect().thenReturn(session))
                .doOnNext(this::after)
                .then()
                .doOnSuccess(ignore -> this.log("create simulators[{}] complete!", total))
                .onErrorContinue((err, v) -> this.error(err));
    }

    @Override
    public Mono<State> state() {
        return Mono.fromCallable(() -> {
            State state = new State();
            state.setComplete(complete.get());
            state.setCurrent(connection.get());
            state.setFailed(failed.get());
            state.setTotal(total.get());
            state.setDistTime(dist
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get())));

            state.setFailedTypeCounts(errorCounter
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get())));

            State.Agg agg = new State.Agg();
            agg.avg = avgTime.intValue();
            agg.max = maxTime.intValue();
            agg.min = minTime.intValue();
            agg.total = totalTime.intValue();
            state.setAggTime(agg);
            return state;
        });
    }

    public void doStart() {
        if (!started.compareAndSet(false, true)) {
            return;
        }
        if (!CollectionUtils.isEmpty(config.getListeners())) {
            for (SimulatorConfig.Listener listener : config.getListeners()) {
                registerListener(listenerBuilder.build(listener));
            }
        }
        int start = config.getRunner().getStartWith();
        int total = config.getRunner().getTotal();
        int batch = config.getRunner().getBatch();
        List<Mono<Void>> jobs = new ArrayList<>();

        int batchSize = total / batch;
        for (int i = 0; i < batchSize; i++) {
            total -= batch;
            jobs.add(createBath(start + i * batch, batch));
        }
        if (total > 0) {
            jobs.add(createBath(start + batchSize * batch, total));
        }
        disposable.add(Flux.concat(jobs)
                .doFinally(f -> complete())
                .doOnError(this::error)
                .subscribe());
    }

    private void complete() {
        complete.set(true);
        completeListener.forEach(Runnable::run);
    }

    public void log(String msg, Object... args) {
        log.debug(msg, args);
        if (logProcessor.hasDownstreams()) {
            String log = MessageFormatter.arrayFormat(msg, args).getMessage();
            logSink.next(log);
        }
    }

    @Override
    public Flux<String> handleLog() {
        return logProcessor;
    }

    @Override
    public void doOnComplete(Runnable runnable) {
        completeListener.add(runnable);
    }

    @Override
    public Mono<Void> start() {
        return Mono.fromRunnable(this::doStart);
    }

    @Override
    public Mono<Void> shutdown() {
        return Mono
                .fromRunnable(() -> {
                    for (Session value : sessions.values()) {
                        value.close();
                    }
                    sessions.clear();
                    fireListener(SimulatorListener::shutdown);
                    disposable.dispose();
                });
    }

    @Override
    public int totalSession() {
        return sessions.size();
    }

    @Override
    public Mono<Session> getSession(String id) {
        return Mono.justOrEmpty(sessions.get(id));
    }

    @Override
    public Flux<Session> getSessions() {
        return Flux.fromIterable(sessions.values());
    }

}
