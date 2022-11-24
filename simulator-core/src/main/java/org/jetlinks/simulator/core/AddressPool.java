package org.jetlinks.simulator.core;

import java.util.List;
import java.util.Optional;

public interface AddressPool {

    List<String> getAllAddress();

    Optional<String> take(List<String> addresses);

    void release(String address);

}
