package org.jetlinks.simulator;

import lombok.SneakyThrows;
import org.jetlinks.simulator.cli.InteractiveCLI;
import org.jetlinks.simulator.cli.SimulatorCli;
import org.jetlinks.simulator.cmd.SimulatorCommands;
import picocli.CommandLine;

import java.io.PrintWriter;

public class Main {

    @SneakyThrows
    public static void main(String[] args) {

        SimulatorCli cli = new SimulatorCli();

        CommandLine command = new CommandLine(cli);
        // Handle exit code error
        int exitCode = command.execute(args);
        if (exitCode != 0)
            System.exit(exitCode);
        // Handle help or version command
        if (command.isUsageHelpRequested() || command.isVersionHelpRequested())
            System.exit(0);

        try {
            // Create Client
            // Print commands help
            SimulatorCommands commands = new SimulatorCommands();

            InteractiveCLI console = new InteractiveCLI(commands);
            console.showHelp();
            // Start interactive console
            console.start();
            commands.dispose();
            System.exit(1);
        } catch (Exception e) {

            // Handler Execution Error
            PrintWriter printer = command.getErr();
            printer.print(command.getColorScheme().errorText("Unable to create and start simulator ..."));
            printer.printf("%n%n");
            printer.print(command.getColorScheme().stackTraceText(e));
            printer.flush();
            System.exit(1);
        }
    }
}
