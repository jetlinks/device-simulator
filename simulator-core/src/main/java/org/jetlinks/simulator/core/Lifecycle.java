package org.jetlinks.simulator.core;

public interface Lifecycle {

    void start();

    void pause();

    void resume();

    void shutdown();

}
