package com.github.touhoumaidaffection.bond.ability;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public interface IBondAbility {
    String getId();

    int getPowerPointCost();

    boolean canUnlock(Player player, EntityMaid maid);

    void unlock(Player player, EntityMaid maid);

    default boolean hasSecondaryAction() {
        return false;
    }

    default boolean canPerformSecondaryAction(Player player, EntityMaid maid) {
        return false;
    }

    default void performSecondaryAction(Player player, EntityMaid maid) {
    }

    default Component getUnlockedButtonLabel() {
        return Component.translatable("bond.unlocked");
    }

    default Component getSecondaryActionButtonLabel() {
        return Component.translatable("bond.use");
    }

    Component getDisplayName();

    Component getDescription();
}
