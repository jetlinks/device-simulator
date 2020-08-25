package org.jetlinks.simulator.tcp;

import io.vertx.core.Vertx;
import lombok.AllArgsConstructor;
import org.jetlinks.simulator.core.*;
import org.jetlinks.simulator.mqtt.MqttSimulator;

@AllArgsConstructor
public class TcpSimulatorProvider implements SimulatorProvider {

    private final Vertx vertx;

    private final KeyManager certificateManager;

    private final SimulatorListenerBuilder listenerBuilder;

    private final AddressPool addressPool;

    @Override
    public String getType() {
        return "tcp_client";
    }

    @Override
    public TcpSimulator createSimulator(SimulatorConfig config) {

        return new TcpSimulator(vertx, certificateManager, config, listenerBuilder, addressPool);
    }

}
