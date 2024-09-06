package org.jetlinks.simulator.core.network.coap;

import lombok.Getter;
import lombok.Setter;

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

    public CoapOptions() {

    }


    public CoapOptions(CoapOptions options) {
        setId(options.getId());
        setBasePath(options.getBasePath());
        setBindAddress(options.getBindAddress());
        setOptions(options.getOptions());
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

    public CoapOptions copy() {
        return new CoapOptions(this);
    }

    private CoapOptions apply(Map<String, Object> args) {
        if (id == null) {
            return this;
        }
        for (Map.Entry<String, Object> entry : args.entrySet()) {
            String key = "{" + entry.getKey() + "}";
            String value = String.valueOf(entry.getValue());

            id = id.replace(key, value);
        }
        return this;
    }

    public CoapOptions refactor(Map<String, Object> args) {
        return copy().apply(args);
    }
}
