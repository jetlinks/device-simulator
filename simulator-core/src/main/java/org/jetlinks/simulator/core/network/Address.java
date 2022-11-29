package org.jetlinks.simulator.core.network;


import java.net.InetAddress;

public interface Address {

    InetAddress getAddress();

    void release();
}
