package org.jetlinks.simulator.manage.device;

import lombok.AllArgsConstructor;
import org.jetlinks.simulator.enums.DeviceType;
import org.jetlinks.simulator.manage.entity.DeviceEntity;
import org.jetlinks.simulator.manage.operator.DeviceOperator;
import reactor.core.publisher.Mono;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@AllArgsConstructor
public class ChildDeviceInstance implements DeviceInstance {

    private DeviceEntity deviceEntity;

    @Override
    public String getDeviceId() {
        return this.deviceEntity.getId();
    }

    @Override
    public DeviceType getType() {
        return DeviceType.CHILD;
    }

    @Override
    public Mono<DeviceOperator> connect() {
        return null;
    }

    @Override
    public Mono<Boolean> disConnect() {
        return null;
    }

    @Override
    public Mono<Boolean> checkState() {
        return null;
    }

    @Override
    public Mono<DeviceOperator> getDeviceOperator() {
        return null;
    }
}
