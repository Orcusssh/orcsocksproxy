package com.orc.client.handler;

import com.orc.client.config.ClientAuthConfiguration;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.v5.DefaultSocks5PasswordAuthRequest;
import io.netty.handler.codec.socksx.v5.DefaultSocks5PasswordAuthResponse;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthResponse;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Socks5PasswordAuthRequestHandler extends SimpleChannelInboundHandler<DefaultSocks5PasswordAuthRequest> {

    private static final Logger logger = LoggerFactory.getLogger(Socks5PasswordAuthRequestHandler.class);

    private ClientAuthConfiguration clientAuthConfiguration;

    public Socks5PasswordAuthRequestHandler(ClientAuthConfiguration clientAuthConfiguration) {
        this.clientAuthConfiguration = clientAuthConfiguration;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DefaultSocks5PasswordAuthRequest msg) throws Exception {
        if(clientAuthConfiguration.auth(msg.username(), msg.password())) {
            logger.info("用户名{}密码{}认证成功", msg.username(), msg.password());
            Socks5PasswordAuthResponse passwordAuthResponse = new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.SUCCESS);
            ctx.writeAndFlush(passwordAuthResponse);
        } else {
            logger.info("用户名{}密码{}认证失败", msg.username(), msg.password());
            Socks5PasswordAuthResponse passwordAuthResponse = new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.FAILURE);
            //发送鉴权失败消息，完成后关闭channel
            ctx.writeAndFlush(passwordAuthResponse).addListener(ChannelFutureListener.CLOSE);
        }
    }

}
