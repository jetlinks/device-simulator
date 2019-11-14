package org.jetlinks.simulator;

import org.jetlinks.core.message.codec.EncodedMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ClientSession {

    String getDeviceId();

    Mono<Boolean> send(EncodedMessage message);

    Flux<? extends EncodedMessage> handleMessage();

    void close();

    boolean isClosed();
}
