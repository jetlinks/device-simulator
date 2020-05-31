package org.jetlinks.simulator.core.aciton;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class ActionConfig {

    private String name;

    private Map<String, Object> configs;
}
