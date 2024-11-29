package org.jetlinks.simulator.cmd.benchmark;

import org.jetlinks.simulator.cmd.NetworkInterfaceCompleter;
import org.jetlinks.simulator.core.Connection;
import org.jetlinks.simulator.core.benchmark.ConnectCreateContext;
import org.jetlinks.simulator.core.network.coap.CoapOptions;
import org.jetlinks.simulator.core.network.coap.CoapTcpClient;
import picocli.CommandLine;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Collections;
import java.util.Map;

@CommandLine.Command(name = "coapTcp",
        showDefaultValues = true,
        description = {
                "Create CoAP TCP Benchmark"
        },
        headerHeading = "%n", sortOptions = false)
class CoAPTcpBenchMark extends AbstractBenchmarkCommand implements Runnable {

    @CommandLine.Mixin
    CommandOptions command;

    @Override
    protected Mono<? extends Connection> createConnection(ConnectCreateContext ctx) {
        CoapOptions commandOptions = command.refactor(Collections.singletonMap("index", ctx.index()));
        ctx.beforeConnect(commandOptions);
        return CoapTcpClient.create(commandOptions);
    }

    static class CommandOptions extends CoapOptions {

        @Override
        @CommandLine.Option(names = {"--id"}, description = "ID", defaultValue = "coap-tcp-client-{index}", order = 1)
        public void setId(String id) {
            super.setId(id);
        }

        @CommandLine.Option(names = {"-o", "--option"}, description = "Default Options", order = 2)
        public void setHeader(Map<String, String> options) {
            setOptions(options);
        }

        @CommandLine.Option(names = {"-u", "--url"}, description = "URL,start with coap or coaps", order = 3)
        public void setBasePath(URI basePath) {
            super.setBasePath(basePath.toString());
        }


        @CommandLine.Option(names = {"--interface"}, paramLabel = "interface", description = "Network Interface", order = 7, completionCandidates = NetworkInterfaceCompleter.class)
        public void setLocalAddress0(String localAddress) {
            super.setBindAddress(localAddress);
        }

    }

}