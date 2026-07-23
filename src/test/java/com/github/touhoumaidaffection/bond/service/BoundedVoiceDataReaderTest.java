package com.github.touhoumaidaffection.bond.service;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoundedVoiceDataReaderTest {
    @Test
    void acceptsDataAtLimitAndRejectsDataBeyondLimit() throws Exception {
        assertArrayEquals(new byte[] {1, 2, 3, 4},
                BoundedVoiceDataReader.read(new ByteArrayInputStream(new byte[] {1, 2, 3, 4}), 4).data());
        BoundedVoiceDataReader.ReadResult oversized =
                BoundedVoiceDataReader.read(new ByteArrayInputStream(new byte[] {1, 2, 3, 4, 5}), 4);
        assertEquals(0, oversized.data().length);
        assertTrue(oversized.exceededLimit());
    }
}
