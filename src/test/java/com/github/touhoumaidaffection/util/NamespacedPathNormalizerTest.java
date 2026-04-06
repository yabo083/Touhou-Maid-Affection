package com.github.touhoumaidaffection.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NamespacedPathNormalizerTest {
    @Test
    void shouldDropNamespaceWithoutArrayIndexingAssumptions() {
        assertEquals("maid/model", NamespacedPathNormalizer.stripNamespace("touhou:maid/model"));
        assertEquals("", NamespacedPathNormalizer.stripNamespace("touhou:"));
        assertEquals("maid/model", NamespacedPathNormalizer.stripNamespace("maid/model"));
        assertEquals("", NamespacedPathNormalizer.stripNamespace(null));
    }

    @Test
    void shouldNormalizeModelIdWithBuiltinPrefixAndDescriptorSuffix() {
        assertEquals("maid/model", NamespacedPathNormalizer.normalizeModelId("builtin:maid/model/ysm.json"));
        assertEquals("", NamespacedPathNormalizer.normalizeModelId("builtin:"));
    }

    @Test
    void shouldNormalizeTextureIdWithTexturePrefixAndPngSuffix() {
        assertEquals("maid/default", NamespacedPathNormalizer.normalizeTextureId("touhou:textures/maid/default.png"));
        assertEquals("", NamespacedPathNormalizer.normalizeTextureId("touhou:"));
    }
}
