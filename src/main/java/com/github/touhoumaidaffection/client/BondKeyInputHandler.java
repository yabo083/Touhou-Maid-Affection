package com.github.touhoumaidaffection.client;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.github.touhoumaidaffection.network.LapPillowExitPayload;
import com.github.touhoumaidaffection.network.LapPillowStartPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = TouhouMaidAffection.MOD_ID, value = Dist.CLIENT)
public class BondKeyInputHandler {
    private static boolean lapPillowActive;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        while (BondKeyMappings.LAP_PILLOW.consumeClick()) {
            if (lapPillowActive) {
                PacketDistributor.sendToServer(new LapPillowExitPayload(0));
                lapPillowActive = false;
                continue;
            }
            if (minecraft.hitResult instanceof EntityHitResult hitResult && hitResult.getEntity() instanceof EntityMaid maid) {
                PacketDistributor.sendToServer(new LapPillowStartPayload(maid.getUUID()));
                lapPillowActive = true;
            }
        }
    }
}
