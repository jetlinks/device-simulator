package org.jetlinks.simulator.core.listener;

import org.jetlinks.simulator.core.*;
import reactor.core.publisher.Mono;

public class TestSimulator extends AbstractSimulator {

    public TestSimulator(SimulatorConfig config, SimulatorListenerBuilder builder, AddressPool pool) {
        super(config, builder, pool);
    }

    @Override
    protected Mono<? extends Session> createSession(int index, String bind) {

        return Mono.just(new AbstractSession("mock-" + index, index) {
            @Override
            public void close() {

            }

            @Override
            protected Mono<Void> doConnect() {
                return Mono.empty();
            }
        });
    }
}
