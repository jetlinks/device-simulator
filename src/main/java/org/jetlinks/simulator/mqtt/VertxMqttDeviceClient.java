package org.jetlinks.simulator.mqtt;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.MqttMessage;
import org.jetlinks.core.message.codec.SimpleMqttMessage;
import org.jetlinks.simulator.ClientSession;
import org.jetlinks.simulator.ClientType;
import org.jetlinks.simulator.DeviceClient;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

@Slf4j
public class VertxMqttDeviceClient implements DeviceClient<MqttClientConfiguration> {


    @Override
    public ClientType getType() {
        return ClientType.MQTT;
    }

    private Vertx vertx = Vertx.vertx();

    public static void main(String[] args) {
        VertxMqttDeviceClient client = new VertxMqttDeviceClient();
        MqttClientConfiguration clientConfiguration =
                MqttClientConfiguration.builder()
                        .host("127.0.0.1")
                        .port(1883)
                        .number(1)
                        .authGenerator((idx) -> {
                            //secureId|timestamp
                            String username = "test|" + System.currentTimeMillis();
                            //md5(secureId|timestamp|secureKey)
                            String password = DigestUtils.md5Hex(username + "|" + "test");
                            return new MqttAuthInfo("test"+idx, username,password);
                        })
                        .options(new MqttClientOptions())
                        .build();
        client.connect(clientConfiguration)
                .subscribe(session -> {
                    session.handleMessage()
                            .subscribe(msg -> {
                                System.out.println(msg);
                            });
                });
    }

    private void doConnect(FluxSink<ClientSession> sink,
                           int index,
                           MqttClientConfiguration configuration,
                           MqttClientOptions options, MqttClientSession session) {
        MqttAuthInfo authInfo = configuration.getAuthGenerator().generate(index);
        options.setClientId(authInfo.getClientId());
        options.setUsername(authInfo.getUsername());
        options.setPassword(authInfo.getPassword());

        MqttClient client = MqttClient.create(vertx, options);
        client.connect(configuration.getPort(), configuration.getHost(), result -> {
            if (result.succeeded()) {
                MqttClientSession mqttClientSession = session == null ? new MqttClientSession() : session;

                mqttClientSession.client = client;
                client.closeHandler((r) -> {
                    if (!mqttClientSession.manualClose) {
                        if (mqttClientSession.retryTimes.incrementAndGet() >= configuration.getOptions().getReconnectAttempts()) {
                            return;
                        }
                        Mono.delay(Duration.ofMillis(Math.max(options.getReconnectInterval(), 1000)))
                                .subscribe(ignore -> doConnect(sink, index, configuration, options, mqttClientSession));
                    }
                });

                client.publishHandler(message -> {
                    mqttClientSession.processor.onNext(
                            SimpleMqttMessage.builder()
                                    .deviceId(options.getClientId())
                                    .messageId(message.messageId())
                                    .payload(message.payload().getByteBuf())
                                    .qosLevel(message.qosLevel().value())
                                    .topic(message.topicName())
                                    .dup(message.isDup())
                                    .retain(message.isRetain())
                                    .build()
                    );
                });
                if (session == null) {
                    sink.next(mqttClientSession);
                }
            } else {
                sink.error(result.cause());
            }
        });
    }

    @Override
    public Flux<? extends ClientSession> connect(MqttClientConfiguration configuration) {
        return Flux.create(sink -> {
            for (int i = 0; i < configuration.getNumber(); i++) {
                MqttClientOptions options = new MqttClientOptions(configuration.getOptions());
                options.setReconnectAttempts(2);
                options.setReconnectInterval(Duration.ofSeconds(5).toMillis());
                doConnect(sink, i, configuration, options, null);
            }
        });
    }

    class MqttClientSession implements ClientSession {
        private boolean manualClose;

        private AtomicLong retryTimes = new AtomicLong();

        private MqttClient client;

        private EmitterProcessor<EncodedMessage> processor;

        public MqttClientSession() {
            this.processor = EmitterProcessor.create(false);
        }

        @Override
        public String getDeviceId() {
            return client.clientId();
        }

        @Override
        public Mono<Boolean> send(EncodedMessage message) {
            return Mono.create(sink -> {
                if (!(message instanceof MqttMessage)) {
                    sink.error(new UnsupportedOperationException("unsupported message type:" + message.getClass()));
                    return;
                }
                MqttMessage mqtt = ((MqttMessage) message);

                client.publish(mqtt.getTopic()
                        , Buffer.buffer(mqtt.getPayload())
                        , MqttQoS.valueOf(mqtt.getQosLevel())
                        , mqtt.isDup()
                        , mqtt.isRetain(), result -> {
                            if (result.succeeded()) {
                                sink.success(true);
                            } else {
                                sink.error(result.cause());
                            }
                        });

            });
        }

        @Override
        public Flux<EncodedMessage> handleMessage() {
            return processor.map(Function.identity());
        }

        @Override
        public void close() {
            manualClose = true;
            client.disconnect();
            processor.dispose();
        }

        public boolean isClosed() {
            return !client.isConnected();
        }
    }
}
