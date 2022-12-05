package org.jetlinks.simulator.cli;

import ch.qos.logback.classic.PatternLayout;
import picocli.CommandLine.Help.Ansi;

/**
 * A Logback Pattern Layout which use Picocli ANSI color heuristic to apply ANSI color only on terminal which support
 * it.
 */
public class ColorAwarePatternLayout extends PatternLayout {

    static {
        if (!Ansi.AUTO.enabled()) {
            DEFAULT_CONVERTER_MAP.put("black", NoColorConverter.class.getName());
            DEFAULT_CONVERTER_MAP.put("red", NoColorConverter.class.getName());
            DEFAULT_CONVERTER_MAP.put("green", NoColorConverter.class.getName());
            DEFAULT_CONVERTER_MAP.put("yellow", NoColorConverter.class.getName());
            DEFAULT_CONVERTER_MAP.put("blue", NoColorConverter.class.getName());
            DEFAULT_CONVERTER_MAP.put("magenta", NoColorConverter.class.getName());
            DEFAULT_CONVERTER_MAP.put("cyan", NoColorConverter.class.getName());
            DEFAULT_CONVERTER_MAP.put("white", NoColorConverter.class.getName());
            DEFAULT_CONVERTER_MAP.put("gray", NoColorConverter.class.getName());
            DEFAULT_CONVERTER_MAP.put("boldRed", NoColorConverter.class.getName());
            DEFAULT_CONVERTER_MAP.put("boldGreen", NoColorConverter.class.getName());
            DEFAULT_CONVERTER_MAP.put("boldYellow", NoColorConverter.class.getName());
            DEFAULT_CONVERTER_MAP.put("boldBlue", NoColorConverter.class.getName());
            DEFAULT_CONVERTER_MAP.put("boldMagenta", NoColorConverter.class.getName());
            DEFAULT_CONVERTER_MAP.put("boldCyan", NoColorConverter.class.getName());
            DEFAULT_CONVERTER_MAP.put("boldWhite", NoColorConverter.class.getName());
            DEFAULT_CONVERTER_MAP.put("highlight", NoColorConverter.class.getName());
        }
    }
}