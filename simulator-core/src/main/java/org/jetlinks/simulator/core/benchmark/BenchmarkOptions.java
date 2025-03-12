package org.jetlinks.simulator.core.benchmark;

import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.util.Map;

@Getter
@Setter
public class BenchmarkOptions {
    private String name;

    private int index = 0;

    private int size = 1;

    private int concurrency = 100;

    private boolean reconnect = false;

    private File file;

    private Map<String,Object> scriptArgs;
}