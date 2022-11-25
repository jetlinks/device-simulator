package org.jetlinks.simulator.core.network.mqtt;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.ReferenceCountUtil;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.mqtt.MqttClientOptions;
import io.vertx.mqtt.MqttConnectionException;
import io.vertx.mqtt.messages.MqttConnAckMessage;
import io.vertx.mqtt.messages.MqttPublishMessage;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.utils.TopicUtils;
import org.jetlinks.simulator.core.ExceptionUtils;
import org.jetlinks.simulator.core.Global;
import org.jetlinks.simulator.core.network.AbstractConnection;
import org.jetlinks.simulator.core.network.NetworkType;
import org.jetlinks.simulator.core.network.NetworkUtils;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
public class MqttClient extends AbstractConnection {
    io.vertx.mqtt.MqttClient client;

    private List<Consumer<MqttPublishMessage>> handlers;

    private final Map<String, Object> subscribers = new ConcurrentHashMap<>();

    private MqttClient(io.vertx.mqtt.MqttClient client) {
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
                .closeHandler(e -> dispose());

    }

    public static Mono<MqttClient> connect(InetSocketAddress server,
                                           MqttOptions options) {
        return connect(Global.vertx(), server, options);
    }

    public static Mono<MqttClient> connect(Vertx vertx,
                                           InetSocketAddress server,
                                           MqttOptions options) {
        return Mono.create(sink -> {
            MqttClientOptions clientOptions = new MqttClientOptions();
            clientOptions.setClientId(options.getClientId());
            clientOptions.setUsername(options.getUsername());
            clientOptions.setPassword(options.getPassword());
            clientOptions.setAutoKeepAlive(true);
            clientOptions.setTcpKeepAlive(true);
            clientOptions.setMaxMessageSize(1024 * 1024);
            clientOptions.setReusePort(true);

            io.vertx.mqtt.MqttClient client = io.vertx.mqtt.MqttClient.create(vertx, clientOptions);

            client.connect(
                    server.getPort(),
                    server.getHostString(),
                    res -> {
                        if (res.failed()) {
                            sink.error(res.cause());
                            return;
                        }

                        MqttConnAckMessage msg = res.result();
                        if (msg != null) {
                            if (msg.code() == MqttConnectReturnCode.CONNECTION_ACCEPTED) {
                                MqttClient mqttClient = new MqttClient(client);
                                mqttClient.attribute("clientId", options.clientId);
                                mqttClient.attribute("username", options.getUsername());
                                mqttClient.changeState(State.connected);
                                sink.success(mqttClient);
                            } else {
                                sink.error(new MqttConnectionException(msg.code()));
                            }
                        }
                        sink.success();

                    });
        });
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

    public Disposable subscribe(String topic, int qoS, Consumer<MqttPublishMessage> handler) {
        if (subscribers.put(topic, handler) == null) {
            client.subscribe(topic, qoS);
        }

        Disposable disposable = handle(msg -> {
            if (TopicUtils.match(topic, msg.topicName())) {
                handler.accept(msg);
            }
        });
        return () -> {
            disposable.dispose();
            if (subscribers.remove(topic, handler)) {
                client.unsubscribe(topic);
            }

        };
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
        return Mono.create(sink -> client
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
                         }));
    }


    @Override
    protected void doDisposed() {
        super.doDisposed();
        if (client.isConnected()) {
            client.disconnect();
        }
    }
}
