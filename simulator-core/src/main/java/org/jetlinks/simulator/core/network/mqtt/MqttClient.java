package org.jetlinks.simulator.core.network.mqtt;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.ReferenceCountUtil;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.mqtt.MqttClientOptions;
import io.vertx.mqtt.MqttConnectionException;
import io.vertx.mqtt.messages.MqttConnAckMessage;
import io.vertx.mqtt.messages.MqttPublishMessage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.utils.TopicUtils;
import org.jetlinks.simulator.core.Connection;
import org.jetlinks.simulator.core.ExceptionUtils;
import org.jetlinks.simulator.core.Global;
import org.jetlinks.simulator.core.network.*;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Slf4j
public class MqttClient extends AbstractConnection {
    io.vertx.mqtt.MqttClient client;

    Address address;

    private List<Consumer<MqttPublishMessage>> handlers;

    private final Map<Tuple2<String, Integer>, Subscriber> subscribers = new ConcurrentHashMap<>();

    private Disposable disposables = Disposables.disposed();

    private AtomicInteger counter = new AtomicInteger(1);

    private static Runnable DEFAULT = () -> {
    };

    public MqttClient(io.vertx.mqtt.MqttClient client, Address address, MqttOptions options, AtomicInteger counter) {
        new MqttClient(client, address, null, this.counter);
    }


    public MqttClient(io.vertx.mqtt.MqttClient client, Address localAddress, MqttOptions options, AtomicInteger counter, Runnable fallback) {
        this.counter = counter;
        this.client = client;
        this.client
                .publishHandler(msg -> {
                    received(msg.payload().length());
                    if (handlers != null) {
                        for (Consumer<MqttPublishMessage> handler : handlers) {
                            try {
                                handler.accept(msg);
                            } catch (Throwable error) {
                                log.warn("handle mqtt message {} {} error:{}", msg.topicName(),
                                         msg.payload().toString(),
                                         ExceptionUtils.getErrorMessage(error));
                            }

                        }
                    }
                })
                .closeHandler(e -> {
                    reconnect(options, fallback);
                    log.info("客户端尝试重连:{},次数：{}", this.client.clientId(), counter.get());
                });

    }


    private void reconnect(MqttOptions options, Runnable retryFallback) {
        //超过重试次数且状态未连接放弃重试
        if (counter.get() > options.getReconnectAttempts() && !isAlive()) {
            counter.set(1);
            dispose();
        } else {
            disposables = Mono.delay(Duration.ofMillis(options.getReconnectInterval()))
                              .doOnNext(i -> retryFallback.run())
                              .flatMap(i -> Mono.<MqttClient>create(sink -> this.connect1(client, options,
                                                                                          retryFallback,
                                                                                          (res, fallback) -> {
                                                                                              if (res.failed()) {
                                                                                                  sink.error(res.cause());
                                                                                                  return;
                                                                                              }
                                                                                              MqttConnAckMessage msg = res.result();
                                                                                              if (msg != null) {
                                                                                                  if (msg.code() == MqttConnectReturnCode.CONNECTION_ACCEPTED) {
                                                                                                      MqttClient mqttClient = this;
                                                                                                      mqttClient.attribute("clientId", options.getClientId());
                                                                                                      mqttClient.attribute("username", options.getUsername());
                                                                                                      mqttClient.changeState(State.connected);
                                                                                                      mqttClient.counter.getAndIncrement();
                                                                                                      mqttClient.DEFAULT = retryFallback;
                                                                                                      sink.success(mqttClient);
                                                                                                  } else {
                                                                                                      sink.error(new MqttConnectionException(msg.code()));
                                                                                                  }
                                                                                              }
                                                                                              sink.success();
                                                                                          })))
                              .subscribe();
        }

    }

    public static Mono<MqttClient> connect(InetSocketAddress server,
                                           MqttOptions options) {
        return connect(Global.vertx(), server, options, DEFAULT);
    }


    public static Mono<? extends Connection> connect(InetSocketAddress server, MqttOptions options, Runnable retryFallback) {
        return connect(Global.vertx(), server, options, retryFallback);
    }

    private static Mono<MqttClient> connect(Vertx vertx, InetSocketAddress server, MqttOptions options, Runnable retryFallback) {
        Address localAddress = AddressManager.global().takeAddress(options.getLocalAddress());

        return Mono.<MqttClient>create(sink -> {
                       MqttClientOptions clientOptions = options.copy();
                       clientOptions.setClientId(options.getClientId());
                       clientOptions.setUsername(options.getUsername());
                       clientOptions.setPassword(options.getPassword());
                       clientOptions.setLocalAddress(localAddress.getAddress().getHostAddress());
                       clientOptions.setAutoKeepAlive(true);
                       clientOptions.setTcpKeepAlive(true);
                       clientOptions.setMaxMessageSize(1024 * 1024);
                       clientOptions.setReusePort(true);

                       io.vertx.mqtt.MqttClient client = io.vertx.mqtt.MqttClient.create(vertx, clientOptions);

                       connect1(client, options,
                                retryFallback,
                                (res, fallback) -> {
                                    if (res.failed()) {
                                        sink.error(res.cause());
                                        return;
                                    }
                                    MqttConnAckMessage msg = res.result();
                                    if (msg != null) {
                                        if (msg.code() == MqttConnectReturnCode.CONNECTION_ACCEPTED) {
                                            MqttClient mqttClient = new MqttClient(client, localAddress, options, new AtomicInteger(1), retryFallback);
                                            mqttClient.attribute("clientId", options.getClientId());
                                            mqttClient.attribute("username", options.getUsername());
                                            mqttClient.changeState(State.connected);
                                            sink.success(mqttClient);
                                        } else {
                                            sink.error(new MqttConnectionException(msg.code()));
                                        }
                                    }
                                    sink.success();

                                });
                   })
                   .doOnError(err -> localAddress.release());
    }


