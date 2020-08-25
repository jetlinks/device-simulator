package org.jetlinks.simulator.core.listener;

import org.jetlinks.core.Value;
import org.jetlinks.core.Values;
import org.jetlinks.simulator.core.SimulatorConfig;
import org.jetlinks.simulator.core.SimulatorListener;

import java.time.Duration;
import java.util.stream.Stream;

public class AutoReconnectListenerProvider implements SimulatorListenerProvider {
    @Override
    public String getType() {
        return "auto-reconnect";
    }

    @Override
    public SimulatorListener build(SimulatorConfig.Listener listener) {
        Values config = Values.of(listener.getConfiguration());

        return new AutoReconnectListener(
                listener.getId(),
                config.getValue("delays")
                        .map(Value::asString)
                        .map(v -> Stream
                                .of(v.split("[,]"))
                                .map(string -> Duration.ofMillis(Integer.parseInt(string)))
                                .toArray(Duration[]::new))
                        .orElse(new Duration[]{Duration.ofSeconds(1)}),
                config.getValue("maxTimes")
                        .map(Value::asInt)
                        .orElse(4)
        );
    }
}
