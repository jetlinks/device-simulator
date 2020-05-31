package org.jetlinks.simulator;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.reader.impl.completer.StringsCompleter;

import java.util.List;

public class FileVarsCompleter implements Completer {

    Completer completer;

    public FileVarsCompleter() {
        this.completer = new StringsCompleter();
    }

    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        completer.complete(reader, line, candidates);
    }

    public void setFileVars(List<String> fileVars) {
        this.completer = new StringsCompleter(fileVars);
    }
}