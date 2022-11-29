package org.jetlinks.simulator.core.network.http;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpVersion;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.util.List;

@AllArgsConstructor
@Getter
public class HttpResponse {

    private HttpVersion version;

    private HttpStatus status;

    private Buffer body;

    private HttpHeaders headers;


    public Buffer body() {
        return body;
    }

    public String header(String key) {
        return headers.getFirst(key);
    }

    public List<String> headers(String key) {
        return headers.get(key);
    }


}
