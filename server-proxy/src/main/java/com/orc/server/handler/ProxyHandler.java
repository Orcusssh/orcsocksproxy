package com.orc.server.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;

public class ProxyHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(ProxyHandler.class);

    private String host ;

    private int port;

    private AtomicReference<Channel> remoteChannel = new AtomicReference<>();

    public ProxyHandler(String host, int port, final ChannelHandlerContext clientChannelContext, Object msg){
        this.host = host;
        this.port = port;
        init(clientChannelContext, msg);
    }

    private void init(final ChannelHandlerContext clientChannelContext, Object msg){
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(clientChannelContext.channel().eventLoop()).channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5 * 1000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new ChannelInboundHandlerAdapter(){
                            @Override
                            public void channelRead(ChannelHandlerContext ctx0, Object msg) throws Exception {
                                logger.info("返回消息");
                                clientChannelContext.channel().writeAndFlush(msg);
                            }

                        });
                    }
                });
        ChannelFuture channelFuture = bootstrap.connect(host, port);
        channelFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    logger.info("连接目标服务器");
                    logger.info("connect success host = " + host + ",port = " + port);
                    remoteChannel.set(future.channel());
                   // future.channel().writeAndFlush(msg);
                } else {
                    logger.info("connect fail host = " + host + ",port = " + port);
                    clientChannelContext.close();
                }
            }
        });
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        //directProxy(ctx, msg);
        logger.info("收到客户端消息");
        if (remoteChannel.get() != null) {
            remoteChannel.get().writeAndFlush(msg);
        }
    }

    //直连
    private void directProxy( ChannelHandlerContext clientChannelContext, final Object msg){

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(clientChannelContext.channel().eventLoop())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        //ch.pipeline().addLast(new LoggingHandler());//in out
                        //将目标服务器信息转发给客户端
                        ch.pipeline().addLast(new ChannelInboundHandlerAdapter(){
                            @Override
                            public void channelRead(ChannelHandlerContext ctx0, Object msg) throws Exception {
                                logger.info("返回消息");
                                clientChannelContext.channel().writeAndFlush(msg);
                            }

                        });
                    }
                });
        logger.info("连接目标服务器");
        ChannelFuture future = bootstrap.connect(host, port);
        future.addListener(new ChannelFutureListener() {

            public void operationComplete(final ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    logger.info("收到客户端消息");
                    future.channel().writeAndFlush(msg);
                } else {
                    clientChannelContext.channel().close();
                }
            }

        });

    }
}
