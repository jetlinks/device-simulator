package org.jetlinks.simulator.mqtt;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.google.common.collect.Maps;
import io.netty.buffer.Unpooled;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.concurrent.Future;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.commons.codec.digest.DigestUtils;
import org.hswebframework.expands.script.engine.DynamicScriptEngine;
import org.hswebframework.expands.script.engine.DynamicScriptEngineFactory;
import org.hswebframework.utils.StringUtils;
import org.jetlinks.mqtt.client.MqttClient;
import org.jetlinks.mqtt.client.MqttClientCallback;
import org.jetlinks.mqtt.client.MqttClientConfig;
import org.jetlinks.mqtt.client.MqttConnectResult;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author zhouhao
 * @since 1.0.0
 */
public class MQTTSimulator {

    transient EventLoopGroup eventLoopGroup;

    transient Class channelClass;


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
    int limit = 10000;

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
    String scriptFile = "./scripts/handler.js";

    @Getter
    @Setter
    private String[] binds = null;

    @Getter
    @Setter
    private int bindPortStart = 10000;

    @Getter
    @Setter
    private boolean ssl = false;

    @Getter
    @Setter
    private String p12Path = "./ssl/jetlinks-client.p12";

    @Getter
    @Setter
    private String p12Password = "jetlinks";

    @Getter
    @Setter
    private String cerPath = "./ssl/jetlinks-server.cer";

    @Getter
    @Setter
    private int batchSize = 100;

    @Getter
    @Setter
    private int timeout = 30;

    @Getter
    @Setter
    private int threadSize = Runtime.getRuntime().availableProcessors() * 2;

    Map<String, ClientSession> clientMap;

    Map<String, MessageHandler> messageHandlerMap = new HashMap<>();

    Map<String, MessageHandler> childMessageHandler = new HashMap<>();

    BiConsumer<Integer, ClientSession> eventDataSuppliers;

    private ScheduledExecutorService executorService;

    BiConsumer<Integer, MQTTAuth> authBiConsumer = (index, auth) -> {
        //secureId|timestamp
        String username = "test|" + System.currentTimeMillis();
        //md5(secureId|timestamp|secureKey)
        String password = DigestUtils.md5Hex(username + "|" + "test");
        auth.setUsername(username);
        auth.setPassword(password);
    };

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
    @Getter
    public class ClientSession {
        private MqttClient client;
        private MQTTAuth auth;

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

    public void init() {
        executorService = Executors.newScheduledThreadPool(batchSize);
        createMqttJob = new LinkedBlockingQueue<>(batchSize);

        if (KQueue.isAvailable()) {
            eventLoopGroup = new KQueueEventLoopGroup(threadSize);
            channelClass = KQueueSocketChannel.class;
        } else if (Epoll.isAvailable()) {
            eventLoopGroup = new EpollEventLoopGroup(threadSize);
            channelClass = EpollSocketChannel.class;
        } else {
            eventLoopGroup = new NioEventLoopGroup(threadSize);
            channelClass = NioSocketChannel.class;
        }
    }


    public void onEvent(BiConsumer<Integer, ClientSession> clientSessionBiConsumer) {
        this.eventDataSuppliers = clientSessionBiConsumer;
    }

    public void bindHandler(String topic, MessageHandler handler) {
        messageHandlerMap.put(topic, handler);
    }

    public void bindChildHandler(String topic, MessageHandler handler) {
        childMessageHandler.put(topic, handler);
    }


    private SslContext sslContext;

    @SneakyThrows
    public SslContext getSSLContext() {

        if (ssl && sslContext == null) {
            Objects.requireNonNull(p12Path, "p12Path不能为空");
            Objects.requireNonNull(cerPath, "cerPath不能为空");
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new FileInputStream(p12Path), p12Password.toCharArray());
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            CertificateFactory cAf = CertificateFactory.getInstance("X.509");
            FileInputStream caIn = new FileInputStream(cerPath);
            X509Certificate ca = (X509Certificate) cAf.generateCertificate(caIn);
            KeyStore caKs = KeyStore.getInstance("JKS");
            caKs.load(null, null);
            caKs.setCertificateEntry("ca-certificate", ca);
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
            tmf.init(caKs);
            keyManagerFactory.init(keyStore, p12Password.toCharArray());
            sslContext = SslContextBuilder.forServer(keyManagerFactory)
                    .trustManager(tmf)
                    .build();
        }
        return sslContext;
    }

