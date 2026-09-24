package com.github.touhoumaidaffection.compat.maidfm;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.github.touhoumaidaffection.bond.BondData;
import io.github.zgxhzhr.maidfm.spi.MaidMigrationProvider;
import io.github.zgxhzhr.maidfm.spi.MaidMigrationRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * 把 TMA 的羁绊数据（{@link BondData}，存在主人玩家 persistentData 的
 * {@code touhou_maid_affection.bond.maids.<女仆UUID>} 子树中）接入
 * MaidFileManager（女仆档案管理器）的迁移 SPI。
 *
 * <p>女仆实体自身的 NBT 由管理器负责导出/导入，本 provider 只处理挂在女仆身上的外部数据。
 * 导出/导入的附加数据格式为「base 名 → 值」的 compound（即该女仆子树本身），与旧版一致，
 * 因此旧导出文件仍可导入。
 *
 * <p>本类仅在 {@code maid_file_manager} 已加载时注册，且注册入口被软依赖守卫包裹，
 * 以避免管理器缺失时触发 SPI 类型的类加载。
 */
public class BondMaidMigrationProvider implements MaidMigrationProvider {

    private static final ResourceLocation ID =
            ResourceLocation.tryParse("touhou_maid_affection:bond");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public String getDependencyModId() {
        return TouhouMaidAffection.MOD_ID;
    }

    @Override
    public CompoundTag export(EntityMaid maid) {
        ServerPlayer owner = resolveOwner(maid);
        if (owner == null) {
            return null;
        }
        CompoundTag data = BondData.of(owner).exportMaidData(maid.getUUID());
        return data.isEmpty() ? null : data;
    }

    @Override
    public void importData(EntityMaid maid, CompoundTag data) {
        ServerPlayer owner = resolveOwner(maid);
        if (owner == null) {
            TouhouMaidAffection.LOGGER.warn(
                    "MaidFileManager import skipped: no owner online for maid {}", maid.getUUID());
            return;
        }
        BondData.of(owner).importMaidData(maid.getUUID(), data);
    }

    /** 解析女仆的主人：优先用实体上的 owner，其次按 ownerUUID 在在线玩家中查找。 */
    private static ServerPlayer resolveOwner(EntityMaid maid) {
        if (maid == null) {
            return null;
        }
        if (maid.getOwner() instanceof ServerPlayer serverPlayer) {
            return serverPlayer;
        }
        UUID ownerId = maid.getOwnerUUID();
        if (ownerId == null) {
            return null;
        }
        MinecraftServer server = maid.getServer();
        return server == null ? null : server.getPlayerList().getPlayer(ownerId);
    }

    /** 注册入口，仅在 {@code maid_file_manager} 已加载时调用。 */
    public static void register() {
        MaidMigrationRegistry.register(new BondMaidMigrationProvider());
    }
}