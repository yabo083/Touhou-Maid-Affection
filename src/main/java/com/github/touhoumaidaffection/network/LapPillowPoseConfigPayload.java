package com.github.touhoumaidaffection.network;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record LapPillowPoseConfigPayload(
        UUID maidUuid,
        String mode,
        double maidOffsetX,
        double maidOffsetY,
        double maidOffsetZ,
        double playerOffsetX,
        double playerOffsetY,
        double playerOffsetZ,
        String maidActionId,
        String playerActionId
) implements CustomPacketPayload {
    public static final Type<LapPillowPoseConfigPayload> TYPE =
            new Type<>(new ResourceLocation(TouhouMaidAffection.MOD_ID, "lap_pillow_pose_config"));

    public static final StreamCodec<ByteBuf, LapPillowPoseConfigPayload> STREAM_CODEC = StreamCodec.of(
            LapPillowPoseConfigPayload::encode,
            LapPillowPoseConfigPayload::decode
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(ByteBuf buf, LapPillowPoseConfigPayload payload) {
        ByteBufCodecs.UUID.encode(buf, payload.maidUuid());
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.mode());
        ByteBufCodecs.DOUBLE.encode(buf, payload.maidOffsetX());
        ByteBufCodecs.DOUBLE.encode(buf, payload.maidOffsetY());
        ByteBufCodecs.DOUBLE.encode(buf, payload.maidOffsetZ());
        ByteBufCodecs.DOUBLE.encode(buf, payload.playerOffsetX());
        ByteBufCodecs.DOUBLE.encode(buf, payload.playerOffsetY());
        ByteBufCodecs.DOUBLE.encode(buf, payload.playerOffsetZ());
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.maidActionId());
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.playerActionId());
    }

    private static LapPillowPoseConfigPayload decode(ByteBuf buf) {
        return new LapPillowPoseConfigPayload(
                ByteBufCodecs.UUID.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.DOUBLE.decode(buf),
                ByteBufCodecs.DOUBLE.decode(buf),
                ByteBufCodecs.DOUBLE.decode(buf),
                ByteBufCodecs.DOUBLE.decode(buf),
                ByteBufCodecs.DOUBLE.decode(buf),
                ByteBufCodecs.DOUBLE.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf)
        );
    }
}
