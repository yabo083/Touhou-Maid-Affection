package com.github.touhoumaidaffection.util;

import java.util.Locale;

public final class NamespacedPathNormalizer {
    private static final String YSM_DESCRIPTOR_SUFFIX = "/ysm.json";
    private static final String BUILTIN_PREFIX = "builtin/";
    private static final String TEXTURE_PREFIX = "textures/";

    private NamespacedPathNormalizer() {
    }

    public static String stripNamespace(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return "";
        }
        String normalized = rawValue.trim().toLowerCase(Locale.ROOT).replace('\\', '/');
        int namespaceSeparator = normalized.indexOf(':');
        if (namespaceSeparator < 0) {
            return normalized;
        }
        if (namespaceSeparator + 1 >= normalized.length()) {
            return "";
        }
        return normalized.substring(namespaceSeparator + 1);
    }

    public static String normalizeModelId(String modelId) {
        String normalized = stripNamespace(modelId);
        if (normalized.endsWith(YSM_DESCRIPTOR_SUFFIX)) {
            normalized = normalized.substring(0, normalized.length() - YSM_DESCRIPTOR_SUFFIX.length());
        }
        if (normalized.startsWith(BUILTIN_PREFIX)) {
            normalized = normalized.substring(BUILTIN_PREFIX.length());
        }
        return normalized;
    }

    public static String normalizeTextureId(String textureId) {
        String normalized = stripNamespace(textureId);
        if (normalized.endsWith(".png")) {
            normalized = normalized.substring(0, normalized.length() - 4);
        }
        if (normalized.startsWith(TEXTURE_PREFIX)) {
            normalized = normalized.substring(TEXTURE_PREFIX.length());
        }
        return normalized;
    }
}
