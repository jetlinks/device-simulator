package org.jetlinks.simulator.core;

import reactor.core.publisher.Mono;

public interface SimulatorManager {

    Mono<Simulator> getSimulator(String id);

    Mono<Simulator> createSimulator(SimulatorConfig config);
}
