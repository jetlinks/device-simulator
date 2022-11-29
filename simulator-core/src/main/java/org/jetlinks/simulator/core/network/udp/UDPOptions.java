package org.jetlinks.simulator.core.network.udp;

import io.vertx.core.datagram.DatagramSocketOptions;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.simulator.core.network.tcp.TcpOptions;

import java.util.Map;

@Getter
@Setter
public class UDPOptions extends DatagramSocketOptions {

    private String id;

    private String host;
    private int port;

    public UDPOptions() {
    }

    public UDPOptions(UDPOptions options) {
        super(options);
        this.id = options.getId();
        this.host = options.getHost();
        this.port = options.getPort();
    }

    public UDPOptions copy() {
        return new UDPOptions(this);
    }

    private UDPOptions apply(Map<String, Object> args) {
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

    public UDPOptions refactor(Map<String, Object> args) {
        return copy().apply(args);
    }

}
