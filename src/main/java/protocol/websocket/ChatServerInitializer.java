package protocol.websocket;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

public class ChatServerInitializer extends ChannelInitializer<Channel> {

    private static final int HTTP_AGGREAGATE_SIZE = 64 * 1024;
    private static final String WS_URL = "/ws";
    private final ChannelGroup group;

    public ChatServerInitializer(ChannelGroup group) {
        this.group = group;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        System.out.println("initChannel " + ch);
        ch.pipeline()
                .addLast(new HttpServerCodec())                             //从字节流到HTTP Request/Response的编解码
                .addLast(new ChunkedWriteHandler())                         //
                .addLast(new HttpObjectAggregator(HTTP_AGGREAGATE_SIZE))    //
                .addLast(new HttpRequestHandler(WS_URL))
                .addLast(new WebSocketServerProtocolHandler(WS_URL))        //根据WebSocket的规范，发送PING、PONG、CLOSE帧
                .addLast(new TextWebSocketFrameHandler(group));             //处理TEXT帧和握手（加入）事件
    }
}
