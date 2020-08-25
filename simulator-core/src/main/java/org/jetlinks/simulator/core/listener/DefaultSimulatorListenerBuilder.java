package org.jetlinks.simulator.core.listener;

import org.jetlinks.simulator.core.SimulatorConfig;
import org.jetlinks.simulator.core.SimulatorListener;
import org.jetlinks.simulator.core.SimulatorListenerBuilder;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultSimulatorListenerBuilder implements SimulatorListenerBuilder {

    private final Map<String, SimulatorListenerProvider> providers = new ConcurrentHashMap<>();

    public DefaultSimulatorListenerBuilder(){
        addProvider(new JSR223ListenerProvider());
        addProvider(new AutoReconnectListenerProvider());
    }

    @Override
    public SimulatorListener build(SimulatorConfig.Listener listener) {
        return Optional.ofNullable(providers.get(listener.getType()))
                .map(provider -> provider.build(listener))
                .orElseThrow(() -> new UnsupportedOperationException("unsupported listener type:" + listener.getType()));
    }

    public void addProvider(SimulatorListenerProvider provider) {
        providers.put(provider.getType(), provider);
    }

}
