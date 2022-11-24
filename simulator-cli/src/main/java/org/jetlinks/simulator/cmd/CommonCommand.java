package org.jetlinks.simulator.cmd;

import picocli.CommandLine;


@CommandLine.Command(name = "CommonCommand", hidden = true)
public class CommonCommand extends AbstractCommand implements Runnable{



    @Override
    public void run() {
        showHelp();
    }
}