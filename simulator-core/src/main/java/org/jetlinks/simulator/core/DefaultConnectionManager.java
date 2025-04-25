package org.jetlinks.simulator.core;

import org.jetlinks.reactor.ql.ReactorQL;
import org.jetlinks.reactor.ql.ReactorQLContext;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.StringUtils;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class DefaultConnectionManager implements ConnectionManager {

    static ConnectionManager global = new DefaultConnectionManager();

    private static final Map<String, ReactorQL> queryCache = new ConcurrentReferenceHashMap<>();

    private final Map<String, Connection> connections = new ConcurrentHashMap<>(10240);
    private final List<Connection> connectionList = new CopyOnWriteArrayList<>();

    private final List<Consumer<Connection>> listener = new CopyOnWriteArrayList<>();

    @Override
    public void dispose() {
        connections.values().forEach(Disposable::dispose);
    }

    @Override
    public long getConnectionSize() {
        return connections.size();
    }

    @Override
    public Flux<Connection> getConnections() {
        return Flux.fromIterable(connections.values());
    }

    @Override
    public Mono<Connection> getConnection(String id) {
        return Mono.justOrEmpty(connections.get(id));
    }

    @Override
    public Optional<Connection> getConnectionNow(String id) {
        return Optional.ofNullable(connections.get(id));
    }

    private ReactorQL createQl(String ql) {
        return ReactorQL
            .builder()
            .sql("select id from dual where ", ql)
            .build();
    }

    @Override
    public Flux<Connection> findConnection(String ql) {
        if (!StringUtils.hasText(ql)) {
            return getConnections();
        }
        return Flux.defer(() -> queryCache
            .computeIfAbsent(ql, this::createQl)
            .start(ReactorQLContext.ofDatasource(ignore -> getConnections().map(Connection::attributes)))
            .mapNotNull(record -> {
                String id = (String) record.asMap().get("id");

                return id == null ? null : connections.get(id);
            }));
    }


    @Override
    public Flux<Connection> randomConnection(int size) {
        return Flux.fromIterable(randomConnectionNow(size));
    }

    @Override
    public List<Connection> randomConnectionNow(int size) {
        int total = connections.size();
        if (total <= size) {
            return connectionList;
        }
        int sub = total - size;
        int skip = ThreadLocalRandom.current().nextInt(0, sub);

        return connectionList.subList(skip, size + skip);
    }

    @Override
    public Disposable onConnectionAdd(Consumer<Connection> consumer) {
        listener.add(consumer);
        return () -> listener.remove(consumer);
    }

    @Override
    public ConnectionManager addConnection(Connection connection) {
        if (!connection.isAlive()) {
            return this;
        }
        connection.attribute("id", connection.getId());
        connection.attribute("type", connection.getType().name());
        connection.attribute("connectTime", connection.getConnectTime());
        connectionList.add(connection);
        Connection conn = connections.put(connection.getId(), connection);
        if (conn != null && conn != connection) {
            conn.dispose();
            connectionList.remove(conn);
        }
        for (Consumer<Connection> consumer : listener) {
            consumer.accept(conn);
        }
        return this;
    }
}
