package com.github.touhoumaidaffection.bond;

import java.util.Locale;
import java.util.List;

public record EmergencyRescueVoiceSettings(
        SourceMode sourceMode,
        TlmPlayMode tlmPlayMode,
        String tlmSelectedGroup,
        String tlmSelectedClip,
        CustomPlayMode customPlayMode,
        String fixedFile,
        boolean useCommonFallback,
        List<String> selectedVoiceIds
) {
    public static final EmergencyRescueVoiceSettings DEFAULT = new EmergencyRescueVoiceSettings(
            SourceMode.TLM_PACK,
            TlmPlayMode.RANDOM_ALL,
            "",
            "",
            CustomPlayMode.RANDOM,
            "",
            true,
            List.of()
    );

    public EmergencyRescueVoiceSettings {
        sourceMode = sourceMode == null ? SourceMode.TLM_PACK : sourceMode;
        tlmPlayMode = tlmPlayMode == null ? TlmPlayMode.RANDOM_ALL : tlmPlayMode;
        tlmSelectedGroup = normalize(tlmSelectedGroup);
        tlmSelectedClip = normalize(tlmSelectedClip);
        customPlayMode = customPlayMode == null ? CustomPlayMode.RANDOM : customPlayMode;
        fixedFile = normalize(fixedFile);
        selectedVoiceIds = selectedVoiceIds == null ? List.of() : selectedVoiceIds.stream()
                .map(EmergencyRescueVoiceSettings::normalize)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    public static EmergencyRescueVoiceSettings of(
            String sourceMode,
            String tlmPlayMode,
            String tlmSelectedGroup,
            String tlmSelectedClip,
            String customPlayMode,
            String fixedFile,
            boolean useCommonFallback
    ) {
        return of(sourceMode, tlmPlayMode, tlmSelectedGroup, tlmSelectedClip, customPlayMode, fixedFile, useCommonFallback, List.of());
    }

    public static EmergencyRescueVoiceSettings of(
            String sourceMode,
            String tlmPlayMode,
            String tlmSelectedGroup,
            String tlmSelectedClip,
            String customPlayMode,
            String fixedFile,
            boolean useCommonFallback,
            List<String> selectedVoiceIds
    ) {
        return new EmergencyRescueVoiceSettings(
                SourceMode.fromSerializedName(sourceMode),
                TlmPlayMode.fromSerializedName(tlmPlayMode),
                tlmSelectedGroup,
                tlmSelectedClip,
                CustomPlayMode.fromSerializedName(customPlayMode),
                fixedFile,
                useCommonFallback,
                selectedVoiceIds
        );
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    public enum SourceMode {
        TLM_PACK("tlm_pack"),
        CUSTOM_FS("custom_fs");

        private final String serializedName;

        SourceMode(String serializedName) {
            this.serializedName = serializedName;
        }

        public String serializedName() {
            return serializedName;
        }

        public static SourceMode fromSerializedName(String raw) {
            if (raw == null) {
                return TLM_PACK;
            }
            String normalized = raw.trim().toLowerCase(Locale.ROOT);
            if (normalized.isEmpty()) {
                return TLM_PACK;
            }
            return switch (normalized) {
                case "tlm_pack", "tlm", "tlmpack", "tlm-pack", "sound_pack", "soundpack", "pack" -> TLM_PACK;
                case "custom_fs", "custom", "custom-file", "custom_file", "filesystem", "fs", "file", "local" -> TLM_PACK;
                default -> {
                    for (SourceMode mode : values()) {
                        if (mode.serializedName.equalsIgnoreCase(normalized)) {
                            yield mode;
                        }
                    }
                    yield TLM_PACK;
                }
            };
        }
    }

    public enum TlmPlayMode {
        RANDOM_ALL("random_all"),
        RANDOM_GROUP("random_group"),
        SPECIFIC_CLIP("specific_clip");

        private final String serializedName;

        TlmPlayMode(String serializedName) {
            this.serializedName = serializedName;
        }

        public String serializedName() {
            return serializedName;
        }

        public static TlmPlayMode fromSerializedName(String raw) {
            if (raw != null) {
                for (TlmPlayMode mode : values()) {
                    if (mode.serializedName.equalsIgnoreCase(raw.trim())) {
                        return mode;
                    }
                }
            }
            return RANDOM_ALL;
        }
    }

    public enum CustomPlayMode {
        RANDOM("random"),
        SEQUENTIAL("sequential"),
        FIXED("fixed");

        private final String serializedName;

        CustomPlayMode(String serializedName) {
            this.serializedName = serializedName;
        }

        public String serializedName() {
            return serializedName;
        }

        public static CustomPlayMode fromSerializedName(String raw) {
            if (raw != null) {
                if ("fixed".equalsIgnoreCase(raw.trim())) {
                    return RANDOM;
                }
                for (CustomPlayMode mode : values()) {
                    if (mode.serializedName.equalsIgnoreCase(raw.trim())) {
                        return mode;
                    }
                }
            }
            return RANDOM;
        }
    }
}
