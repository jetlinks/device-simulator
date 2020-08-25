package org.jetlinks.simulator.tcp;

import com.alibaba.fastjson.JSON;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.simulator.core.AbstractSession;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

@Getter
@Setter
public class TcpSession extends AbstractSession {

    private NetSocket client;

    private NetClientOptions options;

    private Mono<NetSocket> connector;

    public TcpSession(String id, int index) {
        super(id, index);
    }

    @Override
    public void close() {
        client.close();
    }

    public Disposable handle(Consumer<EncodedMessage> consumer) {
        return onDownstream(consumer);
    }

    public void publish(Object data) {
        if (data instanceof String) {
            publish(String.valueOf(data));
            return;
        }
        if (data instanceof byte[]) {
            publish(Unpooled.wrappedBuffer(((byte[]) data)));
            return;
        }
        publish(JSON.toJSONString(data));
    }

    public void publish(String data) {
        publishAsync(data).subscribe();
    }

    public void publish(ByteBuf data) {
        publishAsync(data).subscribe();
    }

    public Mono<Void> publishAsync(String data) {
        if (data.startsWith("0x")) {
            return publishAsync(Unpooled.wrappedBuffer(ByteBufUtil.decodeHexDump(data, 2, data.length())));
        }
        return publishAsync(Unpooled.wrappedBuffer(data.getBytes()));
    }

    public Mono<Void> publishAsync(ByteBuf data) {
        return Mono.create(sink ->
                client.write(Buffer.buffer(data), result -> {
                    if (result.succeeded()) {
                        sink.success();
                    } else {
                        sink.error(result.cause());
                    }
                }));
    }

    private void init(NetSocket client) {
        this.client = client
                .handler(buffer -> downstream(EncodedMessage.simple(buffer.getByteBuf())))
                .closeHandler(nil -> disconnected());
    }

    @Override
    protected Mono<Void> doConnect() {
        return connector
                .doOnSuccess(this::init)
                .doOnError(this::error)
                .then();
    }
}
