package com.orc.common.handler;

import com.orc.common.message.DelimiterMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

public class DelimiterOutboundHandler extends ChannelOutboundHandlerAdapter {

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        ChannelPromise vcp = ctx.voidPromise();
        ctx.write(msg, vcp);
        ctx.write(DelimiterMessage.getDelimiterBuf().copy(), vcp);
    }
}