    private static void connect1(io.vertx.mqtt.MqttClient client,
                                 MqttOptions options,
                                 Runnable retryFallback,
                                 BiConsumer<AsyncResult<MqttConnAckMessage>, Runnable> consumer) {
        client.connect(
                options.getPort(),
                options.getHost(),
                res -> consumer.accept(res, retryFallback));
    }


    @Override
    public String getId() {
        return client.clientId();
    }

    @Override
    public NetworkType getType() {
        return NetworkType.mqtt_client;
    }

    @Override
    public boolean isAlive() {
        return client.isConnected();
    }

    public synchronized Disposable handle(Consumer<MqttPublishMessage> handler) {
        if (this.handlers == null) {
            this.handlers = new ArrayList<>();
        }
        this.handlers.add(handler);

        return () -> this.handlers.remove(handler);
    }

    public void unsubscribe(String topic) {

        for (Subscriber value : subscribers.values()) {
            if (value.topic.equals(topic)) {
                value.dispose();
            }
        }
    }

    public Disposable subscribe(String topic, int qoS, Consumer<MqttPublishMessage> handler) {

        Subscriber subscriber = subscribers.computeIfAbsent(Tuples.of(topic, qoS), tp2 -> new Subscriber(tp2.getT1(), tp2.getT2()));

        return subscriber.addHandler(handler);
    }

    public class Subscriber implements Disposable, Consumer<MqttPublishMessage> {
        @Getter
        private final String topic;
        @Getter
        private final int qos;
        private final List<Consumer<MqttPublishMessage>> handlers = new CopyOnWriteArrayList<>();

        private final Disposable disposable;

        public Subscriber(String topic, int qos) {
            this.topic = topic;
            this.qos = qos;
            disposable = handle(this);
        }


        private Disposable addHandler(Consumer<MqttPublishMessage> handler) {
            if (handlers.isEmpty()) {
                client.subscribe(topic, qos);
            }
            handlers.add(handler);
            return () -> {
                handlers.remove(handler);
                tryDispose();
            };
        }

        private void tryDispose() {
            if (handlers.isEmpty()) {
                dispose();
            }
        }

        @Override
        public void dispose() {
            client.unsubscribe(topic);
            subscribers.clear();
            disposable.dispose();
            subscribers.remove(Tuples.of(topic, qos), this);
        }

        @Override
        public void accept(MqttPublishMessage msg) {
            if (TopicUtils.match(topic, msg.topicName())) {
                for (Consumer<MqttPublishMessage> handler : handlers) {
                    handler.accept(msg);
                }
            }
        }

    }

    public void publish(String topic, int qos, Object payload) {

        publishAsync(topic, qos, payload)
                .doOnError(err -> {
                    log.warn("publish error {} {} {}", topic, payload, ExceptionUtils.getErrorMessage(err));
                })
                .subscribe();

    }

    public Mono<Void> publishAsync(String topic, int qos, Object payload) {
        return publishAsync(topic, qos, NetworkUtils.castToByteBuf(payload));
    }

    public Mono<Void> publishAsync(String topic, int qos, ByteBuf payload) {
        Buffer buffer = Buffer.buffer(payload);
        int len = buffer.length();
        return Mono.<Void>create(sink -> client
                           .publish(topic,
                                    buffer,
                                    MqttQoS.valueOf(qos),
                                    false,
                                    false,
                                    res -> {
                                        ReferenceCountUtil.safeRelease(payload);
                                        sent(len);
                                        if (res.failed()) {
                                            sink.error(res.cause());
                                        } else {
                                            sink.success();
                                        }
                                    }))
                   .doOnError(this::error);
    }

    public List<Subscriber> getSubscriptions() {
        return new ArrayList<>(subscribers.values());
    }

    @Override
    protected void doDisposed() {
        if (!Objects.isNull(address) && !Objects.isNull(address.getAddress())) {
            address.release();
        }
        super.doDisposed();
        disposables.dispose();
        if (client.isConnected()) {
            client.disconnect();
        }
    }

    @Override
    public void reset() {
        super.reset();
        getSubscriptions().forEach(Disposable::dispose);
        subscribers.clear();
    }
}
