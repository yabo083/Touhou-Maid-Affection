package com.github.touhoumaidaffection.handler;

import com.github.touhoumaidaffection.bond.BondManager;
import com.github.touhoumaidaffection.bond.ability.BondAbilityManager;
import com.github.touhoumaidaffection.bond.ability.IBondAbility;
import com.github.touhoumaidaffection.bond.rescue.EmergencyHealListener;
import com.github.touhoumaidaffection.bond.rescue.EmergencyRescueData;
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
            BondManager.syncMaidProfile(player, maid);
            if (!BondManager.isBondUnlocked(player, payload.maidUuid())) {
                return;
            }

            boolean abilityUnlocked = BondManager.isAbilityUnlocked(player, payload.maidUuid(), ability.getId());
            if (!abilityUnlocked) {
                if (!ability.canUnlock(player, maid)) {
                    return;
                }
                int cost = ability.getPowerPointCost();
                if (!hasEnoughPowerPoints(player, cost)) {
                    return;
                }
                consumePowerPoints(player, cost);
                BondManager.unlockAbility(player, maid.getUUID(), ability.getId());
                ability.unlock(player, maid);
                if ("emergency_heal".equals(ability.getId())) {
                    EmergencyHealListener.ensureRescueChargesUpToDate(player);
                    EmergencyRescueData.grantImmediateRescueIfEligible(player, maid.getUUID());
                }
            } else if (ability.hasSecondaryAction()) {
                if (!ability.canPerformSecondaryAction(player, maid)) {
                    return;
                }
                ability.performSecondaryAction(player, maid);
            } else {
                return;
            }

            BondSyncHelper.sendBondState(player, maid);
        });
    }

    private static boolean hasEnoughPowerPoints(ServerPlayer player, int cost) {
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

    private static void consumePowerPoints(ServerPlayer player, int cost) {
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
