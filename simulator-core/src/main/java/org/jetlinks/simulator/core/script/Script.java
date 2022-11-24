package org.jetlinks.simulator.core.script;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@Getter
@AllArgsConstructor(staticName = "of")
public class Script {

    @NonNull
    private final String name;
    @NonNull
    private final String content;

    private final Object source;

    private final boolean returnNative;

    public Script returnNative() {
        return new Script(name, content, source, true);
    }

    public static Script of(String name, String content) {
        return Script.of(name, content, null, false);
    }

    public Script content(String content) {
        return of(name, content, source, returnNative);
    }

}
