package org.jetlinks.simulator.cmd.coap;

import org.jetlinks.simulator.cmd.AbstractCommand;
import org.jetlinks.simulator.cmd.NetworkInterfaceCompleter;
import org.jetlinks.simulator.core.ExceptionUtils;
import org.jetlinks.simulator.core.network.coap.CoapClient;
import org.jetlinks.simulator.core.network.coap.CoapOptions;
import picocli.CommandLine;

import java.net.URI;
import java.util.Map;

@CommandLine.Command(name = "create",
        showDefaultValues = true,
        description = "Create Coap Tcp Client",
        headerHeading = "%n", sortOptions = false)
public class CreateCoapTcpCommand extends AbstractCommand implements Runnable {


    @CommandLine.Mixin
    private Options options;

    @Override
    public void run() {

        printf("create coap tcp client %s", options.getId());
        try {
            CoapClient client = new CoapClient(options);
            main().connectionManager().addConnection(client);
            printf(" success!%n");
            main()
                    .getCommandLine()
                    .execute("coap-tcp", "attach", client.getId());
        } catch (Throwable err) {
            printfError(" error: %s %n", ExceptionUtils.getErrorMessage(err));
        }
    }


    static class Options extends CoapOptions {

        @Override
        @CommandLine.Option(names = {"--id"}, description = "Client ID", defaultValue = "coap-tcp-client")
        public void setId(String id) {
            super.setId(id);
        }

        @CommandLine.Option(names = {"-o", "--option"}, description = "Options")
        public void setHeader(Map<String, String> options) {
            setOptions(options);
        }

        @CommandLine.Option(names = {"--interface"}, paramLabel = "interface", description = "Network Interface", order = 40, completionCandidates = NetworkInterfaceCompleter.class)
        public void setLocalAddress0(String localAddress) {
            super.setBindAddress(localAddress);
        }


        @CommandLine.Parameters(paramLabel = "URL", description = "URL,start with http or https")
        public void setBasePath0(URI basePath) {
            super.setBasePath(basePath.toString());
        }


    }
}
