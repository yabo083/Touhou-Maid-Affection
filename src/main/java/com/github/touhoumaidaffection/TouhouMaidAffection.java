package com.github.touhoumaidaffection;

import com.github.touhoumaidaffection.bond.ability.BondAbilityManager;
import com.github.touhoumaidaffection.client.KissClientHandler;
import com.github.touhoumaidaffection.handler.BondAbilityActivateHandler;
import com.github.touhoumaidaffection.handler.KissCarryRequestHandler;
import com.github.touhoumaidaffection.handler.KissMaidHandler;
import com.github.touhoumaidaffection.network.BondActivateAbilityPayload;
import com.github.touhoumaidaffection.network.KissCarryRequestPayload;
import com.github.touhoumaidaffection.network.KissMaidPayload;
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
    }
}
