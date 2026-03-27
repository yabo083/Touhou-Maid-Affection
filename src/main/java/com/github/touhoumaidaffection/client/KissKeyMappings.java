package com.github.touhoumaidaffection.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class KissKeyMappings {
    public static final String KEY_CATEGORY = "key.categories.touhou_maid_affection";
    public static final String KEY_KISS_CARRIED_MAID = "key.touhou_maid_affection.kiss_carried_maid";

    public static final KeyMapping KISS_CARRIED_MAID = new KeyMapping(
            KEY_KISS_CARRIED_MAID,
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            KEY_CATEGORY
    );

    private KissKeyMappings() {
        throw new IllegalStateException("Utility class");
    }
}
