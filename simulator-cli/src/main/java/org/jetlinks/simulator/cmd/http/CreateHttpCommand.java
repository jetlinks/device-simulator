package org.jetlinks.simulator.cmd.http;

import org.jetlinks.simulator.cmd.AbstractCommand;
import org.jetlinks.simulator.core.ExceptionUtils;
import org.jetlinks.simulator.core.network.http.HTTPClient;
import org.jetlinks.simulator.core.network.http.HTTPClientOptions;
import org.jetlinks.simulator.core.network.udp.UDPClient;
import org.jetlinks.simulator.core.network.udp.UDPOptions;
import org.springframework.http.HttpHeaders;
import picocli.CommandLine;

import java.time.Duration;
import java.util.Map;

@CommandLine.Command(name = "create",
        showDefaultValues = true,
        description = "Create HTTP Client",
        headerHeading = "%n", sortOptions = false)
public class CreateHttpCommand extends AbstractCommand implements Runnable {

    @CommandLine.Mixin
    private Options options;

    @Override
    public void run() {
        printf("create http client %s", options.getId());
        try {
            HTTPClient client = HTTPClient.create(options).block(Duration.ofSeconds(10));
            if (client != null) {
                main().connectionManager().addConnection(client);
                printf(" success!%n");
                main()
                        .getCommandLine()
                        .execute("http", "attach", client.getId());

            } else {
                printf(" error:%n");
            }
        } catch (Throwable err) {
            printfError(" error: %s %n", ExceptionUtils.getErrorMessage(err));
        }
    }

    public static class HttpHeaderConverter implements CommandLine.ITypeConverter<HttpHeaders> {

        @Override
        public HttpHeaders convert(String value) throws Exception {
            HttpHeaders headers = new HttpHeaders();
            for (String s : value.split(",")) {
                String[] split = s.split("=", 2);
                if (split.length == 2) {
                    headers.add(split[0], split[1]);
                }
            }
            return headers;
        }
    }

    static class Options extends HTTPClientOptions {

        @Override
        @CommandLine.Option(names = {"--id"}, description = "Client ID", defaultValue = "http-client")
        public void setId(String id) {
            super.setId(id);
        }

        @CommandLine.Option(names = {"-h", "--header"}, description = "Default Headers")
        public void setHeader(Map<String,String> header){
            HttpHeaders headers=new HttpHeaders();
            header.forEach(headers::add);
            setHeaders(headers);
        }

        @Override
        @CommandLine.Parameters
        public void setBasePath(String basePath) {
            super.setBasePath(basePath);
        }


    }
}
