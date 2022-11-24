package org.jetlinks.simulator.cli;

import java.io.PrintWriter;

import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;
import picocli.CommandLine;

public class JLineInteractiveCommands {

    private PrintWriter out;
    private CommandLine commandLine;

    private Terminal terminal;

    void setConsole(LineReader reader) {
        out = new PrintWriter(reader.getTerminal().writer());
    }

    void setCommandLine(CommandLine commandLine) {
        this.commandLine = commandLine;
    }

    void setTerminal(Terminal terminal) {
        this.terminal = terminal;
    }

    public PrintWriter getConsoleWriter() {
        return out;
    }

    public Terminal getTerminal() {
        return terminal;
    }

    public CommandLine getCommandLine() {
        return commandLine;
    }

    public void printUsageMessage() {
        out.print(commandLine.getUsageMessage());
        out.flush();
    }

    public PrintWriter printf(String format, Object... args) {
        return out.printf(format, args);
    }

    public PrintWriter printfAnsi(String format, Object... args) {
        return out.printf(commandLine.getColorScheme().ansi().string(format), args);
    }

    public PrintWriter printfError(String string, Object... args) {
        out.printf(commandLine.getColorScheme().errorText(string).toString(), args);
        return out;
    }

    public void flush() {
        out.flush();
    }
}