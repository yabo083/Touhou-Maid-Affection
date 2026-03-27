package com.github.touhoumaidaffection.network.rescue;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record RescueSoundSyncCompletePayload(
        String serverId,
        long generation,
        List<String> updatedPaths
) implements CustomPacketPayload {
    public static final Type<RescueSoundSyncCompletePayload> TYPE =
            new Type<>(new ResourceLocation(TouhouMaidAffection.MOD_ID, "rescue_sound_sync_complete"));

    public static final StreamCodec<ByteBuf, RescueSoundSyncCompletePayload> STREAM_CODEC = StreamCodec.of(
            RescueSoundSyncCompletePayload::encode,
            RescueSoundSyncCompletePayload::decode
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(ByteBuf buf, RescueSoundSyncCompletePayload payload) {
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.serverId());
        ByteBufCodecs.VAR_LONG.encode(buf, payload.generation());
        ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8).encode(buf, new ArrayList<>(payload.updatedPaths()));
    }

    private static RescueSoundSyncCompletePayload decode(ByteBuf buf) {
        String serverId = ByteBufCodecs.STRING_UTF8.decode(buf);
        long generation = ByteBufCodecs.VAR_LONG.decode(buf);
        List<String> updatedPaths = ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8).decode(buf);
        return new RescueSoundSyncCompletePayload(serverId, generation, List.copyOf(updatedPaths));
    }
}
