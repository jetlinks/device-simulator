package org.jetlinks.simulator.core.network.http;

import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import lombok.Getter;
import org.jetlinks.simulator.core.Connection;
import org.jetlinks.simulator.core.Global;
import org.jetlinks.simulator.core.network.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

public class HTTPClient extends AbstractConnection {

    public static String statusCountAttr(HttpStatus status) {
        return Connection.statusCountAttr(status.name());
    }

    private final String id;

    @Getter
    private final String basePath;

    private final HttpClient client;

    @Getter
    private final HttpHeaders headers;

    private final Address address;

    private HTTPClient(String id, String basePath, HttpHeaders headers, Address address, HttpClient client) {
        this.id = id;
        this.basePath = basePath.endsWith("/") ? basePath.substring(0, basePath.length() - 1) : basePath;
        this.client = client;
        this.headers = headers == null ? HttpHeaders.EMPTY : headers;
        this.address = address;
        changeState(State.connected);
    }


    public static Mono<HTTPClient> create(HTTPClientOptions options) {
        Address addr = AddressManager.global().takeAddress();
        try {
            options.setLocalAddress(addr.getAddress().getHostAddress());
            return Mono.just(
                    new HTTPClient(
                            options.getId(),
                            options.getBasePath(),
                            options.getHeaders(),
                            addr,
                            Global.vertx().createHttpClient(options)
                    )
            );
        } catch (Throwable err) {
            addr.release();
            throw err;
        }
    }

    public Mono<HttpResponse> patchJsonAsync(String path, Object body) {
        return patchAsync(path, body, MediaType.APPLICATION_JSON_VALUE);
    }

    public Mono<HttpResponse> patchAsync(String path, Object body, String mediaType) {

        return request(HttpMethod.PATCH, path, body, mediaType);
    }

    public Mono<HttpResponse> putJsonAsync(String path, Object body) {
        return putAsync(path, body, MediaType.APPLICATION_JSON_VALUE);
    }

    public Mono<HttpResponse> putAsync(String path, Object body, String mediaType) {

        return request(HttpMethod.PUT, path, body, mediaType);
    }

    public Mono<HttpResponse> postJsonAsync(String path, Object body) {
        return postAsync(path, body, MediaType.APPLICATION_JSON_VALUE);
    }

    public Mono<HttpResponse> postAsync(String path, Object body, String mediaType) {

        return request(HttpMethod.POST, path, body, mediaType);
    }

    public void request(Map<String, Object> map) {
        requestAsync(map)
                .subscribe();
    }

    public Mono<HttpResponse> requestAsync(Map<String, Object> map) {
        HttpMethod method = HttpMethod.valueOf(String.valueOf(map.getOrDefault("method", "GET")).toUpperCase());
        String path = String.valueOf(map.getOrDefault("path", "/"));
        Object body = map.get("body");
        String contentType = (String) map.get("contentType");
        @SuppressWarnings("all")
        Map<String, Object> headers = (Map<String, Object>) map.getOrDefault("headers", Collections.emptyMap());
        HttpHeaders httpHeaders = new HttpHeaders();
        if (headers != null) {
            headers.forEach((k, v) -> httpHeaders.add(k, String.valueOf(v)));
        }

        return request(method, path, body, contentType, httpHeaders);

    }

    public Mono<HttpResponse> request(HttpMethod method, String path, Object body, String mediaType, HttpHeaders headers) {
        ByteBuf payload = NetworkUtils.castToByteBuf(body);
        Buffer buffer = Buffer.buffer(payload);
        int len = buffer.length();
        return Mono.defer(() -> request(
                           createRequest(method, path),
                           headers,
                           req -> {
                               if (null != mediaType) {
                                   req.putHeader(HttpHeaders.CONTENT_TYPE, mediaType);
                               }
                               return req.send(buffer);
                           }))
                   .doAfterTerminate(() -> {
                       sent(len);
                       ReferenceCountUtil.safeRelease(payload);
                   });

    }

    public Mono<HttpResponse> request(HttpMethod method, String path, Object body, String mediaType) {
        return request(method, path, body, mediaType, null);
    }

    public Mono<HttpResponse> deleteAsync(String path) {
        return Mono.defer(() -> request(createRequest(HttpMethod.DELETE, path), null, HttpClientRequest::connect));
    }


    public Mono<HttpResponse> getAsync(String path) {
        return Mono.defer(() -> request(createRequest(HttpMethod.GET, path), null, HttpClientRequest::connect));
    }

    private Future<HttpClientRequest> createRequest(HttpMethod method, String path) {
        URI uri = URI.create(getPath(path));
        String pathAll = uri.getPath();
        if (StringUtils.hasText(uri.getQuery())) {
            pathAll = pathAll + "?" + uri.getQuery();
        }
        return client.request(method, uri.getPort(), uri.getHost(), pathAll);
    }

    private Mono<HttpResponse> request(Future<HttpClientRequest> feature,
                                       HttpHeaders headers,
                                       Function<HttpClientRequest, Future<HttpClientResponse>> sender) {
        return Mono
                .fromCompletionStage(feature.toCompletionStage())
                .doOnNext(request -> {
                    if (this.headers != null) {
                        this.headers.forEach(request::putHeader);
                    }
                    if (headers != null) {
                        headers.forEach(request::putHeader);
                    }
                })
                .flatMap(res -> Mono
                        .fromCompletionStage(sender.apply(res).toCompletionStage())
                        .flatMap(response -> {
                            HttpHeaders head = new HttpHeaders();
                            response.headers().forEach(head::add);
                            return Mono
                                    .fromCompletionStage(response.body().toCompletionStage())
                                    .map(body -> {
                                        received(body.length());
                                        HttpStatus status = HttpStatus.valueOf(response.statusCode());
                                        incr(statusCountAttr(status));
                                        return new HttpResponse(response.version(), status, body, head);
                                    });
                        })
                        .doAfterTerminate(res::end))
                .doOnError(this::error);
    }


    private String getPath(String path) {
        if (path == null) {
            return basePath;
        }
        if ((path.startsWith("http://") || path.startsWith("https://"))) {
            return path;
        }
        if (!path.startsWith("/")) {
            return basePath + "/" + path;
        }
        return basePath + path;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public NetworkType getType() {
        return NetworkType.http_client;
    }

    @Override
    public boolean isAlive() {
        return true;
    }

    @Override
    protected void doDisposed() {
        super.doDisposed();
        address.release();
        client.close();

    }
}
