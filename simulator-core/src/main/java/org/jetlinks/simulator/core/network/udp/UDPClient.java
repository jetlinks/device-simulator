package org.jetlinks.simulator.core.network.udp;

import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.datagram.DatagramPacket;
import io.vertx.core.datagram.DatagramSocket;
import org.jetlinks.simulator.core.Global;
import org.jetlinks.simulator.core.network.*;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class UDPClient extends AbstractConnection {
    private final String id;
    private final DatagramSocket socket;

    private final InetSocketAddress remote;

    private final Address address;

    private final List<Consumer<DatagramPacket>> handlers = new CopyOnWriteArrayList<>();

    public UDPClient(String id,
                     DatagramSocket socket,
                     InetSocketAddress remote,
                     Address address) {
        this.id = id;
        this.socket = socket;
        this.remote = remote;
        this.address = address;
        changeState(State.connected);
        socket.handler(packet -> {
            received(packet.data().length());

            for (Consumer<DatagramPacket> handler : handlers) {
                handler.accept(packet);
            }
        });
    }

    public InetSocketAddress getRemote() {
        return remote;
    }

    public InetSocketAddress getLocal() {
        return InetSocketAddress.createUnresolved(
                socket.localAddress().host(),
                socket.localAddress().port()
        );
    }

    public static Mono<UDPClient> create(UDPOptions options) {
        Address address = AddressManager.global().takeAddress(options.getLocalAddress());
        options.setLocalAddress(address.getAddress().getHostAddress());
        return Mono.fromCompletionStage(() -> {
            options.setReusePort(true);
            return Global
                    .vertx()
                    .createDatagramSocket(options)
                    .listen(0, options.getLocalAddress())
                    .map(socket -> new UDPClient(options.getId(),
                                                 socket,
                                                 InetSocketAddress.createUnresolved(options.getHost(), options.getPort()),
                                                 address))
                    .toCompletionStage();

        });
    }

    @Override
    public String getId() {
        return id;
    }

    public void send(Object packet) {
        sendAsync(packet)
                .subscribe();
    }

    public Disposable handle(Consumer<DatagramPacket> consumer) {
        this.handlers.add(consumer);
        return () -> this.handlers.remove(consumer);
    }

    public Mono<Void> sendAsync(String host, int port, Object packet) {
        ByteBuf buf = NetworkUtils.castToByteBuf(packet);
        Buffer buffer = Buffer.buffer(buf);
        int len = buffer.length();
        return Mono.fromCompletionStage(() -> socket
                           .send(Buffer.buffer(buf), port, host)
                           .toCompletionStage())
                   .doAfterTerminate(() -> {
                       sent(len);
                       ReferenceCountUtil.safeRelease(buf);
                   })
                   .doOnError(this::error);
    }

    public Mono<Void> sendAsync(Object packet) {
        return sendAsync(remote.getHostString(), remote.getPort(), packet);
    }

    @Override
    protected void doDisposed() {
        address.release();
        socket.close();
        super.doDisposed();
    }

    @Override
    public NetworkType getType() {
        return NetworkType.udp_client;
    }

    @Override
    public boolean isAlive() {
        return true;
    }
}
