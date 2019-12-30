package com.orc.common.coder;

import com.orc.common.message.AuthRequestMessage;
import com.orc.common.message.AuthResponseMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.msgpack.MessagePack;

public class AuthResponseMessageEncoder extends MessageToByteEncoder<AuthResponseMessage> {

    @Override
    protected void encode(ChannelHandlerContext ctx, AuthResponseMessage msg, ByteBuf out) throws Exception {
        MessagePack msgpack = new MessagePack();
        out.writeBytes(msgpack.write(msg));
    }
}
