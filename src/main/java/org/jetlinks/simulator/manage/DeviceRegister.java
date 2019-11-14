package org.jetlinks.simulator.manage;

import org.jetlinks.simulator.manage.device.DeviceInstance;
import org.jetlinks.simulator.manage.entity.DeviceEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @version 1.0
 **/
public interface DeviceRegister {

    Mono<DeviceInstance> createDevice(DeviceEntity entity);

    Mono<DeviceInstance> getDevice(String deviceId);

    Flux<DeviceInstance> getAllDevice();

    Mono<Boolean> destroyDevice(String deviceId);
}
