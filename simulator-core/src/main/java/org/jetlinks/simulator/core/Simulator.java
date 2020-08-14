package org.jetlinks.simulator.core;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import lombok.Getter;
import lombok.Setter;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 网络模拟器
 *
 * @author zhouhao
 * @since 1.0.4
 */
public interface Simulator {

    Mono<SimulatorListener> getListener(String id);

    void registerListener(SimulatorListener listener);

    Mono<State> state();

    Mono<Void> start();

    Mono<Void> shutdown();

    Mono<Session> getSession(String id);

    int totalSession();

    Flux<Session> getSessions();

    default Flux<Session> getSessions(int offset, int total) {
        return getSessions()
                .skip(offset)
                .take(total);
    }

    void log(String text, Object... args);

    Flux<String> handleLog();

    void doOnComplete(Runnable runnable);

    default Disposable delay(Runnable task, long delay) {
        return Schedulers.parallel().schedule(task, delay, TimeUnit.MILLISECONDS);
    }

    default Disposable timer(Runnable task, long period) {
        return Schedulers.parallel().schedulePeriodically(task, period, period, TimeUnit.MILLISECONDS);
    }

    @Getter
    @Setter
    class State {
        //已完成
        private boolean complete;
        //总数
        private long total;
        //当前数量
        private long current;
        //失败数量
        private long failed;
        //连接时间统计
        private Agg aggTime;

        //时间分布
        private Map<Integer, Long> distTime;

        //失败类型计数
        private Map<String, Long> failedTypeCounts;

        @Getter
        @Setter
        public static class Agg {
            int total;
            int max;
            int min;
            int avg;
        }

        @Override
        public String toString() {
            return JSON.toJSONString(this, SerializerFeature.PrettyFormat);
        }
    }

}
