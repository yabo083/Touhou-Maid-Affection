package com.github.touhoumaidaffection.bond;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class VoicePoolSelection {
    private VoicePoolSelection() {
    }

    public static boolean shouldIncludeBasePool(String voiceMode, List<String> dataPackVoiceFiles) {
        return !"replace".equalsIgnoreCase(safe(voiceMode)) || dataPackVoiceFiles == null || dataPackVoiceFiles.isEmpty();
    }

    public static List<String> initialSelection(List<String> savedIds, List<String> defaultIds, List<String> availableIds) {
        List<String> source = savedIds == null || savedIds.isEmpty() ? defaultIds : savedIds;
        List<String> selected = retainAvailable(source, availableIds);
        if (selected.isEmpty() && savedIds != null && !savedIds.isEmpty()) {
            selected = retainAvailable(defaultIds, availableIds);
        }
        return selected;
    }

    public static List<String> retainAvailable(List<String> selectedIds, List<String> availableIds) {
        if (selectedIds == null || selectedIds.isEmpty() || availableIds == null || availableIds.isEmpty()) {
            return List.of();
        }
        Set<String> available = new LinkedHashSet<>(availableIds);
        return selectedIds.stream()
                .filter(available::contains)
                .distinct()
                .toList();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
