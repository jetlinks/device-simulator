package org.jetlinks.simulator.cmd.http;

import org.jetlinks.simulator.cmd.CommonCommand;
import picocli.CommandLine;

@CommandLine.Command(name = "http",
        description = "HTTP client operations",
        subcommands = {
                CreateHttpCommand.class,
                HTTPAttachCommand.class
        })
public class HTTPOperationCommand extends CommonCommand implements Runnable {


    @Override
    public void run() {
        showHelp();
    }
}
