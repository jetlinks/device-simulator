package org.jetlinks.simulator;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Simulator {

    List<SimulationStrategy> strategies;

    private Map<String,ClientSession> sessions = new ConcurrentHashMap<>();



}
