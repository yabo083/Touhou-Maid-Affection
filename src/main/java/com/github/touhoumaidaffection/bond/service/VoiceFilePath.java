package com.github.touhoumaidaffection.bond.service;

import com.github.touhoumaidaffection.bond.BondDataLimits;
import com.github.touhoumaidaffection.bond.VoicePoolIds;

import java.util.regex.Pattern;

final class VoiceFilePath {
    private static final Pattern VALID_OGG_PATH = Pattern.compile("[a-z0-9_./-]+\\.ogg");

    private VoiceFilePath() {
    }

    static String normalizeOgg(String raw) {
        if (raw == null) {
            return "";
        }
        String path = raw.trim();
        if (path.isBlank()
                || path.startsWith("/")
                || path.contains("\\")
                || path.contains("..")
                || path.length() > BondDataLimits.MAX_VALUE_LENGTH - VoicePoolIds.DATA_PACK_PREFIX.length()
                || !VALID_OGG_PATH.matcher(path).matches()) {
            return "";
        }
        return path;
    }
}
