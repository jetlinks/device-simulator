package org.jetlinks.simulator.cmd;

import org.jetlinks.simulator.cli.JLineInteractiveCommands;
import org.jetlinks.simulator.cmd.benchmark.BenchmarkCommand;
import org.jetlinks.simulator.cmd.mqtt.MqttOperationCommand;
import org.jetlinks.simulator.cmd.tcp.TcpOperationCommand;
import org.jetlinks.simulator.core.ConnectionManager;
import picocli.CommandLine;
import picocli.shell.jline3.PicocliCommands;
import reactor.core.Disposable;

@CommandLine.Command(name = "",
        description = "@|bold,underline JetLinks Simulator CLI :|@%n",
        footer = {"%n@|italic Enter exit to exit.|@%n"},
        subcommands = {
                CommandLine.HelpCommand.class,
                PicocliCommands.ClearScreen.class,
                ListConnection.class,
                ExecuteScriptCommand.class,
                BenchmarkCommand.class,
                MqttOperationCommand.class,
                TcpOperationCommand.class,
                EditableAttachCommand.class
        },
        customSynopsis = {""},
        synopsisHeading = "")
public class SimulatorCommands extends JLineInteractiveCommands implements Runnable , Disposable {

    final ConnectionManager connectionManager = ConnectionManager.global();


    public ConnectionManager connectionManager() {
        return connectionManager;
    }


    @Override
    public void run() {
        printUsageMessage();
    }


    @Override
    public void dispose() {
        connectionManager.dispose();
    }
}
