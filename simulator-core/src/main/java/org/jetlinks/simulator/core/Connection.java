package org.jetlinks.simulator.core;

import com.google.common.collect.Maps;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.jetlinks.simulator.core.network.NetworkType;
import org.jetlinks.simulator.core.network.NetworkUtils;
import org.springframework.http.HttpStatus;
import reactor.core.Disposable;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

public interface Connection extends Disposable {


    String ATTR_STATE = "state";
    String ATTR_SENT = "sent";
    String ATTR_RECEIVE = "received";

    String ATTR_SENT_BYTES = "sent_bytes";

    String ATTR_RECEIVE_BYTES = "received_bytes";

    static String statusCountAttr(String status) {
        return "status_" + status;
    }

    default Map<String, Integer> statusCount() {
        return Maps.transformValues(Maps.filterKeys(attributes(), k -> k.startsWith("status_")),
                                    v -> CastUtils.castNumber(v).intValue());
    }

    String getId();

    NetworkType getType();

    boolean isAlive();

    State state();

    long getConnectTime();

    Disposable onStateChange(BiConsumer<State, State> listener);

    Optional<Object> attribute(String key);

    void attribute(String key, Object value);

    void attributes(Map<String, Object> attributes);

    default void set(String key, Object value) {
        attribute(key, value);
    }

    default Object get(String key) {
        return attribute(key).orElse(null);
    }

    Map<String, Object> attributes();

    default <T> T unwrap(Class<T> type) {
        return type.cast(this);
    }

    default boolean isWrapFor(Class<?> type) {
        return type.isInstance(this);
    }

    default ByteBuf newBuffer() {
        return Unpooled.buffer();
    }

    default String toHex(Object data) {
        return ByteBufUtil.hexDump(NetworkUtils.castToByteBuf(data));
    }


    enum State {
        connecting,
        connected,
        closed,
        error;
    }

}
