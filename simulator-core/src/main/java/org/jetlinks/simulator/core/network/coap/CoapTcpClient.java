package org.jetlinks.simulator.core.network.coap;

import io.netty.buffer.ByteBufUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.collections.MapUtils;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.*;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.config.TcpConfig;
import org.eclipse.californium.elements.tcp.netty.TcpClientConnector;
import org.jetlinks.core.message.codec.CoapMessage;
import org.jetlinks.protocol.official.ObjectMappers;
import org.jetlinks.simulator.core.network.AbstractConnection;
import org.jetlinks.simulator.core.network.Address;
import org.jetlinks.simulator.core.network.AddressManager;
import org.jetlinks.simulator.core.network.NetworkType;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@AllArgsConstructor
public class CoapTcpClient extends AbstractConnection {
    static Configuration configuration = new Configuration()
            .set(CoapConfig.MAX_RESOURCE_BODY_SIZE, 65535)
            .set(TcpConfig.TCP_WORKER_THREADS, 1)
            .set(TcpConfig.TCP_CONNECT_TIMEOUT, 100, TimeUnit.MILLISECONDS)
            .set(TcpConfig.TCP_CONNECTION_IDLE_TIMEOUT, 100, TimeUnit.SECONDS);


    private final String id;

    @Getter
    private final String basePath;

    private final org.eclipse.californium.core.CoapClient client;

    @Getter
    private final Address address;

    @Getter
    private final CoapOptions options;

    public CoapTcpClient(CoapOptions options) {
        this.id = options.getId();
        this.basePath = options.getBasePath();
        this.options = options;
        client = new org.eclipse.californium.core.CoapClient();

        address = AddressManager
                .global()
                .takeAddress(options.getBindAddress());

        client.setEndpoint(CoapEndpoint
                .builder()
                .setConfiguration(configuration)
                .setConnector(new TcpClientConnector(configuration))
                .build());
        changeState(State.connected);
    }


    public static Mono<CoapTcpClient> create(CoapOptions options) {
        Address address = AddressManager.global().takeAddress(options.getBindAddress());
        try {
            options.setBindAddress(address.getAddress().getHostAddress());
            return Mono.just(new CoapTcpClient(options));
        } catch (Throwable err) {
            address.release();
            throw err;
        }
    }

    @Override
    protected void doDisposed() {
        super.doDisposed();
        address.release();
    }

    private String getFullUri(String uri) {
        return options.createUri(uri);
    }

    @SneakyThrows
    private byte[] parsePayload(Object payload, String format) {
        if (payload instanceof byte[]) {
            return (byte[]) payload;
        }
        if (payload instanceof String) {
            String hexMaybe = ((String) payload);
            if (hexMaybe.startsWith("0x")) {
                return ByteBufUtil.decodeHexDump(hexMaybe, 2, hexMaybe.length() - 2);
            }
            return hexMaybe.getBytes();
        }
        MediaType mediaType = MediaType.valueOf(format);

        if (mediaType.includes(MediaType.APPLICATION_CBOR)) {
            return ObjectMappers.CBOR_MAPPER.writeValueAsBytes(payload);
        } else if (mediaType.includes(MediaType.APPLICATION_JSON)) {
            return ObjectMappers.JSON_MAPPER.writeValueAsBytes(payload);
        }
        return String.valueOf(payload).getBytes();
    }


    public static OptionSet convertToOptions(Map<String, String> options) {
        if (MapUtils.isEmpty(options)) {
            return new OptionSet();
        }
        return convertToOptions(
                options
                        .entrySet()
                        .stream()
                        .map(e -> CoapMessage.parseOption(e.getKey(), e.getValue()))
                        .collect(Collectors.toList())
        );
    }

    public static OptionSet convertToOptions(Collection<Option> options) {
        OptionSet opts = new OptionSet();
        options.forEach(opts::addOption);
        return opts;
    }

    public void request(Map<String, Object> map) {
        requestAsync(map)
                .subscribe();
    }

    public Mono<CoapResponse> requestAsync(Map<String, Object> map) {
        CoAP.Code code = CoAP.Code.valueOf(String.valueOf(map.getOrDefault("code", "POST")).toUpperCase());


        String uri = String.valueOf(map.getOrDefault("uri", "/"));
        String contentType = String.valueOf(map.getOrDefault("contentType", "application/json"));
        Object payload = map.get("payload");
        @SuppressWarnings("all")
        Map<String, String> options = (Map<String, String>) map.getOrDefault("options", Collections.emptyMap());
        return advancedAsync(code, uri, payload, contentType, options);
    }


    public Mono<CoapResponse> getAsync(String uri,
                                       String format,
                                       Map<String, String> opts) {
        return advancedAsync(CoAP.Code.GET, uri, null, format, opts);
    }

    public Mono<CoapResponse> postAsync(String uri,
                                        Object payload,
                                        String format,
                                        Map<String, String> opts) {
        return advancedAsync(CoAP.Code.POST, uri, payload, format, opts);
    }

    public Mono<CoapResponse> putAsync(String uri,
                                       Object payload,
                                       String format,
                                       Map<String, String> opts) {
        return advancedAsync(CoAP.Code.PUT, uri, payload, format, opts);
    }

    public Mono<CoapResponse> patchAsync(String uri,
                                         Object payload,
                                         String format,
                                         Map<String, String> opts) {
        return advancedAsync(CoAP.Code.PATCH, uri, payload, format, opts);
    }


    @SneakyThrows
    public Mono<CoapResponse> advancedAsync(CoAP.Code code,
                                            String uri,
                                            Object payload,
                                            String format,
                                            Map<String, String> opts) {

        Request request = new Request(code);

        OptionSet optionSet = convertToOptions(opts);

        URI uriObj = new URI(getFullUri(uri));


        optionSet.setUriPath(uriObj.getPath());

        optionSet.setContentFormat(MediaTypeRegistry.parse(format));

        request
                .setURI(uriObj)
                .setOptions(optionSet);

        if (payload != null) {
            request.setPayload(parsePayload(payload, format));
        }

        return Mono.create(sink -> {
            client.advanced(new CoapHandler() {
                @Override
                public void onLoad(CoapResponse response) {
                    sink.success(response);
                }

                @Override
                public void onError() {
                    sink.success();
                }
            }, request);
        });
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public NetworkType getType() {
        return NetworkType.coap_tcp_client;
    }

    @Override
    public boolean isAlive() {
        return true;
    }



}
