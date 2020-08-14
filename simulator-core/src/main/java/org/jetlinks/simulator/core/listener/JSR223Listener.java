package org.jetlinks.simulator.core.listener;

import lombok.SneakyThrows;
import org.hswebframework.expands.script.engine.DynamicScriptEngine;
import org.hswebframework.expands.script.engine.DynamicScriptEngineFactory;
import org.jetlinks.simulator.core.Session;
import org.jetlinks.simulator.core.Simulator;
import org.jetlinks.simulator.core.SimulatorListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class JSR223Listener implements SimulatorListener {

    private final String id;

    final List<Consumer<Session>> beforeListener = new CopyOnWriteArrayList<>();
    final List<Consumer<Session>> afterListener = new CopyOnWriteArrayList<>();
    private final DynamicScriptEngine engine;

    @SneakyThrows
    public JSR223Listener(String id, String lang, String script) {
        this.id = id;
        engine = DynamicScriptEngineFactory.getEngine(lang);
        engine.compile("jsr223-listener:" + id, script);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getType() {
        return "jsr223";
    }

    @Override
    public boolean supported(Simulator simulator) {
        return true;
    }

    @Override
    @SneakyThrows
    public void init(Simulator simulator) {
        Map<String, Object> ctx = new HashMap<>();

        ctx.put("listener", this);
        ctx.put("simulator", simulator);

        engine.execute("jsr223-listener:" + id, ctx).getIfSuccess();
    }

    public void onBefore(Consumer<Session> session) {
        this.beforeListener.add(session);
    }

    public void onAfter(Consumer<Session> session) {
        this.afterListener.add(session);
    }

    @Override
    public void before(Session session) {
        for (Consumer<Session> listener : beforeListener) {
            listener.accept(session);
        }
    }

    @Override
    public void after(Session session) {
        for (Consumer<Session> listener : afterListener) {
            listener.accept(session);
        }
    }

    @Override
    public void shutdown() {
        beforeListener.clear();
        afterListener.clear();
        engine.remove("jsr223-listener:" + id);
    }
}
