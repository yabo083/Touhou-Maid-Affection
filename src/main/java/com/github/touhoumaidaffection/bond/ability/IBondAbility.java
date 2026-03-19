package com.github.touhoumaidaffection.bond.ability;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public interface IBondAbility {
    String getId();

    int getPowerPointCost();

    boolean canActivate(Player player, EntityMaid maid);

    void activate(Player player, EntityMaid maid);

    Component getDisplayName();

    Component getDescription();
}
