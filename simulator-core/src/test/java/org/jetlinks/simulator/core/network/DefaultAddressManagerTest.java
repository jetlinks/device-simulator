package org.jetlinks.simulator.core.network;

import static org.junit.Assert.*;

public class DefaultAddressManagerTest {

    public static void main(String[] args) {
        DefaultAddressManager.global.getAliveLocalAddresses().forEach(System.out::println);
    }
}