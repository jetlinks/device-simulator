package org.jetlinks.simulator.cmd.benchmark;

import org.jetlinks.simulator.core.Connection;
import org.jetlinks.simulator.core.Global;
import org.jetlinks.simulator.core.benchmark.ConnectCreateContext;
import org.jetlinks.simulator.core.network.mqtt.MqttClient;
import org.jetlinks.simulator.core.network.mqtt.MqttOptions;
import org.jetlinks.simulator.core.network.tcp.TcpClient;
import org.jetlinks.simulator.core.network.tcp.TcpOptions;
import picocli.CommandLine;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;

@CommandLine.Command(name = "tcp",
        showDefaultValues = true,
        description = {
                "tcp 压力测试,用于批量创建tcp连接",
                "id 支持使用表达式动态生成,如: mqtt-test-\\${index\\}"
        },
        headerHeading = "%n", sortOptions = false)
class TcpBenchMark extends AbstractBenchmarkCommand implements Runnable {

    @CommandLine.Mixin
    TCPCommandOptions command;

    @Override
    protected Mono<? extends Connection> createConnection(ConnectCreateContext ctx) {
        TcpOptions commandOptions = command.refactor(Collections.singletonMap("index", ctx.index()));
        ctx.beforeConnect(commandOptions);
        return TcpClient
                .connect(
                        Global.vertx(),
                        commandOptions
                );
    }


    static class TCPCommandOptions extends TcpOptions {

        @Override
        public String getId() {
            if (super.getId() == null) {
                setId("tcp-client-${index}");
            }
            return super.getId();
        }

        @Override
        @CommandLine.Option(names = {"--id"}, description = "ID", order = 1)
        public void setId(String id) {
            super.setId(id);
        }

        @Override
        @CommandLine.Option(names = {"-h", "--host"}, description = "host", order = 2, defaultValue = "127.0.0.1")
        public void setHost(String host) {
            super.setHost(host);
        }

        @Override
        @CommandLine.Option(names = {"-p", "--port"}, description = "port", order = 3)
        public void setPort(int port) {
            super.setPort(port);
        }

        @Override
        @CommandLine.Option(names = {"--delimited"}, description = "使用分隔符来处理粘拆包", order = 4)
        public void setDelimited(String delimited) {
            super.setDelimited(delimited);
        }

        @Override
        @CommandLine.Option(names = {"--fixedLength"}, description = "使用固定长度来处理粘拆包", order = 5)
        public void setFixedLength(Integer fixedLength) {
            super.setFixedLength(fixedLength);
        }

        @Override
        @CommandLine.Option(names = {"--lengthField"},
                description = "使用某个字段作为长度来处理粘拆包,如: \"0,4\"标识从0字节开始的4字节作为接下来的报文长度",
                split = ",", order = 5)
        public void setLengthField(Integer[] lengthField) {
            super.setLengthField(lengthField);
        }
    }

}