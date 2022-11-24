package org.jetlinks.simulator.core;

import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.TrustOptions;
import reactor.core.publisher.Mono;

public interface KeyManager {

    Mono<TrustKeyCertOptions> getKeyAndTrust(String id);

    interface TrustKeyCertOptions extends TrustOptions, KeyCertOptions {

        @Override
        TrustKeyCertOptions copy();

        @Override
        TrustKeyCertOptions clone();
    }
}
