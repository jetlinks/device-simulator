package org.jetlinks.simulator.cli;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

import lombok.SneakyThrows;
import org.jline.console.SystemRegistry;
import org.jline.console.impl.Builtins;
import org.jline.console.impl.SystemRegistryImpl;
import org.jline.reader.*;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.Display;
import org.jline.utils.InfoCmp;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.Appender;
import picocli.CommandLine;
import picocli.CommandLine.Help;
import picocli.shell.jline3.PicocliCommands;

public class InteractiveCLI {

    private final LineReader console;
    private final CommandLine commandLine;
    private final SystemRegistry systemRegistry;

    public InteractiveCLI(JLineInteractiveCommands commands) throws Exception {

        Terminal terminal = TerminalBuilder
                .builder()
                .build();

        PicocliCommands.PicocliCommandsFactory factory = new PicocliCommands.PicocliCommandsFactory();
        factory.setTerminal(terminal);

        Parser parser = new DefaultParser();

        Supplier<Path> workDir = () -> Paths.get("./");

        commandLine = new CommandLine(commands, factory);

        PicocliCommands picocliCommands = new PicocliCommands(commandLine){
            @Override
            public String name() {
                return "JetLinks Simulator";
            }
        };

        Builtins builtins = new Builtins(workDir, null, null);
        builtins.rename(Builtins.Command.TTOP, "top");

        systemRegistry = new SystemRegistryImpl(parser, terminal, workDir, null);
        systemRegistry.setCommandRegistries(builtins, picocliCommands);

        systemRegistry.register("help", picocliCommands);

        // Create Interactive Shell
        console = LineReaderBuilder
                .builder()
                .terminal(terminal)
                .completer(systemRegistry.completer())
                .parser(parser)
                .variable(LineReader.HISTORY_FILE, "simulator-cli-history")
                .variable(LineReader.HISTORY_IGNORE,"help,cls,clear,exit")
                .variable(LineReader.LIST_MAX, 50)   // max tab completion candidates
                .build();
        // set up the completion
        commands.setConsole(console);
        commands.setCommandLine(commandLine);
        commands.setTerminal(terminal);

        // Configure Terminal appender if it is present.
        Appender<?> appender = ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME))
                .getAppender("TERMINAL");
        if (appender instanceof TerminalAppender<?>) {
            ((TerminalAppender<?>) appender).setConsole(console);
        }
    }

    @SneakyThrows
    public void showHelp() {
        systemRegistry.execute("help");
    }

    @SneakyThrows
    public void start() throws IOException {
        // start the shell and process input until the user quits with Ctl-D
        String line;
        while (true) {
            try {
                systemRegistry.cleanUp();
                line = console.readLine(">", null, (MaskingCallback) null, null);
                systemRegistry.execute(line);
            } catch (UserInterruptException e) {
                // Ignore
            } catch (EndOfFileException|InterruptedException e) {
                return;
            } catch (Exception e) {
                systemRegistry.trace(e);
            }
        }


    }
}