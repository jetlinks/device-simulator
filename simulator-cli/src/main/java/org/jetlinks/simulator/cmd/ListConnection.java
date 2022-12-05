package org.jetlinks.simulator.cmd;

import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.github.freva.asciitable.ColumnData;
import com.github.freva.asciitable.HorizontalAlign;
import org.jetlinks.reactor.ql.utils.SqlUtils;
import org.jetlinks.simulator.core.Connection;
import org.jetlinks.simulator.core.ConnectionManager;
import org.jetlinks.simulator.core.ExceptionUtils;
import org.joda.time.DateTime;
import org.springframework.util.StringUtils;
import picocli.CommandLine;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

@CommandLine.Command(name = "list",
        description = "Search connections",
        headerHeading = "%n")
public class ListConnection extends CommonCommand implements Runnable {

    @CommandLine.Option(names = {"-e", "--expression"}, description = "query expression,e.g. \"type='mqtt'\" and clientId like 'test-%'")
    private String expression;

    @CommandLine.Option(names = {"-l", "--limit"}, description = "limit ,e.g. 20")
    private int limit = 20;

    @CommandLine.Option(names = {"-o", "--offset"}, description = "offset ,e.g. 0")
    private int offset = 0;

    static List<ColumnData<Connection>> columns = Arrays.asList(
            createColumn("id", Connection::getId),
            createColumn("type", Connection::getType),
            createColumn("connectTime", d -> new DateTime(d.getConnectTime()).toString("HH:mm:ss")),
            createColumn(Connection.ATTR_STATE, d -> d.state().name()),
            createAttrColumn(Connection.ATTR_SENT, "0"),
            createAttrColumn(Connection.ATTR_SENT_BYTES, "0"),
            createAttrColumn(Connection.ATTR_RECEIVE, "0"),
            createAttrColumn(Connection.ATTR_RECEIVE_BYTES, "0")
    );

    static ColumnData<Connection> createAttrColumn(String header, String defaultValue) {
        return createColumn(header, c -> c.attribute(header).orElse(defaultValue));
    }

    static ColumnData<Connection> createColumn(String header, Function<Connection, Object> converter) {
        return new Column()
                .header(header)
                .headerAlign(HorizontalAlign.CENTER)
                .dataAlign(HorizontalAlign.LEFT)
                .with(c -> {
                    Object val = converter.apply(c);
                    return val == null ? "" : String.valueOf(val);
                });
    }

    protected ConnectionManager connectionManager() {
        return main().connectionManager;
    }

    @Override
    public void run() {

        Flux<Connection> flux;
        long total = connectionManager().getConnectionSize();

        if (StringUtils.hasText(expression)) {
            printf("find connection, total:%d, expr %s limit %d,offset %d.%n", total, expression, limit, offset);

            flux = connectionManager().findConnection(SqlUtils.getCleanStr(expression));
        } else {
            printf("find connection, total:%d ,limit %d,offset %d.%n", total, limit, offset);

            flux = connectionManager().getConnections();
        }
        if (total == 0) {
            return;
        }

        flux.skip(offset)
            .take(limit)
            .collectList()
            .onErrorResume(err ->{
                printfError("%s%n", ExceptionUtils.getErrorMessage(err));
                return Mono.empty();
            })
            .subscribe(connection -> {
                printf("%s%n", AsciiTable
                        .builder()
                        .data(connection, columns)
                        .toString());
            });


    }
}