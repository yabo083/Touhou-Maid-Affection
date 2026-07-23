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
    private static final int MAX_CONFIG_STRING_LENGTH = 256;
    private static final int MAX_SELECTED_VOICE_IDS = 1_024;
    private static final StreamCodec<ByteBuf, String> CONFIG_STRING_CODEC =
            ByteBufCodecs.stringUtf8(MAX_CONFIG_STRING_LENGTH);
    private static final StreamCodec<ByteBuf, ArrayList<String>> SELECTED_VOICE_IDS_CODEC =
            ByteBufCodecs.collection(ArrayList::new, CONFIG_STRING_CODEC, MAX_SELECTED_VOICE_IDS);

    public static final Type<MorningKissVoiceConfigPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TouhouMaidAffection.MOD_ID, "morning_kiss_voice_config"));

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
        CONFIG_STRING_CODEC.encode(buf, payload.mode());
        CONFIG_STRING_CODEC.encode(buf, payload.selectedGroup());
        CONFIG_STRING_CODEC.encode(buf, payload.selectedClip());
        CONFIG_STRING_CODEC.encode(buf, payload.soundPackId());
        SELECTED_VOICE_IDS_CODEC.encode(buf, new ArrayList<>(payload.selectedVoiceIds()));
    }

    private static MorningKissVoiceConfigPayload decode(ByteBuf buf) {
        return new MorningKissVoiceConfigPayload(
                UUIDUtil.STREAM_CODEC.decode(buf),
                CONFIG_STRING_CODEC.decode(buf),
                CONFIG_STRING_CODEC.decode(buf),
                CONFIG_STRING_CODEC.decode(buf),
                CONFIG_STRING_CODEC.decode(buf),
                SELECTED_VOICE_IDS_CODEC.decode(buf)
        );
    }
}
