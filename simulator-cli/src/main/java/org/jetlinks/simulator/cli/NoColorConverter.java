package org.jetlinks.simulator.cli;

import ch.qos.logback.core.pattern.CompositeConverter;

public class NoColorConverter<E> extends CompositeConverter<E> {

    @Override
    protected String transform(E event, String in) {
        return in;
    }
}