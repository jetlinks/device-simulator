package org.jetlinks.simulator.core.network;

import io.netty.util.AbstractReferenceCounted;
import io.netty.util.ReferenceCounted;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
class DefaultAddressManager implements AddressManager {

    static DefaultAddressManager global = new DefaultAddressManager();

    private static final List<InetAddressRef> addressRefs = new ArrayList<>();

    private static final boolean disableCheckAddressUsable = Boolean.getBoolean("address.check.disabled");

    static Address allAddress;

    static {
        try {
            int maxPorts = Integer.getInteger("simulator.max-ports", 50000);
            String networkInterfaces = System.getProperty("simulator.network-interfaces", ".*");

            Enumeration<NetworkInterface> inf = NetworkInterface.getNetworkInterfaces();
            addAddressRef(networkInterfaces, maxPorts, inf);

            log.debug("load network interfaces: {}", addressRefs);
            allAddress = new SimpleAddress(Inet4Address.getByAddress(new byte[]{
                    0x00, 0x00, 0x00, 0x00
            }));
        } catch (Throwable e) {
            log.error("load network interfaces error loaded: {}", addressRefs, e);
        }

    }

    private static void addAddressRef(String networkInterfaces, int maxPorts, Enumeration<NetworkInterface> inf) throws SocketException, InterruptedException {
        while (inf.hasMoreElements()) {
            NetworkInterface it = inf.nextElement();
            if (StringUtils.hasText(networkInterfaces)) {
                if (!it.getName().matches(networkInterfaces)) {
                    Enumeration<NetworkInterface> sub = it.getSubInterfaces();
                    if (sub.hasMoreElements()) {

                        addAddressRef(networkInterfaces, maxPorts, sub);
                    }
                    continue;
                }
            }
            if (!it.isUp()) {
                break;
            }
//            Enumeration<InetAddress> addr = it.getInetAddresses();
//            while (addr.hasMoreElements()) {
            it.getInterfaceAddresses()
              .forEach(addr -> {
                  InetAddress address = addr.getAddress();
                  if (address instanceof Inet4Address
                          && !address.isLoopbackAddress()
                          && checkAddressUsable(address)) {
                      addressRefs.add(new InetAddressRef(it, address, maxPorts));
                  }
              });

        }
//        }
    }

    private static boolean checkAddressUsable(InetAddress address) {
        if (disableCheckAddressUsable) {
            return true;
        }
        String host = System.getProperty("address.validate.host", "www.baidu.com");
        try (Socket socket = new Socket()) {
            socket.bind(new InetSocketAddress(address, 0));

            socket.connect(new InetSocketAddress(
                    host,
                    Integer.getInteger("address.validate.port", 80)), 2000);
            return true;
        } catch (Throwable err) {
            log.debug("it地址连接{}失败", host);
            return false;
        }
    }

    @Override
    public Address takeAddress() {

        for (InetAddressRef addressRef : addressRefs) {
            if (addressRef.isAlive()) {
                return new AddressInfo(addressRef);
            }
        }
        return allAddress;
    }

    @Override
    public Address takeAddress(String networkInterface) {
        if (networkInterface == null) {
            return takeAddress();
        }
        if ("0.0.0.0".equals(networkInterface)) {
            return allAddress;
        }
        for (InetAddressRef addressRef : addressRefs) {
            if (networkInterface.equals(addressRef.getNetworkInterface().getName())
                    || networkInterface.equals(addressRef.getNetworkInterface().getDisplayName())
                    || networkInterface.equals(addressRef.getAddress().getHostAddress())) {
                if (addressRef.isAlive()) {
                    return new AddressInfo(addressRef);
                } else {
                    throw new IllegalStateException("Too many open ports!");
                }
            }
        }
        throw new IllegalStateException("Unknown network interface:" + networkInterface + ". all interfaces:" + addressRefs);
    }

    @Override
    public List<InetAddress> getAliveLocalAddresses() {
        return addressRefs
                .stream()
                .filter(InetAddressRef::isAlive)
                .map(InetAddressRef::getAddress)
                .collect(Collectors.toList());
    }

    @AllArgsConstructor
    protected static class SimpleAddress implements Address {
        private final InetAddress address;

        @Override
        public InetAddress getAddress() {
            return address;
        }

        @Override
        public void release() {

        }
    }

    protected static class AddressInfo implements Address {
        private final InetAddressRef ref;
        private boolean released;

        protected AddressInfo(InetAddressRef ref) {
            this.ref = ref;
            // ref --
            this.ref.release();
        }

        @Override
        public InetAddress getAddress() {
            return ref.address;
        }

        @Override
        public synchronized void release() {
            if (released) {
                return;
            }
            released = true;
            // ref ++
            ref.retain();
        }
    }

    @Getter
    @AllArgsConstructor
    private static class InetAddressRef extends AbstractReferenceCounted {

        private final NetworkInterface networkInterface;
        @Getter
        private final InetAddress address;

        public InetAddressRef(NetworkInterface networkInterface, InetAddress address, int count) {
            this.networkInterface = networkInterface;
            this.address = address;
            retain(count);
        }

        boolean isAlive() {
            return refCnt() > 1;
        }


        @Override
        public ReferenceCounted touch(Object hint) {
            return this;
        }


        @Override
        protected void deallocate() {
            throw new IllegalStateException(address + " Too many open ports!");
        }

        @Override
        public String toString() {
            return address.getHostAddress() + "(" + refCnt() + ")";
        }
    }
}
