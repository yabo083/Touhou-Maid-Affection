package com.github.touhoumaidaffection.bond.ability;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.touhoumaidaffection.ModConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class AbilityRandomGift implements IBondAbility {
    @Override
    public String getId() {
        return "random_gift";
    }

    @Override
    public int getPowerPointCost() {
        return ModConfig.BOND_COST_RANDOM_GIFT.get();
    }

    @Override
    public boolean canUnlock(Player player, EntityMaid maid) {
        return true;
    }

    @Override
    public void unlock(Player player, EntityMaid maid) {
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            com.github.touhoumaidaffection.bond.BondManager.initializeRandomGiftState(
                    serverPlayer,
                    maid.getUUID(),
                    System.currentTimeMillis()
            );
        }
    }

    @Override
    public Component getUnlockedButtonLabel() {
        return Component.translatable("bond.random_gift.status.running");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("bond.ability.gift");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("bond.ability.gift.desc");
    }
}
