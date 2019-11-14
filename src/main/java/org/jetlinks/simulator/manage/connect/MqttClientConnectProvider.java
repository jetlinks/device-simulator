package org.jetlinks.simulator.manage.connect;

import io.vertx.core.Vertx;
import io.vertx.mqtt.MqttClientOptions;
import org.jetlinks.simulator.enums.ClientType;
import org.jetlinks.simulator.mqtt.MqttAuthInfo;
import org.jetlinks.simulator.manage.client.MqttClientConfiguration;
import org.jetlinks.simulator.manage.client.VertxMqttDeviceClient;

public class MqttClientConnectProvider implements ClientConnectProvider {


    private Vertx vertx = Vertx.vertx();


    @Override
    public ClientType getClientType() {
        return ClientType.MQTT;
    }

    @Override
    public void createConnect(String username, String password) {
        VertxMqttDeviceClient client = new VertxMqttDeviceClient();
        MqttClientConfiguration clientConfiguration =
                MqttClientConfiguration.builder()
                        .host("127.0.0.11")
                        .port(1883)
                        .number(1)
                        .authGenerator((idx) -> {
                            return new MqttAuthInfo("test"+idx, username,password);
                        })
                        .options(new MqttClientOptions())
                        .build();
        client.connect(clientConfiguration)
                .subscribe(session -> {
                    session.handleMessage()
                            .subscribe(msg -> {
                               // System.out.println(msg);
                                //session.send()
                            });
                });
    }
}
