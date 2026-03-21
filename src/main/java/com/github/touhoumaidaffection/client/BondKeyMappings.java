package com.github.touhoumaidaffection.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class BondKeyMappings {
    public static final String KEY_LAP_PILLOW = "key.touhou_maid_affection.lap_pillow";

    public static final KeyMapping LAP_PILLOW = new KeyMapping(
            KEY_LAP_PILLOW,
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            KissKeyMappings.KEY_CATEGORY
    );

    private BondKeyMappings() {
        throw new IllegalStateException("Utility class");
    }
}
