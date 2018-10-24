package protocol.broadcast;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class LogEventHandler extends SimpleChannelInboundHandler<LogEvent> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, LogEvent msg) throws Exception {
        System.out.println(String.format("%d[ %s ][ %s ]%s", msg.getReceived(), msg.getSource(), msg.getLogfile(), msg.getMsg()));
    }
}
