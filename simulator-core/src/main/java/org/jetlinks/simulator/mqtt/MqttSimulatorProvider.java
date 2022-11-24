package org.jetlinks.simulator.mqtt;

import io.vertx.core.Vertx;
import lombok.AllArgsConstructor;
import org.jetlinks.simulator.core.*;

@AllArgsConstructor
public class MqttSimulatorProvider implements SimulatorProvider {

    private final Vertx vertx;

    private final KeyManager certificateManager;

    private final SimulatorListenerBuilder listenerBuilder;

    private final AddressPool addressPool;

    @Override
    public String getType() {
        return "mqtt_client";
    }

    @Override
    public MqttSimulator createSimulator(SimulatorConfig config) {

        return new MqttSimulator(vertx, certificateManager, config, listenerBuilder, addressPool);
    }

}
