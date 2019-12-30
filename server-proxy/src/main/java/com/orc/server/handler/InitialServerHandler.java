package com.orc.server.handler;

import com.orc.common.coder.AuthRequestMessageDecoder;
import com.orc.common.coder.AuthRequestMessageEncoder;
import com.orc.common.coder.AuthResponseMessageEncoder;
import com.orc.common.message.AuthRequestMessage;
import com.orc.common.message.AuthResponseMessage;
import com.orc.server.config.ServerAuthConfiguration;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InitialServerHandler extends SimpleChannelInboundHandler<AuthRequestMessage> {

    private static final Logger logger = LoggerFactory.getLogger(InitialServerHandler.class);

    private ServerAuthConfiguration serverAuthConfiguration;

    public InitialServerHandler(ServerAuthConfiguration serverAuthConfiguration){
        this.serverAuthConfiguration = serverAuthConfiguration;
    }

    @Override
    protected  void channelRead0(ChannelHandlerContext ctx, AuthRequestMessage msg) {
        AuthResponseMessage authResponseMessage = new AuthResponseMessage();
        if(!serverAuthConfiguration.auth(msg.getUser(), msg.getPassword())){
            authResponseMessage.setAuthResult(AuthResponseMessage.AuthResult.FAILURE.getResult());
            logger.debug("用户{}密码{}认证失败",msg.getUser(), msg.getPassword());
        }else{
            authResponseMessage.setAuthResult(AuthResponseMessage.AuthResult.SUCCESS.getResult());
        }

        String host = msg.getHost();
        int port = msg.getPort();
        ctx.pipeline().addLast(new AuthResponseMessageEncoder());
        ctx.channel().writeAndFlush(authResponseMessage);
        ctx.pipeline().remove(AuthResponseMessageEncoder.class);
        ctx.pipeline().remove(AuthRequestMessageDecoder.class);
        if(authResponseMessage.isAuthSuccess()) {
            ctx.channel().pipeline().remove(this);
            ctx.channel().pipeline().addLast(new ProxyHandler(host, port, ctx, msg));
        }
    }
}
