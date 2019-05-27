package org.jetlinks.simulator.mqtt;

import lombok.Getter;
import lombok.Setter;

/**
 * @author zhouhao
 * @since 1.0.0
 */
@Getter
@Setter
public class MQTTAuth {
    private int index;
    private String mqttAddress;

    private int mqttPort;

    private String clientId;

    private String username;

    private String password;
}
