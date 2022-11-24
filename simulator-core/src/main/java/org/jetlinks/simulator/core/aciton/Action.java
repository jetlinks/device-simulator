package org.jetlinks.simulator.core.aciton;

import org.jetlinks.simulator.core.ConnectionManager;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;

public interface Action {

    String getId();

    ActionConfig getConfig();

    void reload(ActionConfig config);

    void start(ConnectionManager connectionManager);

    State getState();

    Flux<Tuple2<State, State>> onStateChange();

    enum State {
        running,
        await,
        complete,
        error
    }
}
