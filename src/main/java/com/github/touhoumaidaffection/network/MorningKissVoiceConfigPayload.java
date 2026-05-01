package com.github.touhoumaidaffection.network;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record MorningKissVoiceConfigPayload(
        UUID maidUuid,
        String mode,
        String selectedGroup,
        String selectedClip,
        String soundPackId,
        List<String> selectedVoiceIds
) implements CustomPacketPayload {
    public static final Type<MorningKissVoiceConfigPayload> TYPE =
            new Type<>(new ResourceLocation(TouhouMaidAffection.MOD_ID, "morning_kiss_voice_config"));

    public static final StreamCodec<ByteBuf, MorningKissVoiceConfigPayload> STREAM_CODEC = StreamCodec.of(
            MorningKissVoiceConfigPayload::encode,
            MorningKissVoiceConfigPayload::decode
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(ByteBuf buf, MorningKissVoiceConfigPayload payload) {
        UUIDUtil.STREAM_CODEC.encode(buf, payload.maidUuid());
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.mode());
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.selectedGroup());
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.selectedClip());
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.soundPackId());
        ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8)
                .encode(buf, new ArrayList<>(payload.selectedVoiceIds()));
    }

    private static MorningKissVoiceConfigPayload decode(ByteBuf buf) {
        return new MorningKissVoiceConfigPayload(
                UUIDUtil.STREAM_CODEC.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8).decode(buf)
        );
    }
}
