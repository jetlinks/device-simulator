package org.jetlinks.simulator.cmd.tcp;

import org.jetlinks.simulator.cmd.CommonCommand;
import picocli.CommandLine;

@CommandLine.Command(name = "tcp",
        description = "tcp相关操作",
        subcommands = {
                TcpSendCommand.class,
                ConnectTcpCommand.class
        })
public class TcpOperationCommand extends CommonCommand implements Runnable {


    @Override
    public void run() {
        showHelp();
    }
}
