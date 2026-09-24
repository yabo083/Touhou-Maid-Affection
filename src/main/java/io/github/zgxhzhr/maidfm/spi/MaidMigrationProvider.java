/*
 * Vendored from MaidFileManager v1.4.0 (io.github.zgxhzhr.maidfm.spi).
 * 编译期 shim：运行期由 maid_file_manager 的 jar 提供同名类。
 * 请勿修改签名；本文件已从本模组产物 jar 中排除（见 build.gradle 的 jar exclude）。
 *
 * 与上游唯一差异：{@link #isAvailable()} 的默认实现改用本加载器的 ModList 检测依赖模组，
 * 因为上游引用的 io.github.zgxhzhr.maidfm.platform.Services 并未被 vendored。
 */
package io.github.zgxhzhr.maidfm.spi;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * 女仆数据迁移 SPI 接口。
 *
 * <p>附属模组实现本接口，把挂在女仆身上、但不在女仆实体 NBT 内的数据
 * （如独立 SavedData、Capability、外部数据库，通过女仆 UUID 关联）
 * 接入 MaidFileManager 的导出/导入流程。
 *
 * <p>使用方式：在附属模组初始化时调用
 * {@link MaidMigrationRegistry#register(MaidMigrationProvider)} 注册即可。
 *
 * <p>设计约束：
 * <ul>
 *   <li>方法参数只依赖 {@link EntityMaid} 与 {@link CompoundTag}，不引入附属模组类型</li>
 *   <li>每个 provider 有唯一 {@link ResourceLocation} 标识</li>
 *   <li>导出返回 {@link CompoundTag}，导入接收 {@link CompoundTag}</li>
 *   <li>{@link #isAvailable()} 用于软依赖检测，附属模组未加载时跳过不报错</li>
 * </ul>
 */
public interface MaidMigrationProvider {

    /**
     * 唯一标识，如 {@code callresponse:npc_event}。
     * 作为 .maid 文件 extras 字段的 key。
     */
    ResourceLocation getId();

    /**
     * 依赖的模组 modId，用于软依赖检测。
     * 返回空串表示不依赖特定模组（始终可用）。
     */
    String getDependencyModId();

    /**
     * 导出：从女仆收集附属数据并序列化。
     * 数据可能在女仆 NBT、SynchedEntityData、Capability、SavedData 或外部系统，
     * 由 provider 自己负责读取与序列化。
     *
     * @param maid 待导出的女仆实体
     * @return 序列化后的 CompoundTag；无数据时返回 null
     */
    CompoundTag export(EntityMaid maid);

    /**
     * 导入：把数据写回女仆或其关联的外部系统。
     * 在女仆已 addFreshEntity 到世界之后调用，此时女仆 UUID 已确定。
     *
     * @param maid 刚导入生成的女仆实体
     * @param data 该 provider 在导出时返回的 CompoundTag
     */
    void importData(EntityMaid maid, CompoundTag data);

    /**
     * 该 provider 是否可用（依赖的模组是否已加载）。
     * 默认实现用 {@link #getDependencyModId()} 检测。
     */
    default boolean isAvailable() {
        String dep = getDependencyModId();
        return dep == null || dep.isEmpty() || net.minecraftforge.fml.ModList.get().isLoaded(dep);
    }
}