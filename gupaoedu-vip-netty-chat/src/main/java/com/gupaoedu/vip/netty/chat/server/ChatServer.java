package com.gupaoedu.vip.netty.chat.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

import java.io.IOException;

import com.gupaoedu.vip.netty.chat.protocol.IMDecoder;
import com.gupaoedu.vip.netty.chat.protocol.IMEncoder;
import com.gupaoedu.vip.netty.chat.server.handler.HttpServerHandler;
import com.gupaoedu.vip.netty.chat.server.handler.TerminalServerHandler;
import com.gupaoedu.vip.netty.chat.server.handler.WebSocketServerHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChatServer{
	
	private int port = 80;
    /**
     * 启动服务器，监听指定端口。
     * 
     * @param port 监听的端口号
     */
    public void start(int port){
        // 创建用于接收连接的EventLoopGroup
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        // 创建用于处理连接的EventLoopGroup
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            // 配置服务器启动参数
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    // 设置连接队列大小
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    // 配置通道初始化器，对每个新建立的连接进行配置
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            // 添加协议解析处理器
                            /** 解析自定义协议 */
                            pipeline.addLast(new IMDecoder());  // 解析输入流
                            pipeline.addLast(new IMEncoder());  // 编码输出流
                            pipeline.addLast(new TerminalServerHandler());  // 处理输入流
    
                            /** 解析Http请求 */
                            pipeline.addLast(new HttpServerCodec());  // 编码和解码Http请求
                            pipeline.addLast(new HttpObjectAggregator(64 * 1024)); // 将Http消息分片组合成完整的HttpRequest或HttpResponse
                            pipeline.addLast(new ChunkedWriteHandler()); // 处理大块数据传输
                            pipeline.addLast(new HttpServerHandler()); // 处理Http请求
    
                            /** 解析WebSocket请求 */
                            pipeline.addLast(new WebSocketServerProtocolHandler("/im")); // 设置WebSocket的路径前缀
                            pipeline.addLast(new WebSocketServerHandler()); // 处理WebSocket请求
                        }
                    });
            // 绑定端口并同步等待
            ChannelFuture f = b.bind(this.port).sync();
            log.info("服务已启动,监听端口" + this.port);
            // 等待通道关闭
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            // 关闭EventLoopGroup，释放资源
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
    public void start() {
        start(this.port);
    }
    
    
    public static void main(String[] args) throws IOException{
        if(args.length > 0) {
            new ChatServer().start(Integer.valueOf(args[0]));
        }else{
            new ChatServer().start();
        }
    }
    
}
