package com.github.touhoumaidaffection.client.screen.page;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.touhoumaidaffection.bond.ability.IBondAbility;
import com.github.touhoumaidaffection.client.screen.component.BondModalPage;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

public interface BondSecondaryPageHost {
    Font getFont();

    EntityMaid getMaid();

    BondModalPage createModal(int width, int height, Component title);

    void closeSecondaryPage();

    boolean isSecondaryPageUnlocked(IBondAbility ability);
}
