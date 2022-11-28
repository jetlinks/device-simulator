package org.jetlinks.simulator.cmd.mqtt;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.vertx.mqtt.messages.MqttPublishMessage;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.jetlinks.simulator.cmd.AbstractCommand;
import org.jetlinks.simulator.cmd.CommonCommand;
import org.jetlinks.simulator.cmd.ConnectionAttachCommand;
import org.jetlinks.simulator.core.Connection;
import org.jetlinks.simulator.core.ExceptionUtils;
import org.jetlinks.simulator.core.network.mqtt.MqttClient;
import org.jline.utils.AttributedString;
import org.joda.time.DateTime;
import picocli.CommandLine;
import reactor.core.Disposable;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@CommandLine.Command(name = "subscribe", aliases = "attach", description = "Subscribe mqtt client topic", hidden = false)
public class MqttAttachCommand extends ConnectionAttachCommand {

    @Override
//    @CommandLine.Option(names = {"-c", "--clientId"}, paramLabel = "clientId", required = true, completionCandidates = MqttPublishCommand.IdComplete.class)
    @CommandLine.Parameters(paramLabel = "clientId", completionCandidates = MqttPublishCommand.IdComplete.class)
    public void setId(String id) {
        super.setId(id);
    }

    @Override
    protected void doInit() {
        disposable.add(connection
                               .unwrap(MqttClient.class)
                               .handle(this::appendMessage));
    }

    @Override
    protected void doDestroy() {
        super.doDestroy();
    }

    @Override
    protected void createHeader(List<AttributedString> lines) {
        super.createHeader(lines);
        lines.add(
                createLine(builder -> {
                    builder.append("Subscriptions: ");
                    for (MqttClient.Subscriber subscription : connection
                            .unwrap(MqttClient.class).getSubscriptions()) {
                        builder.append(subscription.getTopic(), green)
                               .append("(QoS " + subscription.getQos() + ") ", blue);
                    }
                })
        );
    }

    @CommandLine.Command(name = "",
            subcommands = {
                    Publish.class,
                    Subscribe.class,
                    Unsubscribe.class,
                    Disconnect.class
            },
            customSynopsis = {""},
            synopsisHeading = "")
    class AttachCommands extends CommonCommand {

        void publish(Publish publish) {
            Connection connection = main().connectionManager().getConnection(getId()).blockOptional().orElse(null);

            if (null != connection) {
                MqttClient client = connection.unwrap(MqttClient.class);
                try {
                    printf("publishing ");
                    client.publishAsync(publish.topic, publish.qos, publish.payload)
                          .block(Duration.ofSeconds(10));
                    printf("success!%n");
                } catch (Throwable e) {
                    printfError("error:%s%n", ExceptionUtils.getErrorMessage(e));
                }
            } else {
                printfError("error: Not Found %n");
            }
        }

        void unsubscribe(Unsubscribe subscribe) {
            for (String topic : subscribe.topics) {
                connection
                        .unwrap(MqttClient.class)
                        .unsubscribe(topic);
            }

        }

        void subscribe(Subscribe subscribe) {
            for (String topic : subscribe.topics) {
                connection
                        .unwrap(MqttClient.class)
                        .subscribe(topic, subscribe.qos, ignore -> {

                        });
            }
        }

        void disconnect() {
            connection.dispose();
        }
    }


    @CommandLine.Command(name = "publish", description = "Publish mqtt message")
    static class Publish extends CommonCommand {

        @CommandLine.Option(names = {"-t", "--topic"}, required = true, description = "mqtt topic")
        String topic;


        @CommandLine.Option(names = {"-q", "--qos"}, description = "QoS Level", defaultValue = "0")
        int qos;

        @CommandLine.Parameters(arity = "1", description = "0x开头为16进制")
        String payload;

        @Override
        public void run() {
            ((AttachCommands) getParent()).publish(this);
        }
    }

    @CommandLine.Command(name = "subscribe", description = "Subscribe mqtt message")
    static class Subscribe extends CommonCommand {

        @CommandLine.Parameters(description = "mqtt topic")
        String[] topics;


        @CommandLine.Option(names = {"-q", "--qos"}, description = "QoS Level", defaultValue = "0")
        int qos;

        @Override
        public void run() {

            ((AttachCommands) parent).subscribe(this);

        }
    }

    @CommandLine.Command(name = "unsubscribe", description = "Unsubscribe mqtt message")
    static class Unsubscribe extends CommonCommand {

        @CommandLine.Parameters(description = "mqtt topic")
        String[] topics;


        @Override
        public void run() {

            ((AttachCommands) parent).unsubscribe(this);

        }
    }

    @CommandLine.Command(name = "disconnect", description = "Disconnect mqtt")
    static class Disconnect extends CommonCommand {


        @Override
        public void run() {

            ((AttachCommands) parent).disconnect();

        }
    }


    @Override
    protected AbstractCommand createCommand() {
        return new AttachCommands();
    }

    private void appendMessage(MqttPublishMessage message) {

        List<AttributedString> msgLine = new ArrayList<>();

        msgLine.add(createLine(builder -> {
            builder.append(new DateTime().toString("HH:mm:ss"), red)
                   .append(" ")
                   .append(message.topicName(), green)
                   .append(" ")
                   .append("(QoS " + message.qosLevel().value() + ") ", blue);
        }));
        ByteBuf byteBuf = message.payload().getByteBuf();
        String str;
        if (ByteBufUtil.isText(byteBuf, StandardCharsets.UTF_8)) {
            str = byteBuf.toString(StandardCharsets.UTF_8);
        } else {
            str = ByteBufUtil.prettyHexDump(byteBuf);
        }

        for (String n : str.split("\n")) {
            msgLine.add(createLine(builder -> builder.append(n, green)));
        }

        messages.add(msgLine);
        if (messages.size() > 50) {
            messages.removeFirst();
        }

    }

}
