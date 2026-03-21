package com.github.touhoumaidaffection.bond.ability;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.touhoumaidaffection.ModConfig;
import com.github.touhoumaidaffection.bond.service.MorningKissService;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class AbilityMorningKiss implements IBondAbility {
    @Override
    public String getId() {
        return "morning_kiss";
    }

    @Override
    public int getPowerPointCost() {
        return ModConfig.BOND_COST_MORNING_KISS.get();
    }

    @Override
    public boolean canUnlock(Player player, EntityMaid maid) {
        return true;
    }

    @Override
    public void unlock(Player player, EntityMaid maid) {
    }

    @Override
    public boolean hasSecondaryAction() {
        return true;
    }

    @Override
    public boolean canPerformSecondaryAction(Player player, EntityMaid maid) {
        double maxDistance = ModConfig.BOND_MORNING_KISS_MAX_DISTANCE.get();
        return maid.isAlive() && player.distanceToSqr(maid) <= maxDistance * maxDistance;
    }

    @Override
    public void performSecondaryAction(Player player, EntityMaid maid) {
        MorningKissService.start(player, maid);
    }

    @Override
    public Component getSecondaryActionButtonLabel() {
        return Component.translatable("bond.action.call");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("bond.ability.kiss");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("bond.ability.kiss.desc");
    }
}
