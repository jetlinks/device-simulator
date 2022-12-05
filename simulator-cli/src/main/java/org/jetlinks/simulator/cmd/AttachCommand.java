package org.jetlinks.simulator.cmd;

import lombok.SneakyThrows;
import org.jetlinks.simulator.cli.TerminalAppender;
import org.jetlinks.simulator.history.CommandHistory;
import org.jline.builtins.Completers;
import org.jline.keymap.KeyMap;
import org.jline.reader.*;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.LineReaderImpl;
import org.jline.reader.impl.completer.AggregateCompleter;
import org.jline.terminal.MouseEvent;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.jline.utils.InfoCmp;
import org.springframework.util.StringUtils;
import picocli.CommandLine;
import picocli.shell.jline3.PicocliJLineCompleter;
import reactor.core.Disposable;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

import static org.jline.keymap.KeyMap.ctrl;
import static org.jline.keymap.KeyMap.key;

public class AttachCommand extends FullScreenCommand {

    protected static final AttributedStyle helpBg = AttributedStyle.BOLD
            .background(0x57, 0xC2, 0xC5).foreground(AttributedStyle.BLACK);

    protected static final AttributedStyle cursorStyle = AttributedStyle.BOLD
            .background(AttributedStyle.WHITE).foreground(AttributedStyle.BLACK);

    static Parser parser = new DefaultParser();
    protected StringBuilder editBuffer;

    private int curPos;

    private EditorOperation current;

    private int curIndex = 0;
    private boolean paused = false;

    private MouseEvent mouseEvent;

    private boolean windowsTerminal;

    private LinkedList<AttributedString> lastLines;

    private final Deque<AttributedString> errors = new ConcurrentLinkedDeque<>();

    private final List<AttributedString> footers = new ArrayList<>();

    private final StringWriter error = new StringWriter();
    private final StringWriter info = new StringWriter();

    private Disposable logDispose;

    @Override
    protected void destroy() {
        super.destroy();
        logDispose.dispose();
        errors.clear();
        logDispose = null;
        lastLines = null;
        try {
            history().save();
        } catch (IOException ignore) {

        }
    }

    @Override
    protected final void printf(String template, Object... args) {
        info.append(String.format(template, args));
    }

    @Override
    protected final void printfError(String template, Object... args) {
        error.append(createLine(b -> b.append(String.format(template, args), red)).toString());
    }

    @Override
    protected void init() {
        logDispose = TerminalAppender.listenLog(log -> {

            for (String s : log.split("\n")) {
                errors.add(AttributedString.fromAnsi(s));
                if (errors.size() > 100) {
                    errors.removeFirst();
                }
            }

        });
        this.windowsTerminal = terminal.getClass().getSimpleName().endsWith("WinSysTerminal");
        editBuffer = new StringBuilder();

        error.getBuffer().setLength(0);
        info.getBuffer().setLength(0);

        footers.clear();
        curPos = 0;
        current = null;
        curIndex = 0;
        paused = false;
        terminal.trackMouse(Terminal.MouseTracking.Button);
    }


    protected void createHeader(List<AttributedString> lines) {

    }

    protected void createBody(List<AttributedString> lines) {


    }

    protected void createFooter(List<AttributedString> lines) {
        lines.addAll(footers);

        if (info.getBuffer().length() > 0) {
            for (String line : info.toString().split("\n")) {
                lines.add(AttributedString.fromAnsi(line));
            }
        }
        if (error.getBuffer().length() > 0) {
            for (String line : error.toString().split("\n")) {
                lines.add(AttributedString.fromAnsi(line));
            }
        }
    }

    protected AbstractCommand createCommand() {
        return null;
    }

    protected String inputHelp() {
        return "Command:";
    }

    private long lastShowTime = System.currentTimeMillis();


    private LinkedList<AttributedString> prepare(LinkedList<AttributedString> list) {
        int width = terminal.getWidth();

        LinkedList<AttributedString> newArr = new LinkedList<>();

        for (AttributedString str : list) {
            int total = str.columnLength();
            if (total <= width) {
                newArr.add(str);
                continue;
            }
            int offset = 0;

            //自动换行
            while (offset < total) {
                newArr.add(str.columnSubSequence(offset, width + offset));
                offset += width;
            }

        }
        return newArr;
    }

