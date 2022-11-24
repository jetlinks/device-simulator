package org.jetlinks.simulator.core;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;

public interface ConnectionManager extends Disposable {

    static ConnectionManager global() {
        return DefaultConnectionManager.global;
    }

    long getConnectionSize();

    Flux<Connection> getConnections();

    Mono<Connection> getConnection(String id);

    Optional<Connection> getConnectionNow(String id);

    /**
     * <pre>
     *      type = 'mqtt' and clientId like 'test%' limit 0,10
     *  </pre>
     *
     * @param ql 查询表达式
     * @return 查询结果
     */
    Flux<Connection> findConnection(String ql);

    Flux<Connection> randomConnection(int size);

    ConnectionManager addConnection(Connection connection);

}
