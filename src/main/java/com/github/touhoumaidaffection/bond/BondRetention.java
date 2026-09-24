package com.github.touhoumaidaffection.bond;

/**
 * 羁绊数据保留策略（prune）的纯逻辑判定。
 *
 * <p>女仆子树里的 {@link BondKeys#LAST_SEEN_KEY} 记录该女仆最后一次与主人同步档案的时间。
 * 缺失 {@code LastSeen} 的子树视为过旧（历史遗留数据），但只有当保留天数 {@code > 0} 时才删除；
 * {@code retentionDays <= 0} 表示彻底关闭清理，任何子树都不会被删除。
 *
 * <p>纯逻辑实现，不依赖任何 Minecraft 类型，便于单元测试。
 */
public final class BondRetention {

    /** 一天的毫秒数。 */
    public static final long MILLIS_PER_DAY = 86_400_000L;

    private BondRetention() {
    }

    /**
     * 判定某个女仆子树是否应被清理。
     *
     * @param lastSeenEpochMs 子树记录的 {@code LastSeen}；{@code <= 0} 表示缺失
     * @param nowEpochMs      当前时间（epoch millis）
     * @param retentionDays   保留天数；{@code <= 0} 表示关闭清理
     */
    public static boolean isStale(long lastSeenEpochMs, long nowEpochMs, long retentionDays) {
        if (retentionDays <= 0L) {
            return false;
        }
        if (lastSeenEpochMs <= 0L) {
            return true;
        }
        return lastSeenEpochMs < nowEpochMs - retentionDays * MILLIS_PER_DAY;
    }
}