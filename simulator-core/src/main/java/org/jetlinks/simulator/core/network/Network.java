package org.jetlinks.simulator.core.network;

import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;

import java.net.InetSocketAddress;

public interface Network {

    String getId();

    NetworkType getType();

    InetSocketAddress getLocalAddress();

    boolean isAlive();

    State state();

    Flux<Tuple2<State, State>> onStateChange();

    NetworkConfig getConfig();

    void close();

    enum State {
        connection,
        connected,
        error;
    }

}
