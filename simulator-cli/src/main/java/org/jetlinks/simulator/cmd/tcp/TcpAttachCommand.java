package org.jetlinks.simulator.cmd.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.vertx.core.buffer.Buffer;
import org.jetlinks.simulator.cmd.AbstractCommand;
import org.jetlinks.simulator.cmd.CommonCommand;
import org.jetlinks.simulator.cmd.ConnectionAttachCommand;
import org.jetlinks.simulator.cmd.mqtt.MqttAttachCommand;
import org.jetlinks.simulator.cmd.mqtt.MqttPublishCommand;
import org.jetlinks.simulator.core.Connection;
import org.jetlinks.simulator.core.ExceptionUtils;
import org.jetlinks.simulator.core.network.mqtt.MqttClient;
import org.jetlinks.simulator.core.network.tcp.TcpClient;
import org.jline.utils.AttributedString;
import org.joda.time.DateTime;
import picocli.CommandLine;
import reactor.core.Disposable;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@CommandLine.Command(name = "attach", description = "attach TCP connection")
public class TcpAttachCommand extends ConnectionAttachCommand {

    @Override
//    @CommandLine.Option(names = {"--id"}, paramLabel = "id", required = true, completionCandidates = TcpSendCommand.IdComplete.class)
    @CommandLine.Parameters(paramLabel = "id", completionCandidates = TcpSendCommand.IdComplete.class)
    public void setId(String id) {
        super.setId(id);
    }

    @Override
    protected void doInit() {
        disposable.add(connection
                               .unwrap(TcpClient.class)
                               .handlePayload(this::appendMessage));
    }

    @CommandLine.Command(name = "",
            subcommands = {
                    TcpAttachCommand.Send.class,
                    Disconnect.class
            },
            customSynopsis = {""},
            synopsisHeading = "")
    class AttachCommands extends CommonCommand {

        void send(Send send) {
            TcpClient client = connection.unwrap(TcpClient.class);
            try {
                printf("sending ");
                client.sendAsync(send.payload)
                      .block(Duration.ofSeconds(10));
                printf("success!%n");
            } catch (Throwable e) {
                printfError("error:%s%n", ExceptionUtils.getErrorMessage(e));
            }
        }

        void disconnect() {
            main().connectionManager().getConnectionNow(getId())
                  .ifPresent(Disposable::dispose);

        }
    }


    @CommandLine.Command(name = "send", description = "Send TCP packet")
    static class Send extends CommonCommand {

        @CommandLine.Parameters(arity = "1", description = "0x开头为16进制")
        String payload;

        @Override
        public void run() {
            ((TcpAttachCommand.AttachCommands) getParent()).send(this);
        }
    }

    @CommandLine.Command(name = "disconnect", description = "Disconnect TCP")
    static class Disconnect extends CommonCommand {

        @Override
        public void run() {
            ((TcpAttachCommand.AttachCommands) getParent()).disconnect();
        }
    }


    private void appendMessage(Buffer buffer) {

        List<AttributedString> msgLine = new ArrayList<>();


        ByteBuf byteBuf = buffer.getByteBuf();
        String str;
        if (ByteBufUtil.isText(byteBuf, StandardCharsets.UTF_8)) {
            str = byteBuf.toString(StandardCharsets.UTF_8);
        } else {
            str = ByteBufUtil.prettyHexDump(byteBuf);
        }

        msgLine.add(createLine(builder -> builder.append(new DateTime().toString("HH:mm:ss"), red)));

        for (String n : str.split("\n")) {
            msgLine.add(createLine(builder -> builder.append(n, green)));
        }

        messages.add(msgLine);
        if (messages.size() > 5) {
            messages.removeFirst();
        }

    }

    @Override
    protected AttachCommands createCommand() {
        return new AttachCommands();
    }
}
