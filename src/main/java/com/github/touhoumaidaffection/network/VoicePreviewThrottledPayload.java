package com.github.touhoumaidaffection.network;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 服务端在「合法数据包试听请求但被限流」时回给客户端，用于显示一行 action bar 提示。
 * 无字段；仅在试听请求确实被限流时发送，非数据包（本地）请求与非法请求不发送。
 */
public record VoicePreviewThrottledPayload() implements CustomPacketPayload {
    public static final VoicePreviewThrottledPayload INSTANCE = new VoicePreviewThrottledPayload();

    public static final Type<VoicePreviewThrottledPayload> TYPE =
            new Type<>(new ResourceLocation(TouhouMaidAffection.MOD_ID, "voice_preview_throttled"));

    public static final StreamCodec<ByteBuf, VoicePreviewThrottledPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}