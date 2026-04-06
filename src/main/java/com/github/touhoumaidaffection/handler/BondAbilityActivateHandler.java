package com.github.touhoumaidaffection.handler;

import com.github.touhoumaidaffection.bond.BondManager;
import com.github.touhoumaidaffection.bond.ability.BondAbilityManager;
import com.github.touhoumaidaffection.bond.ability.IBondAbility;
import com.github.touhoumaidaffection.bond.rescue.EmergencyRescueData;
import com.github.touhoumaidaffection.bond.rescue.EmergencyRescueService;
import com.github.touhoumaidaffection.network.BondActivateAbilityPayload;
import com.github.touhoumaidaffection.util.PowerPointInventoryHelper;
import net.minecraft.server.level.ServerPlayer;
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
            var maid = MaidPayloadResolver.resolveOwnedMaid(player, payload.maidUuid());
            if (maid == null) {
                return;
            }
            BondManager.setBondLevel(player, maid.getUUID(), maid.getFavorabilityManager().getLevel());
            BondManager.syncMaidProfile(player, maid);
            if (!BondManager.isBondUnlocked(player, payload.maidUuid())) {
                return;
            }

            boolean abilityUnlocked = BondManager.isAbilityUnlocked(player, payload.maidUuid(), ability.getId());
            if (!abilityUnlocked) {
                boolean emergencyHeal = "emergency_heal".equals(ability.getId());
                if (emergencyHeal && EmergencyRescueData.isContributorAlreadyUnlocked(player, maid.getUUID())) {
                    BondManager.unlockAbility(player, maid.getUUID(), ability.getId());
                    EmergencyRescueService.refreshChargesIfNeeded(player);
                    BondSyncHelper.sendBondState(player, maid);
                    return;
                }
                if (!ability.canUnlock(player, maid)) {
                    return;
                }
                int cost = ability.getPowerPointCost();
                if (!PowerPointInventoryHelper.hasEnoughPowerPoints(player, cost)) {
                    return;
                }
                PowerPointInventoryHelper.consumePowerPoints(player, cost);
                BondManager.unlockAbility(player, maid.getUUID(), ability.getId());
                ability.unlock(player, maid);
                if (emergencyHeal) {
                    EmergencyRescueService.refreshChargesIfNeeded(player);
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
}
