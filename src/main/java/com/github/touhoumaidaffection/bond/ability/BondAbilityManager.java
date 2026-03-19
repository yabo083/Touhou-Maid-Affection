package com.github.touhoumaidaffection.bond.ability;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BondAbilityManager {
    private static final Map<String, IBondAbility> ABILITIES = new LinkedHashMap<>();

    private BondAbilityManager() {
    }

    public static void register(IBondAbility ability) {
        ABILITIES.putIfAbsent(ability.getId(), ability);
    }

    public static IBondAbility getAbility(String id) {
        return ABILITIES.get(id);
    }

    public static Collection<IBondAbility> getAllAbilities() {
        return Collections.unmodifiableCollection(ABILITIES.values());
    }

    public static void registerDefaults() {
        if (!ABILITIES.isEmpty()) {
            return;
        }
        register(new AbilityLapPillow());
        register(new AbilityEmergencyHeal());
        register(new AbilityMorningKiss());
        register(new AbilityYSMAction());
        register(new AbilityRandomGift());
    }
}
