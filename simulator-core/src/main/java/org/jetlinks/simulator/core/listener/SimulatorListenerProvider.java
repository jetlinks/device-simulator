package org.jetlinks.simulator.core.listener;


import org.jetlinks.simulator.core.SimulatorConfig;
import org.jetlinks.simulator.core.SimulatorListener;

public interface SimulatorListenerProvider {

    String getType();

    SimulatorListener build(SimulatorConfig.Listener listener);

}
