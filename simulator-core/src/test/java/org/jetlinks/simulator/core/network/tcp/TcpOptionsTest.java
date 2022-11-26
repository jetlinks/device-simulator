package org.jetlinks.simulator.core.network.tcp;

import io.vertx.core.buffer.Buffer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class TcpOptionsTest {

    @Test
    void testLengthField() {
        TcpOptions options = new TcpOptions();
        options.setLengthField(new Integer[]{0, 4});
        List<Buffer> buffers = new ArrayList<>();

        Consumer<Buffer> input = options.createParser(buffers::add);
        input.accept(Buffer.buffer().appendInt(4));
        assertEquals(0, buffers.size());

        input.accept(Buffer.buffer().appendBytes(new byte[4]));

        assertEquals(1, buffers.size());

        input.accept(Buffer.buffer().appendBytes(new byte[6]));
        assertEquals(2, buffers.size());
        input.accept(Buffer.buffer().appendBytes(new byte[2]));
        assertEquals(3, buffers.size());
    }

    @Test
    void testParseFixed() {
        TcpOptions options = new TcpOptions();
        options.setFixedLength(4);

        List<Buffer> buffers = new ArrayList<>();

        Consumer<Buffer> input = options.createParser(buffers::add);

        input.accept(Buffer.buffer(new byte[]{0x01, 0x00}));
        assertEquals(0, buffers.size());

        input.accept(Buffer.buffer(new byte[]{0x01, 0x00, 0x02, 0x03}));
        assertEquals(1, buffers.size());

        input.accept(Buffer.buffer(new byte[]{0x01, 0x00}));
        assertEquals(2, buffers.size());
    }
}