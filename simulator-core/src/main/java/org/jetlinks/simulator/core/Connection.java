package org.jetlinks.simulator.core;

import org.jetlinks.simulator.core.network.NetworkType;
import reactor.core.Disposable;
import reactor.util.function.Tuple2;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface Connection extends Disposable {


    String ATTR_STATE = "state";
    String ATTR_SENT = "sent";
    String ATTR_RECEIVE = "received";

    String ATTR_SENT_BYTES = "sent_bytes";

    String ATTR_RECEIVE_BYTES = "received_bytes";


    String getId();

    NetworkType getType();

    boolean isAlive();

    State state();

    long getConnectTime();

    Disposable onStateChange(BiConsumer<State, State> listener);

    Optional<Object> attribute(String key);

    void attribute(String key, Object value);

    void attributes(Map<String, Object> attributes);

    Map<String, Object> attributes();

    default <T> T unwrap(Class<T> type) {
        return type.cast(this);
    }

    default boolean isWrapFor(Class<?> type) {
        return type.isInstance(this);
    }

    enum State {
        connection,
        connected,
        closed,
        error;
    }

}
