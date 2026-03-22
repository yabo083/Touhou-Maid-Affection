package com.github.touhoumaidaffection.network;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record RescueActionConfigPayload(UUID maidUuid, String actionId) implements CustomPacketPayload {
    public static final Type<RescueActionConfigPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TouhouMaidAffection.MOD_ID, "rescue_action_config"));

    public static final StreamCodec<ByteBuf, RescueActionConfigPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                ByteBufCodecs.STRING_UTF8.encode(buffer, payload.maidUuid().toString());
                ByteBufCodecs.STRING_UTF8.encode(buffer, payload.actionId());
            },
            buffer -> new RescueActionConfigPayload(
                    UUID.fromString(ByteBufCodecs.STRING_UTF8.decode(buffer)),
                    ByteBufCodecs.STRING_UTF8.decode(buffer)
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
