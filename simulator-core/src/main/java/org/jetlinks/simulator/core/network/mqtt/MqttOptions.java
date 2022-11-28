package org.jetlinks.simulator.core.network.mqtt;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;


@Getter
@Setter
public class MqttOptions {
    String host = "127.0.0.1";

    int port = 1883;

    String clientId;

    String username;

    String password;

    @Override
    public String toString() {
        return String.format("mqtt://%s@%s:%d", clientId, host, port);
    }


    public MqttOptions copy() {
        return new MqttOptions().copyFrom(this);
    }

    public MqttOptions copyFrom(MqttOptions options) {
        setClientId(options.getClientId());
        setHost(options.getHost());
        setPort(options.getPort());
        setUsername(options.getUsername());
        setPassword(options.getPassword());
        return this;
    }

    private MqttOptions apply(Map<String, Object> args) {
        for (Map.Entry<String, Object> entry : args.entrySet()) {
            String key = "{" + entry.getKey() + "}";
            String value = String.valueOf(entry.getValue());

            clientId = clientId.replace(key, value);
            if (username != null) {
                username = username.replace(key, value);
            }
            if (password != null) {
                password = password.replace(key, value);
            }
        }

        return this;
    }

    public MqttOptions refactor(Map<String, Object> args) {
        return copy().apply(args);
    }

}
