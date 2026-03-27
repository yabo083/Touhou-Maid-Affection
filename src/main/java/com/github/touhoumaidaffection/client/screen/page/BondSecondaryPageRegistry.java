package com.github.touhoumaidaffection.client.screen.page;

import com.github.touhoumaidaffection.bond.ability.IBondAbility;

import java.util.LinkedHashMap;
import java.util.Map;

public final class BondSecondaryPageRegistry {
    private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();

    private BondSecondaryPageRegistry() {
    }

    public static void register(String abilityId, SecondaryPageAvailability availability, BondSecondaryPageFactory factory) {
        if (abilityId == null || abilityId.isBlank() || availability == null || factory == null) {
            return;
        }
        ENTRIES.putIfAbsent(abilityId, new Entry(availability, factory));
    }

    public static BondSecondaryPage createPage(BondSecondaryPageHost host, IBondAbility ability) {
        if (host == null || ability == null) {
            return null;
        }
        Entry entry = ENTRIES.get(ability.getId());
        if (entry == null || !entry.availability().isAvailable(host, ability)) {
            return null;
        }
        return entry.factory().create(host, ability);
    }

    public static boolean hasPage(BondSecondaryPageHost host, IBondAbility ability) {
        if (host == null || ability == null) {
            return false;
        }
        Entry entry = ENTRIES.get(ability.getId());
        return entry != null && entry.availability().isAvailable(host, ability);
    }

    public static void registerDefaults() {
        if (!ENTRIES.isEmpty()) {
            return;
        }
        register("lap_pillow",
                (host, ability) -> host.getMaid() != null,
                (host, ability) -> new LapPillowPoseSecondaryPage(host));
        register("emergency_heal",
                (host, ability) -> host.getMaid() != null,
                (host, ability) -> new EmergencyRescueVoiceSecondaryPage(host));
        register("morning_kiss",
                (host, ability) -> host.getMaid() != null,
                (host, ability) -> new MorningKissVoiceSecondaryPage(host));
    }

    private record Entry(SecondaryPageAvailability availability, BondSecondaryPageFactory factory) {
    }
}
