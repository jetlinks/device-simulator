package org.jetlinks.simulator.strategy;

import lombok.AllArgsConstructor;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.simulator.ClientSession;
import org.jetlinks.simulator.SimulationStrategy;
import reactor.core.publisher.Flux;

import java.util.function.Function;
import java.util.function.Supplier;

@AllArgsConstructor
public class SendMessageStrategy implements SimulationStrategy {

    private Supplier<Flux<ClientSession>> sessionSupplier;

    private Function<ClientSession, EncodedMessage> messageFactory;

    @Override
    public void start() {

    }

    @Override
    public void execute() {
        sessionSupplier.get()
                .flatMap(session -> session.send(messageFactory.apply(session)))
                .subscribe();
    }

    @Override
    public void stop() {

    }
}
