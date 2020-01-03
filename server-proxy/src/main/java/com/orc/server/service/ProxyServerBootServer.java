package com.orc.server.service;

import com.orc.common.coder.AuthRequestMessageDecoder;
import com.orc.common.handler.DelimiterOutboundHandler;
import com.orc.common.message.DelimiterMessage;
import com.orc.server.config.CommonConfiguration;
import com.orc.server.config.ServerAuthConfiguration;
import com.orc.server.handler.InitialServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

@Service
public class ProxyServerBootServer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(ProxyServerBootServer.class);

    @Autowired
    private CommonConfiguration commonConfiguration;

    @Autowired
    private ServerAuthConfiguration serverAuthConfiguration;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        EventLoopGroup boss = new NioEventLoopGroup(1);
        EventLoopGroup worker = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000*10)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline cp = ch.pipeline();
                            cp.addLast(new DelimiterOutboundHandler());
                            cp.addLast(new DelimiterBasedFrameDecoder(1024 * 1000 * 50, DelimiterMessage.getDelimiterBuf()));
                            cp.addLast(new InitialServerHandler(commonConfiguration, serverAuthConfiguration));
                        }
                    });

            ChannelFuture future = bootstrap.bind(commonConfiguration.getLocalPort()).sync();
            logger.debug("bind port : " + commonConfiguration.getLocalPort());
            future.channel().closeFuture().sync();
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }
}
