package org.jetlinks.simulator.mqtt;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.google.common.collect.Maps;
import io.netty.buffer.Unpooled;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttVersion;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.expands.script.engine.DynamicScriptEngine;
import org.hswebframework.expands.script.engine.DynamicScriptEngineFactory;
import org.jetlinks.mqtt.client.*;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
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

    static final String functionInvokeScriptFile = "./scripts/handler.js";

    String prefix = "test";

    String address = "127.0.0.1";

    int port = 1883;

    int start = 0;

    int limit = 100;

    String scriptFile = functionInvokeScriptFile;

    Map<String, ClientSession> clientMap;

    Map<String, MessageHandler> messageHandlerMap = new HashMap<>();

    @AllArgsConstructor
    public class ClientSession {
        MqttClient client;

        public void sendMessage(String topic, Object msg) {
            String json;
            if (msg instanceof String) {
                json = (String) msg;
            } else {
                json = JSON.toJSONString(msg);
            }
            client.publish(topic, Unpooled.copiedBuffer(json.getBytes()))
                    .addListener(future -> {
                        if (!future.isSuccess()) {
                            System.out.println("发送消息:" + topic + "=>" + json + "  失败：" + future.cause());
                        } else {
                            System.out.println("发送消息:" + topic + "\n" + json);
                        }
                    });
        }
    }

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

    public void bindHandler(String topic, MessageHandler handler) {
        messageHandlerMap.put(topic, handler);
    }

    public void createMqttClient(String clientId, String username, String password) throws Exception {
        MqttClientConfig clientConfig = new MqttClientConfig();
        MqttClient mqttClient = MqttClient.create(clientConfig, (topic, payload) -> {
            String data = payload.toString(StandardCharsets.UTF_8);
            System.out.println(topic + "=>" + data);
            MessageHandler handler = messageHandlerMap.get(topic);
            if (null != handler) {
                handler.handle(JSON.parseObject(data), clientMap.get(clientId));
            }
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
                            clientMap.put(clientId, new ClientSession(mqttClient));
                            System.out.println("success:" + clientId);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).await(2, TimeUnit.SECONDS);
    }

    public void start() throws Exception {
        String scriptFileContent = new String(Files.readAllBytes(Paths.get(scriptFile)));
        DynamicScriptEngine engine = DynamicScriptEngineFactory.getEngine("js");
        engine.compile("handle", scriptFileContent);
        Map<String, Object> context = Maps.newHashMap();
        context.put("simulator", this);
        context.put("logger", LoggerFactory.getLogger("message.handler"));
        engine.execute("handle", context).getIfSuccess();
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
