package com.github.touhoumaidaffection.client;

import com.github.touhoumaidaffection.network.MaidRescuePopPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class EmergencyRescueVisualHandler {
    private EmergencyRescueVisualHandler() {
    }

    public static void play(MaidRescuePopPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gameRenderer == null) {
            return;
        }

        EmergencyRescueSoundPlayer.play(payload);

        boolean overlayStarted = EmergencyRescueOverlayRenderer.show(payload);
        if (!overlayStarted) {
            minecraft.gameRenderer.displayItemActivation(new ItemStack(Items.TOTEM_OF_UNDYING));
        }

        minecraft.player.displayClientMessage(Component.translatable(
                "bond.emergency_rescue.triggered",
                EmergencyRescueOverlayRenderer.getResolvedDisplayName(payload)
        ), true);
    }
}
