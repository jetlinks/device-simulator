package org.jetlinks.simulator;

import org.jetlinks.simulator.enums.ClientType;
import reactor.core.publisher.Flux;

public interface DeviceClient<T extends ClientConfiguration> {

    ClientType getType();

    Flux<? extends ClientSession> connect(T config);

}
