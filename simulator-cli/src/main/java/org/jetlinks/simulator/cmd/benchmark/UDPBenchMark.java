package org.jetlinks.simulator.cmd.benchmark;

import org.jetlinks.simulator.core.Connection;
import org.jetlinks.simulator.core.Global;
import org.jetlinks.simulator.core.benchmark.ConnectCreateContext;
import org.jetlinks.simulator.core.network.tcp.TcpClient;
import org.jetlinks.simulator.core.network.tcp.TcpOptions;
import org.jetlinks.simulator.core.network.udp.UDPClient;
import org.jetlinks.simulator.core.network.udp.UDPOptions;
import picocli.CommandLine;
import reactor.core.publisher.Mono;

import java.util.Collections;

@CommandLine.Command(name = "udp",
        showDefaultValues = true,
        description = {
                "Create UDP Benchmark"
        },
        headerHeading = "%n", sortOptions = false)
class UDPBenchMark extends AbstractBenchmarkCommand implements Runnable {

    @CommandLine.Mixin
    CommandOptions command;

    @Override
    protected Mono<? extends Connection> createConnection(ConnectCreateContext ctx) {
        UDPOptions commandOptions = command.refactor(Collections.singletonMap("index", ctx.index()));
        ctx.beforeConnect(commandOptions);
        return UDPClient.create(commandOptions);
    }


    static class CommandOptions extends UDPOptions {

        @Override
        @CommandLine.Option(names = {"--id"}, description = "ID", defaultValue = "udp-client-{index}", order = 1)
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

    }

}