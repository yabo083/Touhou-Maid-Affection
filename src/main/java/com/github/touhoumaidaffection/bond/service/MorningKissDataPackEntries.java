package com.github.touhoumaidaffection.bond.service;

import com.github.touhoumaidaffection.bond.BondDataLimits;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 早安吻数据包 {@code morning_kiss/profile.json} 的语言化条目模型、解析与按语种选择算法。
 *
 * <p>纯逻辑类：只依赖 Gson、{@link BondDataLimits} 与 {@link MorningKissGeneratedDialogueLanguage}
 * 的语种归一化规则，不引用任何 Minecraft / NeoForge 类型，因此可在无 MC 类路径的单元测试中直接使用。
 * 解析过程中的非法条目通过传入的 {@code debugLog} 回调上报，避免直接依赖模组日志器。</p>
 */
final class MorningKissDataPackEntries {
    static final int MAX_VOICE_FILES = 64;

    private MorningKissDataPackEntries() {
    }

    /** 一条台词。{@code language} 为空串表示未标记（旧写法 / 通配）。 */
    record DialogueLine(String text, String language) {
        boolean marked() {
            return language != null && !language.isBlank();
        }
    }

    /**
     * 数据包台词或内置 i18n 台词的统一候选。
     * {@code builtinIndex} 非空表示内置条目，其语言按客户端语言参与筛选。
     */
    record DialogueChoice(DialogueLine configured, Integer builtinIndex, String language) {
        boolean isBuiltin() {
            return builtinIndex != null;
        }
    }

    /** 一个数据包语音文件。{@code language} 为空串表示未标记；{@code text}/{@code textLanguage} 为可选字幕配对。 */
    record VoiceFile(String file, String language, String text, String textLanguage) {
        static VoiceFile unmarked(String file) {
            return new VoiceFile(file, "", "", "");
        }

        boolean hasText() {
            return text != null && !text.isBlank();
        }

        boolean marked() {
            return language != null && !language.isBlank();
        }
    }

    /** 归一化语言标签，复用 AI 链路的规则：小写、{@code -} → {@code _}，{@code tlm}/{@code auto}/{@code default} 视为未指定。 */
    static String normalizeLanguage(String raw) {
        return MorningKissGeneratedDialogueLanguage.normalizeLocaleCode(raw);
    }

    static List<DialogueLine> parseDialogueList(JsonElement element, Consumer<String> debugLog) {
        if (element == null || !element.isJsonArray()) {
            return List.of();
        }
        JsonArray array = element.getAsJsonArray();
        List<DialogueLine> output = new ArrayList<>();
        int index = 0;
        for (JsonElement value : array) {
            DialogueLine line = parseDialogueLine(value, index, debugLog);
            if (line != null) {
                output.add(line);
            }
            index++;
        }
        return List.copyOf(output);
    }

