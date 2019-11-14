package org.jetlinks.simulator.manage.connect;

import org.jetlinks.simulator.enums.ClientType;

public interface ClientConnectProvider {

    ClientType getClientType();

    void createConnect(String username, String password);
}
