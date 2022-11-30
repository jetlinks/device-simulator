package org.jetlinks.simulator.cmd.benchmark;

import org.jetlinks.simulator.cmd.NetClientCommandOption;
import org.jetlinks.simulator.cmd.NetworkInterfaceCompleter;
import org.jetlinks.simulator.core.Connection;
import org.jetlinks.simulator.core.benchmark.ConnectCreateContext;
import org.jetlinks.simulator.core.network.mqtt.MqttClient;
import org.jetlinks.simulator.core.network.mqtt.MqttOptions;
import picocli.CommandLine;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.Collections;

@CommandLine.Command(name = "mqtt",
        showDefaultValues = true,
        description = {
                "Create MQTT Benchmark"
        },
        headerHeading = "%n", sortOptions = false)
class MQTTBenchMark extends AbstractBenchmarkCommand implements Runnable {

    @CommandLine.Mixin
    MqttCommandOptions command;

    @CommandLine.Mixin
    NetClientCommandOption common;

    @Override
    protected Mono<? extends Connection> createConnection(ConnectCreateContext ctx) {
        if (null != common) {
            common.apply(command);
        }

        MqttOptions commandOptions = command.refactor(Collections.singletonMap("index", ctx.index()));
        ctx.beforeConnect(commandOptions);
        return MqttClient
                .connect(
                        InetSocketAddress.createUnresolved(command.getHost(), command.getPort()),
                        commandOptions
                );
    }


    static class MqttCommandOptions extends org.jetlinks.simulator.core.network.mqtt.MqttOptions {

        @Override
        @CommandLine.Option(names = {"-h", "--host"}, description = "host", order = 1, defaultValue = "127.0.0.1", required = true)
        public void setHost(String host) {
            super.setHost(host);
        }

        @Override
        @CommandLine.Option(names = {"-p", "--port"}, description = "port", order = 2, defaultValue = "1883", required = true)
        public void setPort(int port) {
            super.setPort(port);
        }

        @CommandLine.Option(names = {"-c", "--clientId"},
                description = "clientId template",
                order = 3,
                defaultValue = "mqtt-simulator-{index}",
                required = true)
        public void setClientId0(String clientId) {
            super.setClientId(clientId);
        }

        @CommandLine.Option(names = {"-u", "--username"}, description = "MQTT username", order = 4, defaultValue = "mqtt-simulator")
        public void setUsername0(String username) {
            super.setUsername(username);
        }

        @CommandLine.Option(names = {"-P", "--password"}, description = "MQTT password", order = 5, defaultValue = "mqtt-simulator")
        public void setPassword0(String password) {
            super.setPassword(password);
        }


        @CommandLine.Option(names = {"--topics"}, description = "attach and subscribe topics", order = 6)
        private String[] topics;

        @CommandLine.Option(names = {"--interface"}, description = "Network Interface", order = 7, completionCandidates = NetworkInterfaceCompleter.class)
        public void setLocalAddress0(String localAddress) {
            super.setLocalAddress(localAddress);
        }


    }

}