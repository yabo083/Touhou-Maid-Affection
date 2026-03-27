package com.github.touhoumaidaffection.client.screen.page;

import com.github.touhoumaidaffection.bond.ability.IBondAbility;

@FunctionalInterface
public interface BondSecondaryPageFactory {
    BondSecondaryPage create(BondSecondaryPageHost host, IBondAbility ability);
}
