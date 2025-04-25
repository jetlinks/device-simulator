package org.jetlinks.simulator.core;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.jetlinks.simulator.core.network.AbstractConnection;
import org.jetlinks.simulator.core.network.NetworkType;
import org.junit.Ignore;
import org.junit.Test;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static org.junit.Assert.*;

@Ignore
public class ContinuousConnectionFluxTest {


    @Test
    @SneakyThrows
    public void test() {
        ConnectionManager manager = new DefaultConnectionManager();

        for (int i = 0; i < 100; i++) {
            manager.addConnection(new TestConnection("id_" + i));
        }

        ContinuousConnectionFlux flux = new ContinuousConnectionFlux(manager);

        Disposable disposable = flux
            .flatMap(c -> Mono
                .delay(Duration.ofMillis(1000))
                .thenReturn(c), 6, 32)
            .subscribe(System.out::println);

//        Thread.sleep(1000);
//        disposable.dispose();
        System.in.read();
    }

    @AllArgsConstructor
    static class TestConnection extends AbstractConnection {
        private final String id;

        @Override
        public String getId() {
            return id;
        }

        @Override
        public NetworkType getType() {
            return NetworkType.tcp_client;
        }

        @Override
        public boolean isAlive() {
            return true;
        }

        @Override
        public String toString() {
            return id;
        }
    }

}