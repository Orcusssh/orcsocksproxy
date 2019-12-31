package com.orc.server.handler;

import com.orc.common.auth.AuthMessagePacker;
import com.orc.common.coder.AuthRequestMessageDecoder;
import com.orc.common.coder.AuthRequestMessageEncoder;
import com.orc.common.coder.AuthResponseMessageEncoder;
import com.orc.common.encrypt.CryptFactory;
import com.orc.common.encrypt.CryptUtils;
import com.orc.common.encrypt.ICrypt;
import com.orc.common.message.AuthRequestMessage;
import com.orc.common.message.AuthResponseMessage;
import com.orc.server.config.CommonConfiguration;
import com.orc.server.config.ServerAuthConfiguration;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InitialServerHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private static final Logger logger = LoggerFactory.getLogger(InitialServerHandler.class);

    private CommonConfiguration commonConfiguration;
    private ServerAuthConfiguration serverAuthConfiguration;
    private ICrypt crypt;

    public InitialServerHandler(CommonConfiguration commonConfiguration, ServerAuthConfiguration serverAuthConfiguration){
        this.commonConfiguration = commonConfiguration;
        this.serverAuthConfiguration = serverAuthConfiguration;
        crypt = CryptFactory.get(commonConfiguration.getCryptMethod(), commonConfiguration.getCryptKey());
    }

    @Override
    protected  void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
        AuthResponseMessage authResponseMessage = new AuthResponseMessage();
        // decrypt auth request message
        msg = CryptUtils.decrypt(crypt, msg);
        AuthRequestMessage authRequestMessage = AuthMessagePacker.unpack(msg, AuthRequestMessage.class);
        if(!serverAuthConfiguration.auth(authRequestMessage.getUser(), authRequestMessage.getPassword())){
            authResponseMessage.setAuthResult(AuthResponseMessage.AuthResult.FAILURE.getResult());
            logger.debug("用户{}密码{}认证失败",authRequestMessage.getUser(), authRequestMessage.getPassword());
        }else{
            authResponseMessage.setAuthResult(AuthResponseMessage.AuthResult.SUCCESS.getResult());
        }

        String host = authRequestMessage.getHost();
        int port = authRequestMessage.getPort();
        //ctx.pipeline().addLast(new AuthResponseMessageEncoder());
        ctx.channel().writeAndFlush(CryptUtils.encrypt(crypt, AuthMessagePacker.pack(authResponseMessage)));
        //ctx.pipeline().remove(AuthResponseMessageEncoder.class);
       //ctx.pipeline().remove(AuthRequestMessageDecoder.class);
        if(authResponseMessage.isAuthSuccess()) {
            ctx.channel().pipeline().remove(this);
            ctx.channel().pipeline().addLast(new ProxyHandler(host, port, crypt, ctx, msg));
        }
    }
}
