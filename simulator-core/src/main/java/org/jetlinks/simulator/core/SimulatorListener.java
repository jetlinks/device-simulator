package org.jetlinks.simulator.core;


public interface SimulatorListener extends Comparable<SimulatorListener> {

    String getId();

    String getType();

    boolean supported(Simulator simulator);

    void init(Simulator simulator);

    void before(Session session);

    void after(Session session);

    void shutdown();

    default int order() {
        return Integer.MAX_VALUE;
    }

    @Override
    default int compareTo(SimulatorListener o) {
        return Integer.compare(this.order(), o.order());
    }
}
