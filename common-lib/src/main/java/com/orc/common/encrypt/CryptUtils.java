package com.orc.common.encrypt;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class CryptUtils {

    private static Logger logger = LoggerFactory.getLogger(CryptUtils.class);

    public static ByteBuf encrypt(ICrypt crypt, ByteBuf bytebuff) {
        ByteBuf data = null;
        ByteArrayOutputStream _remoteOutStream = null;
        try {
            _remoteOutStream = new ByteArrayOutputStream();
            int len = bytebuff.readableBytes();
            byte[] arr = new byte[len];
            bytebuff.getBytes(0, arr);
            crypt.encrypt(arr, arr.length, _remoteOutStream);
            data = Unpooled.copiedBuffer(_remoteOutStream.toByteArray());
        } catch (Exception e) {
            logger.error("encrypt error", e);
        } finally {
            if (_remoteOutStream != null) {
                try {
                    _remoteOutStream.close();
                } catch (IOException e) {
                }
            }
        }
        return data;
    }

    public static ByteBuf decrypt(ICrypt crypt, ByteBuf bytebuff) {
        ByteBuf data = null;
        ByteArrayOutputStream _localOutStream = null;
        try {
            _localOutStream = new ByteArrayOutputStream();
            int len = bytebuff.readableBytes();
            byte[] arr = new byte[len];
            bytebuff.getBytes(0, arr);
            crypt.decrypt(arr, arr.length, _localOutStream);
            data = Unpooled.copiedBuffer(_localOutStream.toByteArray());
        } catch (Exception e) {
            logger.error("encrypt error", e);
        } finally {
            if (_localOutStream != null) {
                try {
                    _localOutStream.close();
                } catch (IOException e) {
                }
            }
        }
        return data;
    }

}
