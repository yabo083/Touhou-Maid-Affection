package com.github.touhoumaidaffection.bond;

public final class BondDataLimits {
    public static final int MAX_VALUE_LENGTH = 256;

    private BondDataLimits() {
    }

    public static boolean isValidValue(String value) {
        return value == null || sanitize(value).length() <= MAX_VALUE_LENGTH;
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = sanitize(value);
        return normalized.length() <= MAX_VALUE_LENGTH ? normalized : "";
    }

    private static String sanitize(String value) {
        return value.trim().replace('\r', ' ').replace('\n', ' ');
    }
}
