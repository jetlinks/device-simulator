package org.jetlinks.simulator.cmd;

import org.jetlinks.simulator.cli.StandardHelpOptions;
import picocli.CommandLine;

public abstract class AbstractCommand {

    @CommandLine.ParentCommand
    protected Object parent;

    @CommandLine.Mixin
    StandardHelpOptions options = new StandardHelpOptions();

    @CommandLine.Spec
    protected  CommandLine.Model.CommandSpec spec;

    public SimulatorCommands main() {
        if (parent instanceof SimulatorCommands) {
            return ((SimulatorCommands) parent);
        }
        if (parent instanceof AbstractCommand) {
            return ((AbstractCommand) parent).main();
        }
        throw new UnsupportedOperationException();
    }

    protected void showHelp() {

        main().getCommandLine()
              .execute(spec.name(), "--help");
    }

    protected void printf(String template, Object... args) {
        main().printf(template, args).flush();
    }

    protected void printfError(String template, Object... args) {
        main().printfError(template, args).flush();
    }
}

