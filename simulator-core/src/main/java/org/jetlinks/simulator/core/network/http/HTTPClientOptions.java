package org.jetlinks.simulator.core.network.http;

import io.vertx.core.http.HttpClientOptions;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpHeaders;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Setter
public class HTTPClientOptions extends HttpClientOptions {

    private String id;

    private String basePath;

    private HttpHeaders headers;

    public HTTPClientOptions() {

    }

    public HTTPClientOptions(HTTPClientOptions options) {
        super(options);
        this.id = options.id;
        this.basePath = options.getBasePath();
        this.headers = options.getHeaders();
    }

    public HTTPClientOptions copy() {
        return new HTTPClientOptions(this);
    }

    private HTTPClientOptions apply(Map<String, Object> args) {

        for (Map.Entry<String, Object> entry : args.entrySet()) {
            String key = "{" + entry.getKey() + "}";
            String value = String.valueOf(entry.getValue());
            if (id != null) {
                id = id.replace(key, value);
            }
            if (basePath != null) {
                basePath = basePath.replace(key, value);
            }

            if (headers != null) {
                for (Map.Entry<String, List<String>> e : headers.entrySet()) {
                    e.setValue(
                            e.getValue()
                             .stream()
                             .map(str -> str.replace(key, value))
                             .collect(Collectors.toList())
                    );
                }
            }

        }

        return this;
    }

    public HTTPClientOptions refactor(Map<String, Object> args) {
        return copy().apply(args);
    }
}
