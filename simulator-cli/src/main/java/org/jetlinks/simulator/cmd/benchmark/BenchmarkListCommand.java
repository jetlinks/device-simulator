package org.jetlinks.simulator.cmd.benchmark;

import org.jetlinks.simulator.cmd.ListConnection;
import org.jetlinks.simulator.core.CompositeConnectionManager;
import org.jetlinks.simulator.core.ConnectionManager;
import org.jetlinks.simulator.core.DefaultConnectionManager;
import org.jetlinks.simulator.core.benchmark.Benchmark;
import picocli.CommandLine;

import java.util.stream.Collectors;


@CommandLine.Command(name = "list",
        description = "Search connections",
        headerHeading = "%n")
public class BenchmarkListCommand extends ListConnection {

    @CommandLine.Option(names = {"--name"})
    private String name;

    @Override
    protected ConnectionManager connectionManager() {
        if (name != null) {
            Benchmark benchmark = BenchmarkCommand.allBenchMark.get(name);
            return benchmark == null ? new DefaultConnectionManager() : benchmark.getConnectionManager();
        }
        return new CompositeConnectionManager(BenchmarkCommand.allBenchMark
                                                      .values()
                                                      .stream()
                                                      .map(Benchmark::getConnectionManager)
                                                      .collect(Collectors.toList()));
    }
}
