package org.jetlinks.simulator.manage.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetlinks.simulator.enums.DeviceState;
import org.jetlinks.simulator.enums.DeviceType;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeviceEntity {

    private String id;

    private String name;

    private DeviceState state;

    private DeviceType type;

    private String protocol;

    private String parentId;
}
