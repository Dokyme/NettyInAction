package protocol.broadcast;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/**
 * 日志时间广播器
 * 是客户端
 */
public class LogEventBroadcaster {

    private final EventLoopGroup group;
    private final Bootstrap bootstrap;
    private final File file;

    public LogEventBroadcaster(InetSocketAddress address, File file) {
        group = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST, true)
                .handler(new LogEventEncoder(address));
        this.file = file;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        LogEventBroadcaster broadcaster = new LogEventBroadcaster(new InetSocketAddress("255.255.255.255", 10000), new File("testLogFile"));
        try {
            broadcaster.run();
        } catch (Exception e) {
            broadcaster.stop();
        }
    }

    public void stop() {
        group.shutdownGracefully();
    }

    /**
     * 周期性监视文件变更，通过广播发送粗去
     *
     * @throws Exception
     */
    public void run() throws Exception {
        Channel channel = bootstrap.bind(0).sync().channel();
        long ptr = 0;
        while (true) {
            long len = file.length();
            if (len < ptr) {
                ptr = len;
            } else if (len > ptr) {
                RandomAccessFile raf = new RandomAccessFile(file, "r");
                raf.seek(ptr);
                String line;
                while ((line = raf.readLine()) != null) {
                    channel.writeAndFlush(new LogEvent(null, file.getAbsolutePath(), line, -1));
                }
                ptr = raf.getFilePointer();
                raf.close();
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.interrupted();
            }
        }
    }
}
