package com.github.touhoumaidaffection.bond;

import java.util.List;

public final class VoicePoolIds {
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
        if (ids == null || ids.isEmpty()) {
            return "";
        }
        return String.join("\n", ids.stream()
                .map(VoicePoolIds::safe)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList());
    }

    public static List<String> decode(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return raw.lines()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim().replace('\n', ' ');
    }
}
