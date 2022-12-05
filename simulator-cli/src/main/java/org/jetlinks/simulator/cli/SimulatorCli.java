package org.jetlinks.simulator.cli;


import picocli.CommandLine;

@CommandLine.Command(name = "jetlinks-simulator-cli",
        sortOptions = false,
        description = "%n"//
                + "@|italic " //
                + "JetLinks 网络模拟器.%n" //
                + "支持模拟MQTT,HTTP,COAP,TCP等协议%n" //
                + "%n" //
                + "|@%n%n")
public class SimulatorCli implements Runnable {

    @CommandLine.Mixin
    private StandardHelpOptions options = new StandardHelpOptions();

    @Override
    public void run() {

    }
}
