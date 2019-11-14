package org.jetlinks.simulator.mqtt;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class MqttAuthInfo {
    private String clientId;

    private String username;

    private String password;
}
