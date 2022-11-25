package org.jetlinks.simulator.cmd.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.vertx.core.buffer.Buffer;
import org.jetlinks.simulator.cmd.ConnectionAttachCommand;
import org.jetlinks.simulator.core.network.tcp.TcpClient;
import org.jline.utils.AttributedString;
import org.joda.time.DateTime;
import picocli.CommandLine;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@CommandLine.Command(name = "attach", description = "监听tcp连接")
public class TcpAttachCommand extends ConnectionAttachCommand {

    @Override
    @CommandLine.Option(names = {"--id"}, paramLabel = "id", required = true, completionCandidates = TcpSendCommand.IdComplete.class)
    public void setId(String id) {
        super.setId(id);
    }

    @Override
    protected void doInit() {
        disposable.add(connection
                               .unwrap(TcpClient.class)
                               .handlePayload(this::appendMessage));
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

}
