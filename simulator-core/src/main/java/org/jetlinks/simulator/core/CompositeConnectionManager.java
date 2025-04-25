package org.jetlinks.simulator.core;

import io.netty.util.concurrent.FastThreadLocal;
import io.netty.util.internal.ThreadLocalRandom;
import lombok.AllArgsConstructor;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@AllArgsConstructor
public class CompositeConnectionManager implements ConnectionManager {
    private final List<ConnectionManager> all;

    @Override
    public long getConnectionSize() {
        long count = 0;
        for (ConnectionManager connectionManager : all) {
            count += connectionManager.getConnectionSize();
        }
        return count;
    }

    @Override
    public Flux<Connection> getConnections() {
        return Flux.fromIterable(all)
                   .flatMap(ConnectionManager::getConnections);
    }

    @Override
    public Mono<Connection> getConnection(String id) {
        return Flux
            .fromIterable(all)
            .flatMap(manager -> manager.getConnection(id))
            .take(1)
            .singleOrEmpty();
    }

    @Override
    public Optional<Connection> getConnectionNow(String id) {
        for (ConnectionManager connectionManager : all) {
            Optional<Connection> opt = connectionManager.getConnectionNow(id);
            if (opt.isPresent()) {
                return opt;
            }
        }
        return Optional.empty();
    }

    @Override
    public Flux<Connection> findConnection(String ql) {
        return Flux.fromIterable(all)
                   .flatMap(c -> c.findConnection(ql));
    }

    @Override
    public Flux<Connection> randomConnection(int size) {
        return Flux.concat(Flux.fromIterable(all)
                               .map(c -> c.randomConnection(size)))
                   .take(size);
    }

    @Override
    public Disposable onConnectionAdd(Consumer<Connection> consumer) {
        Disposable.Composite disposable = Disposables.composite();
        for (ConnectionManager connectionManager : all) {
            disposable.add(connectionManager.onConnectionAdd(consumer));
        }
        return disposable;
    }

    @Override
    public ConnectionManager addConnection(Connection connection) {

        for (ConnectionManager connectionManager : all) {
            connectionManager.addConnection(connection);
            return this;
        }
        return this;
    }

    @Override
    public void dispose() {
        for (ConnectionManager connectionManager : all) {
            connectionManager.dispose();
        }
    }
}
