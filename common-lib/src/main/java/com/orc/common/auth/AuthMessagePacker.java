package com.orc.common.auth;

import com.orc.common.encrypt.CryptFactory;
import com.orc.common.encrypt.CryptUtils;
import com.orc.common.encrypt.ICrypt;
import com.orc.common.message.AuthRequestMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.msgpack.MessagePack;

import java.io.IOException;

public class AuthMessagePacker {

    public static ByteBuf pack(Object msg) {
        MessagePack msgpack = new MessagePack();
        try {
            return Unpooled.copiedBuffer(msgpack.write(msg));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static <T> T unpack(ByteBuf msg, Class<T> c){
        final byte[] array;
        final int length = msg.readableBytes();
        array = new byte[length];
        //调用MessagePack 的read方法将其反序列化为
        msg.getBytes(msg.readerIndex(), array, 0, length);
        MessagePack msgpack = new MessagePack();
        try {
            return msgpack.read(array, c);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {
        AuthRequestMessage authMessage = new AuthRequestMessage();
        authMessage.setHost("123");
        authMessage.setPort(1);
        authMessage.setUser("test");
        authMessage.setPassword("testpw");
        ByteBuf byteBuf = AuthMessagePacker.pack(authMessage);
        ICrypt crypt = CryptFactory.get("aes-256-cfb", "worc123");
        ByteBuf byteBufEncrypt = CryptUtils.encrypt(crypt, byteBuf);

        ByteBuf byteBufDecrypt = CryptUtils.decrypt(crypt, byteBufEncrypt);
        AuthRequestMessage result = AuthMessagePacker.unpack(byteBufDecrypt, AuthRequestMessage.class);
        System.out.println(result.getPassword());

    }

}
