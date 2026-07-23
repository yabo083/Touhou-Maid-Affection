package com.github.touhoumaidaffection.bond;

import java.util.List;

public final class VoicePoolIds {
    private static final int MAX_ENCODED_SELECTION_BYTES = 60_000;
    public static final String BUILTIN_MORNING_KISS = "builtin:morning_kiss";
    public static final String TLM_PREFIX = "tlm:";
    public static final String DATA_PACK_PREFIX = "datapack:";

    private VoicePoolIds() {
    }

    public static String tlm(String clipKey) {
        return TLM_PREFIX + safe(clipKey);
    }

    public static String dataPack(String fileName) {
        return DATA_PACK_PREFIX + safe(fileName);
    }

    public static boolean isTlm(String id) {
        return id != null && id.startsWith(TLM_PREFIX);
    }

    public static boolean isDataPack(String id) {
        return id != null && id.startsWith(DATA_PACK_PREFIX);
    }

    public static String value(String id) {
        if (id == null) {
            return "";
        }
        if (id.startsWith(TLM_PREFIX)) {
            return id.substring(TLM_PREFIX.length());
        }
        if (id.startsWith(DATA_PACK_PREFIX)) {
            return id.substring(DATA_PACK_PREFIX.length());
        }
        return id;
    }

    public static String encode(List<String> ids) {
        return String.join("\n", normalizeSelection(ids));
    }

    public static List<String> decode(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return normalizeSelection(raw.lines().toList());
    }

    static List<String> normalizeSelection(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream()
                .map(VoicePoolIds::safe)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    public static boolean isPersistableSelection(List<String> ids) {
        List<String> normalized = normalizeSelection(ids);
        int encodedBytes = 0;
        for (int index = 0; index < normalized.size(); index++) {
            String id = normalized.get(index);
            if (!BondDataLimits.isValidValue(id)) {
                return false;
            }
            if (index > 0) {
                encodedBytes++;
            }
            encodedBytes += modifiedUtf8Length(id);
            if (encodedBytes > MAX_ENCODED_SELECTION_BYTES) {
                return false;
            }
        }
        return true;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim().replace('\r', ' ').replace('\n', ' ');
    }

    private static int modifiedUtf8Length(String value) {
        int length = 0;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character >= 0x0001 && character <= 0x007F) {
                length++;
            } else if (character <= 0x07FF) {
                length += 2;
            } else {
                length += 3;
            }
        }
        return length;
    }
}
