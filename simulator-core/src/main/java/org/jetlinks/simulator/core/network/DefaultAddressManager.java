package org.jetlinks.simulator.core.network;

import io.netty.util.AbstractReferenceCounted;
import io.netty.util.ReferenceCounted;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
class DefaultAddressManager implements AddressManager {

    static DefaultAddressManager global = new DefaultAddressManager();

    private static final List<InetAddressRef> addressRefs = new ArrayList<>();

    static {
        try {
            int maxPorts = Integer.getInteger("simulator.max-ports", 50000);
            String networkInterfaces = System.getProperty("simulator.network-interfaces", ".*");

            Enumeration<NetworkInterface> inf = NetworkInterface.getNetworkInterfaces();
            while (inf.hasMoreElements()) {
                NetworkInterface it = inf.nextElement();
                if (StringUtils.hasText(networkInterfaces)) {
                    if (!it.getName().matches(networkInterfaces)) {
                        continue;
                    }
                }
                Enumeration<InetAddress> addr = it.getInetAddresses();
                while (addr.hasMoreElements()) {
                    InetAddress address = addr.nextElement();
                    if (address instanceof Inet4Address) {
                        addressRefs.add(new InetAddressRef(address, maxPorts));
                    }
                }
            }
            log.debug("load network interfaces: {}", addressRefs);
        } catch (SocketException e) {
            log.error("load network interfaces error loaded: {}", addressRefs, e);
        }

    }

    @Override
    public Address takeAddress() {

        for (InetAddressRef addressRef : addressRefs) {
            if (addressRef.isAlive()) {
                return new AddressInfo(addressRef);
            }
        }
        throw new IllegalStateException("Too many open ports!");
    }

    @Override
    public List<InetAddress> getAliveLocalAddresses() {
        return addressRefs
                .stream()
                .filter(InetAddressRef::isAlive)
                .map(InetAddressRef::getAddress)
                .collect(Collectors.toList());
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
        @Getter
        private final InetAddress address;

        public InetAddressRef(InetAddress address, int count) {
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
            return address + "(" + refCnt() + ")";
        }
    }
}
