package org.jetlinks.simulator.cmd.coap;

import org.jetlinks.simulator.cmd.CommonCommand;
import picocli.CommandLine;

@CommandLine.Command(name = "coap-tcp",
        description = "Coap Tcp client operations",
        subcommands = {
                CreateCoapTcpCommand.class,
                CoapTcpAttachCommand.class
        })
public class CoapTcpOperationCommand extends CommonCommand implements Runnable {


    @Override
    public void run() {
        showHelp();
    }
}
