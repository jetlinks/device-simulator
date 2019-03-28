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
    private String clientId;

    private String username;

    private String password;
}
