package org.jetlinks.simulator.core.network;

import org.jetlinks.simulator.core.Connection;
import reactor.core.Disposable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public abstract class AbstractConnection implements Connection {

    private volatile State state = State.connecting;
    private List<BiConsumer<State, State>> listener;
    private final long connectTime = System.currentTimeMillis();
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    @Override
    public Optional<Object> attribute(String key) {
        return Optional.ofNullable(attributes.get(key));
    }

    public Object attr(String key) {
        return this.attribute(key).orElse(null);
    }

    public void attribute(String key, Object value) {
        this.attributes.put(key, value);
    }

    public void attributes(Map<String, Object> attributes) {
        this.attributes.putAll(attributes);
    }

    public Map<String, Object> attributes() {
        return Collections.unmodifiableMap(attributes);
    }

    @Override
    public final State state() {
        return state;
    }

    public long getConnectTime() {
        return connectTime;
    }

    @SuppressWarnings("all")
    protected <T> T computeAttr(String key, T value, BiFunction<T, T, T> computer) {
        return (T) attributes.compute(key, (ignore, old) -> {

            return old == null ? value : computer.apply(value, (T) old);
        });
    }

    protected void incr(String key) {
        computeAttr(key, 1, Math::addExact);
    }

    protected void error(Throwable throwable) {
        incr(Connection.statusCountAttr("ERROR"));
    }

    protected void sent(int bytesLength) {
        computeAttr(ATTR_SENT, 1, Math::addExact);
        computeAttr(ATTR_SENT_BYTES, bytesLength, Math::addExact);
    }

    protected void received(int bytesLength) {
        computeAttr(ATTR_RECEIVE, 1, Math::addExact);
        computeAttr(ATTR_RECEIVE_BYTES, bytesLength, Math::addExact);
    }

    protected void changeState(State state) {
        attribute(ATTR_STATE, state.name());
        State old = this.state;
        this.state = state;
        if (old != this.state && listener != null) {
            for (BiConsumer<State, State> consumer : listener) {
                consumer.accept(old, this.state);
            }
        }
    }

    @Override
    public final synchronized Disposable onStateChange(BiConsumer<State, State> listener) {
        if (this.listener == null) {
            this.listener = new ArrayList<>();
        }
        this.listener.add(listener);
        return () -> {
            if (this.listener == null) {
                return;
            }
            this.listener.remove(listener);
        };
    }

    protected void doDisposed() {

    }


    @Override
    public final void dispose() {
        changeState(State.closed);
        doDisposed();
    }
}
