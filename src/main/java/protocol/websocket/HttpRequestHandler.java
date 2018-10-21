package protocol.websocket;

import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedNioFile;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.net.URL;

public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final File INDEX;
    private static final String TEXT_HTML_UTF_8 = "text/html; charset=UTF-8";

    static {
        URL location = HttpRequestHandler.class.getProtectionDomain().getCodeSource().getLocation();
        try {
            String path = location.toURI() + "index.html";
            path = !path.contains("file") ? path : path.substring(5);
            INDEX = new File(path);
        } catch (URISyntaxException u) {
            u.printStackTrace();
            throw new IllegalStateException("Unable to load index.html", u);
        }
    }

    private final String wsUri;

    public HttpRequestHandler(String wsUri) {
        this.wsUri = wsUri;
    }

    private static void send100Continue(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE);
        ctx.writeAndFlush(response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        if (msg.uri().equalsIgnoreCase(wsUri)) {
            //发送给下一个inboundHandler，并且增加msg的引用计数来让下一个handler读取
            System.out.println(ctx.channel() + " received ws request");
            ctx.fireChannelRead(msg.retain());
        } else {
            if (HttpUtil.is100ContinueExpected(msg)) {
                //HTTP1.1：在客户端发送请求之前，先试探性地发送一个Expect: 100-continue在请求头中，让服务器决定是否愿意接受真正的请求
                //这对于节省POST请求带宽有很大的帮助
                send100Continue(ctx);
            }
            RandomAccessFile randomAccessFile = new RandomAccessFile(INDEX, "r");
            HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, TEXT_HTML_UTF_8);
            boolean isKeepAlive = HttpUtil.isKeepAlive(msg);
            if (isKeepAlive) {
                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, randomAccessFile.length());
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            }
            ctx.write(response);
            if (ctx.pipeline().get(SslHandler.class) == null) {
                ctx.write(new DefaultFileRegion(randomAccessFile.getChannel(), 0, randomAccessFile.length()));
            } else {
                ctx.write(new ChunkedNioFile(randomAccessFile.getChannel()));
            }
            ChannelFuture future = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
            if (!isKeepAlive) {
                //如果不是keepAlive，则在最后一次写完数据之后关闭channel
                future.addListener(ChannelFutureListener.CLOSE);
            }
        }
    }
}
