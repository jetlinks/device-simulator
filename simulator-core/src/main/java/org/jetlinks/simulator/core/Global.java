package org.jetlinks.simulator.core;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

public class Global {

    static final Vertx vertx;

    static {
        vertx = Vertx.vertx(new VertxOptions()
                                    .setPreferNativeTransport(true));
    }

    public static Vertx vertx() {
        return vertx;
    }
}
