package org.jetlinks.simulator.manage;

import org.jetlinks.simulator.manage.device.DeviceInstance;
import org.jetlinks.simulator.manage.entity.DeviceEntity;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Component
public class DeviceRegisterCenter implements DeviceRegister {

    // TODO: 2019/11/14 不支持特大量设备测试
    private Map<String, DeviceInstance> deviceMap = new ConcurrentHashMap<>();

    @Override
    public Mono<DeviceInstance> createDevice(DeviceEntity entity) {
        return Mono.create(sink -> {
            DeviceInstance deviceInstance = entity.getType().createDeviceInstance(entity);
            deviceMap.put(deviceInstance.getDeviceId(), deviceInstance);
            sink.success(deviceInstance);
        });
    }

    @Override
    public Mono<DeviceInstance> getDevice(String deviceId) {
        return Mono.just(deviceMap.get(deviceId));
    }

    @Override
    public Flux<DeviceInstance> getAllDevice() {
        return Flux.create(sink -> {
            deviceMap.forEach((key, value) -> sink.next(value));
            sink.complete();
        });
    }

    @Override
    public Mono<Boolean> destroyDevice(String deviceId) {
        return Mono.just(deviceMap.remove(deviceId) != null);
    }
}
