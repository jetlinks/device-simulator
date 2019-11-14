package org.jetlinks.simulator.manage.device;

import org.jetlinks.simulator.enums.DeviceType;
import org.jetlinks.simulator.manage.operator.DeviceOperator;
import reactor.core.publisher.Mono;

/**
 * @version 1.0
 **/
public interface DeviceInstance {

    String getDeviceId();

    DeviceType getType();

    Mono<DeviceOperator> connect();

    Mono<Boolean> disConnect();

    Mono<Boolean> checkState();

    Mono<DeviceOperator> getDeviceOperator();
}
