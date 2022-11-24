package org.jetlinks.simulator.cmd.tcp;

import org.jetlinks.simulator.cmd.AbstractCommand;
import org.jetlinks.simulator.core.Connection;
import org.jetlinks.simulator.core.ConnectionManager;
import org.jetlinks.simulator.core.ExceptionUtils;
import org.jetlinks.simulator.core.network.NetworkType;
import org.jetlinks.simulator.core.network.tcp.TcpClient;
import picocli.CommandLine;

import java.time.Duration;
import java.util.Iterator;

@CommandLine.Command(name = "send", description = "发送TCP指令")
public class TcpSendCommand extends AbstractCommand implements Runnable {

    @CommandLine.Option(names = {"--id"}, required = true, description = "ID", completionCandidates = IdComplete.class)
    String id;


    @CommandLine.Parameters(arity = "1", description = "0x开头为16进制")
    String payload;

    static class IdComplete implements Iterable<String> {

        @Override
        public Iterator<String> iterator() {
            return ConnectionManager
                    .global()
                    .getConnections()
                    .filter(c -> c.getType() == NetworkType.tcp_client)
                    .map(Connection::getId)
                    .collectList()
                    .block()
                    .iterator();
        }
    }

    @Override
    public void run() {
        Connection connection = main().connectionManager().getConnection(id).blockOptional().orElse(null);
        if (connection == null) {
            printfError("请先使用命令创建tcp连接: tcp connect --id=%s %n", id);
            return;
        }
        TcpClient client = connection.unwrap(TcpClient.class);
        printf("sending %s ", payload);
        try {
            client.sendAsync(payload)
                  .block(Duration.ofSeconds(10));
            printf("success!%n");
        } catch (Throwable e) {
            printfError("error:%s%n", ExceptionUtils.getErrorMessage(e));
        }

    }


}
