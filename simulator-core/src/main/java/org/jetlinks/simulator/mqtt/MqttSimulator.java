package org.jetlinks.simulator.mqtt;

import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.vertx.core.Vertx;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;
import io.vertx.mqtt.MqttConnectionException;
import org.jetlinks.core.Value;
import org.jetlinks.core.Values;
import org.jetlinks.simulator.core.*;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class MqttSimulator extends AbstractSimulator {

    private final Vertx vertx;

    private final KeyManager keyManager;

    MqttSimulator(Vertx vertx,
                  KeyManager keyManager,
                  SimulatorConfig config,
                  SimulatorListenerBuilder builder,
                  AddressPool pool) {
        super(config, builder, pool);
        this.vertx = vertx;
        this.keyManager = keyManager;
    }

    @Override
    protected Mono<MqttSession> createSession(int index, String bind) {

        Values networkConfig = Values.of(config.getNetwork().getConfiguration());

        Map<String, Object> ctx = new HashMap<>();
        ctx.put("index", index);

        String clientId = networkConfig.getValue("clientId").map(Value::asString).map(id -> processExpression(id, ctx)).orElse(null);
        String username = networkConfig.getValue("username").map(Value::asString).map(id -> processExpression(id, ctx)).orElse(null);
        String password = networkConfig.getValue("password").map(Value::asString).map(id -> processExpression(id, ctx)).orElse(null);

        int keepAliveTimeSeconds = networkConfig.getValue("keepAliveTimeSeconds").map(Value::asInt).orElse(30);
        int reconnectAttempts = networkConfig.getValue("reconnectAttempts").map(Value::asInt).orElse(5);
        int reconnectInterval = networkConfig.getValue("reconnectInterval").map(Value::asInt).orElse(5);

        String certId = networkConfig.getValue("certId").map(Value::asString).orElse(null);
        boolean tls = networkConfig.getValue("tls").map(Value::asBoolean).orElse(false);

        String host = networkConfig.getValue("host").map(Value::asString).orElse("127.0.0.1");
        int port = networkConfig.getValue("port").map(Value::asInt).orElse(1883);

        MqttClientOptions clientOptions = new MqttClientOptions();
        clientOptions.setKeepAliveTimeSeconds(keepAliveTimeSeconds);
        clientOptions.setReconnectAttempts(reconnectAttempts);
        clientOptions.setReconnectInterval(reconnectInterval);
        clientOptions.setClientId(clientId);
        clientOptions.setUsername(username);
        clientOptions.setPassword(password);
        clientOptions.setLocalAddress(bind);

        MqttSession session = new MqttSession(clientId, index);

        session.setOptions(clientOptions);

        Function<MqttClientOptions, Mono<MqttClient>> clientBuilder;

        if (tls && StringUtils.hasText(certId)) {
            clientBuilder = opts -> keyManager
                    .getKeyAndTrust(certId)
                    .map(options -> {
                        opts.setTrustOptions(options);
                        opts.setKeyCertOptions(options);
                        return MqttClient.create(vertx, opts);
                    });
        } else {
            clientBuilder = opts -> Mono.just(MqttClient.create(vertx, opts));
        }

        session.setConnect(Mono.defer(() -> clientBuilder
                .apply(session.getOptions())
                .flatMap(client -> Mono
                        .create(sink -> {
                            try {
                                client.connect(port, host, result -> {
                                    if (result.succeeded()) {
                                        if (result.result().code() == MqttConnectReturnCode.CONNECTION_ACCEPTED) {
                                            sink.success(client);
                                        } else {
                                            sink.error(new MqttConnectionException(result.result().code()));
                                        }
                                    } else {
                                        sink.error(result.cause());
                                    }
                                });
                            } catch (Throwable e) {
                                sink.error(e);
                            }
                        })))
        );

        return Mono.just(session);
    }


}
