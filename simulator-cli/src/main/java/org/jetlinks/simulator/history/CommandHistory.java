package org.jetlinks.simulator.history;

import org.jline.reader.History;

import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class CommandHistory {

    private static final Map<String, History> historyMap = new ConcurrentHashMap<>();

    public static History getHistory(String name) {
        return historyMap.computeIfAbsent(name, ignore -> new DefaultHistory(Paths.get("history", name)));
    }

}
