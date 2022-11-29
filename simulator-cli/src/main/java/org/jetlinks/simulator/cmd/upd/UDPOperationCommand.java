package org.jetlinks.simulator.cmd.upd;

import org.jetlinks.simulator.cmd.CommonCommand;
import picocli.CommandLine;

@CommandLine.Command(name = "udp",
        description = "UDP client operations",
        subcommands = {
                CreateUDPCommand.class,
                UDPAttachCommand.class
        })
public class UDPOperationCommand extends CommonCommand implements Runnable {


    @Override
    public void run() {
        showHelp();
    }
}
