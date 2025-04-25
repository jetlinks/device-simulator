package org.jetlinks.simulator.core;

import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Operators;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.function.Consumer;

public class ContinuousConnectionFlux extends Flux<Connection> {

    private final ConnectionManager manager;

    public ContinuousConnectionFlux(ConnectionManager manager) {
        this.manager = manager;
    }

    @Override
    public void subscribe(@Nonnull CoreSubscriber<? super Connection> actual) {
        ContinuousConnectionSubscriber subscriber = new ContinuousConnectionSubscriber(manager, actual);
        actual.onSubscribe(subscriber);
    }


    static class ContinuousConnectionSubscriber implements Subscription, Consumer<Connection> {
        private final ConnectionManager manager;
        private final CoreSubscriber<? super Connection> actual;
        private static final AtomicLongFieldUpdater<ContinuousConnectionSubscriber> REQUESTED
            = AtomicLongFieldUpdater.newUpdater(ContinuousConnectionSubscriber.class, "requested");
        private volatile long requested;

        private static final AtomicIntegerFieldUpdater<ContinuousConnectionSubscriber> WIP
            = AtomicIntegerFieldUpdater.newUpdater(ContinuousConnectionSubscriber.class, "wip");
        private volatile int wip;

        private final Disposable.Swap disposable = Disposables.swap();

        ContinuousConnectionSubscriber(ConnectionManager manager, CoreSubscriber<? super Connection> actual) {
            this.manager = manager;
            this.actual = actual;
            this.disposable.update(manager.onConnectionAdd(this));
        }

        @Override
        public void request(long n) {
            Operators.addCap(REQUESTED, this, n);
            Schedulers
                .parallel()
                .schedule(this::drain);
        }


        public void drain() {
            if (WIP.getAndIncrement(this) != 0) {
                return;
            }
            W:
            do {
                if (this.disposable.isDisposed()) {
                    return;
                }
                long r;
                while ((r = REQUESTED.get(this)) > 0 && !this.disposable.isDisposed()) {
                    List<Connection> c = manager.randomConnectionNow((int) r);
                    // 没有链接,放弃请求.
                    if (c.isEmpty()) {
                        continue W;
                    }
                    long count = 0;
                    for (Connection connection : c) {
                        if (this.disposable.isDisposed()) {
                            return;
                        }
                        if (connection.isAlive()) {
                            count++;
                            actual.onNext(connection);
                        }
                    }
                    // 没有存活的链接,放弃请求.
                    if (count == 0) {
                        continue W;
                    }
                    REQUESTED.getAndAdd(this, -count);
                }
            } while (WIP.decrementAndGet(this) != 0);
        }

        @Override
        public void cancel() {
            disposable.dispose();
        }

        @Override
        public void accept(Connection connection) {
            drain();
        }
    }
}
