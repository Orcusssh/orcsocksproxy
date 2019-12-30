package com.orc.client.handler;

import com.orc.client.config.CommonConfiguration;
import com.orc.client.config.PacConfiguration;
import com.orc.common.coder.AuthRequestMessageDecoder;
import com.orc.common.coder.AuthRequestMessageEncoder;
import com.orc.common.coder.AuthResponseMessageDecoder;
import com.orc.common.message.AuthRequestMessage;
import com.orc.common.message.AuthResponseMessage;
import com.orc.common.util.SocksUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.v5.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ChannelHandler.Sharable
public class Socks5CommandRequestHandler extends SimpleChannelInboundHandler<DefaultSocks5CommandRequest>{

    private static final Logger logger = LoggerFactory.getLogger(Socks5CommandRequestHandler.class);

    private boolean isProxy = true;

    private final Bootstrap b = new Bootstrap();

    private PacConfiguration pacConfiguration;

    private CommonConfiguration commonConfiguration;

    public Socks5CommandRequestHandler(CommonConfiguration commonConfiguration , PacConfiguration pacConfiguration) {
        this.commonConfiguration = commonConfiguration;
        this.pacConfiguration = pacConfiguration;
    }


    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, DefaultSocks5CommandRequest msg) throws Exception {
       /**
         * 应用和代理客户端的通道
         */
        final Channel inboundChannel = ctx.channel();


        b.group(inboundChannel.eventLoop()).channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10*1000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new AuthResponseMessageDecoder());
                        ch.pipeline().addLast(new ProxyServerAuthHandler(ctx, msg));
                    }
                });

        /**
         * 连接目标服务器
         * ChannelFuture
         * Netty中的IO操作是异步的，
         * 包括bind、write、connect等操作会简单的返回一个ChannelFuture，调用者并不能立刻获得结果。
         * 当future对象刚刚创建时，处于非完成状态。可以通过isDone()方法来判断当前操作是否完成。通过isSuccess()判断已完成的当前操作是否成功，getCause()来获取已完成的当前操作失败的原因，isCancelled()来判断已完成的当前操作是否被取消。
         * 调用者可以通过返回的ChannelFuture来获取操作执行的状态，注册监听函数来执行完成后的操作。
         */
        setProxy(msg);
        logger.info("host = " + msg.dstAddr() + ",port = " + msg.dstPort() + ",isProxy = " + isProxy);
        ChannelFuture f = b.connect(getIpAddr(msg), getPort(msg));
        /**
         * ChannelFutureListener
         * 监听ChannelFuture的状态
         * 注册监听函数来执行完成后的操作
         */
        f.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (!future.isSuccess()) {
                    ctx.channel().writeAndFlush(SocksUtils.getFailureResponse());
                    SocksUtils.closeOnFlush(ctx.channel());
                }else {
                    logger.info("连接代理服务器成功");
                }
            }
        });
    }

    public void setProxy(DefaultSocks5CommandRequest msg) {
        isProxy = pacConfiguration.checkInPac(msg.dstAddr());
    }


    /**
     * 获取远程ip地址
     *
     * @return
     */
    private String getIpAddr(DefaultSocks5CommandRequest msg) {
        if (isProxy) {
            return commonConfiguration.getServerHost();
        } else {
            return msg.dstAddr();
        }
    }

    /**
     * 获取远程端口
     *
     * @return
     */
    private int getPort(DefaultSocks5CommandRequest msg) {
        if (isProxy) {
            return commonConfiguration.getServerPort();
        } else {
            return msg.dstPort();
        }
    }



    public final class ProxyServerAuthHandler extends SimpleChannelInboundHandler<AuthResponseMessage> {

        private final ChannelHandlerContext inCtx;
        private final DefaultSocks5CommandRequest inMsg;

        public ProxyServerAuthHandler(ChannelHandlerContext inCtx, DefaultSocks5CommandRequest inMsg) {
            this.inCtx = inCtx;
            this.inMsg = inMsg;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            /*
             * 1.连接到服务端通道active时,如果需要服务端代理，发送认证及目标地址访问信息
             * 2.不需要服务端代理则直接连接目标服务器
             */
            if (isProxy) {
                AuthRequestMessage authMessage = new AuthRequestMessage();
                authMessage.setHost(inMsg.dstAddr());
                authMessage.setPort(inMsg.dstPort());
                authMessage.setUser(commonConfiguration.getServerUser());
                authMessage.setPassword(commonConfiguration.getServerPassword());
                ctx.pipeline().addFirst(new AuthRequestMessageEncoder());
                ctx.writeAndFlush(authMessage);
                ctx.pipeline().remove(AuthRequestMessageEncoder.class);
            }else{
                addSuccessRelayHandler(inCtx, ctx);
            }
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, AuthResponseMessage authMessage) {
            if(!authMessage.isAuthSuccess()){//认证失败，关闭通道
                inCtx.close();
                ctx.close();
                return;
            }
            //认证通过，添加消息中继handler
            addSuccessRelayHandler(inCtx, ctx);

        }

        private void addSuccessRelayHandler(ChannelHandlerContext inCtx, ChannelHandlerContext outCtx){
            outCtx.pipeline().remove(AuthResponseMessageDecoder.class);//移除之前的认证解码器，因为接下来非认证消息传输
            final InRelayHandler inRelay = new InRelayHandler(inCtx.channel());
            final OutRelayHandler outRelay = new OutRelayHandler(outCtx.channel());
            inCtx.channel().writeAndFlush(SocksUtils.getSuccessResponse()).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) {
                    outCtx.pipeline().remove(ProxyServerAuthHandler.class);//移除作为认证用的handler，接下来是消息的代理
                    inCtx.pipeline().remove(Socks5CommandRequestHandler.class);
                    outCtx.pipeline().addLast(inRelay);
                    inCtx.pipeline().addLast(outRelay);
                }
            });
        }
    }


    public static final class InRelayHandler extends ChannelInboundHandlerAdapter {

        private static Logger logger = LoggerFactory.getLogger(InRelayHandler.class);

        private final Channel relayChannel;

        public InRelayHandler(Channel relayChannel) {
            this.relayChannel = relayChannel;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            ctx.writeAndFlush(Unpooled.EMPTY_BUFFER);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            try {
                if (relayChannel.isActive()) {
                    logger.debug("get remote message" + relayChannel);
                    ByteBuf bytebuff = (ByteBuf) msg;
                    relayChannel.writeAndFlush(bytebuff);
                }
            } catch (Exception e) {
                logger.error("receive remoteServer data error", e);
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            SocksUtils.closeOnFlush(relayChannel);
            SocksUtils.closeOnFlush(ctx.channel());
            logger.info("inRelay channelInactive close");
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }
    }

    public static final class OutRelayHandler extends ChannelInboundHandlerAdapter {

        private static Logger logger = LoggerFactory.getLogger(OutRelayHandler.class);

        private final Channel relayChannel;

        public OutRelayHandler(Channel relayChannel) {
            this.relayChannel = relayChannel;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            ctx.writeAndFlush(Unpooled.EMPTY_BUFFER);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            try {
                if (relayChannel.isActive()) {
                    if(!relayChannel.remoteAddress().toString().contains("mozilla") && !relayChannel.remoteAddress().toString().contains("firefox")) {
                        logger.debug("send data to remoteServer ", relayChannel);
                    }
                    ByteBuf bytebuff = (ByteBuf) msg;
                    relayChannel.writeAndFlush(bytebuff);
                }
            } catch (Exception e) {
                logger.error("send data to remoteServer error", e);
            } finally {
               // ReferenceCountUtil.release(msg);
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            SocksUtils.closeOnFlush(relayChannel);
            SocksUtils.closeOnFlush(ctx.channel());
            logger.info("outRelay channelInactive close");
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }

    }





}
