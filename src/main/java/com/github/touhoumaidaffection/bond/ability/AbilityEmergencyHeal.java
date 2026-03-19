package com.github.touhoumaidaffection.bond.ability;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class AbilityEmergencyHeal implements IBondAbility {
    @Override
    public String getId() {
        return "emergency_heal";
    }

    @Override
    public int getGemCost() {
        return 0;
    }

    @Override
    public boolean canActivate(Player player, EntityMaid maid) {
        return false;
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