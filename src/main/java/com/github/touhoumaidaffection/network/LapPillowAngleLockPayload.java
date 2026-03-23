package com.github.touhoumaidaffection.network;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record LapPillowAngleLockPayload(boolean enabled, float lockedYaw) implements CustomPacketPayload {
    public static final Type<LapPillowAngleLockPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TouhouMaidAffection.MOD_ID, "lap_pillow_angle_lock"));

    public static final StreamCodec<ByteBuf, LapPillowAngleLockPayload> STREAM_CODEC = StreamCodec.of(
            LapPillowAngleLockPayload::encode,
            LapPillowAngleLockPayload::decode
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(ByteBuf buf, LapPillowAngleLockPayload payload) {
        ByteBufCodecs.BOOL.encode(buf, payload.enabled());
        ByteBufCodecs.FLOAT.encode(buf, payload.lockedYaw());
    }

    private static LapPillowAngleLockPayload decode(ByteBuf buf) {
        return new LapPillowAngleLockPayload(
                ByteBufCodecs.BOOL.decode(buf),
                ByteBufCodecs.FLOAT.decode(buf)
        );
    }
}
