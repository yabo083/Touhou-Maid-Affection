package com.github.touhoumaidaffection.compat.maidfm;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MaidDataKeyCodecTest {

    private static final UUID MAID = UUID.fromString("12345678-1234-1234-1234-123456789abc");
    private static final UUID OTHER = UUID.fromString("87654321-4321-4321-4321-cba987654321");

    @Test
    void shouldRoundTripBaseNameAndKey() {
        String key = MaidDataKeyCodec.keyFor("BondLevel", MAID);
        assertEquals("BondLevel_" + MAID, key);
        assertEquals("BondLevel", MaidDataKeyCodec.baseName(key, MAID));
    }

    @Test
    void shouldOnlyStripOneUuidSuffix() {
        String key = "BondLevel__" + MAID;
        String base = MaidDataKeyCodec.baseName(key, MAID);
        assertEquals("BondLevel_", base);
        // base 中保留的尾部下划线不会被误删，重建后仍是原键
        assertEquals(key, MaidDataKeyCodec.keyFor(base, MAID));
    }

    @Test
    void shouldRejectKeysBelongingToAnotherMaid() {
        assertNull(MaidDataKeyCodec.baseName("BondLevel_" + OTHER, MAID));
        assertNull(MaidDataKeyCodec.baseName("BondLevel_" + MAID, OTHER));
    }

    @Test
    void shouldNotBeFooledByUuidAppearingAsSubstring() {
        // UUID 出现在中间而不是结尾
        assertNull(MaidDataKeyCodec.baseName("Bond_" + MAID + "_Level", MAID));
        // 结尾有额外字符
        assertNull(MaidDataKeyCodec.baseName("BondLevel_" + MAID + "x", MAID));
        // 只有 UUID、没有 base 与下划线
        assertNull(MaidDataKeyCodec.baseName(MAID.toString(), MAID));
        // 有下划线但 base 为空
        assertNull(MaidDataKeyCodec.baseName("_" + MAID, MAID));
    }

    @Test
    void shouldRejectKeysWithoutUuidSuffix() {
        assertNull(MaidDataKeyCodec.baseName("BondLevel", MAID));
        assertNull(MaidDataKeyCodec.baseName("BondLevel_", MAID));
        assertNull(MaidDataKeyCodec.baseName("", MAID));
    }

    @Test
    void shouldMatchUuidSuffixCaseInsensitively() {
        String upper = "BondLevel_" + MAID.toString().toUpperCase();
        assertEquals("BondLevel", MaidDataKeyCodec.baseName(upper, MAID));
    }

    @Test
    void shouldRejectBlankOrNullBaseWhenBuildingKey() {
        assertNull(MaidDataKeyCodec.keyFor(null, MAID));
        assertNull(MaidDataKeyCodec.keyFor("", MAID));
        assertNull(MaidDataKeyCodec.keyFor("   ", MAID));
        assertNull(MaidDataKeyCodec.keyFor("\t\n", MAID));
        assertNull(MaidDataKeyCodec.keyFor("BondLevel", null));
    }

    @Test
    void shouldTolerateNullKeyInput() {
        assertNull(MaidDataKeyCodec.baseName(null, MAID));
        assertNull(MaidDataKeyCodec.baseName("BondLevel_" + MAID, null));
    }
}