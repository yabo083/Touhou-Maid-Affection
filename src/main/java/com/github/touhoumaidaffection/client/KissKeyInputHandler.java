package com.github.touhoumaidaffection.client;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.github.touhoumaidaffection.network.KissCarryRequestPayload;
import com.github.touhoumaidaffection.network.KissTargetedMaidRequestPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

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

        int carriedKeyClicks = consumeClicks(KissKeyMappings.KISS_CARRIED_MAID);
        int targetedKeyClicks = consumeClicks(KissKeyMappings.KISS_TARGETED_MAID);
        int requestCount = Math.max(carriedKeyClicks, targetedKeyClicks);
        if (requestCount <= 0) {
            return;
        }

        boolean isCarryingMaid = minecraft.player.getPassengers().stream().anyMatch(passenger -> passenger instanceof EntityMaid);
        EntityMaid targetedMaid = getTargetedMaid(minecraft);
        boolean targetedKeyPressed = targetedKeyClicks > 0;
        for (int i = 0; i < requestCount; i++) {
            KissKeyAction action = KissKeyAction.choose(isCarryingMaid, targetedKeyPressed && targetedMaid != null);
            switch (action) {
                case CARRIED_MAID -> TouhouMaidAffection.CHANNEL.sendToServer(new KissCarryRequestPayload(0));
                case TARGETED_MAID -> TouhouMaidAffection.CHANNEL.sendToServer(new KissTargetedMaidRequestPayload(targetedMaid.getId()));
                case NONE -> {
                }
            }
        }
    }

    private static int consumeClicks(net.minecraft.client.KeyMapping keyMapping) {
        int count = 0;
        while (keyMapping.consumeClick()) {
            count++;
        }
        return count;
    }

    private static EntityMaid getTargetedMaid(Minecraft minecraft) {
        if (!(minecraft.hitResult instanceof EntityHitResult entityHitResult)) {
            return null;
        }
        Entity entity = entityHitResult.getEntity();
        return entity instanceof EntityMaid maid ? maid : null;
    }
}
