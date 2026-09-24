/*
 * Vendored from MaidFileManager v1.4.0 (io.github.zgxhzhr.maidfm.spi).
 * 编译期 shim：运行期由 maid_file_manager 的 jar 提供同名类。
 * 请勿修改签名；本文件已从本模组产物 jar 中排除（见 build.gradle 的 jar exclude）。
 */
package io.github.zgxhzhr.maidfm.spi;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 女仆迁移 provider 注册表。
 *
 * <p>附属模组在自己的初始化代码中调用
 * {@link #register(MaidMigrationProvider)} 即可接入，一行代码完成注册。
 */
public final class MaidMigrationRegistry {

    private static final List<MaidMigrationProvider> PROVIDERS = new CopyOnWriteArrayList<>();

    private MaidMigrationRegistry() {
    }

    /**
     * 注册一个迁移 provider。重复 id 自动去重。
     *
     * @param provider 要注册的 provider
     */
    public static void register(MaidMigrationProvider provider) {
        if (provider == null || provider.getId() == null) {
            return;
        }
        for (MaidMigrationProvider p : PROVIDERS) {
            if (p.getId().equals(provider.getId())) {
                return;
            }
        }
        PROVIDERS.add(provider);
    }

    /** 返回所有已注册且当前可用的 provider（导出时遍历）。 */
    public static List<MaidMigrationProvider> getAvailable() {
        return PROVIDERS.stream()
                .filter(MaidMigrationProvider::isAvailable)
                .toList();
    }

    /** 按 id 查找 provider（导入时按 id 匹配），找不到返回 null。 */
    public static MaidMigrationProvider get(ResourceLocation id) {
        if (id == null) {
            return null;
        }
        for (MaidMigrationProvider p : PROVIDERS) {
            if (p.getId().equals(id)) {
                return p;
            }
        }
        return null;
    }
}