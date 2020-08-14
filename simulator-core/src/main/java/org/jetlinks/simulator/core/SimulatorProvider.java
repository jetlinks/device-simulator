package org.jetlinks.simulator.core;


public interface SimulatorProvider {

    String getType();

    Simulator createSimulator(SimulatorConfig config);

}
