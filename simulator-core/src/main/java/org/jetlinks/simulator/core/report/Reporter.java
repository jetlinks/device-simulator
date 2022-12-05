package org.jetlinks.simulator.core.report;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

public interface Reporter {

    static Reporter create() {
        return new DefaultReporter();
    }

    Point newPoint(String name);

    Map<String, Aggregate> aggregates();

    Aggregate aggregate(String name);

    interface Aggregate {
        int getTotal();

        int getExecuting();

        long getTotalTimeNanos();

        Map<Duration, Long> getDistribution();
    }

    interface Point {
        void start();

        void success();

        void error(String message);
    }


}
