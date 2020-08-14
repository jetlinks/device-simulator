package org.jetlinks.simulator.core.listener;

import lombok.SneakyThrows;
import org.jetlinks.simulator.core.SimulatorConfig;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class JSR223ListenerTest {

    @Test
    @SneakyThrows
    public void test(){
        String script = StreamUtils.copyToString(new ClassPathResource("simulator.js").getInputStream(), StandardCharsets.UTF_8);

        JSR223Listener listener=new JSR223Listener("test","js",script);

        listener.init(new TestSimulator(null,null,null));

        assertFalse(listener.beforeListener.isEmpty());
        assertFalse(listener.afterListener.isEmpty());

    }

}