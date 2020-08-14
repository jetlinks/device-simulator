package org.jetlinks.simulator.core.listener;

import org.jetlinks.simulator.core.SimulatorConfig;
import org.jetlinks.simulator.core.SimulatorListener;

import java.util.Map;

public class JSR223ListenerProvider implements SimulatorListenerProvider {

    @Override
    public String getType() {
        return "jsr223";
    }

    @Override
    public SimulatorListener build(SimulatorConfig.Listener listener) {

        Map<String,Object> conf = listener.getConfiguration();

        String lang = (String) conf.getOrDefault("lang","js");
        String script = (String) conf.get("script");

        return new JSR223Listener(listener.getId(),lang,script);
    }
}
