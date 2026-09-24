package com.github.touhoumaidaffection.bond.service;

import com.github.touhoumaidaffection.ModConfig;

/**
 * 早安吻「文本语种 / 配音语种」的运行时解析。
 *
 * <p>两个配置把 {@code ModConfig} 的原始字符串归一化为 {@link MorningKissGeneratedDialogueLanguage}
 * 的 locale 规则：小写、{@code -} → {@code _}，{@code auto}/{@code tlm}/{@code inherit}/{@code default}
 * 视为未指定（空串），因此旧关键字继续被接受。</p>
 */
final class MorningKissLanguageSettings {
    private MorningKissLanguageSettings() {
    }

    /**
     * 文本语种（台词：内置数据包 + AI 生成）；{@code auto} 时返回空串。
     *
     * <p>空串表示不筛选：内置台词按客户端语言渲染，数据包未标记台词通配，AI 台词沿用女仆的
     * TLM 聊天语言（1.7.3.0 行为）。</p>
     */
    static String displayLanguage() {
        return MorningKissGeneratedDialogueLanguage.normalizeLocaleCode(ModConfig.BOND_MORNING_KISS_DISPLAY_LANGUAGE.get());
    }

    /**
     * 配音语种（语音：数据包语音 + AI 合成）；{@code auto} 时返回空串。
     *
     * <p>空串表示不筛选：数据包语音不按语言过滤，AI 合成语音沿用女仆的 TLM TTS 语言。</p>
     */
    static String voiceLanguage() {
        return MorningKissGeneratedDialogueLanguage.normalizeLocaleCode(ModConfig.BOND_MORNING_KISS_VOICE_LANGUAGE.get());
    }

    /**
     * 即时 AI 台词回退（TLM chat）使用的语言。
     *
     * <p>文本语种显式时原样透传；{@code auto} 时透传 {@code tlm}，交由 TLM 按女仆配置解析。</p>
     */
    static String liveChatLanguage() {
        String display = displayLanguage();
        return display.isBlank() ? "tlm" : display;
    }
}