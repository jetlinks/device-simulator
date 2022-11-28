package org.jetlinks.simulator.cmd;


import lombok.extern.slf4j.Slf4j;
import org.jetlinks.simulator.core.ExceptionUtils;
import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.utils.*;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.EnumSet;
import java.util.function.Consumer;

import static org.jline.utils.InfoCmp.Capability.keypad_local;

@Slf4j
public abstract class FullScreenCommand extends CommonCommand {

    protected static final AttributedStyle green = AttributedStyle.BOLD.foreground(AttributedStyle.GREEN);
    protected static final AttributedStyle blue = AttributedStyle.BOLD.foreground(AttributedStyle.BLUE);
    protected static final AttributedStyle red = AttributedStyle.BOLD.foreground(AttributedStyle.RED);


    protected Display display;
    private KeyMap<Operation> keys;
    protected BindingReader bindingReader;
    protected Terminal terminal;

    protected final Size size = new Size();
    protected Disposable disposable;

    @Override
    public final void run() {
        terminal = main().getTerminal();
        bindingReader = new BindingReader(terminal.reader());
        keys = new KeyMap<>();
        bindKey();
        display = new Display(terminal, true);
        int delay = 500;

        init();

        Terminal.SignalHandler prevHandler = terminal.handle(Terminal.Signal.WINCH, this::handle);
        Attributes attr = terminal.getAttributes();

        Throwable[] error = new Throwable[1];

        try {
            Attributes newAttr = new Attributes(attr);
//            newAttr.setLocalFlags(EnumSet.of(Attributes.LocalFlag.ICANON, Attributes.LocalFlag.ECHO, Attributes.LocalFlag.IEXTEN), false);
//            newAttr.setInputFlags(EnumSet.of(Attributes.InputFlag.IXON, Attributes.InputFlag.ICRNL, Attributes.InputFlag.INLCR), false);
//            newAttr.setControlChar(Attributes.ControlChar.VMIN, 0);
//            newAttr.setControlChar(Attributes.ControlChar.VTIME, 1);
            newAttr.setLocalFlags(EnumSet.of(Attributes.LocalFlag.ICANON, Attributes.LocalFlag.ECHO, Attributes.LocalFlag.IEXTEN), false);
            newAttr.setInputFlags(EnumSet.of(Attributes.InputFlag.IXON, Attributes.InputFlag.ICRNL, Attributes.InputFlag.INLCR), false);
            newAttr.setControlChar(Attributes.ControlChar.VMIN, 0);
            newAttr.setControlChar(Attributes.ControlChar.VTIME, 1);
            newAttr.setControlChar(Attributes.ControlChar.VINTR, 0);
            terminal.setAttributes(newAttr);

            if (!terminal.puts(InfoCmp.Capability.enter_ca_mode)) {
                terminal.puts(InfoCmp.Capability.clear_screen);
            }
            terminal.puts(InfoCmp.Capability.keypad_xmit);

            terminal.puts(InfoCmp.Capability.cursor_invisible);


            size.copy(terminal.getSize());
            display.resize(size.getRows(), size.getColumns());

            //定时刷新数据
            disposable = Flux
                    .interval(Duration.ofMillis(delay), Schedulers.boundedElastic())
                    .onBackpressureDrop()
                    .limitRate(1)
                    .doOnNext(ignore -> {
                        try {
                            Size tSize = terminal.getSize();
                            if (tSize.getRows() > 0 && tSize.getColumns() > 0) {
                                if (!tSize.equals(size)) {
                                    size.copy(tSize);
                                    display.resize(size.getRows(), size.getColumns());
                                    display.clear();
                                }
                            }
                            display();
                        } catch (Throwable err) {
                            error[0] = err;
                        }
                    })
                    .subscribe();

            display();
            do {
                checkInterrupted();
                Operation op = bindingReader.readBinding(keys);
                if (op == null) {
                    continue;
                }
                if (op == DefaultOperation.EXIT) {
                    break;
                }
                if (op == DefaultOperation.CLEAR) {
                    display.clear();
                    break;
                }
                if (!doOn(op)) {
                    break;
                }
                display();
            } while (error[0] == null);
        } catch (InterruptedException ie) {
            // Do nothing
        } catch (Throwable err) {
            error[0] = err;
        } finally {
            disposable.dispose();
            destroy();

            display.clear();

            terminal.setAttributes(attr);
            if (prevHandler != null) {
                terminal.handle(Terminal.Signal.WINCH, prevHandler);
            }
            // Use main buffer
            if (!terminal.puts(InfoCmp.Capability.exit_ca_mode)) {
                terminal.puts(InfoCmp.Capability.clear_screen);
            }
            terminal.puts(keypad_local);
            terminal.puts(InfoCmp.Capability.cursor_visible);
            // terminal.puts(cursor_address, cursor.getY(), cursor.getX());
            terminal.flush();
            if (null != error[0]) {
                log.error("", ExceptionUtils.tryGetRealError(error[0]));
            }

        }

    }

    protected boolean doOn(Operation operation) {
        return true;
    }

    protected void bindDefault(KeyMap<Operation> keys) {
        keys.bind(DefaultOperation.EXIT, "q", ":q", "Q", ":Q", "ZZ");
        keys.bind(DefaultOperation.CLEAR, KeyMap.ctrl('L'));
    }

    private KeyMap<Operation> getKeyMap() {
        return keys;
    }


    protected interface Operation {

    }

    public enum DefaultOperation implements Operation {
        EXIT, CLEAR
    }

    private void handle(Terminal.Signal signal) {
        int prevw = size.getColumns();
        size.copy(terminal.getSize());
        try {
            if (size.getColumns() < prevw) {
                display.clear();
            }
            display();
        } catch (Throwable e) {
            // ignore
        }
    }

    protected void init() {

    }

    protected void destroy() {

    }

    protected abstract boolean display();

    static final ThreadLocal<AttributedStringBuilder> LINE_BUILDER = ThreadLocal.withInitial(AttributedStringBuilder::new);

    public static AttributedString createLine(Consumer<AttributedStringBuilder> consumer) {
        AttributedStringBuilder builder = LINE_BUILDER.get();
        try {
            consumer.accept(builder);
            return builder.toAttributedString();
        } finally {
            builder.setLength(0);
        }
    }

    protected void bindKey() {
        bindDefault(keys);
    }


    private void checkInterrupted() throws InterruptedException {
        Thread.yield();
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }
    }


    static String[] formats = {"B", "KB", "MB", "GB"};

    protected static String formatBytes(long bytes) {
        int i = 0;
        float total = bytes;
        while (total >= 1024 && i < formats.length - 1) {
            total /= 1024;
            i++;
        }

        return String.format("%.2f%s", total, formats[i]);
    }
}
