package org.jetlinks.simulator.core.benchmark;

import com.alibaba.fastjson.JSON;
import org.hswebframework.web.utils.DigestUtils;
import org.jetlinks.simulator.core.script.Script;
import org.jetlinks.simulator.core.script.Scripts;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

public interface BenchmarkHelper {

      Object require(String location);

    default long now() {
        return System.currentTimeMillis();
    }

    default String toJson(Object obj) {
        return JSON.toJSONString(Benchmark.scriptFactory.convertToJavaType(obj));
    }

    default String md5(Object obj) {
        return DigestUtils.md5Hex(String.valueOf(Benchmark.scriptFactory.convertToJavaType(obj)));
    }

    default float randomFloat(float from, float to) {
        return (float) ThreadLocalRandom.current().nextDouble(from, to);
    }

    default int randomInt(int from, int to) {
        return ThreadLocalRandom.current().nextInt(from, to);
    }

}
