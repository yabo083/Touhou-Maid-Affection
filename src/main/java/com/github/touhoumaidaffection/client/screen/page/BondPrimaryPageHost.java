package com.github.touhoumaidaffection.client.screen.page;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.touhoumaidaffection.bond.ability.IBondAbility;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public interface BondPrimaryPageHost {
    Font getFont();

    EntityMaid getMaid();

    Player getLocalPlayer();

    List<IBondAbility> getAbilities();

    int getPowerPointCount();

    boolean isBondUnlocked();

    void openSecondaryPageForAbility(IBondAbility ability);

    Component getStatusText(IBondAbility ability, boolean unlocked, boolean abilityUnlocked, boolean enoughPowerPoint, boolean canUnlockNow, boolean canUseSecondary);

    boolean isMainButtonClickable(IBondAbility ability, boolean unlocked, boolean abilityUnlocked, boolean enoughPowerPoint, boolean canUnlockNow, boolean canUseSecondary);

    boolean hasSecondaryPageButton(IBondAbility ability, boolean abilityUnlocked);

    Component getSecondaryPageButtonLabel(IBondAbility ability);

    boolean isRandomGiftAbility(IBondAbility ability);

    boolean isMorningKissAbility(IBondAbility ability);

    boolean isEmergencyHealAbility(IBondAbility ability);

    boolean isRescueActionConfigAvailable();

    String getRescueActionModelId();

    String getRescueActionTextureId();

    String resolveSelectedRescueActionLabel(String actionId);

    String formatRemainingDuration(int totalSeconds);

    void activateAbility(IBondAbility ability);
}
