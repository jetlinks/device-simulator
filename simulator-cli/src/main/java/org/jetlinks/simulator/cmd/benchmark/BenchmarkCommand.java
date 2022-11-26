package org.jetlinks.simulator.cmd.benchmark;

import org.jetlinks.simulator.cmd.CommonCommand;
import org.jetlinks.simulator.cmd.AttachCommand;
import org.jetlinks.simulator.core.ConnectionManager;
import org.jetlinks.simulator.core.ExceptionUtils;
import org.jetlinks.simulator.core.benchmark.Benchmark;
import org.jetlinks.simulator.core.benchmark.BenchmarkOptions;
import org.jetlinks.simulator.core.report.Reporter;
import org.jline.utils.AttributedString;
import org.springframework.util.StringUtils;
import picocli.CommandLine;

import java.io.File;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@CommandLine.Command(name = "benchmark",
        description = "Run Benchmark",
        headerHeading = "%n",
        subcommands = {
                MqttBenchMark.class,
                BenchmarkCommand.StatsCommand.class,
                BenchmarkListCommand.class,
                TcpBenchMark.class})
public class BenchmarkCommand extends CommonCommand implements Runnable {

    static Map<String, Benchmark> allBenchMark = new ConcurrentHashMap<>();
    static Map<String, Integer> benchmarkNameIndex = new ConcurrentHashMap<>();

    public static void addBenchmark(Benchmark benchmark) {
        Benchmark old = allBenchMark.put(benchmark.getName(), benchmark);
        if (null != old) {
            old.dispose();
        }
    }

    public static String createBenchmarkName(String name) {
        if (allBenchMark.containsKey(name)) {
            return name + "_" + benchmarkNameIndex.compute(name, (ignore, i) -> i == null ? 2 : i + 1);
        }
        return name;
    }

    @Override
    public void run() {
        showHelp();
    }

    @CommandLine.Command(name = "stats",
            description = "Show Benchmark stats",
            headerHeading = "%n")
    static class StatsCommand extends AttachCommand implements Runnable {

        @CommandLine.Option(names = {"--name"}, description = "名称")
        private String name;

        private Collection<Benchmark> benchmarks;

        @Override
        protected void init() {
            super.init();
            if (StringUtils.hasText(name)) {
                Benchmark benchmark = allBenchMark.get(name);
                if (benchmark == null) {
                    throw new IllegalArgumentException("Benchmark [%s] not found");
                }
                benchmarks = Collections.singleton(benchmark);
            } else {
                benchmarks = allBenchMark.values();
            }
        }

        @Override
        protected void createHeader(List<AttributedString> lines) {
            for (Benchmark benchmark : benchmarks) {
                lines.add(
                        createLine(builder -> {
                            Reporter.Aggregate connection = benchmark
                                    .getReporter()
                                    .aggregate(Benchmark.REPORT_CONNECTING);

                            builder.append("Benchmark(")
                                   .append(benchmark.getName(), green)
                                   .append(") size: ")
                                   .append(String.valueOf(benchmark.getOptions().getSize()), green)
                                   .append(" completed: ")
                                   .append(String.valueOf(Math.abs(connection.getTotal() - connection.getExecuting())), green)
                                   .append(" connecting: ")
                                   .append(String.valueOf(connection.getExecuting()), green)
                                   .append(" Time distribution: ");

                            int i = 0;
                            for (Map.Entry<Duration, Long> entry : connection.getDistribution().entrySet()) {
                                if (i++ > 0) {
                                    builder.append(",");
                                }
                                builder
                                        .append(String.valueOf(entry.getValue()), green)
                                        .append(">=")
                                        .append(String.valueOf(entry.getKey().toMillis()))
                                        .append("ms");
                            }

                        })
                );

                lines.add(
                        createLine(builder -> {
                            ConnectionManager.Summary summary = benchmark.getConnectionManager().summary().block();
                            if (summary != null) {
                                builder.append("               ")
                                       .append(" alive: ")
                                       .append(String.valueOf(summary.getConnected()), green)
                                       .append(" sent: ")
                                       .append(String.valueOf(summary.getSent()), green)
                                       .append("(")
                                       .append(formatBytes(summary.getSentBytes()), blue)
                                       .append(")")
                                       .append(" received: ")
                                       .append(String.valueOf(summary.getReceived()), green)
                                       .append("(")
                                       .append(formatBytes(summary.getReceivedBytes()), blue)
                                       .append(")");
                            }

                            Throwable lastError = benchmark.getLastError();
                            if (null != lastError) {
                                builder.append(" Last Error: ")
                                       .append(ExceptionUtils.getErrorMessage(lastError), red);

                            }
                        })
                );
            }
        }

        @Override
        protected void createBody(List<AttributedString> lines) {
            for (Benchmark benchmark : benchmarks) {

                for (String log : benchmark.getLogs()) {
                    for (String l : log.split("\n")) {
                        lines.add(createLine(builder -> builder.append(l)));
                    }
                }
            }

        }

    }

    public static class Options extends BenchmarkOptions {
        @Override
        @CommandLine.Option(names = {"--name"}, description = "Set Unique name", order = 90)
        public void setName(String name) {
            super.setName(name);
        }

        @Override
        @CommandLine.Option(names = {"--index"}, description = "Start index", defaultValue = "0", order = 100)
        public void setIndex(int index) {
            super.setIndex(index);
        }

        @CommandLine.Option(names = {"--size"}, description = "Number of create", defaultValue = "1", order = 101)
        @Override
        public void setSize(int size) {
            super.setSize(size);
        }

        @CommandLine.Option(names = {"--concurrency"}, description = "Concurrency", defaultValue = "8", order = 102)
        @Override
        public void setConcurrency(int concurrency) {
            super.setConcurrency(concurrency);
        }

        @CommandLine.Option(names = {"--script"}, description = "Script File", order = 103)
        @Override
        public void setFile(File file) {
            super.setFile(file);
        }

        @CommandLine.Parameters
        @Override
        public void setScriptArgs(Map<String, Object> scriptArgs) {
            super.setScriptArgs(scriptArgs);
        }
    }
}
