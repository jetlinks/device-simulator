package org.jetlinks.simulator.core.network.mqtt;

import io.vertx.mqtt.MqttClientOptions;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;


@Getter
@Setter
public class MqttOptions extends MqttClientOptions {
    String host = "127.0.0.1";

    int port = 1883;

    public MqttOptions() {

    }

    private MqttOptions(MqttOptions options) {
        super(options);
        setHost(options.getHost());
        setPort(options.getPort());
    }

    @Override
    public String toString() {
        return String.format("mqtt://%s@%s:%d", getClientId(), host, port);
    }


    public MqttOptions copy() {
        return new MqttOptions(this);
    }

    private MqttOptions apply(Map<String, Object> args) {
        for (Map.Entry<String, Object> entry : args.entrySet()) {
            String key = "{" + entry.getKey() + "}";
            String value = String.valueOf(entry.getValue());

            setClientId(getClientId().replace(key, value));

            if (getUsername() != null) {
                setUsername(getUsername().replace(key, value));
            }
            if (getPassword() != null) {
                setPassword(getPassword().replace(key, value));
            }
        }

        return this;
    }

    public MqttOptions refactor(Map<String, Object> args) {
        return copy().apply(args);
    }

}
