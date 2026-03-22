package com.github.touhoumaidaffection.client.screen.page;

import com.github.touhoumaidaffection.bond.ability.IBondAbility;

@FunctionalInterface
public interface SecondaryPageAvailability {
    boolean isAvailable(BondSecondaryPageHost host, IBondAbility ability);
}
