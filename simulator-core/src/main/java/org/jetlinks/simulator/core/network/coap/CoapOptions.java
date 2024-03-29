package org.jetlinks.simulator.core.network.coap;

import lombok.Getter;
import lombok.Setter;

import java.net.InetSocketAddress;
import java.util.Map;

@Getter
@Setter
public class CoapOptions {
    private String id;

    private String basePath;

    private Map<String, String> options;

    private String bindAddress;

    public void setBasePath(String basePath) {
        this.basePath = basePath.endsWith("/") ? basePath.substring(0, basePath.length() - 1) : basePath;
    }

    public String createUri(String uri) {
        if (uri.startsWith("coap")) {
            return uri;
        }
        if (!uri.startsWith("/")) {
            return basePath + "/" + uri;
        }
        return basePath + uri;
    }
}
