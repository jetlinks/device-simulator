package org.jetlinks.simulator.cmd;

import org.jetlinks.simulator.core.network.AddressManager;

import java.net.InetAddress;
import java.util.Iterator;

public class NetworkInterfaceCompleter implements Iterable<String> {
    @Override
    public Iterator<String> iterator() {
        return AddressManager
                .global()
                .getAliveLocalAddresses()
                .stream()
                .map(InetAddress::getHostAddress)
                .iterator();
    }
}
