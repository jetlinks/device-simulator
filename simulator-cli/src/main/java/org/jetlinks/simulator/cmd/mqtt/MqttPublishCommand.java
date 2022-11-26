package org.jetlinks.simulator.cmd.mqtt;

import lombok.SneakyThrows;
import org.jetlinks.simulator.cmd.AbstractCommand;
import org.jetlinks.simulator.core.Connection;
import org.jetlinks.simulator.core.ConnectionManager;
import org.jetlinks.simulator.core.ExceptionUtils;
import org.jetlinks.simulator.core.network.NetworkType;
import org.jetlinks.simulator.core.network.mqtt.MqttClient;
import picocli.CommandLine;

import java.time.Duration;
import java.util.Iterator;

@CommandLine.Command(name = "publish", description = "Publish mqtt message")
public class MqttPublishCommand extends AbstractCommand implements Runnable {

    @CommandLine.Option(names = {"-c", "--clientId"}, required = true, description = "clientId", completionCandidates = IdComplete.class)
    String clientId;

    @CommandLine.Option(names = {"-t", "--topic"}, required = true, description = "mqtt topic")
    String topic;


    @CommandLine.Option(names = {"-q", "--qos"}, description = "QoS Level", defaultValue = "0")
    int qos;

    @CommandLine.Parameters(arity = "1", description = "0x开头为16进制")
    String payload;

    static class IdComplete implements Iterable<String> {

        @Override
        @SneakyThrows
        public Iterator<String> iterator() {
            return ConnectionManager
                    .global()
                    .getConnections()
                    .filter(c -> c.getType() == NetworkType.mqtt_client)
                    .map(Connection::getId)
                    .collectList()
                    .block()
                    .iterator();
        }
    }

    @Override
    public void run() {
        Connection connection = main().connectionManager().getConnection(clientId).blockOptional().orElse(null);
        if (connection == null) {
            printfError("请先使用命令创建mqtt连接: mqtt connect -c=%s %n", clientId);
            return;
        }
        MqttClient client = connection.unwrap(MqttClient.class);
        printf("publishing %s %s ", topic, payload);
        try {
            client.publishAsync(topic, qos, payload)
                  .block(Duration.ofSeconds(10));
            printf("success!%n");
        } catch (Throwable e) {
            printfError("error:%s%n", ExceptionUtils.getErrorMessage(e));
        }

    }


}
