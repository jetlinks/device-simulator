package org.jetlinks.simulator.cmd;

import org.jline.keymap.KeyMap;
import org.jline.terminal.MouseEvent;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.jline.utils.InfoCmp;
import picocli.CommandLine;

import java.util.LinkedList;
import java.util.List;

import static org.jline.keymap.KeyMap.key;

@CommandLine.Command(name = "edit")
public class EditableAttachCommand extends AttachCommand {

    protected static final AttributedStyle helpBg = AttributedStyle.BOLD
            .background(AttributedStyle.BLUE).foreground(AttributedStyle.WHITE);
    protected static final AttributedStyle black = AttributedStyle.BOLD.background(AttributedStyle.BLACK);

    protected StringBuilder editBuffer;

    private int curPos;

    private EditorOperation current;

    private int curIndex = 0;
    private boolean paused = false;

    private MouseEvent mouseEvent;

    @Override
    protected void init() {
        editBuffer = new StringBuilder();
        curPos = 0;
        current = null;
        curIndex = 0;
        paused = false;
        terminal.trackMouse(Terminal.MouseTracking.Button);
    }


    protected void createDisplay(List<AttributedString> container) {


    }

    protected String inputHelp() {
        return "Command -> ";
    }

    @Override
    protected synchronized final boolean display() {
        if (paused || disposable == null || disposable.isDisposed()) {
            return true;
        }
        display.clear();

        LinkedList<AttributedString> info = new LinkedList<>();

        createDisplay(info);

        LinkedList<AttributedString> footer = new LinkedList<>();

        String help = inputHelp();
        footer.add(createLine(builder -> builder
                .append(help, helpBg)
                .append(editBuffer.toString(), helpBg)));

        addFooter(footer);

        int over = terminal.getHeight() - footer.size() - info.size();
        if (over != 0) {
            for (int i = 0; i < Math.abs(over); i++) {
                //超过缓冲区
                if (over < 0) {
                    info.removeLast();
                } else {
                    info.add(createLine(builder -> builder.append("")));
                }
            }
        }

        info.addAll(footer);


        // int cursorNeed = terminal.getHeight() - footer.size() - curIndex;
        curIndex = terminal.getHeight() - footer.size();

        display.update(info, 0, false);
//        for (int i = 0; i < Math.abs(cursorNeed); i++) {
//            if (cursorNeed > 0) {
//                if (terminal.puts(parm_down_cursor, curIndex)) {
//                    break;
//                }
//                terminal.puts(InfoCmp.Capability.cursor_down);
//            } else {
//                if (terminal.puts(parm_up_cursor, curIndex)) {
//                    break;
//                }
//                terminal.puts(InfoCmp.Capability.cursor_up);
//            }
//        }

        //   terminal.puts(cursor_visible);
        //  if (cursorNeed != 0) {
        //  terminal.puts(InfoCmp.Capability.save_cursor);
        if (mouseEvent != null) {
            terminal.puts(InfoCmp.Capability.cursor_address, mouseEvent.getY(), mouseEvent.getX());
        } else {
            terminal.puts(InfoCmp.Capability.cursor_address, curIndex, curPos + help.length());
        }

//        terminal.puts(save_cursor);
        terminal.flush();
        //   }


//        if (oldLineSize != lineSize) {
//            InfoCmp.Capability capability = oldLineSize < lineSize ? InfoCmp.Capability.cursor_down : InfoCmp.Capability.cursor_up;
//            for (int i = 0, s = Math.abs(oldLineSize - lineSize); i < s; i++) {
//                terminal.puts(capability);
//            }
//            terminal.puts(InfoCmp.Capability.save_cursor);
//            terminal.puts(InfoCmp.Capability.cursor_address, lineSize, 0);
//        }


        return true;
    }

    protected final void addFooter(List<AttributedString> source) {
        if (current == EditorOperation.TAP) {
            source.add(AttributedString.fromAnsi("commands: "));
        } else {
            source.add(AttributedString.fromAnsi("Enter Execute"));
        }

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

        keys.bind(EditorOperation.RIGHT, key(terminal, InfoCmp.Capability.key_right));
        keys.bind(EditorOperation.LEFT, key(terminal, InfoCmp.Capability.key_left));
        keys.bind(EditorOperation.UP, key(terminal, InfoCmp.Capability.key_up));
        keys.bind(EditorOperation.DOWN, key(terminal, InfoCmp.Capability.key_down));

        keys.bind(EditorOperation.FIRST, KeyMap.ctrl('a'));
        keys.bind(EditorOperation.LAST, KeyMap.ctrl('e'));


        keys.bind(EditorOperation.BACKSPACE, KeyMap.del());

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
