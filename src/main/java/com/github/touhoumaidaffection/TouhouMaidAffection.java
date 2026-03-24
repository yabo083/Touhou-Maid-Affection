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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(TouhouMaidAffection.MOD_ID)
public class TouhouMaidAffection {
    public static final String MOD_ID = "touhou_maid_affection";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public TouhouMaidAffection(IEventBus modEventBus, ModContainer modContainer) {
        // Register config
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, ModConfig.SPEC);

        // Register sound events
        ModSounds.SOUNDS.register(modEventBus);

        // Register mob effects
        ModEffects.MOB_EFFECTS.register(modEventBus);

        // Register entities
        ModEntityTypes.ENTITY_TYPES.register(modEventBus);

        // Register data attachments
        ModAttachments.ATTACHMENTS.register(modEventBus);

        // Register network packets on mod bus
        modEventBus.addListener(this::registerPayloads);

        // Register game event handlers on NeoForge bus
        NeoForge.EVENT_BUS.register(KissMaidHandler.class);

        BondAbilityManager.registerDefaults();

        LOGGER.info("Touhou Maid: Affection loaded! Now you can kiss your maid~");
    }

    private void registerPayloads(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1.0.0").optional();
        registrar.playToClient(
                KissMaidPayload.TYPE,
                KissMaidPayload.STREAM_CODEC,
                KissClientHandler::handle
        );
        registrar.playToServer(
                KissCarryRequestPayload.TYPE,
                KissCarryRequestPayload.STREAM_CODEC,
                KissCarryRequestHandler::handle
        );
        registrar.playToServer(
                BondActivateAbilityPayload.TYPE,
                BondActivateAbilityPayload.STREAM_CODEC,
                BondAbilityActivateHandler::handle
        );
        registrar.playToServer(
                BondStateRequestPayload.TYPE,
                BondStateRequestPayload.STREAM_CODEC,
                BondStateRequestHandler::handle
        );
        registrar.playToClient(
                BondStateSyncPayload.TYPE,
                BondStateSyncPayload.STREAM_CODEC,
                BondClientPayloadHandler::handleBondStateSync
        );
        registrar.playToClient(
                MaidRescuePopPayload.TYPE,
                MaidRescuePopPayload.STREAM_CODEC,
                BondClientPayloadHandler::handleRescuePop
        );
        registrar.playToServer(
                RescueActionConfigPayload.TYPE,
                RescueActionConfigPayload.STREAM_CODEC,
                RescueActionConfigHandler::handle
        );
        registrar.playToServer(
                RescueVoiceConfigPayload.TYPE,
                RescueVoiceConfigPayload.STREAM_CODEC,
                RescueVoiceConfigHandler::handle
        );
        registrar.playToServer(
                MorningKissVoiceConfigPayload.TYPE,
                MorningKissVoiceConfigPayload.STREAM_CODEC,
                MorningKissVoiceConfigHandler::handle
        );
        registrar.playToClient(
                MorningKissVoicePlayPayload.TYPE,
                MorningKissVoicePlayPayload.STREAM_CODEC,
                BondClientPayloadHandler::handleMorningKissVoicePlay
        );
        registrar.playToClient(
                RescueSoundSyncManifestPayload.TYPE,
                RescueSoundSyncManifestPayload.STREAM_CODEC,
                BondClientPayloadHandler::handleRescueSoundSyncManifest
        );
        registrar.playToClient(
                RescueSoundSyncClearPayload.TYPE,
                RescueSoundSyncClearPayload.STREAM_CODEC,
                BondClientPayloadHandler::handleRescueSoundSyncClear
        );
        registrar.playToClient(
                RescueSoundSyncChunkPayload.TYPE,
                RescueSoundSyncChunkPayload.STREAM_CODEC,
                BondClientPayloadHandler::handleRescueSoundSyncChunk
        );
        registrar.playToClient(
                RescueSoundSyncCompletePayload.TYPE,
                RescueSoundSyncCompletePayload.STREAM_CODEC,
                BondClientPayloadHandler::handleRescueSoundSyncComplete
        );
        registrar.playToClient(
                RescueSoundReloadPayload.TYPE,
                RescueSoundReloadPayload.STREAM_CODEC,
                BondClientPayloadHandler::handleRescueSoundReload
        );
        registrar.playToServer(
                LapPillowStartPayload.TYPE,
                LapPillowStartPayload.STREAM_CODEC,
                LapPillowHandler::handleStart
        );
        registrar.playToServer(
                LapPillowExitPayload.TYPE,
                LapPillowExitPayload.STREAM_CODEC,
                LapPillowHandler::handleExit
        );
        registrar.playToServer(
                LapPillowPoseConfigPayload.TYPE,
                LapPillowPoseConfigPayload.STREAM_CODEC,
                LapPillowPoseConfigHandler::handle
        );
        registrar.playToServer(
                LapPillowAngleLockPayload.TYPE,
                LapPillowAngleLockPayload.STREAM_CODEC,
                LapPillowAngleLockHandler::handle
        );
    }
}
