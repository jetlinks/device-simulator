package org.jetlinks.simulator.manage.client;

import io.vertx.mqtt.MqttClientOptions;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.jetlinks.simulator.ClientConfiguration;
import org.jetlinks.simulator.mqtt.MqttAuthGenerator;

@Getter
@Setter
@Builder
public class MqttClientConfiguration implements ClientConfiguration {

    private int number;

    private String host;

    private int port;

    private MqttClientOptions options;

    @NonNull
    private MqttAuthGenerator authGenerator;


}
