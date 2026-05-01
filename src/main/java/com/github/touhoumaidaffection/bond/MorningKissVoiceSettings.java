package com.github.touhoumaidaffection.bond;

import java.util.List;

public record MorningKissVoiceSettings(
        Mode mode,
        String selectedGroup,
        String selectedClip,
        String soundPackId,
        List<String> selectedVoiceIds
) {
    public static final MorningKissVoiceSettings DEFAULT = new MorningKissVoiceSettings(Mode.RANDOM_ALL, "", "", "", List.of());

    public MorningKissVoiceSettings {
        mode = mode == null ? Mode.RANDOM_ALL : mode;
        selectedGroup = normalize(selectedGroup);
        selectedClip = normalize(selectedClip);
        soundPackId = normalize(soundPackId);
        selectedVoiceIds = selectedVoiceIds == null ? List.of() : selectedVoiceIds.stream()
                .map(MorningKissVoiceSettings::normalize)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    public boolean usesGroupSelection() {
        return mode == Mode.RANDOM_GROUP;
    }

    public boolean usesClipSelection() {
        return mode == Mode.SPECIFIC_CLIP;
    }

    public MorningKissVoiceSettings withSoundPackId(String currentSoundPackId) {
        return new MorningKissVoiceSettings(mode, selectedGroup, selectedClip, currentSoundPackId, selectedVoiceIds);
    }

    public static MorningKissVoiceSettings of(String mode, String selectedGroup, String selectedClip, String soundPackId) {
        return of(mode, selectedGroup, selectedClip, soundPackId, List.of());
    }

    public static MorningKissVoiceSettings of(String mode, String selectedGroup, String selectedClip, String soundPackId, List<String> selectedVoiceIds) {
        return new MorningKissVoiceSettings(Mode.fromSerializedName(mode), selectedGroup, selectedClip, soundPackId, selectedVoiceIds);
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
                if ("specific_clip".equalsIgnoreCase(value.trim())) {
                    return RANDOM_ALL;
                }
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
