package org.jetlinks.simulator.core.report;

import lombok.Getter;

import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

class DefaultReporter implements Reporter {
    private final Map<String, List<PointImpl>> points = new ConcurrentHashMap<>();

    static Duration[] distArray = {
            Duration.ofSeconds(5),
            Duration.ofSeconds(1),
            Duration.ofMillis(500),
            Duration.ofMillis(100),
            Duration.ofMillis(10),
            Duration.ofMillis(1)};


    public List<PointImpl> points(String name) {
        return points.computeIfAbsent(name, ignore -> new CopyOnWriteArrayList<>());
    }

    @Override
    public Point newPoint(String name) {
        PointImpl point = new PointImpl();
        points(name).add(point);
        return point;
    }

    @Override
    public Map<String, Reporter.Aggregate> aggregates() {
        Map<String, Reporter.Aggregate> aggregateMap = new HashMap<>();
        for (Map.Entry<String, List<PointImpl>> stringListEntry : points.entrySet()) {
            AggregateImpl impl = new AggregateImpl();
            stringListEntry.getValue().forEach(impl::update);
            aggregateMap.put(stringListEntry.getKey(), impl);
        }
        return aggregateMap;
    }

    @Override
    public Aggregate aggregate(String name) {
        AggregateImpl impl = new AggregateImpl();
        points.getOrDefault(name,Collections.emptyList()).forEach(impl::update);
        return impl;
    }

    @Getter
    static class AggregateImpl implements Aggregate {

        int total;
        int executing;
        long totalTimeNanos;

        Map<Duration, Long> distribution = new TreeMap<>(Comparator.comparingLong(Duration::toNanos).reversed());


        public void update(PointImpl point) {
            if (point.endWithNanos == 0) {
                executing++;
                return;
            }

            total++;
            long useNanos = point.endWithNanos - point.startWithNanos;

            for (Duration duration : distArray) {
                if (useNanos >= duration.toNanos()) {
                    distribution.compute(duration, (ignore, x) -> x == null ? 1 : x + 1);
                    break;
                }
            }

            totalTimeNanos += useNanos;

        }
    }

    @Getter
    static class PointImpl implements Point {
        private long startWithNanos;
        private long endWithNanos;
        private boolean error;
        private String errorMessage;

        @Override
        public void start() {
            startWithNanos = System.nanoTime();
        }

        @Override
        public void success() {
            endWithNanos = System.nanoTime();
        }

        @Override
        public void error(String message) {
            endWithNanos = System.nanoTime();
            this.error = error;
            this.errorMessage = message;
        }
    }
}
