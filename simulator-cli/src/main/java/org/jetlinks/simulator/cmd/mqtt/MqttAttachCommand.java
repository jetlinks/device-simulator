package org.jetlinks.simulator.cmd.mqtt;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.vertx.mqtt.messages.MqttPublishMessage;
import org.jetlinks.simulator.cmd.ConnectionAttachCommand;
import org.jetlinks.simulator.core.network.mqtt.MqttClient;
import org.jline.utils.AttributedString;
import org.joda.time.DateTime;
import picocli.CommandLine;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@CommandLine.Command(name = "attach",aliases = "subscribe",description = "订阅MQTT消息")
public class MqttAttachCommand extends ConnectionAttachCommand {

    @Override
    @CommandLine.Option(names = {"-c", "--clientId"}, paramLabel = "clientId", required = true, completionCandidates = MqttPublishCommand.IdComplete.class)
    public void setId(String id) {
        super.setId(id);
    }

    @CommandLine.Option(names = {"-t", "--topics"}, split = ",")
    private String[] topics;

    @CommandLine.Option(names = {"-q", "--qos"})
    private int qos;

    @Override
    protected void doInit() {
        if (topics == null || topics.length == 0) {
            disposable.add(connection
                                   .unwrap(MqttClient.class)
                                   .handle(this::appendMessage));
        } else {
            for (String topic : topics) {
                disposable.add(connection
                                       .unwrap(MqttClient.class)
                                       .subscribe(topic, qos, this::appendMessage));
            }
        }
    }

    private void appendMessage(MqttPublishMessage message) {

        List<AttributedString> msgLine = new ArrayList<>();

        msgLine.add(createLine(builder -> {
            builder.append(new DateTime().toString("HH:mm:ss"), red)
                   .append(" ")
                   .append(message.topicName(), green)
                   .append(" ")
                   .append(message.qosLevel().name() + "(QoS " + message.qosLevel().value() + ") ", blue);
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
        if (messages.size() > 5) {
            messages.removeFirst();
        }

    }

}
