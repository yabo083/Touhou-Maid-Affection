package com.github.touhoumaidaffection.handler;

import com.github.touhoumaidaffection.bond.BondManager;
import com.github.touhoumaidaffection.bond.ability.BondAbilityManager;
import com.github.touhoumaidaffection.bond.ability.IBondAbility;
import com.github.touhoumaidaffection.bond.rescue.EmergencyHealListener;
import com.github.touhoumaidaffection.bond.rescue.EmergencyRescueData;
import com.github.touhoumaidaffection.network.BondActivateAbilityPayload;
import com.github.touhoumaidaffection.network.BondStateSyncPayload;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
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

            long nowMs = System.currentTimeMillis();
            int queuedGiftCount = BondManager.isAbilityUnlocked(player, maid.getUUID(), "random_gift")
                    ? BondManager.reconcileRandomGiftQueue(player, maid.getUUID(), nowMs)
                    : 0;
            long nextGiftReadyAtMs = BondManager.isAbilityUnlocked(player, maid.getUUID(), "random_gift")
                    ? BondManager.getNextRandomGiftReadyAtMs(player, maid.getUUID(), nowMs)
                    : 0L;
            int nextGiftReadySeconds = nextGiftReadyAtMs > nowMs
                    ? (int) Math.min(Integer.MAX_VALUE, (nextGiftReadyAtMs - nowMs + 999L) / 1000L)
                    : 0;
            PacketDistributor.sendToPlayer(player, new BondStateSyncPayload(
                    maid.getUUID(),
                    BondManager.getUnlockedAbilityIds(player, maid.getUUID()),
                    queuedGiftCount,
                    Math.max(1, com.github.touhoumaidaffection.ModConfig.BOND_RANDOM_GIFT_MAX_QUEUED.get()),
                    nextGiftReadySeconds,
                    "",
                    "",
                    "",
                    ""
            ));
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
