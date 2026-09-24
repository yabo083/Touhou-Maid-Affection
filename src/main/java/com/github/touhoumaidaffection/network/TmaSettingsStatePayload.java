package com.github.touhoumaidaffection.network;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.github.touhoumaidaffection.bond.settings.TmaSettingsWire;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Server to client push of the full effective settings state.
 *
 * <p>{@code entries} always contains every whitelisted key with its current server value;
 * {@code canEdit} reports whether the receiving player may change them (operator permission level 2).
 */
public record TmaSettingsStatePayload(List<TmaSettingsWire.Entry> entries, boolean canEdit) implements CustomPacketPayload {
    public static final Type<TmaSettingsStatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TouhouMaidAffection.MOD_ID, "tma_settings_state"));

    public static final StreamCodec<ByteBuf, TmaSettingsStatePayload> STREAM_CODEC = StreamCodec.of(
            TmaSettingsStatePayload::encode,
            TmaSettingsStatePayload::decode
    );

    public TmaSettingsStatePayload {
        entries = TmaSettingsWire.sanitize(entries);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(ByteBuf buf, TmaSettingsStatePayload payload) {
        TmaSettingsWire.writeEntries(TmaSettingsByteBuf.sink(buf), payload.entries());
        ByteBufCodecs.BOOL.encode(buf, payload.canEdit());
    }

    private static TmaSettingsStatePayload decode(ByteBuf buf) {
        List<TmaSettingsWire.Entry> entries = TmaSettingsWire.readEntries(TmaSettingsByteBuf.source(buf));
        return new TmaSettingsStatePayload(entries, ByteBufCodecs.BOOL.decode(buf));
    }
}