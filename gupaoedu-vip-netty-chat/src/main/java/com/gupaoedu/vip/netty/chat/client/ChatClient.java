package com.gupaoedu.vip.netty.chat.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.IOException;

import com.gupaoedu.vip.netty.chat.client.handler.ChatClientHandler;
import com.gupaoedu.vip.netty.chat.protocol.IMDecoder;
import com.gupaoedu.vip.netty.chat.protocol.IMEncoder;

/**
 * 客户端
 *
 * @author Tom
 */
public class ChatClient {

    private ChatClientHandler clientHandler;
    private String host;
    private int port;

    public ChatClient(String nickName) {
        this.clientHandler = new ChatClientHandler(nickName);
    }

    /**
     * 建立客户端与服务器之间的连接。
     * 使用Netty框架通过NIO（非阻塞I/O）方式实现连接建立和数据传输。
     *
     * @param host 客户端连接的服务器主机名
     * @param port 客户端连接的服务器端口号
     */
    public void connect(String host, int port) {
        // 保存主机名
        this.host = host;
        // 保存端口号
        this.port = port;

        // 创建用于处理I/O操作的EventLoopGroup
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            // 配置客户端连接参数
            Bootstrap b = new Bootstrap();
            b.group(workerGroup);
            // 选择NioSocketChannel作为通信通道
            b.channel(NioSocketChannel.class);
            // 开启SO_KEEPALIVE选项
            b.option(ChannelOption.SO_KEEPALIVE, true);

            // 设置ChannelInitializer，在连接建立后对SocketChannel进行初始化
            b.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    // 添加解码器、编码器和客户端处理器到处理链
                    ch.pipeline().addLast(new IMDecoder());
                    ch.pipeline().addLast(new IMEncoder());
                    ch.pipeline().addLast(clientHandler);
                }
            });

            // 连接到指定的服务器
            ChannelFuture f = b.connect(this.host, this.port).sync();

            // 等待连接关闭
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            // 释放资源，关闭EventLoopGroup
            workerGroup.shutdownGracefully();
        }
    }


    public static void main(String[] args) throws IOException {
        new ChatClient("Cover").connect("127.0.0.1", 8080);

        String url = "http://localhost:8080/images/a.png";
        System.out.println(url.toLowerCase().matches(".*\\.(gif|png|jpg)$"));

    }

}
