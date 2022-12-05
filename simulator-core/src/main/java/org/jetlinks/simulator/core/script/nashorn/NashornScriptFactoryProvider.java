package org.jetlinks.simulator.core.script.nashorn;

import org.jetlinks.simulator.core.script.AbstractScriptFactoryProvider;
import org.jetlinks.simulator.core.script.ScriptFactory;

public class NashornScriptFactoryProvider extends AbstractScriptFactoryProvider {

    public NashornScriptFactoryProvider() {
        super("js", "javascript", "nashorn");
    }

    @Override
    public ScriptFactory factory() {
        return new NashornScriptFactory();
    }
}
