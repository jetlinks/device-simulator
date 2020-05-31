package org.jetlinks.simulator.core.network;

import java.net.InetSocketAddress;

public interface Address {

    InetSocketAddress getAddress();

    void release();
}
