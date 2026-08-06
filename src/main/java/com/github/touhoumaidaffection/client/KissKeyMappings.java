package com.github.touhoumaidaffection.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

public final class KissKeyMappings {
    public static final String KEY_CATEGORY = "key.categories.touhou_maid_affection";
    public static final String KEY_KISS_MAID = "key.touhou_maid_affection.kiss";

    public static final KeyMapping KISS_MAID = new KeyMapping(
            KEY_KISS_MAID,
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            TmaKeyDefaults.KISS_MAID,
            KEY_CATEGORY
    );

    private KissKeyMappings() {
        throw new IllegalStateException("Utility class");
    }
}
