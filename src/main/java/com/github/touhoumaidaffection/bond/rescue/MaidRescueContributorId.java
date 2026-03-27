package com.github.touhoumaidaffection.bond.rescue;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.UUID;

public final class MaidRescueContributorId {
    private static final String MAID_PERSISTENT_KEY = "touhou_maid_affection.rescue_contributor_id";
    private static final String TRANSFORM_TAG_KEY = "TouhouMaidAffectionRescueContributorId";

    private MaidRescueContributorId() {
    }

    public static String ensure(EntityMaid maid) {
        if (maid == null) {
            return "";
        }
        CompoundTag persistent = maid.getPersistentData();
        String current = persistent.getString(MAID_PERSISTENT_KEY);
        if (!current.isBlank()) {
            return current;
        }
        String generated = UUID.randomUUID().toString();
        persistent.putString(MAID_PERSISTENT_KEY, generated);
        return generated;
    }

    public static void writeToTransformTag(EntityMaid maid, CompoundTag transformTag) {
        if (transformTag == null || maid == null) {
            return;
        }
        String contributorId = ensure(maid);
        if (!contributorId.isBlank()) {
            transformTag.putString(TRANSFORM_TAG_KEY, contributorId);
        }
    }

    public static void restoreFromTransformTag(EntityMaid maid, CompoundTag transformTag) {
        if (maid == null || transformTag == null || !transformTag.contains(TRANSFORM_TAG_KEY, Tag.TAG_STRING)) {
            return;
        }
        String contributorId = transformTag.getString(TRANSFORM_TAG_KEY);
        if (!contributorId.isBlank()) {
            maid.getPersistentData().putString(MAID_PERSISTENT_KEY, contributorId);
        }
    }
}
