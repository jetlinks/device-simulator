package org.jetlinks.simulator.cmd.benchmark;

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
                "mqtt 压力测试,用于批量创建mqtt连接",
                "clientId,username,password支持使用表达式动态生成,如: mqtt-test-\\${index\\}"
        },
        headerHeading = "%n", sortOptions = false)
class MqttBenchMark extends AbstractBenchmarkCommand implements Runnable {

    @CommandLine.Mixin
    MqttCommandOptions command;

    @Override
    protected Mono<? extends Connection> createConnection(ConnectCreateContext ctx) {
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

        @Override
        @CommandLine.Option(names = {"-c", "--clientId"}, description = "clientId,支持表达式:${index}", order = 3, defaultValue = "mqtt-simulator-${index}", required = true)
        public void setClientId(String clientId) {
            super.setClientId(clientId);
        }

        @Override
        @CommandLine.Option(names = {"-u", "--username"}, description = "username,支持表达式:${index}", order = 4, defaultValue = "mqtt-simulator")
        public void setUsername(String username) {
            super.setUsername(username);
        }

        @Override
        @CommandLine.Option(names = {"-P", "--password"}, description = "password,支持表达式:${index}", order = 5, defaultValue = "mqtt-simulator")
        public void setPassword(String password) {
            super.setPassword(password);
        }


    }

}