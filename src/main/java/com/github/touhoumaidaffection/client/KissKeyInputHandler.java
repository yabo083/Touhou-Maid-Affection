package com.github.touhoumaidaffection.client;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.github.touhoumaidaffection.network.KissCarryRequestPayload;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.network.PacketDistributor;

@EventBusSubscriber(modid = TouhouMaidAffection.MOD_ID, value = Dist.CLIENT)
public class KissKeyInputHandler {
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        while (KissKeyMappings.KISS_CARRIED_MAID.consumeClick()) {
            boolean isCarryingMaid = minecraft.player.getPassengers().stream().anyMatch(passenger -> passenger instanceof EntityMaid);
            if (isCarryingMaid) {
                TouhouMaidAffection.CHANNEL.sendToServer(new KissCarryRequestPayload(0));
            }
        }
    }
}
