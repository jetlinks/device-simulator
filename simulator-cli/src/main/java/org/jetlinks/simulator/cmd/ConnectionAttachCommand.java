package org.jetlinks.simulator.cmd;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.jetlinks.simulator.core.Connection;
import org.jline.utils.AttributedString;
import org.joda.time.DateTime;
import reactor.core.Disposable;
import reactor.core.Disposables;

import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

public class ConnectionAttachCommand extends AttachCommand {


    @Getter
    @Setter
    private String id;

    protected Connection connection;
    protected Deque<List<AttributedString>> messages;

    protected Disposable.Composite disposable;

    protected Connection getConnection() {
        return main()
                .connectionManager()
                .getConnectionNow(getId())
                .orElse(null);
    }

    @Override
    protected final void destroy() {
        disposable.dispose();
    }

    @Override
    protected final void init() {
        super.init();

        disposable = Disposables.composite();

        connection = getConnection();
        if (connection == null) {
            throw new IllegalArgumentException(String.format("connection [%s] not found", getId()));
        }
        messages = (Deque) connection
                .attribute("__msgCache")
                .orElseGet(ConcurrentLinkedDeque::new);

        connection.attribute("__msgCache", messages);

        doInit();
    }

    protected void doInit() {

    }


    @Override
    protected void createHeader(List<AttributedString> lines) {
        lines.add(
                createLine(builder -> builder
                        .append(connection.getId(), blue)
                        .append("(")
                        .append(connection
                                        .state()
                                        .name(), connection.state() == Connection.State.connected ? green : red)
                        .append(")")
                        .append(" ")
                        .append(new DateTime(connection.getConnectTime()).toString("yyyy-MM-dd HH:mm:ss"))
                        .append(" sent: ")
                        .append(String.valueOf(connection.attribute(Connection.ATTR_SENT).orElse(0)), green)
                        .append("(")
                        .append(formatBytes(connection
                                                    .attribute(Connection.ATTR_SENT_BYTES)
                                                    .map(CastUtils::castNumber)
                                                    .orElse(0)
                                                    .longValue()), green)
                        .append(")")
                        .append(" received: ")
                        .append(String.valueOf(connection.attribute(Connection.ATTR_RECEIVE).orElse(0)), green)
                        .append("(")
                        .append(formatBytes(connection
                                                    .attribute(Connection.ATTR_RECEIVE_BYTES)
                                                    .map(CastUtils::castNumber)
                                                    .orElse(0)
                                                    .longValue()), green)
                        .append(")"))
        );
    }


    @Override
    protected void createBody(List<AttributedString> lines) {

        for (List<AttributedString> message : messages) {
            lines.addAll(message);
        }
    }

}
