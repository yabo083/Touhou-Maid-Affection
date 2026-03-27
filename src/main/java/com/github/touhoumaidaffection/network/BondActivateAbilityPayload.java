package com.github.touhoumaidaffection.network;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record BondActivateAbilityPayload(String abilityId, UUID maidUuid) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BondActivateAbilityPayload> TYPE =
            new CustomPacketPayload.Type<>(new ResourceLocation(TouhouMaidAffection.MOD_ID, "bond_activate_ability"));

    public static final StreamCodec<ByteBuf, BondActivateAbilityPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, BondActivateAbilityPayload::abilityId,
            ByteBufCodecs.UUID, BondActivateAbilityPayload::maidUuid,
            BondActivateAbilityPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
