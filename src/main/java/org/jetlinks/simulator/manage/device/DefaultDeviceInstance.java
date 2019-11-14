package org.jetlinks.simulator.manage.device;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.simulator.enums.DeviceType;
import org.jetlinks.simulator.manage.entity.DeviceEntity;
import org.jetlinks.simulator.manage.operator.DeviceOperator;
import reactor.core.publisher.Mono;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@AllArgsConstructor
@Getter
public class DefaultDeviceInstance implements DeviceInstance {

    private DeviceEntity deviceEntity;

    @Override
    public String getDeviceId() {
        return this.deviceEntity.getDeviceId();
    }

    @Override
    public DeviceType getType() {
        return DeviceType.DEFAULT;
    }

    @Override
    public Mono<DeviceOperator> connect(DeviceType deviceType) {
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
