package org.jetlinks.simulator.core;

import lombok.SneakyThrows;
import org.apache.commons.collections.CollectionUtils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultAddressPool implements AddressPool {

    Map<String, AtomicInteger> usedIp = new ConcurrentHashMap<>();

    @Override
    @SneakyThrows
    public List<String> getAllAddress() {
        List<String> address = new ArrayList<>();
        Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
        while (enumeration.hasMoreElements()) {
            Enumeration<InetAddress> addrs = enumeration.nextElement().getInetAddresses();
            while (addrs.hasMoreElements()) {
                InetAddress ip = addrs.nextElement();
                if (ip instanceof Inet4Address) {
                    String addr = ip.getHostAddress();
                    if (!"127.0.0.1".equals(addr)) {
                        address.add(addr);
                    }
                }
            }
        }
        return address;
    }

    @Override
    public Optional<String> take(List<String> addresses) {
        if (CollectionUtils.isEmpty(addresses)) {
            addresses = getAllAddress();
        }
        for (String address : addresses) {
            AtomicInteger counter = usedIp.computeIfAbsent(address, ignore -> new AtomicInteger(1024));
            if (counter.get() < 65500) {
                counter.incrementAndGet();
                return Optional.of(address);
            }
        }
        return Optional.empty();
    }

    @Override
    public void release(String address) {
        Optional
                .ofNullable(usedIp.get(address))
                .ifPresent(AtomicInteger::decrementAndGet);
    }
}
