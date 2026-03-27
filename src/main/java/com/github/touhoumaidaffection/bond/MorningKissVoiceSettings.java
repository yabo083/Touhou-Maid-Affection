package com.github.touhoumaidaffection.bond;

public record MorningKissVoiceSettings(
        Mode mode,
        String selectedGroup,
        String selectedClip,
        String soundPackId
) {
    public static final MorningKissVoiceSettings DEFAULT = new MorningKissVoiceSettings(Mode.RANDOM_ALL, "", "", "");

    public MorningKissVoiceSettings {
        mode = mode == null ? Mode.RANDOM_ALL : mode;
        selectedGroup = normalize(selectedGroup);
        selectedClip = normalize(selectedClip);
        soundPackId = normalize(soundPackId);
    }

    public boolean usesGroupSelection() {
        return mode == Mode.RANDOM_GROUP;
    }

    public boolean usesClipSelection() {
        return mode == Mode.SPECIFIC_CLIP;
    }

    public MorningKissVoiceSettings withSoundPackId(String currentSoundPackId) {
        return new MorningKissVoiceSettings(mode, selectedGroup, selectedClip, currentSoundPackId);
    }

    public static MorningKissVoiceSettings of(String mode, String selectedGroup, String selectedClip, String soundPackId) {
        return new MorningKissVoiceSettings(Mode.fromSerializedName(mode), selectedGroup, selectedClip, soundPackId);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    public enum Mode {
        RANDOM_ALL("random_all"),
        RANDOM_GROUP("random_group"),
        SPECIFIC_CLIP("specific_clip");

        private final String serializedName;

        Mode(String serializedName) {
            this.serializedName = serializedName;
        }

        public String serializedName() {
            return serializedName;
        }

        public static Mode fromSerializedName(String value) {
            if (value != null) {
                for (Mode mode : values()) {
                    if (mode.serializedName.equalsIgnoreCase(value.trim())) {
                        return mode;
                    }
                }
            }
            return RANDOM_ALL;
        }
    }
}
