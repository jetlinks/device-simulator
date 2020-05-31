package org.jetlinks.simulator.core.aciton;

import org.reactivestreams.Publisher;

public interface ExecutableAction<IN, OUT> {

    Publisher<? extends OUT> execute(Publisher<? extends IN> input);

}
