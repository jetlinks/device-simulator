package org.jetlinks.simulator.cmd;

import picocli.CommandLine;

import java.io.File;

@CommandLine.Command(name = "exec", description = "Execute JavaScript File", headerHeading = "%n")
class ExecuteScriptCommand extends CommonCommand implements Runnable {

    @CommandLine.Option(names = {"-f", "--file"},
            descriptionKey = "exec.file",
            description = "javascript file.", required = true)
    File file;

    @Override
    public void run() {

    }
}