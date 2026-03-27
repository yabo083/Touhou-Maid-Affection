package com.github.touhoumaidaffection.bond.rescue;

import com.github.tartaricacid.touhoulittlemaid.api.event.MaidAndItemTransformEvent;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.github.touhoumaidaffection.bond.BondData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = TouhouMaidAffection.MOD_ID)
public final class MaidRescueContributorSyncHandler {
    private MaidRescueContributorSyncHandler() {
    }

    @SubscribeEvent
    public static void onMaidToItem(MaidAndItemTransformEvent.ToItem event) {
        String contributorId = MaidRescueContributorId.ensure(event.getMaid());
        MaidRescueContributorId.writeToTransformTag(event.getMaid(), event.getData());
        if (!contributorId.isBlank() && event.getMaid().getOwner() instanceof ServerPlayer player) {
            BondData.of(player).setMaidRescueProviderId(event.getMaid().getUUID(), contributorId);
        }
    }

    @SubscribeEvent
    public static void onItemToMaid(MaidAndItemTransformEvent.ToMaid event) {
        MaidRescueContributorId.restoreFromTransformTag(event.getMaid(), event.getData());
    }
}
