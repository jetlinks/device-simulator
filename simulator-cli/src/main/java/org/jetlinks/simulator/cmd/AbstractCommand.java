package org.jetlinks.simulator.cmd;

import lombok.Getter;
import org.jetlinks.simulator.cli.StandardHelpOptions;
import picocli.CommandLine;

public abstract class AbstractCommand {

    @CommandLine.ParentCommand
    @Getter
    protected Object parent;

    @CommandLine.Mixin
    StandardHelpOptions options = new StandardHelpOptions();

    @CommandLine.Spec
    protected CommandLine.Model.CommandSpec spec;

    protected void setParent(Object parent) {
        this.parent = parent;
    }

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
        if (spec != null) {

            main().getCommandLine()
                  .execute(spec.name(), "--help");
        }

    }

    protected void printf(String template, Object... args) {
        if (parent instanceof AbstractCommand) {
            ((AbstractCommand) parent).printf(template, args);
            return;
        }
        main().printf(template, args).flush();
    }

    protected void printfError(String template, Object... args) {
        if (parent instanceof AbstractCommand) {
            ((AbstractCommand) parent).printfError(template, args);
            return;
        }
        main().printfError(template, args).flush();
    }
}

