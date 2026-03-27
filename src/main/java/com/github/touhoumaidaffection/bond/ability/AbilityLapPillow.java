package com.github.touhoumaidaffection.bond.ability;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.touhoumaidaffection.ModConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class AbilityLapPillow implements IBondAbility {
    @Override
    public String getId() {
        return "lap_pillow";
    }

    @Override
    public int getPowerPointCost() {
        return ModConfig.BOND_COST_LAP_PILLOW.get();
    }

    @Override
    public boolean canUnlock(Player player, EntityMaid maid) {
        return true;
    }

    @Override
    public void unlock(Player player, EntityMaid maid) {
    }

    @Override
    public Component getUnlockedButtonLabel() {
        return Component.translatable("bond.action.press_b");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("bond.ability.lap");
    }

    @Override
    public Component getDescription() {
        return Component.translatable("bond.ability.lap.desc");
    }
}
