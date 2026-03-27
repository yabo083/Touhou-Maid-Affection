package com.github.touhoumaidaffection;

import com.github.touhoumaidaffection.bond.ability.BondAbilityManager;
import com.github.touhoumaidaffection.client.BondClientPayloadHandler;
import com.github.touhoumaidaffection.client.KissClientHandler;
import com.github.touhoumaidaffection.handler.BondAbilityActivateHandler;
import com.github.touhoumaidaffection.handler.BondStateRequestHandler;
import com.github.touhoumaidaffection.handler.KissCarryRequestHandler;
import com.github.touhoumaidaffection.handler.KissMaidHandler;
import com.github.touhoumaidaffection.handler.LapPillowAngleLockHandler;
import com.github.touhoumaidaffection.handler.LapPillowHandler;
import com.github.touhoumaidaffection.handler.LapPillowPoseConfigHandler;
import com.github.touhoumaidaffection.handler.MorningKissVoiceConfigHandler;
import com.github.touhoumaidaffection.handler.RescueActionConfigHandler;
import com.github.touhoumaidaffection.handler.RescueVoiceConfigHandler;
import com.github.touhoumaidaffection.network.BondActivateAbilityPayload;
import com.github.touhoumaidaffection.network.BondStateRequestPayload;
import com.github.touhoumaidaffection.network.BondStateSyncPayload;
import com.github.touhoumaidaffection.network.ForgePayloadContext;
import com.github.touhoumaidaffection.network.KissCarryRequestPayload;
import com.github.touhoumaidaffection.network.KissMaidPayload;
import com.github.touhoumaidaffection.network.LapPillowAngleLockPayload;
import com.github.touhoumaidaffection.network.LapPillowExitPayload;
import com.github.touhoumaidaffection.network.LapPillowPoseConfigPayload;
import com.github.touhoumaidaffection.network.LapPillowStartPayload;
import com.github.touhoumaidaffection.network.MaidRescuePopPayload;
import com.github.touhoumaidaffection.network.MorningKissVoiceConfigPayload;
import com.github.touhoumaidaffection.network.MorningKissVoicePlayPayload;
import com.github.touhoumaidaffection.network.RescueActionConfigPayload;
import com.github.touhoumaidaffection.network.RescueVoiceConfigPayload;
import com.github.touhoumaidaffection.network.rescue.RescueSoundReloadPayload;
import com.github.touhoumaidaffection.network.rescue.RescueSoundSyncChunkPayload;
import com.github.touhoumaidaffection.network.rescue.RescueSoundSyncClearPayload;
import com.github.touhoumaidaffection.network.rescue.RescueSoundSyncCompletePayload;
import com.github.touhoumaidaffection.network.rescue.RescueSoundSyncManifestPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;

