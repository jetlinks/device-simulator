package org.jetlinks.simulator.core.script;

public interface ScriptFactoryProvider {

    boolean isSupport(String langOrMediaType);

    ScriptFactory factory();

}
