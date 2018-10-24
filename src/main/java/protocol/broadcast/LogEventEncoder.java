package protocol.broadcast;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.util.CharsetUtil;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * 数据出站编码
 */
public class LogEventEncoder extends MessageToMessageEncoder<LogEvent> {

    private final InetSocketAddress address;

    public LogEventEncoder(InetSocketAddress address) {
        this.address = address;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, LogEvent logEvent, List<Object> out) throws Exception {
        byte[] msg = logEvent.getMsg().getBytes(CharsetUtil.UTF_8);
        byte[] file = logEvent.getLogfile().getBytes(CharsetUtil.UTF_8);
        ByteBuf byteBuf = ctx.alloc().buffer(msg.length + file.length + 1);
        byteBuf.writeBytes(getBytes(logEvent.getLogfile()))
                .writeByte(LogEvent.SEPARATOR)
                .writeBytes(getBytes(logEvent.getMsg()));
        out.add(new DatagramPacket(byteBuf, address));
    }

    private byte[] getBytes(String str) {
        return str.getBytes(CharsetUtil.UTF_8);
    }
}
