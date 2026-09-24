package com.github.touhoumaidaffection.bond.service;

import com.github.touhoumaidaffection.ModConfig;

/**
 * 早安吻全局显示 / 配音语种的运行时解析。
 *
 * <p>把 {@code ModConfig} 的原始字符串归一化为 {@link MorningKissGeneratedDialogueLanguage} 的 locale 规则：
 * 小写、{@code -} → {@code _}，{@code tlm}/{@code auto}/{@code default} 视为未指定（空串）。</p>
 */
final class MorningKissLanguageSettings {
    private MorningKissLanguageSettings() {
    }

    /** 全局字幕 / 文本语种；{@code auto} 时返回空串（不过滤）。 */
    static String displayLanguage() {
        return MorningKissGeneratedDialogueLanguage.normalizeLocaleCode(ModConfig.BOND_MORNING_KISS_DISPLAY_LANGUAGE.get());
    }

    /** 全局配音语种；{@code auto} 时返回空串（不过滤）。 */
    static String voiceLanguage() {
        return MorningKissGeneratedDialogueLanguage.normalizeLocaleCode(ModConfig.BOND_MORNING_KISS_VOICE_LANGUAGE.get());
    }

    /**
     * 即时 AI 台词回退（TLM chat）使用的语言。
     *
     * <p>保持旧行为：{@code aiDialogueLanguage} 显式时原样透传；否则若全局 {@code displayLanguage} 显式则使用它；
     * 两者都未指定时继续透传原始 {@code tlm}/{@code auto} 值，交由 TLM 按女仆配置解析。</p>
     */
    static String liveChatLanguage() {
        String configured = ModConfig.BOND_MORNING_KISS_AI_DIALOGUE_LANGUAGE.get();
        if (!MorningKissGeneratedDialogueLanguage.normalizeLocaleCode(configured).isBlank()) {
            return configured;
        }
        String global = displayLanguage();
        return global.isBlank() ? configured : global;
    }
}