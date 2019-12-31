package com.orc.common.message;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.Arrays;

/**
 * 分隔符握手报文
 * +-------+---------------------------------------+
 * | Magic |              Delimiter                |
 * |6 Bytes|          128 Bit / 16 bytes           |
 * +-------+---------------------------------------+
 *
 * 6字节魔数+16字节分隔符
 */
public class DelimiterMessage implements Message {

    /**
     * 魔数
     */
    private static final byte[] MAGIC = new byte[] {(byte) 0xE4, (byte) 0xBC, (byte) 0x8A,
            (byte) 0xE8, (byte)0x94, (byte)0x93};

    /**
     * 分隔符字节数，建议16字节以上避免与报文数据混淆
     */
    private static final int DEFAULT_SIZE = 16;

    /**
     * 消息长度
     */
    public static final int LENGTH = MAGIC.length + DEFAULT_SIZE;

    /**
     * 分隔符内容
     */
    private static byte[] delimiter = new byte[] {-78, -120, 61, 75, -6, 57, -87, -29, -103, 63, -13, -82, -67, -41, -53, -40};

    public static ByteBuf getDelimiterBuf(){
        ByteBuf buf = Unpooled.buffer(LENGTH);
        buf.writeBytes(MAGIC);
        buf.writeBytes(delimiter);
        return buf;

    }


}
