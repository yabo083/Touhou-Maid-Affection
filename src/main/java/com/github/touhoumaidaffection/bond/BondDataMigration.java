package com.github.touhoumaidaffection.bond;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 把旧扁平布局（{@code <base>_<女仆UUID>} 直接挂在根上）一次性迁移到嵌套布局
 * （{@code maids.<女仆UUID>.<base>}）。
 *
 * <p>规则：
 * <ul>
 *   <li>只有能被 {@link BondKeys#parseFlatKey(String)} 解析的键才会迁移；玩家粒度键
 *       （无 UUID 后缀）与无法解析的键原样保留。</li>
 *   <li>先写后删：值写入女仆子树成功后才移除旧扁平键，中途失败不会丢数据。</li>
 *   <li>幂等：已迁移的键不复存在，重复执行只会处理剩余扁平键，结果一致。</li>
 * </ul>
 *
 * <p>纯逻辑实现：只依赖 {@link Sink} 抽象，不依赖任何 Minecraft 类型，
 * 生产环境由 {@code BondData} 适配到 {@code CompoundTag}，测试可直接用内存 Map。
 */
public final class BondDataMigration {

    /** 迁移目标的抽象视图，由调用方适配到具体存储。 */
    public interface Sink<V> {
        /** 当前根级所有键的快照。 */
        Set<String> rootKeys();

        /** 读取根级键对应的值；不存在时返回 {@code null}。 */
        V value(String key);

        /** 把值写入 {@code maids.<maidUuid>.<baseName>}。 */
        void writeMaidValue(UUID maidUuid, String baseName, V value);

        /** 移除已迁移的扁平键。 */
        void removeRootKey(String key);
    }

    /**
     * 迁移统计。
     *
     * @param migrated 迁移到女仆子树并移除的扁平键数量
     * @param retained 原样保留的根键数量（玩家粒度键、无法解析的键）
     */
    public record Result(int migrated, int retained) {
    }

    /**
     * 执行一次迁移。调用方负责在成功后写入新的 {@code SchemaVersion}。
     */
    public static <V> Result migrate(Sink<V> sink) {
        List<String> keys = new ArrayList<>(sink.rootKeys());
        int migrated = 0;
        for (String key : keys) {
            Optional<BondKeys.ParsedFlatKey> parsed = BondKeys.parseFlatKey(key);
            if (parsed.isEmpty()) {
                continue;
            }
            V value = sink.value(key);
            if (value == null) {
                continue;
            }
            BondKeys.ParsedFlatKey flatKey = parsed.get();
            sink.writeMaidValue(flatKey.maidUuid(), flatKey.baseName(), value);
            sink.removeRootKey(key);
            migrated++;
        }
        return new Result(migrated, keys.size() - migrated);
    }

    private BondDataMigration() {
    }
}