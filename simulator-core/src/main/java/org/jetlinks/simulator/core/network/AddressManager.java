package org.jetlinks.simulator.core.network;


import java.net.InetAddress;
import java.util.List;

public interface AddressManager {

    Address getAddress(int port);

    List<InetAddress> getAliveLocalAddresses();
}
