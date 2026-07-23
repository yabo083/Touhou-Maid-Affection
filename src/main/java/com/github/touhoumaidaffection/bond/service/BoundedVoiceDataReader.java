package com.github.touhoumaidaffection.bond.service;

import java.io.IOException;
import java.io.InputStream;

final class BoundedVoiceDataReader {
    private BoundedVoiceDataReader() {
    }

    static ReadResult read(InputStream input, int maxBytes) throws IOException {
        if (input == null || maxBytes < 1) {
            return new ReadResult(new byte[0], false);
        }
        byte[] data = input.readNBytes(maxBytes + 1);
        if (data.length > maxBytes) {
            return new ReadResult(new byte[0], true);
        }
        return new ReadResult(data, false);
    }

    record ReadResult(byte[] data, boolean exceededLimit) {
    }
}
