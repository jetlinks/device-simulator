package org.jetlinks.simulator;

import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.simulator.mqtt.MqttClientConfiguration;
import org.jetlinks.simulator.mqtt.VertxMqttDeviceClient;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

@Slf4j
class DefaultSimulator implements Simulator {

    static Vertx vertx = Vertx.vertx();

    private Map<String, ClientSession> storage = new ConcurrentHashMap<>();

    private VertxMqttDeviceClient deviceClient = new VertxMqttDeviceClient(vertx);

    private List<SimulationStrategy> strategies = new CopyOnWriteArrayList<>();

    private EmitterProcessor<ClientSession> processor = EmitterProcessor.create(false);

    private AtomicBoolean started = new AtomicBoolean();

    @Override
    public Map<String, ClientSession> getSessions() {
        return storage;
    }

    @Override
    public Flux<ClientSession> onConnected() {
        return processor
                .map(Function.identity());
    }

    @Override
    public void registerClient(MqttClientConfiguration mqtt) {
        // TODO: 2019/12/5 monitor
        deviceClient
                .connect(mqtt)
                .doOnNext(session -> {
                    if (processor.hasDownstreams()) {
                        processor.onNext(session);
                    }
                })
                .onErrorContinue((err, obj) -> {
                    log.error(err.getMessage(), err);
                })
                .subscribe(session -> storage.put(session.getDeviceId(), session));
    }

    @Override
    public void registerStrategy(SimulationStrategy strategy) {
        if (started.get()) {
            strategy.start();
        }
        strategies.add(strategy);
    }

    @Override
    public void start() {
        started.set(true);
        for (SimulationStrategy strategy : strategies) {
            strategy.start();
        }
    }

    @Override
    public void stop() {
        for (SimulationStrategy strategy : strategies) {
            strategy.stop();
        }
    }
}
