package com.github.touhoumaidaffection.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

public final class BondKeyMappings {
    public static final String KEY_LAP_PILLOW = "key.touhou_maid_affection.lap_pillow";
    public static final String KEY_LAP_PILLOW_ANGLE_LOCK = "key.touhou_maid_affection.lap_pillow_angle_lock";
    public static final String KEY_VOICE_PREVIEW = "key.touhou_maid_affection.voice_preview";

    public static final KeyMapping LAP_PILLOW = new KeyMapping(
            KEY_LAP_PILLOW,
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            TmaKeyDefaults.LAP_PILLOW,
            KissKeyMappings.KEY_CATEGORY
    );

    public static final KeyMapping LAP_PILLOW_ANGLE_LOCK = new KeyMapping(
            KEY_LAP_PILLOW_ANGLE_LOCK,
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            TmaKeyDefaults.LAP_PILLOW_ANGLE_LOCK,
            KissKeyMappings.KEY_CATEGORY
    );

    public static final KeyMapping VOICE_PREVIEW = new KeyMapping(
            KEY_VOICE_PREVIEW,
            KeyConflictContext.GUI,
            InputConstants.Type.MOUSE,
            TmaKeyDefaults.VOICE_PREVIEW,
            KissKeyMappings.KEY_CATEGORY
    );

    private BondKeyMappings() {
        throw new IllegalStateException("Utility class");
    }
}
