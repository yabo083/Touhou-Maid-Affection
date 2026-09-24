package com.github.touhoumaidaffection.bond.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MorningKissDataPackEntriesTest {
    private static MorningKissDataPackEntries.DialogueLine line(String text, String language) {
        return new MorningKissDataPackEntries.DialogueLine(text, language);
    }

    @Test
    void normalisesLanguageTagsAndTreatsAutoAsUnspecified() {
        assertEquals("zh_cn", MorningKissDataPackEntries.normalizeLanguage("zh-CN"));
        assertEquals("ja_jp", MorningKissDataPackEntries.normalizeLanguage(" JA_JP "));
        assertEquals("", MorningKissDataPackEntries.normalizeLanguage("tlm"));
        assertEquals("", MorningKissDataPackEntries.normalizeLanguage("auto"));
        assertEquals("", MorningKissDataPackEntries.normalizeLanguage("default"));
        assertEquals("", MorningKissDataPackEntries.normalizeLanguage(null));
    }

    @Test
    void prefersLanguageMatchThenUnmarkedThenEverything() {
        List<MorningKissDataPackEntries.DialogueLine> lines = List.of(
                line("ja", "ja_jp"),
                line("zh", "zh_cn"),
                line("unmarked", "")
        );

        assertEquals(List.of(line("zh", "zh_cn")),
                MorningKissDataPackEntries.selectByLanguage(lines, MorningKissDataPackEntries.DialogueLine::language, "zh_cn"));
        assertEquals(List.of(line("unmarked", "")),
                MorningKissDataPackEntries.selectByLanguage(lines, MorningKissDataPackEntries.DialogueLine::language, "ko_kr"));
    }

    @Test
    void fallsBackToEverythingWhenAllEntriesAreMarkedAndNoneMatch() {
        List<MorningKissDataPackEntries.DialogueLine> lines = List.of(
                line("ja", "ja_jp"),
                line("zh", "zh_cn")
        );

        assertEquals(lines,
                MorningKissDataPackEntries.selectByLanguage(lines, MorningKissDataPackEntries.DialogueLine::language, "ko_kr"));
    }

    @Test
    void autoTargetLanguageKeepsTheOriginalListOrder() {
        List<MorningKissDataPackEntries.DialogueLine> lines = List.of(
                line("ja", "ja_jp"),
                line("zh", "zh_cn"),
                line("unmarked", "")
        );

        assertEquals(lines,
                MorningKissDataPackEntries.selectByLanguage(lines, MorningKissDataPackEntries.DialogueLine::language, ""));
        assertEquals(lines,
                MorningKissDataPackEntries.selectByLanguage(lines, MorningKissDataPackEntries.DialogueLine::language, "auto"));
    }

    @Test
    void appendModeTreatsBuiltinLinesAsClientLanguage() {
        List<MorningKissDataPackEntries.DialogueLine> configured = List.of(
                line("dp_zh", "zh_cn"),
                line("dp_unmarked", "")
        );
        List<String> builtin = List.of("builtin.1", "builtin.2");

        // 显式显示语种命中数据包条目时，客户端语言的内置条目被排除
        MorningKissDataPackEntries.DialogueChoice zh = MorningKissDataPackEntries.pickDialogue(
                MorningKissDataPackEntries.buildDialogueChoices(configured, builtin, "en_us", true), "zh_cn", 0);
        assertEquals("dp_zh", zh.configured().text());

        // 显示语种与客户端语言一致时内置条目参与并优先
        MorningKissDataPackEntries.DialogueChoice en = MorningKissDataPackEntries.pickDialogue(
                MorningKissDataPackEntries.buildDialogueChoices(configured, builtin, "en_us", true), "en_us", 0);
        assertTrue(en.isBuiltin());

        // 无匹配命中时回落未标记的数据包条目，而不是语言不一致的内置条目
        MorningKissDataPackEntries.DialogueChoice fallback = MorningKissDataPackEntries.pickDialogue(
                MorningKissDataPackEntries.buildDialogueChoices(configured, builtin, "en_us", true), "ko_kr", 0);
        assertEquals("dp_unmarked", fallback.configured().text());
    }

    @Test
    void appendModeWithoutBuiltinKeepsReplaceSemantics() {
        List<MorningKissDataPackEntries.DialogueLine> configured = List.of(line("dp", "zh_cn"));

        List<MorningKissDataPackEntries.DialogueChoice> choices =
                MorningKissDataPackEntries.buildDialogueChoices(configured, List.of("builtin.1"), "en_us", false);

        assertEquals(1, choices.size());
        assertTrue(!choices.get(0).isBuiltin());
    }

    @Test
    void allUnmarkedPoolKeepsLegacyBehaviourUnderExplicitLanguage() {
        List<MorningKissDataPackEntries.DialogueLine> lines = List.of(
                line("a", ""),
                line("b", ""),
                line("c", "")
        );

        assertEquals(lines,
                MorningKissDataPackEntries.selectByLanguage(lines, MorningKissDataPackEntries.DialogueLine::language, "zh_cn"));
    }

    @Test
    void autoKeepsLegacyAppendPoolOrdering() {
        List<MorningKissDataPackEntries.DialogueLine> configured = List.of(line("a", ""), line("b", ""));
        List<String> builtin = List.of("builtin.1", "builtin.2", "builtin.3");

        List<MorningKissDataPackEntries.DialogueChoice> choices =
                MorningKissDataPackEntries.buildDialogueChoices(configured, builtin, "en_us", true);

        assertEquals(5, choices.size());
        assertEquals("a", choices.get(0).configured().text());
        assertEquals("b", choices.get(1).configured().text());
        assertTrue(choices.get(2).isBuiltin());
        assertTrue(choices.get(4).isBuiltin());
        assertEquals(choices,
                MorningKissDataPackEntries.selectByLanguage(
                        choices, MorningKissDataPackEntries.DialogueChoice::language, ""));
    }

    @Test
    void pickDialogueIsStableForEveryRoll() {
        List<MorningKissDataPackEntries.DialogueLine> configured = List.of(line("a", "zh_cn"), line("b", "zh_cn"));

        for (int roll = -5; roll < 5; roll++) {
            MorningKissDataPackEntries.DialogueChoice choice = MorningKissDataPackEntries.pickDialogue(
                    MorningKissDataPackEntries.buildDialogueChoices(configured, List.of(), "", false), "zh_cn", roll);
            assertTrue(choice.configured().text().equals("a") || choice.configured().text().equals("b"));
        }
    }

    @Test
    void pairedSubtitleRequiresMatchingTextLanguage() {
        MorningKissDataPackEntries.VoiceFile zhSubtitle =
                new MorningKissDataPackEntries.VoiceFile("jp.ogg", "ja_jp", "配对字幕", "zh_cn");
        MorningKissDataPackEntries.VoiceFile untaggedSubtitle =
                new MorningKissDataPackEntries.VoiceFile("jp.ogg", "ja_jp", "任意语言", "");
        MorningKissDataPackEntries.VoiceFile noSubtitle =
                new MorningKissDataPackEntries.VoiceFile("jp.ogg", "ja_jp", "", "");

        assertEquals("配对字幕", MorningKissDataPackEntries.pairedSubtitle(zhSubtitle, "zh_cn"));
        assertEquals("配对字幕", MorningKissDataPackEntries.pairedSubtitle(zhSubtitle, "zh-CN"));
        assertEquals("", MorningKissDataPackEntries.pairedSubtitle(zhSubtitle, "ja_jp"));
        assertEquals("配对字幕", MorningKissDataPackEntries.pairedSubtitle(zhSubtitle, "auto"));
        assertEquals("任意语言", MorningKissDataPackEntries.pairedSubtitle(untaggedSubtitle, "ja_jp"));
        assertEquals("", MorningKissDataPackEntries.pairedSubtitle(noSubtitle, "zh_cn"));
        assertEquals("", MorningKissDataPackEntries.pairedSubtitle(null, "zh_cn"));
    }
}