package org.jetlinks.simulator.core;

import org.jetlinks.simulator.core.aciton.Action;
import org.jetlinks.simulator.core.network.Network;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

public interface Simulator extends Lifecycle{

    long getConnectionSize();

    Flux<Network> getConnections();

    Mono<Network> getConnection(String id);

    /**
     * <pre>
     *      type = 'mqtt' and clientId like 'test%' limit 0,10
     *  </pre>
     *
     * @param ql 查询表达式
     * @return 查询结果
     */
    Flux<Network> findConnection(String ql);

    Simulator addConnection(Network network);

    List<Action> getActions();

    Optional<Action> getAction(String id);

    Simulator addAction(Action action);

    Simulator removeAction(String action);

}
