package com.github.touhoumaidaffection.network.rescue;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record RescueSoundSyncClearPayload(
        String serverId,
        long generation,
        boolean clearAll,
        List<String> relativePaths
) implements CustomPacketPayload {
    public static final Type<RescueSoundSyncClearPayload> TYPE =
            new Type<>(new ResourceLocation(TouhouMaidAffection.MOD_ID, "rescue_sound_sync_clear"));

    public static final StreamCodec<ByteBuf, RescueSoundSyncClearPayload> STREAM_CODEC = StreamCodec.of(
            RescueSoundSyncClearPayload::encode,
            RescueSoundSyncClearPayload::decode
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(ByteBuf buf, RescueSoundSyncClearPayload payload) {
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.serverId());
        ByteBufCodecs.VAR_LONG.encode(buf, payload.generation());
        ByteBufCodecs.BOOL.encode(buf, payload.clearAll());
        ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8).encode(buf, new ArrayList<>(payload.relativePaths()));
    }

    private static RescueSoundSyncClearPayload decode(ByteBuf buf) {
        String serverId = ByteBufCodecs.STRING_UTF8.decode(buf);
        long generation = ByteBufCodecs.VAR_LONG.decode(buf);
        boolean clearAll = ByteBufCodecs.BOOL.decode(buf);
        List<String> relativePaths = ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8).decode(buf);
        return new RescueSoundSyncClearPayload(serverId, generation, clearAll, List.copyOf(relativePaths));
    }
}
