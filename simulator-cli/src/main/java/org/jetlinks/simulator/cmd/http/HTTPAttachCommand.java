package org.jetlinks.simulator.cmd.http;

import io.netty.buffer.ByteBufUtil;
import io.vertx.core.buffer.Buffer;
import lombok.Setter;
import lombok.SneakyThrows;
import org.jetlinks.simulator.cmd.AbstractCommand;
import org.jetlinks.simulator.cmd.CommonCommand;
import org.jetlinks.simulator.cmd.ConnectionAttachCommand;
import org.jetlinks.simulator.core.Connection;
import org.jetlinks.simulator.core.ConnectionManager;
import org.jetlinks.simulator.core.ExceptionUtils;
import org.jetlinks.simulator.core.network.NetworkType;
import org.jetlinks.simulator.core.network.http.HTTPClient;
import org.jetlinks.simulator.core.network.http.HttpResponse;
import org.jline.utils.AttributedString;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import picocli.CommandLine;

import java.io.File;
import java.time.Duration;
import java.util.*;

@CommandLine.Command(name = "attach", description = "Attach HTTP Client")
public class HTTPAttachCommand extends ConnectionAttachCommand {


    @Override
    @CommandLine.Parameters(paramLabel = "id", completionCandidates = IdComplete.class)
    public void setId(String id) {
        super.setId(id);
    }

    @Override
    protected void doInit() {

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
                    HTTPClient client = connection.unwrap(HTTPClient.class);
                    builder.append("          ");
                    builder.append("BasePath: ");
                    builder.append(client.getBasePath() == null ? "/" : client.getBasePath(), green);
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
                    .filter(c -> c.getType() == NetworkType.http_client)
                    .map(Connection::getId)
                    .collectList()
                    .block()
                    .iterator();
        }
    }

    @Override
    protected CommandLine commandLine() {
        CommandLine line = super.commandLine();
        line.registerConverter(HttpHeaders.class, new CreateHttpCommand.HttpHeaderConverter());
        return line;
    }

    @CommandLine.Command(name = "",
            subcommands = {
                    Request.class,
                    Close.class
            },
            customSynopsis = {""},
            synopsisHeading = "")
    class AttachCommands extends CommonCommand {

        void request(Request request) {

            HTTPClient client = connection.unwrap(HTTPClient.class);
            List<AttributedString> lines = new ArrayList<>();

            lines.add(AttributedString.EMPTY);

            request.createMessage(lines, client.getHeaders());
            messages.add(lines);
            long time = System.currentTimeMillis();
            try {
                HttpResponse response = client
                        .request(io.vertx.core.http.HttpMethod.valueOf(request.method.name()),
                                 request.path,
                                 request.createBody(),
                                 request.mediaType,
                                 request.headers)
                        .block(Duration.ofSeconds(30));
                long t = System.currentTimeMillis() - time;
                lines.add(AttributedString.EMPTY);
                if (response != null) {
                    createResponseLine(lines, response, t);
                } else {
                    lines.add(AttributedString.fromAnsi("No Response!"));
                }
            } catch (Throwable e) {
                lines.add(AttributedString.EMPTY);
                lines.add(createLine(builder -> builder.append("Error: " + ExceptionUtils.getErrorMessage(e), red)));
            } finally {
                if (messages.size() > 50) {
                    messages.removeFirst();
                }
            }
        }

        void disconnect() {
            connection.dispose();
        }
    }

    private void createResponseLine(List<AttributedString> lines, HttpResponse response, long time) {

        lines.add(createLine(builder -> builder
                          .append(response.getVersion().alpnName().toUpperCase())
                          .append(" ")
                          .append(String.valueOf(response.getStatus().value()))
                          .append(" ")
                          .append(response.getStatus().name(), response.getStatus().is2xxSuccessful() ? green : red)
                          .append(" take ").append(String.valueOf(time))
                          .append("ms")
                  )
        );

        createHeaderLine(lines, response.getHeaders());

        Buffer body = response.getBody();
        if (body.length() > 0) {
            lines.add(AttributedString.EMPTY);
            String bodyStr;
            MediaType mediaType = response.getHeaders().getContentType();
            if (mediaType != null && mediaType.includes(MediaType.APPLICATION_OCTET_STREAM)) {
                bodyStr = ByteBufUtil.hexDump(body.getByteBuf());
            } else if (mediaType != null && mediaType.getCharset() != null) {
                bodyStr = body.toString(mediaType.getCharset());
            } else {
                bodyStr = body.toString();
            }

            String[] str = bodyStr.split("\n");
            for (String s : str) {
                lines.add(createLine(builder -> builder.append(s)));
            }
        }

    }

    public static void createHeaderLine(List<AttributedString> lines, HttpHeaders headers) {
        if (headers == null) {
            return;
        }
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            lines.add(createLine(builder -> builder
                    .append(entry.getKey(), blue)
                    .append(": ")
                    .append(String.join(",", entry.getValue()), blue)));
        }
    }

    public static class MediaTypeCompletion implements Iterable<String> {

        @Override
        public Iterator<String> iterator() {
            return Arrays
                    .asList(
                            MediaType.APPLICATION_JSON_VALUE,
                            MediaType.APPLICATION_XML_VALUE,
                            MediaType.TEXT_PLAIN_VALUE
                    ).iterator();
        }
    }


    @CommandLine.Command(name = "request", description = "Send HTTP request")
    @Setter
    static class Request extends CommonCommand {

        @CommandLine.Option(names = {"-X", "--method"}, description = "Request method", required = true, defaultValue = "GET")
        HttpMethod method;

        String payload;

        @CommandLine.Option(names = {"-d", "--data"}, description = "Request Data")
        public void setPayload(String payload) {
            this.payload = payload.replace("\\n", "\n");
        }

        @CommandLine.Option(names = {"--mediaType"}, description = "Media Type", completionCandidates = MediaTypeCompletion.class)
        String mediaType;


        @CommandLine.Option(names = {"--file"}, description = "Request File Data")
        File file;

        HttpHeaders headers;

        @CommandLine.Option(names = {"-h", "--header"}, description = "Default Headers")
        public void setHeaders(Map<String, String> header) {
            headers = new HttpHeaders();
            header.forEach(headers::add);
        }

        @CommandLine.Parameters
        String path;

        public void createMessage(List<AttributedString> lines, HttpHeaders defaultHeaders) {
            lines.add(createLine(builder -> {
                builder.append(method.name())
                       .append(" ")
                       .append(path == null ? "/" : path);
            }));

            createHeaderLine(lines, headers);
            createHeaderLine(lines, defaultHeaders);

            if (StringUtils.hasText(payload)) {
                lines.add(AttributedString.EMPTY);
                for (String str : payload.split("\n")) {
                    lines.add(createLine(builder -> builder.append(str)));
                }
            }
        }

        private Object createBody() {

            return path;
        }

        @Override
        public void run() {
            ((AttachCommands) getParent()).request(this);
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


}
