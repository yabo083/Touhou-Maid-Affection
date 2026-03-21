package com.github.touhoumaidaffection.client;

import com.github.touhoumaidaffection.network.BondStateSyncPayload;
import com.github.touhoumaidaffection.network.MaidRescuePopPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.LinkedHashSet;

public final class BondClientPayloadHandler {
    private BondClientPayloadHandler() {
    }

    public static void handleBondStateSync(BondStateSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> BondClientStateCache.update(
                payload.maidUuid(),
                new LinkedHashSet<>(payload.unlockedAbilityIds()),
                payload.queuedGiftCount(),
                payload.maxQueuedGiftCount(),
                payload.nextGiftReadySeconds()
        ));
    }

    public static void handleRescuePop(MaidRescuePopPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(
                        Component.translatable("bond.emergency_rescue.triggered", payload.maidModelId()),
                        true
                );
            }
        });
    }
}
