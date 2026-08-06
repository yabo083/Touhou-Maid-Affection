package com.github.touhoumaidaffection.client;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.github.touhoumaidaffection.network.KissCarryRequestPayload;
import com.github.touhoumaidaffection.network.KissTargetedMaidRequestPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;

@EventBusSubscriber(modid = TouhouMaidAffection.MOD_ID, value = Dist.CLIENT)
public class KissKeyInputHandler {

    private static boolean kissKeyWasDown = false;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean keyDown = KissKeyMappings.KISS_MAID.isDown();
        if (minecraft.player == null || minecraft.level == null) {
            kissKeyWasDown = keyDown;
            return;
        }

        // Edge trigger on the key state: one physical press = one kiss. consumeClick()
        // also fires on OS key auto-repeat, which would spam kisses while holding the
        // key (and restart the kiss camera every repeat) — the edge check avoids that,
        // so holding K gives exactly one kiss ("按一下亲一次，长按也是亲一次").
        if (keyDown && !kissKeyWasDown) {
            boolean isCarryingMaid = minecraft.player.getPassengers().stream().anyMatch(passenger -> passenger instanceof EntityMaid);
            EntityMaid targetedMaid = getTargetedMaid(minecraft);
            KissKeyAction action = KissKeyAction.choose(isCarryingMaid, targetedMaid != null);
            switch (action) {
                case CARRIED_MAID -> PacketDistributor.sendToServer(new KissCarryRequestPayload(0));
                case TARGETED_MAID -> PacketDistributor.sendToServer(new KissTargetedMaidRequestPayload(targetedMaid.getId()));
                case NONE -> {
                }
            }
        }
        kissKeyWasDown = keyDown;
    }

    private static EntityMaid getTargetedMaid(Minecraft minecraft) {
        if (!(minecraft.hitResult instanceof EntityHitResult entityHitResult)) {
            return null;
        }
        Entity entity = entityHitResult.getEntity();
        return entity instanceof EntityMaid maid ? maid : null;
    }
}
