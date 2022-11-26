package org.jetlinks.simulator.cmd;

import org.jline.keymap.KeyMap;
import org.jline.terminal.MouseEvent;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.jline.utils.InfoCmp;
import picocli.CommandLine;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static org.jline.keymap.KeyMap.ctrl;
import static org.jline.keymap.KeyMap.key;

@CommandLine.Command(name = "edit", hidden = true)
public class AttachCommand extends FullScreenCommand {

    protected static final AttributedStyle helpBg = AttributedStyle.BOLD
            .background(0x57, 0xC2, 0xC5).foreground(AttributedStyle.BLACK);

    protected static final AttributedStyle cursorStyle = AttributedStyle.BOLD
            .background(AttributedStyle.WHITE).foreground(AttributedStyle.BLACK);

    protected static final AttributedStyle black = AttributedStyle.BOLD.background(AttributedStyle.BLACK);

    protected StringBuilder editBuffer;

    private int curPos;

    private EditorOperation current;

    private int curIndex = 0;
    private boolean paused = false;

    private MouseEvent mouseEvent;

    private List<AttributedString> currentFooter;

    private boolean windowsTerminal;

    private LinkedList<AttributedString> lastLines;

    @Override
    protected void destroy() {
        super.destroy();
        lastLines = null;
    }

    @Override
    protected void init() {
        this.windowsTerminal = terminal.getClass().getSimpleName().endsWith("WinSysTerminal");
        editBuffer = new StringBuilder();
        curPos = 0;
        current = null;
        curIndex = 0;
        paused = false;
        terminal.trackMouse(Terminal.MouseTracking.Button);
//        setFooter(Collections.singletonList(
//                createLine(builder -> {
//                    builder.append("Ctrl+c exit.", green);
//                })
//        ));
    }


    protected void createHeader(List<AttributedString> lines) {

    }

    protected void createBody(List<AttributedString> lines) {


    }

    protected void setFooter(List<AttributedString> footer) {
        this.currentFooter = footer;
    }


    protected String inputHelp() {
        return "Command:";
    }

    private long lastShowTime = System.currentTimeMillis();

    protected boolean showCursor() {
        if(windowsTerminal){
            return true;
        }
        if (System.currentTimeMillis() - lastShowTime >= 500) {
            lastShowTime = System.currentTimeMillis();
            return true;
        }
        return false;
    }


    @Override
    protected synchronized final boolean display() {
        if (paused || disposable == null || disposable.isDisposed()) {
            return true;
        }
        LinkedList<AttributedString> header = new LinkedList<>();
        LinkedList<AttributedString> body = new LinkedList<>();
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

        if (currentFooter != null) {
            footer.addAll(currentFooter);
        }

        while (footer.size() < 3) {
            footer.add(createLine(builder -> builder.append("")));
        }

        int bodyAliveHeight = terminal.getHeight() - footer.size() - header.size();

        while (body.size() != bodyAliveHeight) {
            if (body.size() > bodyAliveHeight) {
                body.removeFirst();
            } else {
                body.add(createLine(builder -> builder.append("")));
            }
        }

        header.addAll(body);
        header.addAll(footer);

        if (windowsTerminal && !Objects.equals(header, lastLines)) {
            display.clear();
        }
        lastLines = header;
        display.update(header, 0, true);

        return true;
    }

    @Override
    protected void bindDefault(KeyMap<Operation> keys) {
        keys.bind(DefaultOperation.EXIT, KeyMap.ctrl('c'));

        keys.bind(DefaultOperation.CLEAR, KeyMap.ctrl('l'));

        keys.setUnicode(EditorOperation.INSERT);
        for (char i = 32; i < 256; i++) {
            keys.bind(EditorOperation.INSERT, Character.toString(i));
        }

        keys.bind(EditorOperation.HELP, KeyMap.ctrl('?'));

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
    protected boolean doOn(Operation operation) {
        super.doOn(operation);
        paused = false;
        mouseEvent = null;
        if (operation instanceof EditorOperation) {
            EditorOperation opt = ((EditorOperation) operation);
            switch (opt) {
                case EXECUTE:
                    return execute();
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

    private boolean execute() {


        String command = editBuffer.toString().trim();
        switch (command) {
            case "q:":
            case "exit":

                return false;
            case "clear":
                display.clear();
                break;
        }

        editBuffer.setLength(0);
        curPos = 0;
        return true;
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
