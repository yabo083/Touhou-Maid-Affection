package com.github.touhoumaidaffection.bond;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BondKeysTest {

    private static final UUID MAID = UUID.fromString("12345678-1234-1234-1234-1234567890ab");
    private static final UUID OTHER = UUID.fromString("87654321-4321-4321-4321-ba0987654321");

    @Test
    void flatKeyAndBaseOfFlatKeyRoundTrip() {
        String key = BondKeys.flatKey(BondKeys.BOND_LEVEL, MAID);

        assertEquals(BondKeys.BOND_LEVEL + "_" + MAID, key);
        assertEquals(Optional.of(BondKeys.BOND_LEVEL), BondKeys.baseOfFlatKey(key, MAID));
    }

    @Test
    void flatKeyRejectsBlankBaseAndNullUuid() {
        assertNull(BondKeys.flatKey(null, MAID));
        assertNull(BondKeys.flatKey("", MAID));
        assertNull(BondKeys.flatKey("   ", MAID));
        assertNull(BondKeys.flatKey(BondKeys.BOND_LEVEL, null));
    }

    @Test
    void baseOfFlatKeyHandlesLapPillowPrefixesWithUnderscores() {
        String mode = BondKeys.flatKey(BondKeys.LAP_PILLOW_MODE, MAID);
        String offset = BondKeys.flatKey(BondKeys.LAP_PILLOW_PLAYER_OFFSET_X, MAID);
        String legacy = BondKeys.flatKey(BondKeys.LAP_PILLOW_LEGACY_OFFSET_X, MAID);

        assertEquals(Optional.of("BondMaidLapPillow_Mode"), BondKeys.baseOfFlatKey(mode, MAID));
        assertEquals(Optional.of("BondMaidLapPillow_PlayerOffsetX"), BondKeys.baseOfFlatKey(offset, MAID));
        assertEquals(Optional.of("BondMaidLapPillow_OffsetX"), BondKeys.baseOfFlatKey(legacy, MAID));
    }

    @Test
    void baseOfFlatKeyMatchesUuidCaseInsensitively() {
        String upper = BondKeys.BOND_LEVEL + "_" + MAID.toString().toUpperCase(java.util.Locale.ROOT);

        assertEquals(Optional.of(BondKeys.BOND_LEVEL), BondKeys.baseOfFlatKey(upper, MAID));
    }

    @Test
    void baseOfFlatKeyRejectsUnrelatedKeys() {
        assertTrue(BondKeys.baseOfFlatKey(BondKeys.BOND_LEVEL + "_" + OTHER, MAID).isEmpty());
        assertTrue(BondKeys.baseOfFlatKey(BondKeys.BOND_LEVEL, MAID).isEmpty());
        assertTrue(BondKeys.baseOfFlatKey(BondKeys.BOND_LEVEL + "_", MAID).isEmpty());
        assertTrue(BondKeys.baseOfFlatKey("_" + MAID, MAID).isEmpty());
        assertTrue(BondKeys.baseOfFlatKey("", MAID).isEmpty());
        assertTrue(BondKeys.baseOfFlatKey(null, MAID).isEmpty());
        assertTrue(BondKeys.baseOfFlatKey(BondKeys.BOND_LEVEL + "_" + MAID, null).isEmpty());
        assertTrue(BondKeys.baseOfFlatKey(BondKeys.BOND_LEVEL + "_" + MAID + "x", MAID).isEmpty());
    }

    @Test
    void parseFlatKeySplitsAtLastUnderscore() {
        BondKeys.ParsedFlatKey parsed = BondKeys.parseFlatKey(BondKeys.flatKey(BondKeys.LAP_PILLOW_MODE, MAID))
                .orElseThrow();

        assertEquals(MAID, parsed.maidUuid());
        assertEquals("BondMaidLapPillow_Mode", parsed.baseName());
    }

    @Test
    void parseFlatKeyPreservesPlayerLevelKeysAndMalformedKeys() {
        assertTrue(BondKeys.parseFlatKey(BondKeys.MORNING_KISS_SELECTED_WINDOW_ID).isEmpty());
        assertTrue(BondKeys.parseFlatKey(BondKeys.MORNING_KISS_SELECTED_MAID_ID).isEmpty());
        assertTrue(BondKeys.parseFlatKey(BondKeys.SCHEMA_VERSION_KEY).isEmpty());
        assertTrue(BondKeys.parseFlatKey("SomeKey_notauuid").isEmpty());
        assertTrue(BondKeys.parseFlatKey("trailing_").isEmpty());
        assertTrue(BondKeys.parseFlatKey("_" + MAID).isEmpty());
        assertTrue(BondKeys.parseFlatKey(MAID.toString()).isEmpty());
        assertTrue(BondKeys.parseFlatKey("").isEmpty());
        assertTrue(BondKeys.parseFlatKey(null).isEmpty());
    }

    @Test
    void parseFlatKeyToleratesUpperCaseUuidSuffix() {
        String key = BondKeys.BOND_LEVEL + "_" + MAID.toString().toUpperCase(java.util.Locale.ROOT);

        BondKeys.ParsedFlatKey parsed = BondKeys.parseFlatKey(key).orElseThrow();

        assertEquals(MAID, parsed.maidUuid());
        assertEquals(BondKeys.BOND_LEVEL, parsed.baseName());
    }

    @Test
    void schemaConstantsAreStable() {
        assertEquals("touhou_maid_affection.bond", BondKeys.ROOT);
        assertEquals("maids", BondKeys.MAIDS);
        assertEquals("SchemaVersion", BondKeys.SCHEMA_VERSION_KEY);
        assertEquals("LastSeen", BondKeys.LAST_SEEN_KEY);
        assertEquals(2, BondKeys.CURRENT_SCHEMA);
    }

    @Test
    void playerLevelKeysHaveNoUuidSuffix() {
        assertFalse(BondKeys.MORNING_KISS_SELECTED_WINDOW_ID.endsWith("_" + MAID));
        assertFalse(BondKeys.MORNING_KISS_SELECTED_MAID_ID.endsWith("_" + MAID));
    }
}