package com.orc.common.coder;

import com.orc.common.message.AuthRequestMessage;
import com.orc.common.message.AuthResponseMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.msgpack.MessagePack;

import java.util.List;

public class AuthRequestMessageDecoder extends MessageToMessageDecoder<ByteBuf> {

    //@Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        final byte[] array;
        final int length = msg.readableBytes();
        array = new byte[length];
        //调用MessagePack 的read方法将其反序列化为
        msg.getBytes(msg.readerIndex(), array, 0, length);
        MessagePack msgpack = new MessagePack();
        AuthRequestMessage authMessage = msgpack.read(array, AuthRequestMessage.class);
        out.add(authMessage);
    }



}


