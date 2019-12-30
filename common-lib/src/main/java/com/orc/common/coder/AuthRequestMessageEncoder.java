package com.orc.common.coder;

import com.orc.common.message.AuthRequestMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.msgpack.MessagePack;

public class AuthRequestMessageEncoder extends MessageToByteEncoder<AuthRequestMessage> {

    @Override
    protected void encode(ChannelHandlerContext ctx, AuthRequestMessage msg, ByteBuf out) throws Exception {
        MessagePack msgpack = new MessagePack();
        out.writeBytes(msgpack.write(msg));
    }
}
