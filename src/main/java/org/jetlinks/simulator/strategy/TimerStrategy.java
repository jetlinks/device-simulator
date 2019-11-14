package org.jetlinks.simulator.strategy;

import org.jetlinks.simulator.SimulationStrategy;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.time.Duration;

public class TimerStrategy implements SimulationStrategy {

    private SimulationStrategy strategy;

    private Duration period;

    public TimerStrategy(Duration period, SimulationStrategy strategy) {
        this.period = period;
        this.strategy = strategy;
    }

    private Disposable disposable;

    @Override
    public void start() {
        if (disposable != null) {
            return;
        }
        disposable = Flux.interval(period, period)
                .subscribe(i -> execute());
    }

    @Override
    public void execute() {
        strategy.execute();
    }

    @Override
    public void stop() {
        disposable.dispose();
        disposable = null;
    }
}
