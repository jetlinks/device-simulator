package org.jetlinks.simulator.cmd.mqtt;

import org.jetlinks.simulator.cmd.AbstractCommand;
import org.jetlinks.simulator.core.ExceptionUtils;
import org.jetlinks.simulator.core.network.mqtt.MqttClient;
import org.springframework.util.CollectionUtils;
import picocli.CommandLine;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.StringJoiner;

@CommandLine.Command(name = "connect",
        showDefaultValues = true,
        description = "Create new mqtt connection",
        headerHeading = "%n", sortOptions = false)
class ConnectMqttCommand extends AbstractCommand implements Runnable {

    @CommandLine.Mixin
    MqttCommandOptions command;

    @Override
    public void run() {
        printf("connecting %s", command);
        try {
            MqttClient client = MqttClient
                    .connect(InetSocketAddress.createUnresolved(command.getHost(), command.getPort()), command)
                    .block(Duration.ofSeconds(10));
            if (client != null) {
                main().connectionManager().addConnection(client);
                printf(" success!%n");
                main()
                        .getCommandLine()
                        .execute("mqtt", "attach", client.getId());

            } else {
                printf(" error:%n");
            }
        } catch (Throwable err) {
            printfError(" error: %s %n", ExceptionUtils.getErrorMessage(err));
        }

    }

    static class MqttCommandOptions extends org.jetlinks.simulator.core.network.mqtt.MqttOptions {

        @Override
        @CommandLine.Option(names = {"-h", "--host"}, description = "host", order = 1, defaultValue = "127.0.0.1")
        public void setHost(String host) {
            super.setHost(host);
        }

        @Override
        @CommandLine.Option(names = {"-p", "--port"}, description = "port", order = 2, defaultValue = "1883")
        public void setPort(int port) {
            super.setPort(port);
        }

        @Override
        @CommandLine.Option(names = {"-c", "--clientId"}, description = "MQTT clientId", order = 3, defaultValue = "mqtt-simulator")
        public void setClientId(String clientId) {
            super.setClientId(clientId);
        }

        @Override
        @CommandLine.Option(names = {"-u", "--username"}, description = "MQTT username", order = 4, defaultValue = "mqtt-simulator")
        public void setUsername(String username) {
            super.setUsername(username);
        }

        @Override
        @CommandLine.Option(names = {"-P", "--password"}, description = "MQTT password", order = 5, defaultValue = "mqtt-simulator")
        public void setPassword(String password) {
            super.setPassword(password);
        }

        @CommandLine.Option(names = {"--topics"}, description = "attach and subscribe topics", order = 6)
        private String[] topics;

    }
}

