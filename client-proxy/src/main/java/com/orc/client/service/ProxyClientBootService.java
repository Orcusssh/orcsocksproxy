package com.orc.client.service;

import com.orc.client.config.ClientAuthConfiguration;
import com.orc.client.config.CommonConfiguration;
import com.orc.client.config.PacConfiguration;
import com.orc.client.handler.ProxyIdleHandler;
import com.orc.client.handler.Socks5CommandRequestHandler;
import com.orc.client.handler.Socks5InitialRequestHandler;
import com.orc.client.handler.Socks5PasswordAuthRequestHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5InitialRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5ServerEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

@Service
public class ProxyClientBootService implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(ProxyClientBootService.class);

    @Autowired
    private CommonConfiguration commonConfiguration;

    @Autowired
    private ClientAuthConfiguration clientAuthConfiguration;

    @Autowired
    private PacConfiguration pacConfiguration;

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
                            //channel超时处理
                            ch.pipeline().addLast(new IdleStateHandler(30, 30, 0));
                            ch.pipeline().addLast(new ProxyIdleHandler());

                            //Socks5MessagByteBuf
                            ch.pipeline().addLast(Socks5ServerEncoder.DEFAULT);
                            //sock5 init
                            ch.pipeline().addLast(new Socks5InitialRequestDecoder());
                            //sock5 init
                            ch.pipeline().addLast(new Socks5InitialRequestHandler(clientAuthConfiguration.getOpen()));
                            if(clientAuthConfiguration.getOpen()) {
                                //socks auth
                                ch.pipeline().addLast(new Socks5PasswordAuthRequestDecoder());
                                //socks auth
                                ch.pipeline().addLast(new Socks5PasswordAuthRequestHandler(clientAuthConfiguration));
                            }
                            //socks connection
                            ch.pipeline().addLast(new Socks5CommandRequestDecoder());
                            //Socks connection
                            ch.pipeline().addLast(new Socks5CommandRequestHandler(commonConfiguration, pacConfiguration));
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
