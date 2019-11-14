package org.jetlinks.simulator.manage;

import org.jetlinks.simulator.DeviceInstance;
import org.jetlinks.simulator.enums.DeviceState;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author bsetfeng
 * @since 1.0
 **/
public interface DeviceManager {

    Mono<Integer> connectAll();

    Mono<Integer> disconnnectAll();

    Mono<Boolean> connect(String deviceId);

    Mono<Boolean> disconnnect(String deviceId);

    Mono<DeviceState> getDeviceState(String deviceId);

    Flux<DeviceInstance> getConnectedDevice();
}
