package org.jetlinks.simulator.core.benchmark;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.simulator.core.Connection;
import org.jetlinks.simulator.core.ConnectionManager;
import org.jetlinks.simulator.core.ExceptionUtils;
import org.jetlinks.simulator.core.report.Reporter;
import org.jetlinks.simulator.core.script.Script;
import org.jetlinks.simulator.core.script.ScriptFactory;
import org.jetlinks.simulator.core.script.Scripts;
import org.joda.time.DateTime;
import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
public class Benchmark implements Disposable, BenchmarkHelper {

    public static final String REPORT_CONNECTING = "connecting";

    public static final ScriptFactory scriptFactory = Scripts.getFactory("js");

    @Getter
    private final String name;

    @Getter
    private final BenchmarkOptions options;

    private final Reporter reporter;

    @Getter
    private final ConnectionManager connectionManager;

    private final Function<ConnectCreateContext, Mono<? extends Connection>> connectionFactory;

    private final List<BiConsumer<Integer, Object>> beforeConnectHandler = new CopyOnWriteArrayList<>();
    private final List<Consumer<Connection>> connectionHandler = new CopyOnWriteArrayList<>();
    private final List<Runnable> completeHandler = new CopyOnWriteArrayList<>();

    private final Disposable.Composite disposable = Disposables.composite();

    private final Set<String> errors = ConcurrentHashMap.newKeySet();

    private final Deque<String> logs = new ConcurrentLinkedDeque<>();

    @Getter
    private Throwable lastError;


    public Benchmark(String name,
                     BenchmarkOptions options,
                     Reporter reporter,
                     ConnectionManager connectionManager,
                     Function<ConnectCreateContext, Mono<? extends Connection>> connectionFactory) {
        this.name = name;
        this.options = options;
        this.reporter = reporter;
        this.connectionManager = connectionManager;
        this.connectionFactory = connectionFactory;
    }

    public static Benchmark create(String name,
                                   BenchmarkOptions options,
                                   ConnectionManager connectionManager,
                                   Function<ConnectCreateContext, Mono<? extends Connection>> connectionFactory) {
        return new Benchmark(name, options, Reporter.create(), connectionManager, connectionFactory);
    }

    public Reporter getReporter() {
        return reporter;
    }

    public Deque<String> getLogs() {
        return logs;
    }

    public void start() {
        if (disposable.size() > 0) {
            return;
        }

        if (options.getFile() != null) {
            executeScript(Paths.get(options.getFile().toURI()));
        }
        disposable.add(
                Flux
                        .range(options.getIndex(), options.getSize())
                        .flatMap(this::connect, options.getConcurrency(), options.getConcurrency())
                        .doOnNext(this::handleConnected)
                        .then()
                        .doFinally(ignore -> {
                            for (Runnable runnable : completeHandler) {
                                runnable.run();
                            }
                        })
                        .subscribe()
        );
    }

    private void handleConnected(Connection connection) {

        connectionManager
                .addConnection(connection);

        for (Consumer<Connection> consumer : connectionHandler) {
            try {
                consumer.accept(connection);
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    void handleBeforeConnect(int index, Object ctx) {
        for (BiConsumer<Integer, Object> consumer : beforeConnectHandler) {
            try {
                consumer.accept(index, ctx);
            } catch (Throwable e) {
                log.warn("handleBeforeConnect error:{}", ExceptionUtils.getErrorMessage(e));
            }
        }
    }

    public Benchmark onConnected(Consumer<Connection> consumer) {
        this.connectionHandler.add(consumer);
        return this;
    }

    public Benchmark beforeConnect(BiConsumer<Integer, Object> handler) {
        beforeConnectHandler.add(handler);
        return this;
    }

    public Benchmark onComplete(Runnable runnable) {
        completeHandler.add(runnable);
        return this;
    }

    private Mono<Object> castMono(Object obj) {
        if (obj instanceof Publisher) {
            return Mono.from(((Publisher<?>) obj));
        }
        return Mono.justOrEmpty(obj);
    }

    @Override
    public Object require(String location) {
        return executeScript(Paths.get(location));
    }

    @SneakyThrows
    protected Object executeScript(Path file) {
        String script = new String(Files.readAllBytes(file));
        Map<String, Object> context = new HashMap<>();
        if (options.getScriptArgs() != null) {
            context.putAll(options.getScriptArgs());
            context.put("args", options.getScriptArgs());
        } else {
            context.put("args", Collections.emptyMap());
        }
        context.put("benchmark", this);
        return scriptFactory
                .compileExpose(Script.of("benchmark_" + name, script)
                                     .returnNative(),
                               BenchmarkHelper.class)
                .call(this, context);

    }

    public Mono<Void> randomConnectionAsync(int size, Function<Connection, Object> handler) {
        return connectionManager
                .randomConnection(size)
                .flatMap(conn -> castMono(handler.apply(conn)), size)
                .then();
    }

    public Disposable randomConnection(int size, Function<Connection, Object> handler) {
        return randomConnectionAsync(size, handler)
                .subscribe();
    }

    public Disposable delay(Callable<Object> callable, int ms) {
        return Mono
                .delay(Duration.ofMillis(ms))
                .flatMap(ignore -> Mono
                        .fromCallable(callable)
                        .flatMap(this::castMono)
                        .onErrorResume(err -> {
                            error("delay execute", err);
                            return Mono.empty();
                        }))
                .subscribe();
    }


    public Disposable interval(Callable<Object> callable, int ms) {
        Disposable interval = Flux
                .interval(Duration.ofMillis(ms))
                .onBackpressureDrop()
                .concatMap(ignore -> Mono
                        .fromCallable(callable)
                        .flatMap(this::castMono)
                        .onErrorResume(err -> {
                            error("interval execute", err);
                            return Mono.empty();
                        }))
                .subscribe();

        disposable.add(interval);
        return () -> {
            disposable.remove(interval);
            interval.dispose();
        };
    }

    private void error(String operation, Throwable e) {
        lastError = e;
        if (errors.size() > 100) {
            errors.clear();
        }
        errors.add(operation + ":" + ExceptionUtils.getErrorMessage(e));
        // log.warn(operation, e);
    }

    private Mono<? extends Connection> connect(int index) {
        Reporter.Point point = reporter.newPoint(REPORT_CONNECTING);
        point.start();

        return connectionFactory
                .apply(new ConnectCreateContextImpl(index))
                .doOnNext(ignore -> point.success())
                .onErrorResume(err -> {
                    point.error(getErrorMessage(err));
                    error("connect", err);
                    return Mono.empty();
                });
    }

    private String getErrorMessage(Throwable error) {
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }

    @Override
    public void dispose() {
        disposable.dispose();
    }

    public void doOnDispose(Disposable disposable) {
        this.disposable.add(disposable);
    }

    @AllArgsConstructor
    class ConnectCreateContextImpl implements ConnectCreateContext {

        private final int index;

        @Override
        public int index() {
            return index;
        }

        @Override
        public void beforeConnect(Object context) {
            handleBeforeConnect(index, context);
        }
    }

    public void print(String log, Object... args) {
        logs.add(new DateTime().toString("HH:mm:ss.SSS") + " " + String.format(log, args));
        if (logs.size() > 100) {
            logs.removeFirst();
        }
    }

}
