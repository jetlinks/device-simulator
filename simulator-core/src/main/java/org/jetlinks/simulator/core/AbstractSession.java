package org.jetlinks.simulator.core;

import com.alibaba.fastjson.JSON;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.codec.EncodedMessage;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Getter
public abstract class AbstractSession implements Session {

    private final String id;

    private final int index;

    private final long createTime = System.currentTimeMillis();

    private long connectTime;

    private boolean connected;

    private Throwable lastError;

    public AbstractSession(String id, int index) {
        this.id = id;
        this.index = index;
    }

    private final List<Runnable> connectedListener = new CopyOnWriteArrayList<>();
    private final List<Runnable> disconnectListener = new CopyOnWriteArrayList<>();
    private final List<Consumer<Throwable>> errorListener = new CopyOnWriteArrayList<>();
    private final List<Consumer<EncodedMessage>> upstreamListener = new CopyOnWriteArrayList<>();
    private final List<Consumer<EncodedMessage>> downstreamListener = new CopyOnWriteArrayList<>();


    @Override
    public final Disposable onConnected(Runnable callback) {
        connectedListener.add(callback);
        if (connected) {
            callback.run();
        }
        return () -> connectedListener.remove(callback);
    }

    @Override
    public final Disposable onDisconnected(Runnable callback) {
        disconnectListener.add(callback);
        return () -> connectedListener.remove(callback);
    }

    @Override
    public final Disposable onError(Consumer<Throwable> callback) {
        errorListener.add(callback);
        return () -> errorListener.remove(callback);
    }

    @Override
    public final Disposable onUpstream(Consumer<EncodedMessage> msg) {
        upstreamListener.add(msg);
        return () -> upstreamListener.remove(msg);
    }

    @Override
    public final Disposable onDownstream(Consumer<EncodedMessage> msg) {
        downstreamListener.add(msg);
        return () -> downstreamListener.remove(msg);
    }

    protected void upstream(EncodedMessage msg) {
        upstreamListener.forEach(consumer -> consumer.accept(msg));
    }

    protected void downstream(EncodedMessage msg) {
        downstreamListener.forEach(consumer -> consumer.accept(msg));
    }

    protected ByteBuf encodePayload(Object obj) {
        if (obj instanceof String) {
            String strVal = (String) obj;
            if (strVal.startsWith("0x")) {
                obj = ByteBufUtil.decodeHexDump(strVal, 2, strVal.length() - 2);
            } else {
                obj = strVal.getBytes();
            }
        }
        if (obj instanceof byte[]) {
            return Unpooled.wrappedBuffer((byte[]) obj);
        }
        if (obj instanceof ByteBuf) {
            return ((ByteBuf) obj);
        }

        return Unpooled.wrappedBuffer(JSON.toJSONBytes(obj));

    }

    @Override
    public abstract void close();

    protected void disconnected() {
        if (connected) {
            disconnectListener.forEach(Runnable::run);
        }
        connected = false;
    }

    protected void connected() {
        connected = true;
        connectTime = System.currentTimeMillis();
        connectedListener.forEach(Runnable::run);
    }

    protected void error(Throwable error) {
        lastError = error;
        errorListener.forEach(consumer -> consumer.accept(error));
    }

    @Override
    public final Optional<Throwable> lastError() {
        return Optional.ofNullable(lastError);
    }

    protected abstract Mono<Void> doConnect();

    @Override
    public final Mono<Void> connect() {
        if (connected) {
            return Mono.empty();
        }
        return doConnect()
                .doOnSuccess(v -> connected())
                .doOnError(this::error);
    }
}
