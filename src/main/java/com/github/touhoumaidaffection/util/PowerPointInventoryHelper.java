package com.github.touhoumaidaffection.util;

import com.github.tartaricacid.touhoulittlemaid.init.InitItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class PowerPointInventoryHelper {
    private PowerPointInventoryHelper() {
    }

    public static int countPowerPoints(Player player) {
        if (player == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(InitItems.POWER_POINT.get())) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public static boolean hasEnoughPowerPoints(Player player, int cost) {
        return cost <= 0 || countPowerPoints(player) >= cost;
    }

    public static void consumePowerPoints(ServerPlayer player, int cost) {
        if (player == null || cost <= 0) {
            return;
        }
        int remaining = cost;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.is(InitItems.POWER_POINT.get())) {
                continue;
            }
            int consume = Math.min(stack.getCount(), remaining);
            stack.shrink(consume);
            remaining -= consume;
        }
    }
}
