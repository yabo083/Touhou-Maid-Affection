package com.github.touhoumaidaffection.network;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record RescueVoiceConfigPayload(
        UUID maidUuid,
        String sourceMode,
        String tlmPlayMode,
        String tlmSelectedGroup,
        String tlmSelectedClip,
        String customPlayMode,
        String fixedFile,
        boolean useCommonFallback,
        List<String> selectedVoiceIds
) implements CustomPacketPayload {
    private static final int MAX_CONFIG_STRING_LENGTH = 256;
    private static final int MAX_SELECTED_VOICE_IDS = 1_024;
    private static final StreamCodec<ByteBuf, String> CONFIG_STRING_CODEC =
            ByteBufCodecs.stringUtf8(MAX_CONFIG_STRING_LENGTH);
    private static final StreamCodec<ByteBuf, ArrayList<String>> SELECTED_VOICE_IDS_CODEC =
            ByteBufCodecs.collection(ArrayList::new, CONFIG_STRING_CODEC, MAX_SELECTED_VOICE_IDS);

    public static final Type<RescueVoiceConfigPayload> TYPE =
            new Type<>(new ResourceLocation(TouhouMaidAffection.MOD_ID, "rescue_voice_config"));

    public static final StreamCodec<ByteBuf, RescueVoiceConfigPayload> STREAM_CODEC = StreamCodec.of(
            RescueVoiceConfigPayload::encode,
            RescueVoiceConfigPayload::decode
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(ByteBuf buf, RescueVoiceConfigPayload payload) {
ByteBufCodecs.UUID.encode(buf, payload.maidUuid());
        CONFIG_STRING_CODEC.encode(buf, payload.sourceMode());
        CONFIG_STRING_CODEC.encode(buf, payload.tlmPlayMode());
        CONFIG_STRING_CODEC.encode(buf, payload.tlmSelectedGroup());
        CONFIG_STRING_CODEC.encode(buf, payload.tlmSelectedClip());
        CONFIG_STRING_CODEC.encode(buf, payload.customPlayMode());
        CONFIG_STRING_CODEC.encode(buf, payload.fixedFile());
        ByteBufCodecs.BOOL.encode(buf, payload.useCommonFallback());
        SELECTED_VOICE_IDS_CODEC.encode(buf, new ArrayList<>(payload.selectedVoiceIds()));
    }

    private static RescueVoiceConfigPayload decode(ByteBuf buf) {
        return new RescueVoiceConfigPayload(
ByteBufCodecs.UUID.decode(buf),
                CONFIG_STRING_CODEC.decode(buf),
                CONFIG_STRING_CODEC.decode(buf),
                CONFIG_STRING_CODEC.decode(buf),
                CONFIG_STRING_CODEC.decode(buf),
                CONFIG_STRING_CODEC.decode(buf),
                CONFIG_STRING_CODEC.decode(buf),
                ByteBufCodecs.BOOL.decode(buf),
                SELECTED_VOICE_IDS_CODEC.decode(buf)
        );
    }
}
