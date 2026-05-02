package com.github.touhoumaidaffection.network;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record MorningKissVoicePlayPayload(
        int maidEntityId,
        UUID maidUuid,
        String soundPackId,
        String mode,
        String selectedGroup,
        String selectedClip,
        String selectedVoiceId
) implements CustomPacketPayload {
    public static final Type<MorningKissVoicePlayPayload> TYPE =
            new Type<>(new ResourceLocation(TouhouMaidAffection.MOD_ID, "morning_kiss_voice_play"));

    public static final StreamCodec<ByteBuf, MorningKissVoicePlayPayload> STREAM_CODEC = StreamCodec.of(
            MorningKissVoicePlayPayload::encode,
            MorningKissVoicePlayPayload::decode
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(ByteBuf buf, MorningKissVoicePlayPayload payload) {
        ByteBufCodecs.INT.encode(buf, payload.maidEntityId());
        ByteBufCodecs.UUID.encode(buf, payload.maidUuid());
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.soundPackId());
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.mode());
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.selectedGroup());
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.selectedClip());
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.selectedVoiceId());
    }

    private static MorningKissVoicePlayPayload decode(ByteBuf buf) {
        return new MorningKissVoicePlayPayload(
                ByteBufCodecs.INT.decode(buf),
                ByteBufCodecs.UUID.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf)
        );
    }
}
