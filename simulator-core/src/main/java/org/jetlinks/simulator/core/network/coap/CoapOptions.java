package org.jetlinks.simulator.core.network.coap;

import lombok.Getter;
import lombok.Setter;

import java.net.InetSocketAddress;
import java.util.Map;

@Getter
@Setter
public class CoapOptions {
    private String id;

    private String basePath;

    private Map<String,String> options;

    private String bindAddress;
}
