package org.jetlinks.simulator.enums;

import org.jetlinks.simulator.manage.device.ChildDeviceInstance;
import org.jetlinks.simulator.manage.device.DefaultDeviceInstance;
import org.jetlinks.simulator.manage.device.DeviceInstance;
import org.jetlinks.simulator.manage.entity.DeviceEntity;

/**
 * @author bsetfeng
 * @since 1.0
 **/
public enum DeviceType {
    DEFAULT{
        @Override
        public DeviceInstance createDeviceInstance(DeviceEntity entity) {
            return new DefaultDeviceInstance(entity);
        }
    },CHILD{
        @Override
        public DeviceInstance createDeviceInstance(DeviceEntity entity) {
            return new ChildDeviceInstance(entity);
        }
    };

    public abstract DeviceInstance createDeviceInstance(DeviceEntity entity);
}
