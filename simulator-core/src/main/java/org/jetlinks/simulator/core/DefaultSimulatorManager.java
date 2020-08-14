package org.jetlinks.simulator.core;

import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultSimulatorManager implements SimulatorManager {

    Map<String, Simulator> simulators = new ConcurrentHashMap<>();

    Map<String, SimulatorProvider> providers = new ConcurrentHashMap<>();

    @Override
    public Mono<Simulator> getSimulator(String id) {
        return Mono.justOrEmpty(simulators.get(id));
    }

    @Override
    public Mono<Simulator> createSimulator(SimulatorConfig config) {

        return Mono
                .justOrEmpty(providers.get(config.getType()))
                .switchIfEmpty(Mono.error(() -> new UnsupportedOperationException("unsupported type:" + config.getType())))
                .map(provider -> provider.createSimulator(config))
                .doOnNext(simulator -> simulators.put(config.getId(), simulator))
                ;
    }

    public void addProvider(SimulatorProvider provider) {
        providers.put(provider.getType(), provider);
    }
}
