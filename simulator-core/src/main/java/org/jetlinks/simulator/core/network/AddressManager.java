package org.jetlinks.simulator.core.network;


import java.net.InetAddress;
import java.util.List;

public interface AddressManager {

    static AddressManager global() {
        return DefaultAddressManager.global;
    }

    Address takeAddress();

    Address takeAddress(String networkInterface);

    List<InetAddress> getAliveLocalAddresses();
}
