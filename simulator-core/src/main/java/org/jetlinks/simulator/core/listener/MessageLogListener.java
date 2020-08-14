package org.jetlinks.simulator.core.listener;

import lombok.Getter;
import org.jetlinks.simulator.core.Session;
import org.jetlinks.simulator.core.Simulator;
import org.jetlinks.simulator.core.SimulatorListener;

public class MessageLogListener implements SimulatorListener {
    @Getter
    private final String id;

    private Simulator simulator;

    public MessageLogListener(String id) {
        this.id = id;
    }

    @Override
    public String getType() {
        return "message";
    }

    @Override
    public boolean supported(Simulator simulator) {
        return true;
    }

    @Override
    public void init(Simulator simulator) {
        this.simulator = simulator;
    }

    @Override
    public void before(Session session) {
    }

    @Override
    public void after(Session session) {
        session.onDownstream(msg -> simulator.log("{} downstream : \n{}", session, msg));
        session.onUpstream(msg -> simulator.log("{} upstream : \n{}", session, msg));
    }

    @Override
    public void shutdown() {

    }

    @Override
    public int order() {
        return Integer.MIN_VALUE;
    }
}
