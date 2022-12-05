package org.jetlinks.simulator.cmd.tcp;

import org.jetlinks.simulator.cmd.CommonCommand;
import picocli.CommandLine;

@CommandLine.Command(name = "tcp",
        description = "tcp client operations",
        subcommands = {
                TcpSendCommand.class,
                ConnectTcpCommand.class,
                TcpAttachCommand.class
        })
public class TcpOperationCommand extends CommonCommand implements Runnable {


    @Override
    public void run() {
        showHelp();
    }
}
