package org.jetlinks.simulator.cmd.upd;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.vertx.core.datagram.DatagramPacket;
import lombok.SneakyThrows;
import org.jetlinks.simulator.cmd.AbstractCommand;
import org.jetlinks.simulator.cmd.CommonCommand;
import org.jetlinks.simulator.cmd.ConnectionAttachCommand;
import org.jetlinks.simulator.core.Connection;
import org.jetlinks.simulator.core.ConnectionManager;
import org.jetlinks.simulator.core.ExceptionUtils;
import org.jetlinks.simulator.core.network.NetworkType;
import org.jetlinks.simulator.core.network.udp.UDPClient;
import org.jline.utils.AttributedString;
import org.joda.time.DateTime;
import picocli.CommandLine;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@CommandLine.Command(name = "attach", description = "Attach UDP Client")
public class UDPAttachCommand extends ConnectionAttachCommand {

    @Override
    @CommandLine.Parameters(paramLabel = "id", completionCandidates = IdComplete.class)
    public void setId(String id) {
        super.setId(id);
    }

    @Override
    protected void doInit() {
        disposable.add(connection
                               .unwrap(UDPClient.class)
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
                    UDPClient client = connection.unwrap(UDPClient.class);
                    builder.append("          ");
                    builder.append("Remote Address: ");
                    builder.append(client.getRemote().toString(), green);
                    builder.append(" Local Address: ");
                    builder.append(client.getLocal().toString(), green);
                })
        );
    }

    static class IdComplete implements Iterable<String> {

        @Override
        @SneakyThrows
        public Iterator<String> iterator() {
            return ConnectionManager
                    .global()
                    .getConnections()
                    .filter(c -> c.getType() == NetworkType.udp_client)
                    .map(Connection::getId)
                    .collectList()
                    .block()
                    .iterator();
        }
    }


    @CommandLine.Command(name = "",
            subcommands = {
                    Send.class,
                    Close.class
            },
            customSynopsis = {""},
            synopsisHeading = "")
    class AttachCommands extends CommonCommand {

        void publish(Send publish) {
            UDPClient client = connection.unwrap(UDPClient.class);
            try {
                printf("sending ");
                if (publish.host != null && publish.port > 0) {
                    client.sendAsync(publish.host, publish.port, publish.payload)
                          .block(Duration.ofSeconds(10));
                } else {
                    client.sendAsync(publish.payload)
                          .block(Duration.ofSeconds(10));
                }
                printf("success!%n");
            } catch (Throwable e) {
                printfError("error:%s%n", ExceptionUtils.getErrorMessage(e));
            }
        }

        void disconnect() {
            connection.dispose();
        }
    }


    @CommandLine.Command(name = "send", description = "Send UDP packet")
    static class Send extends CommonCommand {

        @CommandLine.Option(names = {"-h", "--host"}, description = "Sent to custom host")
        String host;

        @CommandLine.Option(names = {"-p", "--port"}, description = "Sent to custom port")
        int port;

        @CommandLine.Parameters(arity = "1", description = "0x开头为16进制")
        String payload;

        @Override
        public void run() {
            ((AttachCommands) getParent()).publish(this);
        }
    }

    @CommandLine.Command(name = "close", description = "Close UDP Client")
    static class Close extends CommonCommand {


        @Override
        public void run() {

            ((AttachCommands) parent).disconnect();

        }
    }


    @Override
    protected AbstractCommand createCommand() {
        return new AttachCommands();
    }

    private void appendMessage(DatagramPacket message) {

        List<AttributedString> msgLine = new ArrayList<>();

        msgLine.add(createLine(builder -> {
            builder.append(new DateTime().toString("HH:mm:ss"), red)
                   .append(" ")
                   .append(message.sender().hostAddress() + ":" + message.sender().port(), green);
        }));
        ByteBuf byteBuf = message.data().getByteBuf();
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