@Mod(TouhouMaidAffection.MOD_ID)
public class TouhouMaidAffection {
    public static final String MOD_ID = "touhou_maid_affection";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final String PROTOCOL_VERSION = "1.7.1.1-forge";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            s -> PROTOCOL_VERSION.equals(s) || NetworkRegistry.ABSENT.equals(s) || NetworkRegistry.ACCEPTVANILLA.equals(s),
            s -> PROTOCOL_VERSION.equals(s) || NetworkRegistry.ABSENT.equals(s) || NetworkRegistry.ACCEPTVANILLA.equals(s)
    );

    public TouhouMaidAffection() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, com.github.touhoumaidaffection.ModConfig.SPEC);

        ModSounds.SOUNDS.register(modEventBus);
        ModEffects.MOB_EFFECTS.register(modEventBus);
        ModEntityTypes.ENTITY_TYPES.register(modEventBus);

        registerPayloads();

        MinecraftForge.EVENT_BUS.register(KissMaidHandler.class);

        BondAbilityManager.registerDefaults();

        LOGGER.info("Touhou Maid: Affection loaded! Now you can kiss your maid~");
    }

    private void registerPayloads() {
        int id = 0;
        id = registerMessage(id, KissMaidPayload.class, KissMaidPayload.STREAM_CODEC, KissClientHandler::handle);
        id = registerMessage(id, KissCarryRequestPayload.class, KissCarryRequestPayload.STREAM_CODEC, KissCarryRequestHandler::handle);
        id = registerMessage(id, BondActivateAbilityPayload.class, BondActivateAbilityPayload.STREAM_CODEC, BondAbilityActivateHandler::handle);
        id = registerMessage(id, BondStateRequestPayload.class, BondStateRequestPayload.STREAM_CODEC, BondStateRequestHandler::handle);
        id = registerMessage(id, BondStateSyncPayload.class, BondStateSyncPayload.STREAM_CODEC, BondClientPayloadHandler::handleBondStateSync);
        id = registerMessage(id, MaidRescuePopPayload.class, MaidRescuePopPayload.STREAM_CODEC, BondClientPayloadHandler::handleRescuePop);
        id = registerMessage(id, RescueActionConfigPayload.class, RescueActionConfigPayload.STREAM_CODEC, RescueActionConfigHandler::handle);
        id = registerMessage(id, RescueVoiceConfigPayload.class, RescueVoiceConfigPayload.STREAM_CODEC, RescueVoiceConfigHandler::handle);
        id = registerMessage(id, MorningKissVoiceConfigPayload.class, MorningKissVoiceConfigPayload.STREAM_CODEC, MorningKissVoiceConfigHandler::handle);
        id = registerMessage(id, MorningKissVoicePlayPayload.class, MorningKissVoicePlayPayload.STREAM_CODEC, BondClientPayloadHandler::handleMorningKissVoicePlay);
        id = registerMessage(id, RescueSoundSyncManifestPayload.class, RescueSoundSyncManifestPayload.STREAM_CODEC, BondClientPayloadHandler::handleRescueSoundSyncManifest);
        id = registerMessage(id, RescueSoundSyncClearPayload.class, RescueSoundSyncClearPayload.STREAM_CODEC, BondClientPayloadHandler::handleRescueSoundSyncClear);
        id = registerMessage(id, RescueSoundSyncChunkPayload.class, RescueSoundSyncChunkPayload.STREAM_CODEC, BondClientPayloadHandler::handleRescueSoundSyncChunk);
        id = registerMessage(id, RescueSoundSyncCompletePayload.class, RescueSoundSyncCompletePayload.STREAM_CODEC, BondClientPayloadHandler::handleRescueSoundSyncComplete);
        id = registerMessage(id, RescueSoundReloadPayload.class, RescueSoundReloadPayload.STREAM_CODEC, BondClientPayloadHandler::handleRescueSoundReload);
        id = registerMessage(id, LapPillowStartPayload.class, LapPillowStartPayload.STREAM_CODEC, LapPillowHandler::handleStart);
        id = registerMessage(id, LapPillowExitPayload.class, LapPillowExitPayload.STREAM_CODEC, LapPillowHandler::handleExit);
        id = registerMessage(id, LapPillowPoseConfigPayload.class, LapPillowPoseConfigPayload.STREAM_CODEC, LapPillowPoseConfigHandler::handle);
        registerMessage(id, LapPillowAngleLockPayload.class, LapPillowAngleLockPayload.STREAM_CODEC, LapPillowAngleLockHandler::handle);
    }

    private <T> int registerMessage(
            int id,
            Class<T> payloadType,
            StreamCodec<ByteBuf, T> codec,
            BiConsumer<T, IPayloadContext> handler
    ) {
        CHANNEL.registerMessage(
                id,
                payloadType,
                (payload, buf) -> codec.encode(buf, payload),
                codec::decode,
                (payload, contextSupplier) -> {
                    NetworkEvent.Context context = contextSupplier.get();
                    handler.accept(payload, ForgePayloadContext.wrap(context));
                    context.setPacketHandled(true);
                }
        );
        return id + 1;
    }
}
