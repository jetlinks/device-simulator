package org.jetlinks.simulator;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import io.vertx.core.Vertx;
import io.vertx.core.logging.SLF4JLogDelegateFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.expands.script.engine.DynamicScriptEngineFactory;
import org.hswebframework.web.utils.ExpressionUtils;
import org.jetlinks.simulator.core.*;
import org.jetlinks.simulator.core.listener.DefaultSimulatorListenerBuilder;
import org.jetlinks.simulator.core.listener.MessageLogListener;
import org.jetlinks.simulator.mqtt.MqttSimulatorProvider;
import org.jetlinks.simulator.tcp.TcpSimulatorProvider;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;

@Slf4j(topic = "console-simulator")
public class ConsoleSimulator {

    @Setter
    @Getter
    private String path = System.getProperty("simulator.scripts.path","./scripts/");

    private final DefaultSimulatorManager manager = new DefaultSimulatorManager();

    private final KeyManager keyManager = (id) -> Mono.empty();

    private static final CountDownLatch await = new CountDownLatch(1);

    private final Map<String, Object> scriptEnv = new HashMap<>();

    public ConsoleSimulator() {
        Vertx vertx = Vertx.vertx();

        DefaultSimulatorListenerBuilder listenerBuilder = new DefaultSimulatorListenerBuilder();

        AddressPool addressPool = new DefaultAddressPool();

        manager.addProvider(new MqttSimulatorProvider(vertx, keyManager, listenerBuilder, addressPool));
        manager.addProvider(new TcpSimulatorProvider(vertx, keyManager, listenerBuilder, addressPool));

        scriptEnv.put("env", System.getenv());

        DynamicScriptEngineFactory.getEngine("spel").addGlobalVariable(scriptEnv);

    }

    @SneakyThrows
    public static void main(String[] args) {
        System.setProperty("vertx.logger-delegate-factory-class-name", SLF4JLogDelegateFactory.class.getName());
        ConsoleSimulator simulator = new ConsoleSimulator();
        Files.walk(Paths.get(simulator.path))
                .filter(path -> path.toString().endsWith(".simulator.json"))
                .forEach(simulator::init);
        await.await();
    }

    @SneakyThrows
    private String readFileToText(Path path) {
        try (InputStream stream = new FileInputStream(path.toFile())) {
            return StreamUtils.copyToString(stream, StandardCharsets.UTF_8);
        }
    }

    @SneakyThrows
    private <T> void resolveConfig(JSONObject main, String key, Function<String, Object> mapper) {
        String value = main.getString(key);
        if (value != null && value.contains("$")) {
            main.put(key, mapper.apply(ExpressionUtils.analytical(value,  "spel")));
        }
    }

    protected SimulatorConfig applyConfig(Path path, JSONObject jsonConfig) {
        JSONObject runner = (JSONObject) jsonConfig.computeIfAbsent("runner", (k) -> new JSONObject());
        this.resolveConfig(runner, "batch", Integer::parseInt);
        this.resolveConfig(runner, "total", Integer::parseInt);
        this.resolveConfig(runner, "startWith", Integer::parseInt);
        this.resolveConfig(runner, "binds", v -> {
            if (v.contains(",")) {
                return Arrays.asList(v.split("[,]"));
            }
            return v;
        });
        SimulatorConfig config = jsonConfig.toJavaObject(SimulatorConfig.class);
        if (!CollectionUtils.isEmpty(config.getListeners())) {
            for (SimulatorConfig.Listener listener : config.getListeners()) {
                if ("jsr223".equals(listener.getType())) {
                    String location = (String) listener.getConfiguration().get("location");
                    if (StringUtils.hasText(location)) {
                        if (location.startsWith("./")) {
                            listener.getConfiguration().put("script", readFileToText(path.getParent().resolve(location)));
                        } else {
                            listener.getConfiguration().put("script", readFileToText(Paths.get(location)));
                        }
                    }
                }
            }
        }
        return config;
    }

    @SneakyThrows
    public void init(Path simulatorJsonFile) {
        String json = readFileToText(simulatorJsonFile);
        JSONObject config = JSON.parseObject(json);
        SimulatorConfig simulatorConfig = applyConfig(simulatorJsonFile, config);
        System.out.println(simulatorJsonFile.toAbsolutePath().toString());
        System.out.println(JSON.toJSONString(config, SerializerFeature.PrettyFormat));

        Simulator simulator = manager.createSimulator(simulatorConfig).block();
        simulator.registerListener(new MessageLogListener("console"));

        simulator.handleLog()
                .subscribe(log::debug);
        simulator.start().block();
    }


}
