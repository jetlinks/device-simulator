package org.jetlinks.simulator.mqtt;

import io.vertx.core.Vertx;
import lombok.SneakyThrows;
import org.jetlinks.simulator.core.DefaultAddressPool;
import org.jetlinks.simulator.core.SimulatorConfig;
import org.jetlinks.simulator.core.listener.DefaultSimulatorListenerBuilder;
import org.jetlinks.simulator.core.listener.MessageLogListener;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MqttSimulatorTest {


    @Test
    @SneakyThrows
    public void test() {

        SimulatorConfig config = new SimulatorConfig();

        SimulatorConfig.Runner runner = new SimulatorConfig.Runner();
        runner.setTotal(1);
        runner.setBatch(100);
        runner.setStartWith(0);

        config.setRunner(runner);

        config.setId("test");
        SimulatorConfig.Network network = new SimulatorConfig.Network();
        Map<String, Object> configuration = new HashMap<>();

        configuration.put("clientId", "test${index}");
        configuration.put("username", "test");
        configuration.put("password", "test");
        configuration.put("port", 1889);
        network.setConfiguration(configuration);

        config.setListeners(Arrays.asList(
                new SimulatorConfig.Listener()
                        .id("test")
                        .type("jsr223")
                        .with("lang", "js")
                        .with("script", StreamUtils.copyToString(new ClassPathResource("mqtt-simulator.js").getInputStream(), StandardCharsets.UTF_8))
        ));

        config.setNetwork(network);

        MqttSimulator simulator = new MqttSimulator(Vertx.vertx(), null, config, new DefaultSimulatorListenerBuilder(), new DefaultAddressPool());

        simulator.registerListener(new MessageLogListener("test-message"));

        simulator.doOnComplete(() -> {
            simulator.state()
                    .subscribe(state -> {
                        System.out.println(state);
                    });
        });


        simulator.start()
                .subscribe();

        Thread.sleep(1000000);
        simulator.state()
                .subscribe(state -> {
                    System.out.println(state);
                });


    }

}