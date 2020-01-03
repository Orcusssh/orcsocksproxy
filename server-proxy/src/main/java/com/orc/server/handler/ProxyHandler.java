package com.orc.server.handler;

import com.orc.common.encrypt.CryptUtils;
import com.orc.common.encrypt.ICrypt;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class ProxyHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(ProxyHandler.class);

    private String host ;

    private int port;

    private ICrypt crypt;

    private final Bootstrap b = new Bootstrap();

    private AtomicReference<Channel> remoteChannel = new AtomicReference<>();

    private CountDownLatch waitLatch = new CountDownLatch(1);

    public ProxyHandler(String host, int port, ICrypt crypt, final ChannelHandlerContext clientChannelContext, Object msg){
        this.host = host;
        this.port = port;
        this.crypt = crypt;
        init(clientChannelContext, msg);
    }

    private void init(final ChannelHandlerContext clientChannelContext, Object msg){
        b.group(new NioEventLoopGroup()).channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10*1000)
                .option(ChannelOption.SO_KEEPALIVE, true)
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
            ChannelFuture f = b.connect(host, port);
            f.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        logger.info("连接目标服务器");
                        logger.info("connect success host = " + host + ",port = " + port);
                        remoteChannel.set(future.channel());
                        waitLatch.countDown();
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
    public void channelRead(ChannelHandlerContext ctx, Object msg)  {
        //directProxy(ctx, msg);
        logger.info("收到客户端消息");
        try {
            long start = System.currentTimeMillis();
            waitLatch.await(5000, TimeUnit.MILLISECONDS);
            long end = System.currentTimeMillis();
//            if(end - start >= 5000){
//                logger.info("连接超时");
//                ctx.close();
//            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (remoteChannel.get() != null) {
            remoteChannel.get().writeAndFlush(CryptUtils.decrypt(crypt, (ByteBuf)msg));
        }
    }

}
