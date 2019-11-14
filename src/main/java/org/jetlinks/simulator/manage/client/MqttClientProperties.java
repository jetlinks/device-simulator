package org.jetlinks.simulator.manage.client;


import lombok.Getter;
import lombok.Setter;

//@ConfigurationProperties(prefix = "jetlinks.redis")
@Getter
@Setter
public class MqttClientProperties {

    private String host;

    private String port;

}
