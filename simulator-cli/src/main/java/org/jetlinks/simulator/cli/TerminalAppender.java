package org.jetlinks.simulator.cli;

import java.io.IOException;

import ch.qos.logback.core.ConsoleAppender;
import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;

/**
 * A logback Console appender compatible with a Jline 2 Console reader.
 */
public class TerminalAppender<E> extends ConsoleAppender<E> {

    private LineReader console;

    @Override
    protected void subAppend(E event) {
        if (console == null)
            super.subAppend(event);
        else {
//            // stash prompt
//            String stashed =console.getBuffer().copy().toString();

            console.printAbove(new String(getEncoder().encode(event)));

        }
    }

    public void setConsole(LineReader console) {
        this.console = console;
    }
}
