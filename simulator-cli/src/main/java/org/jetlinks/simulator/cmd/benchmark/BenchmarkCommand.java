package org.jetlinks.simulator.cmd.benchmark;

import org.apache.commons.collections.MapUtils;
import org.jetlinks.simulator.cmd.AbstractCommand;
import org.jetlinks.simulator.cmd.CommonCommand;
import org.jetlinks.simulator.cmd.AttachCommand;
import org.jetlinks.simulator.core.Connection;
import org.jetlinks.simulator.core.ConnectionManager;
import org.jetlinks.simulator.core.ExceptionUtils;
import org.jetlinks.simulator.core.benchmark.Benchmark;
import org.jetlinks.simulator.core.benchmark.BenchmarkOptions;
import org.jetlinks.simulator.core.monitor.SystemMonitor;
import org.jetlinks.simulator.core.report.Reporter;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.util.StringUtils;
import picocli.CommandLine;

import java.io.File;
import java.io.FileDescriptor;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@CommandLine.Command(name = "benchmark",
        description = "Run Benchmark",
        headerHeading = "%n",
        subcommands = {
                MqttBenchMark.class,
                BenchmarkCommand.StatsCommand.class,
                BenchmarkListCommand.class,
                TcpBenchMark.class,
                UDPBenchMark.class,
                HTTPBenchMark.class
        })
public class BenchmarkCommand extends CommonCommand implements Runnable {
    private final static MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

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

    static class NameComplete implements Iterable<String> {

        @Override
        public Iterator<String> iterator() {
            return allBenchMark.keySet().iterator();
        }
    }

    @CommandLine.Command(name = "stats",
            description = "Show Benchmark stats",
            headerHeading = "%n")
    static class StatsCommand extends AttachCommand implements Runnable {

        @CommandLine.Option(names = {"--name"}, description = "名称")
        private String name;

        private Collection<Benchmark> benchmarks;

        @Override
        protected void doClear() {
            super.doClear();
            if (benchmarks != null) {
                for (Benchmark benchmark : benchmarks) {
                    benchmark.clear();
                }
            }
        }

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

                Deque<Benchmark.Snapshot> snapshots = benchmark.snapshots();

                Benchmark.Snapshot last = snapshots.peekLast();
                Benchmark.Snapshot lastQps = last == null ? null : last.getDiff();

                lines.add(
                        createLine(builder -> {
                            Reporter.Aggregate connection = benchmark
                                    .getReporter()
                                    .aggregate(Benchmark.REPORT_CONNECTING);

                            builder.append("Benchmark(")
                                   .append(benchmark.getName(), green)
                                   .append(") size: ")
                                   .append(String.valueOf(benchmark.getOptions().getSize()), green)
                                   .append(" connecting: ")
                                   .append(lastQps == null ? "0" : String.valueOf(lastQps
                                                                                          .getSummary()
                                                                                          .getSize()), green)
                                   .append("/s");


                            builder.append(" Time distribution: ");

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

                            double cpu = SystemMonitor.jvmCpuUsage.value();
                            MemoryUsage heap = memoryMXBean.getHeapMemoryUsage();
                            double heapUsage = (double) heap.getUsed() / heap.getMax();

                            builder.append(" JVM CPU: ")
                                   .append(String.format("%.2f", cpu * 100) + "%", cpu > 0.8 ? red : green);

                            builder.append(" JVM Mem: ")
                                   .append(formatBytes(heap.getUsed()) + "/" + formatBytes(heap.getMax()), heapUsage > 0.8 ? red : green);

                            Throwable lastError = benchmark.getLastError();
                            if (null != lastError) {
                                builder.append(" Last Error: ")
                                       .append(ExceptionUtils.getErrorMessage(lastError), red);

                            }

                        })
                );


