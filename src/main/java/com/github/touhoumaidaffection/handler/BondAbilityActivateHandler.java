package com.github.touhoumaidaffection.handler;

import com.github.touhoumaidaffection.bond.BondManager;
import com.github.touhoumaidaffection.bond.ability.BondAbilityManager;
import com.github.touhoumaidaffection.bond.ability.IBondAbility;
import com.github.touhoumaidaffection.network.BondActivateAbilityPayload;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class BondAbilityActivateHandler {
    private BondAbilityActivateHandler() {
    }

    public static void handle(BondActivateAbilityPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            IBondAbility ability = BondAbilityManager.getAbility(payload.abilityId());
            if (ability == null) {
                return;
            }
            Entity entity = player.serverLevel().getEntity(payload.maidUuid());
            if (!(entity instanceof EntityMaid maid)) {
                return;
            }
            BondManager.setBondLevel(player, maid.getUUID(), maid.getFavorabilityManager().getLevel());
            if (!BondManager.canUseAbility(player, payload.maidUuid(), ability.getId())) {
                return;
            }
            if (!ability.canActivate(player, maid)) {
                return;
            }
            int cost = ability.getPowerPointCost();
            if (!hasEnoughGems(player, cost)) {
                return;
            }
            consumeGems(player, cost);
            ability.activate(player, maid);
        });
    }

    private static boolean hasEnoughGems(ServerPlayer player, int cost) {
        if (cost <= 0) {
            return true;
        }
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(InitItems.POWER_POINT.get())) {
                count += stack.getCount();
                if (count >= cost) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void consumeGems(ServerPlayer player, int cost) {
        if (cost <= 0) {
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