    @Override
    protected synchronized final boolean display() {
        if (paused || disposable == null || disposable.isDisposed()) {
            return true;
        }
        LinkedList<AttributedString> header = new LinkedList<>();
        LinkedList<AttributedString> body = new LinkedList<>(this.errors);
        LinkedList<AttributedString> footer = new LinkedList<>();

        createHeader(header);
        createBody(body);

        AttributedString input = createLine(builder -> {
            builder.append(inputHelp(), helpBg);

            if (!showCursor()) {
                builder.append(editBuffer.toString());
                return;
            }
            //模拟光标
            if (curPos == 0 && editBuffer.length() > 0) {
                builder.append(editBuffer.substring(0, 1), cursorStyle);
                if (editBuffer.length() > 1) {
                    builder.append(editBuffer.substring(1, editBuffer.length()));
                }
            } else if (curPos < editBuffer.length()) {
                builder.append(editBuffer.substring(0, curPos));
                builder.append(editBuffer.substring(curPos, curPos + 1), cursorStyle);
                builder.append(editBuffer.substring(curPos + 1, editBuffer.length()));
            } else {
                builder.append(editBuffer.toString())
                       .append(" ", cursorStyle);
            }
        });
        footer.add(input);

        createFooter(footer);

        while (footer.size() < 3) {
            footer.add(createLine(builder -> builder.append("")));
        }

        header = prepare(header);
        body = prepare(body);
        footer = prepare(footer);

        int bodyAliveHeight = terminal.getHeight() - footer.size() - header.size();

        while (body.size() != bodyAliveHeight) {
            if (body.size() > bodyAliveHeight) {
                if (body.size() > 0) {
                    body.removeFirst();
                } else {
                    break;
                }
            } else {
                body.add(AttributedString.EMPTY);
            }
        }

        header.addAll(body);
        header.addAll(footer);

        if (needFlush(header)) {
            display.clear();
        }
        lastLines = header;
        display.update(header, 0, true);

        return true;
    }

    protected boolean showCursor() {
        return true;
//        if (windowsTerminal) {
//            return true;
//        }
//        if (System.currentTimeMillis() - lastShowTime >= 500) {
//            lastShowTime = System.currentTimeMillis();
//            return true;
//        }
//        return false;
    }

    protected boolean needFlush(LinkedList<AttributedString> lines) {
        List<AttributedString> temp = this.lastLines;
        this.lastLines = lines;
        return windowsTerminal && !Objects.equals(lines, temp);
    }

    @Override
    protected void bindDefault(KeyMap<Operation> keys) {
        keys.bind(DefaultOperation.EXIT, KeyMap.ctrl('q'));

        keys.bind(DefaultOperation.CLEAR, KeyMap.ctrl('c'));

        keys.setUnicode(EditorOperation.INSERT);
        for (char i = 32; i < 256; i++) {
            keys.bind(EditorOperation.INSERT, Character.toString(i));
        }

        keys.bind(EditorOperation.TAP, key(terminal, InfoCmp.Capability.tab));

        keys.bind(EditorOperation.EXECUTE, "\r", key(terminal, InfoCmp.Capability.key_enter));

        keys.bind(EditorOperation.RIGHT, key(terminal, InfoCmp.Capability.key_right), ctrl('b'));
        keys.bind(EditorOperation.LEFT, key(terminal, InfoCmp.Capability.key_left), ctrl('f'));

        keys.bind(EditorOperation.UP, key(terminal, InfoCmp.Capability.key_up));
        keys.bind(EditorOperation.DOWN, key(terminal, InfoCmp.Capability.key_down));

        keys.bind(EditorOperation.FIRST, KeyMap.ctrl('a'));
        keys.bind(EditorOperation.LAST, KeyMap.ctrl('e'));


        keys.bind(EditorOperation.BACKSPACE, ctrl('h'), KeyMap.del(), key(terminal, InfoCmp.Capability.delete_character));

        keys.bind(EditorOperation.MOUSE_EVENT, key(terminal, InfoCmp.Capability.key_mouse));


    }

    @Override
    protected final boolean doOn(Operation operation) {
        super.doOn(operation);
        paused = false;
        mouseEvent = null;
        if (operation instanceof EditorOperation) {
            EditorOperation opt = ((EditorOperation) operation);
            switch (opt) {
                case EXECUTE:
                    return execute();
                case TAP:
                    complete();
                    break;
                case UP:
                    history().previous();
                    showHistory();
                    break;
                case DOWN:
                    history().next();
                    showHistory();
                    break;
                case MOUSE_EVENT:
                    // paused = true;
                    mouseEvent = terminal.readMouseEvent();

                    break;
                default:
                    curPos = editInputBuffer(opt, curPos);
                    break;
            }
        }

        return true;

    }

    private void showHistory() {
        String history = history().current();
        if (StringUtils.hasText(history)) {
            editBuffer.setLength(0);
            editBuffer.append(history);
            curPos = editBuffer.length();
        }
    }

