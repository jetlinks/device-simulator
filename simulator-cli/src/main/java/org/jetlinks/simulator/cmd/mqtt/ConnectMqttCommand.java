package org.jetlinks.simulator.cmd.mqtt;

import org.jetlinks.simulator.cmd.AbstractCommand;
import org.jetlinks.simulator.cmd.NetClientCommandOption;
import org.jetlinks.simulator.core.ExceptionUtils;
import org.jetlinks.simulator.core.network.mqtt.MqttClient;
import picocli.CommandLine;

import java.net.InetSocketAddress;
import java.time.Duration;

@CommandLine.Command(name = "connect",
        showDefaultValues = true,
        description = "Create new mqtt connection",
        headerHeading = "%n", sortOptions = false)
class ConnectMqttCommand extends AbstractCommand implements Runnable {

    @CommandLine.Mixin
    MqttCommandOptions command;


    @CommandLine.Mixin
    private NetClientCommandOption common;

    @Override
    public void run() {
        if (common != null) {
            common.apply(command);
        }
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

        @CommandLine.Option(names = {"-c", "--clientId"}, description = "MQTT clientId", order = 3, defaultValue = "mqtt-simulator")
        public void setClientId0(String clientId) {
            super.setClientId(clientId);
        }

        @CommandLine.Option(names = {"-u", "--username"}, description = "MQTT username", order = 4, defaultValue = "mqtt-simulator")
        public void setUsername0(String username) {
            super.setUsername(username);
        }

        @CommandLine.Option(names = {"-P", "--password"}, description = "MQTT password", order = 5, defaultValue = "mqtt-simulator")
        public void setPassword0(String password) {
            super.setPassword(password);
        }


        @CommandLine.Option(names = {"--topics"}, description = "attach and subscribe topics", order = 6)
        private String[] topics;

        @CommandLine.Option(names = {"--reconnectAttempts"}, description = "MQTT reconnect times", order = 7)
        public void setReconnect(int attemps) {
            super.setReconnectAttempts(attemps);
        }

        @CommandLine.Option(names = {"--reconnectInterval"}, description = "MQTT reconnect interval", order = 8)
        public void setReconnectInterval0(long interval) {
            super.setReconnectInterval(interval);
        }


    }
}

