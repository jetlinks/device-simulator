package org.jetlinks.simulator.cmd.coap;

import org.jetlinks.simulator.cmd.CommonCommand;
import picocli.CommandLine;

@CommandLine.Command(name = "coap",
        description = "Coap client operations",
        subcommands = {
                CreateCoapCommand.class,
                CoapAttachCommand.class
        })
public class CoapOperationCommand extends CommonCommand implements Runnable {


    @Override
    public void run() {
        showHelp();
    }
}