    private boolean execute() {
        String command = editBuffer.toString().trim();
        switch (command) {
            case "q:":
            case "exit":
                return false;
            case "?":
            case "":
            case "help":
                printf(commandLine().getUsageMessage());
                break;
            case "clear":
                doClear();
                clearFooter();
                display.clear();
                break;
            default:
                if (executeCommand(command)) {
                    history().add(command);
                    break;
                }
                return true;
        }

        editBuffer.setLength(0);
        curPos = 0;
        return true;
    }

    protected History history() {
        return CommandHistory.getHistory(spec.qualifiedName("_"));
    }

    protected void doClear() {
        errors.clear();
    }


    private void clearFooter() {
        footers.clear();
        error.getBuffer().setLength(0);
        info.getBuffer().setLength(0);
    }

    @SneakyThrows
    private void complete() {
        clearFooter();

        String buffer = editBuffer.toString();
        CommandLine commandLine = commandLine();

        if (null != commandLine) {

//            PicocliCommands commands = new PicocliCommands(commandLine);

            List<Candidate> candidates = new ArrayList<>();

//            SystemCompleter completer = commands.compileCompleters();
//            completer.compile();

            ParsedLine line = parser.parse(buffer, curPos);

            new AggregateCompleter(new PicocliJLineCompleter(commandLine.getCommandSpec()),
                                   new Completers.FileNameCompleter())
                    .complete(new LineReaderImpl(terminal), line, candidates);

            String word = line.word();

            candidates = candidates
                    .stream()
                    .filter(candidate -> matchComplete(candidate, word))
                    .collect(Collectors.toList());

            if (candidates.size() == 1) {
                List<String> w = new ArrayList<>(line.words());

                String candidate = candidates.get(0).value();

                if (candidate.startsWith(word)) {
                    w.set(w.size() - 1, candidate);
                } else {
                    String[] arr = word.split("=");
                    if (arr.length == 2) {
                        arr[1] = candidate;
                        w.set(w.size() - 1, String.join("=", arr));
                    }
                }

                String val = String.join(" ", w);

                editBuffer.replace(0, curPos, val);

                curPos = val.length();
            } else if (candidates.size() > 1) {

                showComplete(candidates);
            }
        }
    }

    private boolean matchComplete(Candidate candidate, String word) {
        String[] arg = word.split("=");
        if (arg.length == 2) {
            word = arg[1];
        }
        return candidate.value().contains(word);
    }

    protected void showComplete(List<Candidate> candidates) {


        createLine(builder -> {
            for (Candidate candidate : candidates) {
                String word = candidate.value();

                if (builder.columnLength() >= terminal.getWidth() - word.length()) {
                    footers.add(builder.toAttributedString());
                    builder.setLength(0);
                }
                builder.append(candidate.value(), blue)
                       .append("\t");
            }

            if (builder.length() > 0) {
                footers.add(builder.toAttributedString());
            }
        });


    }

    protected CommandLine commandLine() {
        AbstractCommand commands = createCommand();
        if (commands == null) {
            return null;
        }
        commands.setParent(this);

        CommandLine commandLine = new CommandLine(commands);
        commandLine.setErr(new PrintWriter(error));
        commandLine.setOut(new PrintWriter(info));

        return commandLine;
    }

    protected final boolean executeCommand(String command) {
        clearFooter();

        CommandLine commandLine = commandLine();
        if (commandLine == null) {
            footers.add(AttributedString.fromAnsi("unsupported command:" + command));
            return false;
        }
        ParsedLine line = parser.parse(command, command.length());

        int state = commandLine.execute(line.words().toArray(new String[0]));


        return error.getBuffer().length() == 0;
    }

    private int editInputBuffer(EditorOperation operation, int curPos) {
        switch (operation) {
            case INSERT:
                editBuffer.insert(curPos++, bindingReader.getLastBinding());
                break;
            case BACKSPACE:
                if (curPos > 0) {
                    editBuffer.deleteCharAt(--curPos);
                }
                break;
            case FIRST:
                curPos = 0;
                break;
            case LAST:
                curPos = editBuffer.length();
                break;
            case LEFT:
                if (curPos > 0) {
                    curPos--;
                }
                break;
            case RIGHT:
                if (curPos < editBuffer.length()) {
                    curPos++;
                }
                break;
        }
        return curPos;
    }


    enum EditorOperation implements Operation {
        INSERT,
        BACKSPACE,
        PRE,
        NEXT,
        FIRST,
        LAST,
        LEFT,
        RIGHT,
        UP,
        DOWN,
        EXECUTE,
        TAP,
        MOUSE_EVENT,
        HELP
    }
}
