package org.jetlinks.simulator.core.network.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.simulator.core.ExceptionUtils;
import org.jetlinks.simulator.core.network.AbstractConnection;
import org.jetlinks.simulator.core.network.NetworkType;
import org.jetlinks.simulator.core.network.NetworkUtils;
import reactor.core.publisher.Mono;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

@Slf4j
public class TcpClient extends AbstractConnection {

    static final AtomicLong count = new AtomicLong();
    public static final String ATTR_ADDRESS = "address";
    private final String id;
    private final NetSocket socket;

    private final List<Consumer<Buffer>> bufferHandlers = new CopyOnWriteArrayList<>();

    private Consumer<Buffer> parser;

    public TcpClient(String id, NetSocket socket) {
        this.id = id;
        this.socket = socket;
        attribute(ATTR_ADDRESS, socket.localAddress().host() + ":" + socket.localAddress().port());


        this.socket
                .handler(buffer -> {
                    received(buffer.length());
                    parser.accept(buffer);
                })
                .closeHandler((ignore) -> changeState(State.closed));
        changeState(State.connected);
    }


    private void handleBuffer(Buffer buffer) {
        for (Consumer<Buffer> bufferHandler : bufferHandlers) {
            try {
                bufferHandler.accept(buffer);
            } catch (Throwable e) {
                log.warn("handle socket error :{}", ExceptionUtils.getErrorMessage(e));
            }
        }
    }

    public static Mono<TcpClient> connect(Vertx vertx, TcpOptions tcpOptions) {
        return Mono.create(sink -> vertx
                .createNetClient(tcpOptions)
                .connect(tcpOptions.getPort(), tcpOptions.getHost())
                .map(socket -> {
                    try {
                        String id = tcpOptions.getId() == null ? "tcp-client-" + count.incrementAndGet() : tcpOptions.getId();
                        TcpClient client = new TcpClient(id, socket);
                        client.parser = tcpOptions.createParser(client::handleBuffer);
                        sink.success(client);
                    } catch (Throwable e) {
                        sink.error(e);
                        socket.close();
                    }
                    return socket;
                })
                .onFailure(sink::error));
    }


    @Override
    public String getId() {
        return id;
    }

    public TcpClient handlePayload(Consumer<Buffer> buffer) {
        this.bufferHandlers.add(buffer);
        return this;
    }

    public ByteBuf newBuffer() {
        return Unpooled.buffer();
    }

    public String toHex(Object data){
        return ByteBufUtil.hexDump(NetworkUtils.castToByteBuf(data));
    }

    public void send(Object data) {
        sendAsync(data)
                .subscribe(
                        ignore -> {
                        },
                        error -> {
                            log.warn("send tcp [{}] error:{}", id, ExceptionUtils.getErrorMessage(error));
                        });
    }

    public void sendFile(String data) {
        sendFileAsync(new File(data))
                .subscribe(
                        ignore -> {
                        },
                        error -> log.warn("send tcp file [{}] error:{}", id, ExceptionUtils.getErrorMessage(error)));
    }

    public Mono<Void> sendAsync(Object data) {
        return Mono.create(sink -> {
            ByteBuf buf = NetworkUtils.castToByteBuf(data);

            Buffer buffer = Buffer.buffer(buf);
            int len = buffer.length();
            socket.write(buffer, (res) -> {
                try {
                    if (res.succeeded()) {
                        sent(len);
                        sink.success();
                    } else {
                        sink.error(res.cause());
                    }
                } finally {
                    ReferenceCountUtil.safeRelease(buf);
                }
            });

        });
    }

    public Mono<Void> sendFileAsync(String file) {
        return sendFileAsync(new File(file));
    }

    public Mono<Void> sendFileAsync(File file) {
        return Mono.create(sink -> {
            long len = file.length();
            socket.sendFile(file.getAbsolutePath(), (res) -> {
                if (res.succeeded()) {
                    sent((int) len);
                    sink.success();
                } else {
                    sink.error(res.cause());
                }
            });

        });
    }


    @Override
    public NetworkType getType() {
        return NetworkType.tcp_client;
    }

    @Override
    public boolean isAlive() {
        return state() != State.closed;
    }
}