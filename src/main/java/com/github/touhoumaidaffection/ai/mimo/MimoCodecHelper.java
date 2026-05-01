package com.github.touhoumaidaffection.ai.mimo;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

final class MimoCodecHelper {
    static final Codec<Map<String, String>> STRING_MAP_CODEC = Codec.unboundedMap(Codec.STRING, Codec.STRING)
            .xmap(LinkedHashMap::new, map -> map);

    private MimoCodecHelper() {
    }

    static ResourceLocation icon(String path) {
        return new ResourceLocation("touhou_maid_affection", path);
    }
}
