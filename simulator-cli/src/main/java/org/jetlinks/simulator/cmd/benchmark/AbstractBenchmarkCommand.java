package org.jetlinks.simulator.cmd.benchmark;

import org.jetlinks.simulator.cmd.AbstractCommand;
import org.jetlinks.simulator.core.Connection;
import org.jetlinks.simulator.core.DefaultConnectionManager;
import org.jetlinks.simulator.core.benchmark.Benchmark;
import org.jetlinks.simulator.core.benchmark.ConnectCreateContext;
import picocli.CommandLine;
import reactor.core.publisher.Mono;

@CommandLine.Command(name = "benchmark", hidden = true)
public abstract class AbstractBenchmarkCommand extends AbstractCommand implements Runnable {
    @CommandLine.Mixin
    protected BenchmarkCommand.Options options;


    protected String getDefaultName() {
        return spec.name();
    }

    protected abstract Mono<? extends Connection> createConnection(ConnectCreateContext context);

    @Override
    public final void run() {
        String name = options.getName() == null ? getDefaultName() : options.getName();

//        printf("start benchmark %s, index %d,size %d.args:%s %n ",
//               name,
//               options.getIndex(),
//               options.getSize(),
//               options.getScriptArgs()==null?"":String.valueOf(options.getScriptArgs()));
        DefaultConnectionManager connectionManager = new DefaultConnectionManager();

        Benchmark benchmark = Benchmark.create(
                name,
                options,
                connectionManager,
                this::createConnection
        );

        BenchmarkCommand.addBenchmark(benchmark);
        benchmark.start();
        benchmark.doOnDispose(connectionManager);
        main().getCommandLine().execute("benchmark", "stats", "--name=" + name);
    }
}
