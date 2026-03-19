package com.github.touhoumaidaffection.bond.ability;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.touhoumaidaffection.ModConfig;
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
    public boolean canActivate(Player player, EntityMaid maid) {
        return true;
    }

    @Override
    public void activate(Player player, EntityMaid maid) {
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
