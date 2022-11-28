package org.jetlinks.simulator.core.network.tcp;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.parsetools.RecordParser;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

@Getter
@Setter
public class TcpOptions extends NetClientOptions {

    private String id;

    private String host;

    private int port;

    private Integer fixedLength;

    private String delimited;

    private Integer[] lengthField;

    public TcpOptions() {

    }

    public Consumer<Buffer> createParser(Consumer<Buffer> handler) {
        RecordParser parser = null;
        if (fixedLength != null) {
            parser = RecordParser
                    .newFixed(fixedLength)
                    .handler(handler::accept);
        } else if (delimited != null) {
            parser = RecordParser
                    .newDelimited(delimited)
                    .handler(handler::accept);
        }
        if (lengthField != null && lengthField.length > 0) {
            int len = lengthField.length >= 2 ? lengthField[1] : lengthField[0];
            int offset = lengthField.length >= 2 ? lengthField[0] : 0;

            RecordParser temp = RecordParser.newFixed(len + offset);
            AtomicReference<Buffer> current = new AtomicReference<>();

            Function<Buffer, Integer> fieldReader;
            if (len == 1) {
                fieldReader = buf -> (int) buf.getUnsignedByte(offset);
            } else if (len == 2) {
                fieldReader = buf -> buf.getUnsignedShort(offset);
            } else if (len == 3) {
                fieldReader = buf -> buf.getMedium(offset);
            } else if (len == 4) {
                fieldReader = buf -> buf.getInt(offset);
            } else {
                throw new IllegalArgumentException("lengthField only support [2,3,4]");
            }

            parser = temp.handler(buff -> {
                if (current.get() == null) {
                    current.set(buff);
                    int next = fieldReader.apply(buff);
                    temp.fixedSizeMode(next);
                } else {
                    temp.fixedSizeMode(len + offset);
                    Buffer buffer = current.getAndSet(null);
                    handler.accept(buffer.appendBuffer(buff));
                }
            });
        }
        return parser == null ? handler : parser::handle;
    }

    private TcpOptions(TcpOptions options) {
        super(options);
        this.id = options.getId();
        this.host = options.getHost();
        this.port = options.getPort();
        this.lengthField = options.getLengthField();
        this.fixedLength = options.getFixedLength();
        this.delimited = options.getDelimited();
    }

    @Override
    public String toString() {
        return "tcp://" + host + ":" + port;
    }

    public TcpOptions copy() {
        return new TcpOptions(this);
    }

    private TcpOptions apply(Map<String, Object> args) {
        if (id == null) {
            return this;
        }
        for (Map.Entry<String, Object> entry : args.entrySet()) {
            String key = "${" + entry.getKey() + "}";
            String value = String.valueOf(entry.getValue());

            id = id.replace(key, value);
        }

        return this;
    }

    public TcpOptions refactor(Map<String, Object> args) {
        return copy().apply(args);
    }

}
