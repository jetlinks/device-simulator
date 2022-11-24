package org.jetlinks.simulator.cli;

import picocli.CommandLine;

public class StandardHelpOptions {

    @CommandLine.Option(names = {"--help"}, description = "Display help information.", usageHelp = true)
    private boolean help;

}