                lines.add(
                        createLine(builder -> {
                            ConnectionManager.Summary summary = benchmark.getConnectionManager().summary().block();

                            Map<String, Integer> statusCount = benchmark
                                    .getConnectionManager()
                                    .getConnections()
                                    .map(Connection::statusCount)
                                    .reduce(new LinkedHashMap<String, Integer>(), ((a, b) -> {
                                        b.forEach((key, v) -> a.compute(key.replace("status_", ""), (ignore, old) -> old == null ? v : v + old));
                                        return a;
                                    }))
                                    .block();

                            if (summary != null) {
                                builder.append("                ");

                                if (benchmark.isDisposed()) {
                                    builder.append("stopped", red);
                                } else {
                                    builder.append("alive: ").append(String.valueOf(summary.getConnected()), green);
                                }

                                if (lastQps != null) {
                                    builder.append(" sent: ")
                                           .append(String.valueOf(summary.getSent()), green)
                                           .append(",")
                                           .append(String.valueOf(lastQps.getSummary().getSent()), green)
                                           .append("/s(")
                                           .append(formatBytes(summary.getSentBytes()), blue)
                                           .append(",")
                                           .append(formatBytes(lastQps.getSummary().getSentBytes()), blue)
                                           .append("/s)");

                                    builder.append(" received: ")
                                           .append(String.valueOf(summary.getReceived()), green)
                                           .append(",")
                                           .append(String.valueOf(lastQps.getSummary().getReceived()), green)
                                           .append("/s(")
                                           .append(formatBytes(summary.getReceivedBytes()), blue)
                                           .append(",")
                                           .append(formatBytes(lastQps.getSummary().getReceivedBytes()), blue)
                                           .append("/s)");
                                } else {
                                    builder.append(" sent: ")
                                           .append(String.valueOf(summary.getSent()), green)
                                           .append("(")
                                           .append(formatBytes(summary.getSentBytes()), blue)
                                           .append(")");

                                    builder.append(" received: ")
                                           .append(String.valueOf(summary.getReceived()), green)
                                           .append("(")
                                           .append(formatBytes(summary.getReceivedBytes()), blue)
                                           .append(")");
                                }
                            }

                            if (MapUtils.isNotEmpty(statusCount)) {
                                builder.append(" Status:");
                                for (Map.Entry<String, Integer> str : statusCount.entrySet()) {
                                    builder.append(" ");
                                    AttributedStyle style = statusIsBad(str.getKey()) ? red : green;
                                    builder
                                            .append(str.getKey(), style)
                                            .append("(")
                                            .append(String.valueOf(str.getValue()), blue)
                                            .append(")");
                                }
                            }
                        })
                );
            }
        }

        public boolean statusIsBad(String status) {
            return !"OK".equals(status) && !"SUCCESS".equals(status);
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

        @Override
        protected AbstractCommand createCommand() {
            return new AttachCommands();
        }

        @CommandLine.Command(name = "",
                subcommands = {
                        ReloadCommand.class,
                        Stop.class
                },
                customSynopsis = {""},
                synopsisHeading = "")
        class AttachCommands extends CommonCommand {

            void reload(ReloadCommand command) {
                for (Benchmark benchmark : benchmarks) {
                    if (command.file != null && command.file.isFile() && command.file.exists()) {
                        benchmark.getOptions().setFile(command.file);
                    }
                    if (command.scriptArgs != null) {
                        benchmark.getOptions().setScriptArgs(command.scriptArgs);
                    }
                    try {
                        benchmark.reload();
                    } catch (Throwable err) {
                        printfError("reload error:%s%n", ExceptionUtils.getErrorMessage(err));
                    }
                }
            }

            void stop() {
                for (Benchmark benchmark : benchmarks) {
                    benchmark.dispose();
                }
            }

        }

        @CommandLine.Command(name = "reload")
        static class ReloadCommand extends CommonCommand {

            @CommandLine.Option(names = {"--script"}, description = "Script File", order = 1)
            private File file;

            @CommandLine.Parameters(paramLabel = "Script arguments")
            Map<String, Object> scriptArgs;

            @Override
            public void run() {
                ((AttachCommands) parent).reload(this);
            }
        }

        @CommandLine.Command(name = "stop")
        static class Stop extends CommonCommand {
            @Override
            public void run() {
                ((AttachCommands) parent).stop();
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
