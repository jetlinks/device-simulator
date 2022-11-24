package org.jetlinks.simulator.cmd.benchmark;

import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.github.freva.asciitable.HorizontalAlign;
import org.jetlinks.simulator.cmd.AbstractCommand;
import org.jetlinks.simulator.cmd.CommonCommand;
import org.jetlinks.simulator.core.benchmark.Benchmark;
import org.jetlinks.simulator.core.benchmark.BenchmarkOptions;
import org.jetlinks.simulator.core.report.Reporter;
import org.springframework.util.StringUtils;
import picocli.CommandLine;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@CommandLine.Command(name = "benchmark",
        description = "压力测试",
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
    static class StatsCommand extends AbstractCommand implements Runnable {
        static Column[] columns = new Column[]{
                new Column().header("Name").headerAlign(HorizontalAlign.CENTER).dataAlign(HorizontalAlign.LEFT),
                new Column().header("Connections").headerAlign(HorizontalAlign.CENTER),
                new Column().header("TimeDist").headerAlign(HorizontalAlign.CENTER)
        };

        @CommandLine.Option(names = {"--name"}, description = "名称")
        private String name;


        @Override
        public void run() {
            Collection<Benchmark> benchmarks;
            if (StringUtils.hasText(name)) {
                Benchmark benchmark = allBenchMark.get(name);
                if (benchmark == null) {
                    printf("Benchmark [%s] not found", name);
                }
                benchmarks = Collections.singleton(benchmark);
            } else {
                benchmarks = allBenchMark.values();
            }

            Object[][] data = benchmarks
                    .stream()
                    .map(benchmark -> {
                        Reporter.Aggregate aggregate = benchmark
                                .getReporter()
                                .aggregate(Benchmark.REPORT_CONNECTING);
                        return new Object[]{
                                benchmark.getName(),
                                aggregate.getExecuting() == 0 ?
                                        aggregate.getTotal()
                                        : aggregate.getTotal() + "(" + aggregate.getExecuting() + ")",
                                aggregate.getDistribution()
                                         .entrySet().stream()
                                         .map(entry -> ">=" + entry.getKey().toMillis() + "ms:" + entry.getValue())
                                        .collect(Collectors.joining(",", "[", "]"))
                        };
                    })
                    .toArray(Object[][]::new);

            printf("%s%n", AsciiTable
                    .builder()
                    .data(columns, data)
                    .toString());
        }
    }

    static class Options extends BenchmarkOptions {
        @Override
        @CommandLine.Option(names = {"--name"}, description = "名称", order = 90)
        public void setName(String name) {
            super.setName(name);
        }

        @Override
        @CommandLine.Option(names = {"--index"}, description = "起始序号,可以在脚本中获取序号来动态生成设备标识", defaultValue = "0", order = 100)
        public void setIndex(int index) {
            super.setIndex(index);
        }

        @CommandLine.Option(names = {"--size"}, description = "创建客户端数量", defaultValue = "1", order = 101)
        @Override
        public void setSize(int size) {
            super.setSize(size);
        }

        @CommandLine.Option(names = {"--concurrency"}, description = "并发量,同时请求的最大数量.", defaultValue = "8", order = 102)
        @Override
        public void setConcurrency(int concurrency) {
            super.setConcurrency(concurrency);
        }

        @CommandLine.Option(names = {"--script"}, description = "自定义处理脚本,参照: template.js", order = 103)
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