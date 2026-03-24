package com.github.touhoumaidaffection.bond;

public record EmergencyRescueVoiceSettings(
        SourceMode sourceMode,
        TlmPlayMode tlmPlayMode,
        String tlmSelectedGroup,
        String tlmSelectedClip,
        CustomPlayMode customPlayMode,
        String fixedFile,
        boolean useCommonFallback
) {
    public static final EmergencyRescueVoiceSettings DEFAULT = new EmergencyRescueVoiceSettings(
            SourceMode.CUSTOM_FS,
            TlmPlayMode.RANDOM_ALL,
            "",
            "",
            CustomPlayMode.RANDOM,
            "",
            true
    );

    public EmergencyRescueVoiceSettings {
        sourceMode = sourceMode == null ? SourceMode.CUSTOM_FS : sourceMode;
        tlmPlayMode = tlmPlayMode == null ? TlmPlayMode.RANDOM_ALL : tlmPlayMode;
        tlmSelectedGroup = normalize(tlmSelectedGroup);
        tlmSelectedClip = normalize(tlmSelectedClip);
        customPlayMode = customPlayMode == null ? CustomPlayMode.RANDOM : customPlayMode;
        fixedFile = normalize(fixedFile);
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
        return new EmergencyRescueVoiceSettings(
                SourceMode.fromSerializedName(sourceMode),
                TlmPlayMode.fromSerializedName(tlmPlayMode),
                tlmSelectedGroup,
                tlmSelectedClip,
                CustomPlayMode.fromSerializedName(customPlayMode),
                fixedFile,
                useCommonFallback
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
            if (raw != null) {
                for (SourceMode mode : values()) {
                    if (mode.serializedName.equalsIgnoreCase(raw.trim())) {
                        return mode;
                    }
                }
            }
            return CUSTOM_FS;
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
