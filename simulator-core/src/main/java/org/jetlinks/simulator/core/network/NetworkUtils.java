package org.jetlinks.simulator.core.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

public class NetworkUtils {


    public static ByteBuf castToByteBuf(Object buf) {
        if (buf instanceof ByteBuf) {
            return ((ByteBuf) buf);
        }
        if (buf instanceof byte[]) {
            return Unpooled.wrappedBuffer(((byte[]) buf));
        }
        String str = String.valueOf(buf);
        if (str.charAt(0) == '0' && str.charAt(1) == 'x') {
            try {
                str = str.replace(" ", "");
                return Unpooled.wrappedBuffer(ByteBufUtil.decodeHexDump(str, 2, str.length() - 2));
            } catch (Throwable e) {
                throw new IllegalArgumentException("Bad hex data: " + str);
            }
        }

        return Unpooled.wrappedBuffer(str.getBytes());
    }
}
