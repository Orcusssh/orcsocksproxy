package com.orc.server.handler;

import com.orc.common.encrypt.CryptUtils;
import com.orc.common.encrypt.ICrypt;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicReference;

public class ProxyHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(ProxyHandler.class);

    private String host ;

    private int port;

    private ICrypt crypt;

    private AtomicReference<Channel> remoteChannel = new AtomicReference<>();

    public ProxyHandler(String host, int port, ICrypt crypt, final ChannelHandlerContext clientChannelContext, Object msg){
        this.host = host;
        this.port = port;
        this.crypt = crypt;
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
                                clientChannelContext.channel().writeAndFlush(CryptUtils.encrypt(crypt, (ByteBuf)msg));
                            }

                        });
                    }
                });
        try {
            ChannelFuture channelFuture = bootstrap.connect(InetAddress.getByName(host), port);
            channelFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        logger.info("连接目标服务器");
                        logger.info("connect success host = " + host + ",port = " + port);
                        remoteChannel.set(future.channel());
//                        if(port == 443){
//                            ByteBuf buffer = clientChannelContext.channel().alloc().buffer("HTTP/1.1 200 Connection Established\r\n\r\n".getBytes().length);
//                            buffer.writeBytes("HTTP/1.1 200 Connection Established\r\n\r\n".getBytes());
//                            clientChannelContext.channel().writeAndFlush(buffer);
//                        }
                        // future.channel().writeAndFlush(msg);
                    } else {
                        logger.info("connect fail host = " + host + ",port = " + port);
                        clientChannelContext.close();
                    }
                }
            });
        }catch (Exception e){
            logger.error("connect intenet error", e);
            clientChannelContext.close();
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        //directProxy(ctx, msg);
        logger.info("收到客户端消息");
        if (remoteChannel.get() != null) {
            remoteChannel.get().writeAndFlush(CryptUtils.decrypt(crypt, (ByteBuf)msg));
        }
    }
}
