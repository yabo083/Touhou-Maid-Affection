package com.github.touhoumaidaffection.compat.maidfm;

import java.util.UUID;

/**
 * {@code BondData} 持久化键的编解码工具：羁绊数据以 {@code <base>_<maidUuid>} 形式
 * 存放在主人玩家的 persistentData 里，本类负责后缀匹配、剥离与重建。
 *
 * <p>纯逻辑实现，不依赖任何 Minecraft 类型，便于单元测试。
 */
public final class MaidDataKeyCodec {

    private MaidDataKeyCodec() {
    }

    /**
     * 若 {@code key} 以 {@code "_" + maidUuid} 结尾且前缀（base）非空，返回该 base；否则返回 {@code null}。
     *
     * <p>UUID 后缀按不区分大小写匹配，以容忍历史上可能写入的大写形式。
     */
    public static String baseName(String key, UUID maidUuid) {
        if (key == null || maidUuid == null) {
            return null;
        }
        String suffix = "_" + maidUuid;
        int baseLength = key.length() - suffix.length();
        if (baseLength <= 0) {
            return null;
        }
        if (!key.regionMatches(true, baseLength, suffix, 0, suffix.length())) {
            return null;
        }
        return key.substring(0, baseLength);
    }

    /**
     * 由 base 名与女仆 UUID 重建持久化键 {@code <baseName>_<maidUuid>}。
     * base 为 {@code null} 或空白时返回 {@code null}。
     */
    public static String keyFor(String baseName, UUID maidUuid) {
        if (baseName == null || maidUuid == null || baseName.isBlank()) {
            return null;
        }
        return baseName + "_" + maidUuid;
    }
}