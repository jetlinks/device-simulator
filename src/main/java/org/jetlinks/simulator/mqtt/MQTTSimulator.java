package org.jetlinks.simulator.mqtt;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttVersion;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.mqtt.client.*;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author zhouhao
 * @since 1.0.0
 */
@Getter
@Setter
public class MQTTSimulator {

    static final EventLoopGroup eventLoopGroup;

    static final Class channelClass;

    String prefix = "test";

    String address = "127.0.0.1";

    int port = 1883;

    int start = 0;

    int limit = 10000;

    Map<String, MqttClient> clientMap;

    static {
        String os = System.getProperty("os.name");
        if (os.toLowerCase().startsWith("win")) {
            eventLoopGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2);
            channelClass = NioSocketChannel.class;

        } else if (os.toLowerCase().startsWith("linux")) {
            eventLoopGroup = new EpollEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2);
            channelClass = EpollSocketChannel.class;

        } else {
            eventLoopGroup = new KQueueEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2);
            channelClass = KQueueSocketChannel.class;
        }

    }

    public void createMqttClient(String clientId, String username, String password) throws Exception {
        MqttClientConfig clientConfig = new MqttClientConfig();
        MqttClient mqttClient = MqttClient.create(clientConfig, (topic, payload) -> {
            System.out.println(topic + "=>" + payload.toString(StandardCharsets.UTF_8));
            // TODO: 19-3-19 Reply
        });
        mqttClient.setEventLoop(eventLoopGroup);
        mqttClient.getClientConfig().setChannelClass(channelClass);
        mqttClient.getClientConfig().setClientId(clientId);
        mqttClient.getClientConfig().setUsername(username);
        mqttClient.getClientConfig().setPassword(password);
        mqttClient.getClientConfig().setProtocolVersion(MqttVersion.MQTT_3_1_1);
        mqttClient.getClientConfig().setReconnect(true);
        AtomicLong errorCounter = new AtomicLong();

        mqttClient.setCallback(new MqttClientCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                if (errorCounter.incrementAndGet() >= 5) {
                    mqttClient.disconnect();
                } else {
                    System.out.println("客户端" + clientId + "连接失败:" + cause.getMessage());
                }
            }

            @Override
            public void onSuccessfulReconnect() {

            }
        });
        mqttClient.connect(address, port)
                .addListener(future -> {
                    try {
                        MqttConnectResult result = (MqttConnectResult) future.get(15, TimeUnit.SECONDS);
                        if (result.getReturnCode() != MqttConnectReturnCode.CONNECTION_ACCEPTED) {
                            mqttClient.disconnect();
                        } else {
                            clientMap.put(clientId, mqttClient);
                            System.out.println("success:" + clientId);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).await(2, TimeUnit.SECONDS);
    }

    public void start() throws Exception {
        int end = start + limit;
        for (int i = start; i < end; i++) {
            createMqttClient(prefix + i, "simulator", "Simulator");
        }
    }

    public static void main(String[] args) throws Exception {
        JSONObject jsonObject = new JSONObject();
        System.getProperties().entrySet()
                .stream()
                .flatMap(e -> System.getenv().entrySet().stream())
                .filter(e -> String.valueOf(e.getKey()).startsWith("mqtt."))
                .forEach(e -> jsonObject.put(String.valueOf(e.getKey()).substring(5), e.getValue()));
        for (String arg : args) {
            String[] split = arg.split("[=]");
            jsonObject.put(split[0], split.length == 2 ? split[1] : true);
        }
        System.out.println(JSON.toJSONString(jsonObject, SerializerFeature.PrettyFormat));
        MQTTSimulator MQTTSimulator = jsonObject.toJavaObject(MQTTSimulator.class);
        MQTTSimulator.clientMap = new HashMap<>(MQTTSimulator.limit);
        MQTTSimulator.start();
    }

}
