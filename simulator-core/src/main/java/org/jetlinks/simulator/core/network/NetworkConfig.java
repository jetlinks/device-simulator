package org.jetlinks.simulator.core.network;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class NetworkConfig {
    private String id;

    private NetworkType type;

    private int maxRetry = 5;

    private Map<String, Object> configs;
}
