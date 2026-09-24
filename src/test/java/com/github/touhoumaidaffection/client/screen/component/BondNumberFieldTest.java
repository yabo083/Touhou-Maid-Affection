package com.github.touhoumaidaffection.client.screen.component;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Value grammar of {@link BondNumberField}: the field only accepts digits and the value it commits
 * is always clamped into the range of the backing config entry, so the client can never send an
 * out-of-range number (the server rejects one anyway).
 */
class BondNumberFieldTest {
    @Test
    void digitsOnlyFilterAllowsAnEmptyInProgressEdit() {
        assertTrue(BondNumberField.isDigitsOnly(""));
        assertTrue(BondNumberField.isDigitsOnly("1200"));
        assertFalse(BondNumberField.isDigitsOnly("12a"));
        assertFalse(BondNumberField.isDigitsOnly("-1"));
        assertFalse(BondNumberField.isDigitsOnly("1 2"));
        assertFalse(BondNumberField.isDigitsOnly(null));
    }

    @Test
    void parseClampedAcceptsDigitsInsideTheRange() {
        assertEquals(OptionalInt.of(1), BondNumberField.parseClamped("1", 1, 8));
        assertEquals(OptionalInt.of(8), BondNumberField.parseClamped("8", 1, 8));
        assertEquals(OptionalInt.of(4), BondNumberField.parseClamped(" 4 ", 1, 8));
        assertEquals(OptionalInt.of(1200), BondNumberField.parseClamped("1200", 20, 72000));
    }

    @Test
    void parseClampedClampsValuesOutsideTheRange() {
        assertEquals(OptionalInt.of(1), BondNumberField.parseClamped("0", 1, 8));
        assertEquals(OptionalInt.of(8), BondNumberField.parseClamped("99", 1, 8));
        assertEquals(OptionalInt.of(20), BondNumberField.parseClamped("19", 20, 72000));
        assertEquals(OptionalInt.of(72000), BondNumberField.parseClamped("72001", 20, 72000));
        assertEquals(OptionalInt.of(72000), BondNumberField.parseClamped("99999999999999999999", 20, 72000));
    }

    @Test
    void parseClampedRejectsNonNumericText() {
        for (String raw : List.of("", " ", "-1", "+1", "1.0", "1e3", "abc", "12a")) {
            assertTrue(BondNumberField.parseClamped(raw, 1, 8).isEmpty(), raw);
        }
        assertTrue(BondNumberField.parseClamped(null, 1, 8).isEmpty());
    }
}