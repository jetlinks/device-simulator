package org.jetlinks.simulator.core.listener;

import lombok.AllArgsConstructor;
import org.jetlinks.simulator.core.Session;
import org.jetlinks.simulator.core.Simulator;
import org.jetlinks.simulator.core.SimulatorListener;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@AllArgsConstructor
public class AutoReconnectListener implements SimulatorListener {

    private final String id;

    private final Duration[] delays;

    private final int maxTimes;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getType() {
        return "auto-reconnect";
    }

    @Override
    public boolean supported(Simulator simulator) {
        return true;
    }

    @Override
    public void init(Simulator simulator) {

    }

    @Override
    public void before(Session session) {

    }

    @Override
    public void after(Session session) {
        AtomicInteger times = new AtomicInteger();
        Runnable doReconnect = () -> {
            int currentTimes = Math.min(times.get(), delays.length - 1);
            if (maxTimes > 0 && times.incrementAndGet() >= maxTimes) {
                return;
            }
            Duration delay = this.delays[currentTimes];
            Mono.delay(delay)
                    .flatMap(ignore -> session.connect())
                    .subscribe();
        };
        session.onError(err -> doReconnect.run());
        session.onConnected(() -> times.set(0));
        session.onDisconnected(doReconnect);
    }

    @Override
    public void shutdown() {

    }
}
