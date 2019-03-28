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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.codec.digest.DigestUtils;
import org.hswebframework.expands.script.engine.DynamicScriptEngine;
import org.hswebframework.expands.script.engine.DynamicScriptEngineFactory;
import org.hswebframework.utils.StringUtils;
import org.jetlinks.mqtt.client.*;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public class MQTTSimulator {

    static final EventLoopGroup eventLoopGroup;

    static final Class channelClass;

    static final String functionInvokeScriptFile = "./scripts/handler.js";

    @Getter
    @Setter
    String prefix = "test";

    @Getter
    @Setter
    String address = "127.0.0.1";

    @Getter
    @Setter
    int port = 1883;

    @Getter
    @Setter
    int start = 0;

    @Getter
    @Setter
    int limit = 100;

    //开启事件上报
    @Getter
    @Setter

    boolean enableEvent = false;

    //一次事件上报设备最大数量
    @Getter
    @Setter
    int eventLimit = 10;

    //事件上报频率
    @Getter
    @Setter
    int eventRate = 10000;

    @Getter
    @Setter
    String scriptFile = functionInvokeScriptFile;

    @Getter
    @Setter
    private String[] binds = null;

    @Getter
    @Setter
    private int bindPortStart = 10000;


    Map<String, ClientSession> clientMap;

    Map<String, MessageHandler> messageHandlerMap = new HashMap<>();

    Map<String, MessageHandler> childMessageHandler = new HashMap<>();

    Supplier<String> eventDataSuppliers;

    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors() * 2);

    BiConsumer<Integer, MQTTAuth> authBiConsumer;

    public void onAuth(BiConsumer<Integer, MQTTAuth> consumer) {
        this.authBiConsumer = consumer;
    }

    public void runRate(Runnable runnable, long time) {
        executorService.scheduleAtFixedRate(runnable, 2000, time, TimeUnit.MILLISECONDS);
    }

    public void runDelay(Runnable runnable, long time) {
        executorService.schedule(runnable, time, TimeUnit.MILLISECONDS);
    }

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
                            System.out.println("发送消息:" + topic + "=>" + json);
                        }
                    });
        }

        public void sendChilDeviceMessage(String topic, String deviceId, Object msg) {
            JSONObject json;
            if (msg instanceof String) {
                json = JSON.parseObject((String) msg);
            } else {
                json = (JSONObject) JSON.toJSON(msg);
            }
            json.put("clientId", deviceId);
            JSONObject message = new JSONObject();
            message.put("topic", topic);
            message.put("message", json);
            message.put("childDeviceId", deviceId);
            client.publish("/child-device-message", Unpooled.copiedBuffer(JSON.toJSONBytes(message)))
                    .addListener(future -> {
                        if (!future.isSuccess()) {
                            System.out.println("发送消息:/child-device-message=>" + message + "  失败：" + future.cause());
                        } else {
                            System.out.println("发送消息:/child-device-message=>" + message);
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

    public void onEvent(Supplier<String> eventDataSuppliers) {
        this.eventDataSuppliers = eventDataSuppliers;
    }

    public void bindHandler(String topic, MessageHandler handler) {
        messageHandlerMap.put(topic, handler);
    }

    public void bindChildHandler(String topic, MessageHandler handler) {
        childMessageHandler.put(topic, handler);
    }

    public void createMqttClient(MQTTAuth auth, InetSocketAddress bind) throws Exception {
        MqttClientConfig clientConfig = new MqttClientConfig();
        MqttClient mqttClient = MqttClient.create(clientConfig, (topic, payload) -> {
            String data = payload.toString(StandardCharsets.UTF_8);
            System.out.println(topic + "=>" + data);
            MessageHandler handler = messageHandlerMap.get(topic);
            if (null != handler) {
                handler.handle(JSON.parseObject(data), clientMap.get(auth.getClientId()));
            } else {
                if ("/child-device-message".equals(topic)) {
                    JSONObject jsonObject = JSON.parseObject(data);
                    String childTopic = jsonObject.getString("childTopic");
                    handler = childMessageHandler.get(childTopic);
                    if (null != handler) {
                        handler.handle(jsonObject.getJSONObject("childMessage"), clientMap.get(auth.getClientId()));
                    }
                }
            }
        });

        mqttClient.getClientConfig().setBindAddress(bind);
        mqttClient.setEventLoop(eventLoopGroup);
        mqttClient.getClientConfig().setChannelClass(channelClass);
        mqttClient.getClientConfig().setClientId(auth.getClientId());
        mqttClient.getClientConfig().setUsername(auth.getUsername());
        mqttClient.getClientConfig().setPassword(auth.getPassword());
        mqttClient.getClientConfig().setProtocolVersion(MqttVersion.MQTT_3_1_1);
        mqttClient.getClientConfig().setReconnect(true);
        AtomicLong errorCounter = new AtomicLong();

        mqttClient.setCallback(new MqttClientCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                if (errorCounter.incrementAndGet() >= 5) {
                    mqttClient.disconnect();
                } else {
                    System.out.println("客户端" + auth.getClientId() + "连接失败," + cause.getClass().getSimpleName() + ":" + cause.getMessage());
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
                            clientMap.put(auth.getClientId(), new ClientSession(mqttClient));
                            System.out.println("success:" + auth.getClientId());
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
        int len = 0;
        for (int i = start; i < end; i++) {
            MQTTAuth auth = new MQTTAuth();
            auth.setClientId(prefix + i);
            if (authBiConsumer != null) {
                authBiConsumer.accept(i, auth);
            } else {
                //secureId|timestamp
                String username = "test|" + System.currentTimeMillis();
                //md5(secureId|timestamp|secureKey)
                String password = DigestUtils.md5Hex(username + "|" + "test");
                auth.setUsername(username);
                auth.setPassword(password);
            }
            createMqttClient(auth, createAddress(len++));
        }
        if (enableEvent && eventDataSuppliers != null) {
            runRate(this::doPushEvent, eventRate);
        }
    }

    private Map<String, AtomicInteger> portCounter = new ConcurrentHashMap<>();

    public InetSocketAddress createAddress(int index) {
        if (binds == null || binds.length == 0) {
            return null;
        }
        //选择网卡
        //----------------当前设备索引/(总数/网卡数量)
        String host = binds[index / (limit / binds.length)];
        return new InetSocketAddress(host, portCounter
                .computeIfAbsent(host, h -> new AtomicInteger(bindPortStart))
                .incrementAndGet());
    }


    public void doPushEvent() {
        int clientSize = this.clientMap.size();
        int eventLimit = Math.min(this.eventLimit, clientSize);
        Random random = new Random();
        while (eventLimit >= 0) {
            ClientSession session = clientMap.get(prefix + random.nextInt(clientSize));
            if (session != null) {
                session.sendMessage("/event", eventDataSuppliers.get());
            }
            eventLimit--;
        }
    }

    public static void main(String[] args) throws Exception {
        JSONObject jsonObject = new JSONObject();

        System.getProperties()
                .entrySet()
                .stream()
                .flatMap(e -> System.getenv().entrySet().stream())
                .filter(e -> String.valueOf(e.getKey()).startsWith("mqtt."))
                .forEach(e -> jsonObject.put(String.valueOf(e.getKey()).substring(5), e.getValue()));
        for (String arg : args) {
            String[] split = arg.split("[=]");
            jsonObject.put(split[0].startsWith("mqtt.") ? split[0].substring(5) : split[0], split.length == 2 ? split[1] : true);
        }
        String binds = jsonObject.getString("binds");
        if (!StringUtils.isNullOrEmpty(binds)) {
            jsonObject.put("binds", binds.split("[,]"));
        }
        MQTTSimulator MQTTSimulator = jsonObject.toJavaObject(MQTTSimulator.class);
        MQTTSimulator.clientMap = new HashMap<>(MQTTSimulator.limit);
        System.out.println("使用配置:\n" + JSON.toJSONString(MQTTSimulator, SerializerFeature.PrettyFormat));
        MQTTSimulator.start();
    }

}
