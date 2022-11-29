package org.jetlinks.simulator.cli;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import ch.qos.logback.core.ConsoleAppender;
import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;
import reactor.core.Disposable;

/**
 * A logback Console appender compatible with a Jline 2 Console reader.
 */
public class TerminalAppender<E> extends ConsoleAppender<E> {

    private LineReader console;

    private static final List<Consumer<String>> logConsumer = new CopyOnWriteArrayList<>();

    public static Disposable listenLog(Consumer<String> consumer) {
        logConsumer.add(consumer);
        return () -> logConsumer.remove(consumer);
    }

    @Override
    protected void subAppend(E event) {
        if (console == null)
            super.subAppend(event);
        else {
//            // stash prompt
//            String stashed =console.getBuffer().copy().toString();

            if (logConsumer.isEmpty()) {
                console.printAbove(new String(getEncoder().encode(event)));
            } else {
                String log = new String(getEncoder().encode(event));
                for (Consumer<String> stringConsumer : logConsumer) {
                    stringConsumer.accept(log);
                }
            }

        }
    }

    public void setConsole(LineReader console) {
        this.console = console;
    }
}
