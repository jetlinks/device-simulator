package org.jetlinks.simulator.core;

import org.jetlinks.core.message.codec.EncodedMessage;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public interface Session {

    String getId();

    int getIndex();

    long getCreateTime();

    long getConnectTime();

    default long getConnectUseTime() {
        return getConnectTime() - getCreateTime();
    }

    boolean isConnected();

    Disposable onConnected(Runnable callback);

    Disposable onDisconnected(Runnable callback);

    Disposable onError(Consumer<Throwable> callback);

    Disposable onUpstream(Consumer<EncodedMessage> msg);

    Disposable onDownstream(Consumer<EncodedMessage> msg);

    void close();

    Optional<Throwable> lastError();

    Mono<Void> connect();
}
