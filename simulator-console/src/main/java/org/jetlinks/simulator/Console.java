package org.jetlinks.simulator;

import lombok.SneakyThrows;
import org.jline.builtins.ConsoleEngineImpl;
import org.jline.builtins.Nano;
import org.jline.builtins.SystemRegistryImpl;
import org.jline.builtins.Widgets;
import org.jline.builtins.ssh.Ssh;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

public class Console {

    @SneakyThrows
    public static void main(String[] args) {

        FileOutputStream fileOutputStream = new FileOutputStream(new File("./target/temp.txt"));
        Terminal terminal = TerminalBuilder
                .builder()
                .streams(System.in, new OutputStream() {

                    @Override
                    public void write(byte[] b, int off, int len) throws IOException {
                        System.out.write(b, off, len);
                        fileOutputStream.write(Arrays.toString(b).getBytes());
                    }

                    @Override
                    public void write(int b) throws IOException {
                        System.out.write(b);
                        fileOutputStream.write(b);
                    }

                    @Override
                    public void flush() throws IOException {
                        System.out.flush();
                        fileOutputStream.flush();
                    }
                })
//                .system(true)
                .dumb(false)
                .name("Jetlinks Device Simulator")
                .build();
        Completer commandCompleter = new StringsCompleter("CREATE", "OPEN", "WRITE", "CLOSE");

        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(commandCompleter)
                .history(new DefaultHistory())
                .parser(new DefaultParser().eofOnEscapedNewLine(true))
                .variable(LineReader.SECONDARY_PROMPT_PATTERN, "%M%P > ")
                .variable(LineReader.INDENTATION, 2)
                .build();
        try {
            while (true) {
                String line = reader.readLine(">");

                System.out.println(line);
//                new Nano(terminal, new File("/Users/zhouhao/IdeaProjects/device-simulator/simulator-core/src/test/resources/simulator.js"))
//                        .run();

            }
        } finally {
            fileOutputStream.flush();
            fileOutputStream.close();
        }


    }
}
