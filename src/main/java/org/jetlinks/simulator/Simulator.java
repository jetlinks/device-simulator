package org.jetlinks.simulator;

import org.jetlinks.simulator.mqtt.MqttClientConfiguration;
import reactor.core.publisher.Flux;

import java.util.Map;

public interface Simulator {

    void registerClient(MqttClientConfiguration mqtt);

    void registerStrategy(SimulationStrategy strategy);

    void start();

    void stop();

    Map<String, ClientSession> getSessions();

    Flux<ClientSession> onConnected();

    static Simulator newSimulator(){
        return new DefaultSimulator();
    }
}
