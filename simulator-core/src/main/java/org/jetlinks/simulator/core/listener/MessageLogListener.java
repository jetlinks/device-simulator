package org.jetlinks.simulator.core.listener;

import lombok.Getter;
import org.jetlinks.simulator.core.Session;
import org.jetlinks.simulator.core.Simulator;
import org.jetlinks.simulator.core.SimulatorListener;

import java.util.Map;

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
        this.simulator.doOnComplete(() -> {
            simulator.state()
                    .subscribe(state -> {
                        StringBuilder builder = new StringBuilder();
                        long total = state.getTotal();
                        builder.append("=======complete(total:").append(total).append(")==========").append("\n");
                        builder.append("max: ").append(state.getAggTime().getMax()).append("ms").append("\n");
                        builder.append("min: ").append(state.getAggTime().getMin()).append("ms").append("\n");
                        builder.append("avg: ").append(state.getAggTime().getAvg()).append("ms").append("\n");
                        for (Map.Entry<Integer, Long> entry : state.getDistTime().entrySet()) {
                            builder.append("> ")
                                    .append(entry.getKey()).append("ms").append(": ")
                                    .append(entry.getValue())
                                    .append("(").append(total == 0 ? 0 : String.format("%.1f", (entry.getValue() / (double) total) * 100)).append("%)")
                                    .append("\n");
                        }
                        simulator.log("\n{}", builder);
                    });

        });
    }

    @Override
    public void before(Session session) {
    }

    @Override
    public void after(Session session) {
        session.onDownstream(msg -> simulator.log("{} downstream => \n{}", session, msg));
        session.onUpstream(msg -> simulator.log("{} upstream <= \n{}", session, msg));
        session.onDisconnected(() -> simulator.log("{} disconnect", session));
    }

    @Override
    public void shutdown() {

    }

    @Override
    public int order() {
        return Integer.MIN_VALUE;
    }
}
