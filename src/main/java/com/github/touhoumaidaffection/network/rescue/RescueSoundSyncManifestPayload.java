package com.github.touhoumaidaffection.network.rescue;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record RescueSoundSyncManifestPayload(
        String serverId,
        long generation,
        boolean fullSnapshot,
        List<String> relativePaths
) implements CustomPacketPayload {
    public static final Type<RescueSoundSyncManifestPayload> TYPE =
            new Type<>(new ResourceLocation(TouhouMaidAffection.MOD_ID, "rescue_sound_sync_manifest"));

    public static final StreamCodec<ByteBuf, RescueSoundSyncManifestPayload> STREAM_CODEC = StreamCodec.of(
            RescueSoundSyncManifestPayload::encode,
            RescueSoundSyncManifestPayload::decode
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(ByteBuf buf, RescueSoundSyncManifestPayload payload) {
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.serverId());
        ByteBufCodecs.VAR_LONG.encode(buf, payload.generation());
        ByteBufCodecs.BOOL.encode(buf, payload.fullSnapshot());
        ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8).encode(buf, new ArrayList<>(payload.relativePaths()));
    }

    private static RescueSoundSyncManifestPayload decode(ByteBuf buf) {
        String serverId = ByteBufCodecs.STRING_UTF8.decode(buf);
        long generation = ByteBufCodecs.VAR_LONG.decode(buf);
        boolean fullSnapshot = ByteBufCodecs.BOOL.decode(buf);
        List<String> paths = ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8).decode(buf);
        return new RescueSoundSyncManifestPayload(serverId, generation, fullSnapshot, List.copyOf(paths));
    }
}
