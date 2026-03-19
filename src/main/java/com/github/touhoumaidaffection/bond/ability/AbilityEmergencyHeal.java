package com.github.touhoumaidaffection.bond.ability;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.touhoumaidaffection.ModConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class AbilityEmergencyHeal implements IBondAbility {
    @Override
    public String getId() {
        return "emergency_heal";
    }

    @Override
    public int getPowerPointCost() {
        return ModConfig.BOND_COST_EMERGENCY_HEAL.get();
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
        return Component.translatable("bond.ability.heal");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("bond.ability.heal.desc");
    }
}