    private Consumer<ClientSession> onConnect;

    public void onConnect(Consumer<ClientSession> consumer) {
        this.onConnect = consumer;
    }

    public Future<MqttConnectResult> createMqttClient(MQTTAuth auth, InetSocketAddress bind, Consumer<MqttConnectResult> call) throws Exception {
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

        if (ssl) {
            //开启双向认证
            mqttClient.getClientConfig().setSslEngineConsumer(engine -> {
                engine.setUseClientMode(true);
            });
        }
        Runnable initAuth = () -> {
            mqttClient.getClientConfig().setClientId(auth.getClientId());
            mqttClient.getClientConfig().setUsername(auth.getUsername());
            mqttClient.getClientConfig().setPassword(auth.getPassword());
        };

        mqttClient.getClientConfig().setBindAddress(bind);
        mqttClient.setEventLoop(eventLoopGroup);
        mqttClient.getClientConfig().setChannelClass(channelClass);
        mqttClient.getClientConfig().setProtocolVersion(MqttVersion.MQTT_3_1_1);
        mqttClient.getClientConfig().setReconnect(true);
        mqttClient.getClientConfig().setRetryInterval(5);
        mqttClient.getClientConfig().setTimeoutSeconds(timeout);
        mqttClient.getClientConfig().setSslContext(getSSLContext());
        initAuth.run();
        AtomicLong errorCounter = new AtomicLong();

        mqttClient.setCallback(new MqttClientCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                authBiConsumer.accept(auth.getIndex(), auth);
                initAuth.run();
                if (errorCounter.incrementAndGet() >= 20) {
                    mqttClient.disconnect();
                } else {
                    System.out.println("客户端" + auth.getClientId() + "连接失败" + (bind == null ? "," : "(本地ip:" + bind + "),") + cause.getClass().getSimpleName() + ":" + cause.getMessage());
                }
            }

            @Override
            public void onSuccessfulReconnect() {
                ClientSession session = clientMap.get(auth.getClientId());
                if (null != session && null != onConnect) {
                    onConnect.accept(session);
                }
            }
        });
        return mqttClient.connect(auth.getMqttAddress(), auth.getMqttPort())
                .addListener(future -> {
                    MqttConnectResult result = null;
                    try {
                        result = (MqttConnectResult) future.get(5, TimeUnit.SECONDS);
                        if (result.getReturnCode() != MqttConnectReturnCode.CONNECTION_ACCEPTED) {
                            mqttClient.disconnect();
                        } else {
                            ClientSession session = new ClientSession(mqttClient, auth);
                            clientMap.put(auth.getClientId(), session);
                            if (null != onConnect) {
                                onConnect.accept(session);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        call.accept(result);
                    }
                });

    }

    private BlockingQueue<Runnable> createMqttJob;

    private void printReport() {
        int[] useTime = {5000, 2000, 1000, 500, 200, 100, 10};
        Map<Integer, LongAdder> adder = new LinkedHashMap<>(useTime.length);
        for (int i : useTime) {
            adder.put(i, new LongAdder());
        }

        long max = 0;
        long min = 999999;
        long total = 0;
        //打印报告
        for (ClientSession session : clientMap.values()) {
            long use = session.getAuth().getConnectedTime() - session.getAuth().getRequestTime();
            max = Math.max(max, use);
            min = Math.min(min, use);
            total += use;
            for (int i1 : useTime) {
                if (use >= i1) {
                    adder.get(i1).increment();
                    break;
                }
            }
        }
        System.out.println();
        System.out.println("max : " + max + "ms");
        System.out.println("min : " + min + "ms");
        System.out.println("avg : " + total / limit + "ms");

        System.out.println();
        for (Map.Entry<Integer, LongAdder> entry : adder.entrySet()) {
            System.out.println("> " + entry.getKey() + "ms : " + entry.getValue() + "("
                    + new BigDecimal((entry.getValue().doubleValue() / limit) * 100)
                    .setScale(2, BigDecimal.ROUND_HALF_UP) + "%)");
        }
        System.out.println();
    }

    public void start() throws Exception {
        init();
        String scriptFileContent = new String(Files.readAllBytes(Paths.get(scriptFile)));
        DynamicScriptEngine engine = DynamicScriptEngineFactory.getEngine("js");
        engine.compile("handle", scriptFileContent);
        Map<String, Object> context = Maps.newHashMap();
        context.put("simulator", this);
        context.put("logger", LoggerFactory.getLogger("message.handler"));
        engine.execute("handle", context).getIfSuccess();
        int end = start + limit;
        AtomicInteger started = new AtomicInteger();

        int queueSizePeekSize = Math.max(4, threadSize - 4);
        for (int i = 0; i < queueSizePeekSize; i++) {
            executorService.submit(() -> {
                while (started.longValue() <= limit) {
                    try {
                        Runnable job = createMqttJob.poll();
                        if (job == null) {
                            Thread.sleep(100);
                        } else {
                            job.run();
                        }
                    } catch (Exception ignore) {

                    }
                }
            });
        }
        System.out.println("开始创建连接...");

        for (int i = start; i < end; i++) {
            MQTTAuth auth = new MQTTAuth();
            auth.setMqttAddress(address);
            auth.setMqttPort(port);
            auth.setIndex(i);
            auth.setClientId(prefix + i);

            authBiConsumer.accept(i, auth);

            createMqttJob.put(() -> {
                try {
                    createMqttClient(auth, createAddress(auth.getIndex()), (result) -> {
                        auth.setConnectedTime(System.currentTimeMillis());
                        int current = started.incrementAndGet();
                        if (current % batchSize == 0 || current == limit) {
                            System.out.println("create mqtt client: " + current + " ok");
                        }
                        if (current == limit) {
                            printReport();
                        }
                    }).await();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        if (enableEvent && eventDataSuppliers != null) {
            runRate(this::doPushEvent, eventRate);
        }
        //  System.out.println("create mqtt client done :" + len);
    }

    private Map<String, AtomicInteger> portCounter = new ConcurrentHashMap<>();

    public InetSocketAddress createAddress(int index) {
        if (binds == null || binds.length == 0) {
            return null;
        }
        //选择网卡
        //----------------当前设备索引/(总数/网卡数量)
        try {
            String host = binds[Math.min(binds.length - 1, index / (limit / binds.length))];
            return new InetSocketAddress(host, portCounter
                    .computeIfAbsent(host, h -> new AtomicInteger(bindPortStart))
                    .incrementAndGet());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void doPushEvent() {
        System.out.println("开始上报设备事件");
        try {
            int clientSize = this.clientMap.size();
            List<ClientSession> all = new ArrayList<>(this.clientMap.values());

            int eventLimit = Math.min(this.eventLimit, clientSize);
            Random random = new Random();
            while (eventLimit >= 0) {
                ClientSession session = all.get(random.nextInt(clientSize));
                if (session != null) {
                    eventDataSuppliers.accept(eventLimit, session);
                }
                eventLimit--;
            }
        } catch (Throwable e) {
            e.printStackTrace();
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
        MQTTSimulator simulator = jsonObject.toJavaObject(MQTTSimulator.class);
        simulator.clientMap = new ConcurrentHashMap<>(simulator.limit);
        System.out.println("使用配置:\n" + JSON.toJSONString(simulator, SerializerFeature.PrettyFormat));
        simulator.start();
    }

}
