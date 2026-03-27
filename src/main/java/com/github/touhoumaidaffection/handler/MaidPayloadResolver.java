package com.github.touhoumaidaffection.handler;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

final class MaidPayloadResolver {
    private MaidPayloadResolver() {
    }

    static EntityMaid resolveMaid(ServerPlayer player, UUID maidUuid) {
        if (player == null || maidUuid == null) {
            return null;
        }
        Entity entity = player.serverLevel().getEntity(maidUuid);
        if (!(entity instanceof EntityMaid maid) || !maid.isAlive()) {
            return null;
        }
        return maid;
    }

    static EntityMaid resolveOwnedMaid(ServerPlayer player, UUID maidUuid) {
        EntityMaid maid = resolveMaid(player, maidUuid);
        if (maid == null || !maid.isOwnedBy(player)) {
            return null;
        }
        return maid;
    }
}