    private static DialogueLine parseDialogueLine(JsonElement value, int index, Consumer<String> debugLog) {
        if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            String text = value.getAsString();
            if (text == null || text.isBlank()) {
                debug(debugLog, "Skipping blank morning kiss dialogue entry at index {}", index);
                return null;
            }
            return new DialogueLine(text.trim(), "");
        }
        if (value != null && value.isJsonObject()) {
            JsonObject object = value.getAsJsonObject();
            String text = readString(object, "text");
            if (text.isBlank()) {
                debug(debugLog, "Skipping morning kiss dialogue entry at index {}: missing text", index);
                return null;
            }
            return new DialogueLine(text, normalizeLanguage(readString(object, "language")));
        }
        debug(debugLog, "Skipping malformed morning kiss dialogue entry at index {}", index);
        return null;
    }

    static List<VoiceFile> parseVoiceFiles(JsonElement element, Consumer<String> debugLog) {
        if (element == null || !element.isJsonArray()) {
            return List.of();
        }
        JsonArray array = element.getAsJsonArray();
        LinkedHashMap<String, VoiceFile> output = new LinkedHashMap<>();
        int index = 0;
        for (JsonElement value : array) {
            VoiceFile entry = parseVoiceFile(value, index, debugLog);
            if (entry != null && output.size() < MAX_VOICE_FILES) {
                output.putIfAbsent(entry.file(), entry);
            }
            index++;
        }
        return List.copyOf(output.values());
    }

    private static VoiceFile parseVoiceFile(JsonElement value, int index, Consumer<String> debugLog) {
        if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            String file = VoiceFilePath.normalizeOgg(value.getAsString());
            if (file.isBlank()) {
                debug(debugLog, "Skipping invalid morning kiss voice file entry at index {}", index);
                return null;
            }
            return VoiceFile.unmarked(file);
        }
        if (value != null && value.isJsonObject()) {
            JsonObject object = value.getAsJsonObject();
            String file = VoiceFilePath.normalizeOgg(readString(object, "file"));
            if (file.isBlank()) {
                debug(debugLog, "Skipping morning kiss voice entry at index {}: missing or invalid file", index);
                return null;
            }
            String language = normalizeLanguage(readString(object, "language"));
            String text = BondDataLimits.normalize(readString(object, "text"));
            String textLanguage = normalizeLanguage(readString(object, "text_language"));
            return new VoiceFile(file, language, text, textLanguage);
        }
        debug(debugLog, "Skipping malformed morning kiss voice entry at index {}", index);
        return null;
    }

    /**
     * 按目标语种筛选候选条目，优先级：语言匹配 → 未标记（旧写法）→ 全部。
     * 目标语种为空（{@code auto}）时原样返回，保证与 1.7.3.0 行为一致。
     */
    static <T> List<T> selectByLanguage(List<T> items, Function<T, String> languageOf, String targetLanguage) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        String target = normalizeLanguage(targetLanguage);
        if (target.isBlank()) {
            return List.copyOf(items);
        }
        List<T> matched = new ArrayList<>();
        List<T> unmarked = new ArrayList<>();
        for (T item : items) {
            String language = normalizeLanguage(languageOf.apply(item));
            if (language.equals(target)) {
                matched.add(item);
            } else if (language.isBlank()) {
                unmarked.add(item);
            }
        }
        if (!matched.isEmpty()) {
            return List.copyOf(matched);
        }
        if (!unmarked.isEmpty()) {
            return List.copyOf(unmarked);
        }
        return List.copyOf(items);
    }

    /**
     * 构建台词候选列表：数据包台词 + （{@code append} 模式下）以客户端语言标记的内置台词。
     */
    static List<DialogueChoice> buildDialogueChoices(List<DialogueLine> configured, List<String> builtinKeys,
                                                     String clientLanguage, boolean append) {
        List<DialogueChoice> choices = new ArrayList<>();
        if (configured != null) {
            for (DialogueLine line : configured) {
                choices.add(new DialogueChoice(line, null, line.language()));
            }
        }
        if (append && builtinKeys != null) {
            String language = normalizeLanguage(clientLanguage);
            for (int index = 0; index < builtinKeys.size(); index++) {
                choices.add(new DialogueChoice(null, index, language));
            }
        }
        return List.copyOf(choices);
    }

    /** 按目标显示语种挑选台词候选；{@code roll} 用于在候选内均匀取一条。 */
    static DialogueChoice pickDialogue(List<DialogueChoice> choices, String targetLanguage, int roll) {
        List<DialogueChoice> eligible = selectByLanguage(choices, DialogueChoice::language, targetLanguage);
        return eligible.get(Math.floorMod(roll, eligible.size()));
    }

    /**
     * 返回语音文件配对的可选字幕。
     * 没有配对文本、或配对文本的 {@code text_language} 与目标显示语种不匹配时返回空串。
     */
    static String pairedSubtitle(VoiceFile entry, String targetDisplayLanguage) {
        if (entry == null || !entry.hasText()) {
            return "";
        }
        String textLanguage = entry.textLanguage();
        String target = normalizeLanguage(targetDisplayLanguage);
        if (!target.isBlank() && !textLanguage.isBlank() && !textLanguage.equals(target)) {
            return "";
        }
        return entry.text();
    }

    private static String readString(JsonObject object, String key) {
        if (object == null || !object.has(key) || !object.get(key).isJsonPrimitive()) {
            return "";
        }
        String value = object.get(key).getAsString();
        return value == null ? "" : value.trim();
    }

    private static void debug(Consumer<String> debugLog, String message, int index) {
        if (debugLog != null) {
            debugLog.accept(message.replace("{}", Integer.toString(index)));
        }
    }
}