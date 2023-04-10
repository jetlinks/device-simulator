package org.jetlinks.simulator.cmd.coap;

import lombok.Setter;
import lombok.SneakyThrows;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Option;
import org.jetlinks.protocol.official.ObjectMappers;
import org.jetlinks.simulator.cmd.AbstractCommand;
import org.jetlinks.simulator.cmd.CommonCommand;
import org.jetlinks.simulator.cmd.ConnectionAttachCommand;
import org.jetlinks.simulator.core.Connection;
import org.jetlinks.simulator.core.ConnectionManager;
import org.jetlinks.simulator.core.ExceptionUtils;
import org.jetlinks.simulator.core.network.NetworkType;
import org.jetlinks.simulator.core.network.coap.CoapClient;
import org.jetlinks.simulator.core.network.coap.CoapOptions;
import org.jline.utils.AttributedString;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import picocli.CommandLine;

import java.time.Duration;
import java.util.*;

@CommandLine.Command(name = "attach", description = "Attach Coap Client")
public class CoapAttachCommand extends ConnectionAttachCommand {


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
                    CoapClient client = connection.unwrap(CoapClient.class);
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
                    .filter(c -> c.getType() == NetworkType.coap_client)
                    .map(Connection::getId)
                    .toStream()
                    .iterator();
        }
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


            CoapClient client = connection.unwrap(CoapClient.class);
            List<AttributedString> lines = new ArrayList<>();

            lines.add(AttributedString.EMPTY);

            request.createMessage(lines, client.getOptions());
            messages.add(lines);
            long time = System.currentTimeMillis();
            try {
                CoapResponse response = client
                        .advancedAsync(request.method,
                                       request.path,
                                       request.createBody(),
                                       request.mediaType,
                                       request.options)
                        .block(Duration.ofSeconds(10));
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

    @SneakyThrows
    private void createResponseLine(List<AttributedString> lines, CoapResponse response, long time) {

        lines.add(
                createLine(builder -> builder
                        .append(response.getCode().name(), response.getCode().isSuccess() ? green : red)
                        .append(" ")
                        .append(" take ").append(String.valueOf(time))
                        .append("ms")
                )
        );

        for (Option option : response.getOptions().asSortedList()) {
            lines.add(createLine(builder -> builder.append(option.toString(), blue)));
        }

        byte[] body = response.getPayload();

        if (body.length > 0) {
            lines.add(AttributedString.EMPTY);
            String bodyStr;
            MediaType mediaType = MediaType.valueOf(MediaTypeRegistry.toString(
                    response
                            .getOptions()
                            .getContentFormat()));

            if (mediaType.includes(MediaType.APPLICATION_CBOR)) {
                bodyStr = ObjectMappers.JSON_MAPPER
                        .writeValueAsString(ObjectMappers.CBOR_MAPPER
                                                    .readValue(body, Object.class));

            } else if (mediaType.getCharset() != null) {
                bodyStr = new String(body, mediaType.getCharset());
            } else {
                bodyStr = response.getResponseText();
            }

            String[] str = bodyStr.split("\n");
            for (String s : str) {
                lines.add(createLine(builder -> builder.append(s)));
            }
        }

    }

    public static void createHeaderLine(List<AttributedString> lines, Map<String, String> headers) {
        if (headers == null) {
            return;
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            lines.add(createLine(builder -> builder
                    .append(entry.getKey(), blue)
                    .append(": ")
                    .append(entry.getValue(), blue)));
        }
    }

    public static class MediaTypeCompletion implements Iterable<String> {

        @Override
        public Iterator<String> iterator() {
            return Arrays
                    .asList(
                            MediaType.APPLICATION_JSON_VALUE,
                            MediaType.APPLICATION_CBOR_VALUE,
                            MediaType.TEXT_PLAIN_VALUE
                    ).iterator();
        }
    }


    @CommandLine.Command(name = "request", description = "Send Coap request")
    @Setter
    static class Request extends CommonCommand {

        @CommandLine.Option(names = {"-X", "--method"}, description = "Request method", required = true, defaultValue = "GET")
        CoAP.Code method;

        String payload;

        Map<String, String> options;

        @CommandLine.Option(names = {"-d", "--data"}, description = "Request Data")
        public void setPayload(String payload) {
            this.payload = payload.replace("\\n", "\n");
        }

        @CommandLine.Option(names = {"--format"},
                description = "Content Format",
                completionCandidates = MediaTypeCompletion.class,
                defaultValue = MediaType.APPLICATION_JSON_VALUE)
        String mediaType;


        @CommandLine.Option(names = {"-o", "--option"}, description = "options")
        public void setHeaders(Map<String, String> options) {
            this.options = options;
        }

        @CommandLine.Parameters
        String path;

        public void createMessage(List<AttributedString> lines, CoapOptions options) {

            lines.add(createLine(builder -> builder
                    .append(method.name())
                    .append(" ")
                    .append(options.createUri(path))));


            createHeaderLine(lines, options.getOptions());
            createHeaderLine(lines, this.options);

            if (StringUtils.hasText(payload)) {
                lines.add(AttributedString.EMPTY);
                for (String str : payload.split("\n")) {
                    lines.add(createLine(builder -> builder.append(str)));
                }
            }
        }

        private Object createBody() {

            return payload;
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
