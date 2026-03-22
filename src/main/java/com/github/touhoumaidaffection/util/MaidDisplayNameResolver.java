package com.github.touhoumaidaffection.util;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.touhoumaidaffection.mixin.EntityMaidAccessorMixin;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class MaidDisplayNameResolver {
    private MaidDisplayNameResolver() {
    }

    public static Component resolveDisplayName(EntityMaid maid) {
        if (maid == null) {
            return Component.empty();
        }

        Component typeName = ((EntityMaidAccessorMixin) maid).touhou_maid_affection$invokeGetTypeName();
        if (hasVisibleText(typeName)) {
            return typeName.copy();
        }

        Component translated = resolveDisplayName(maid.getModelId());
        if (hasVisibleText(translated)) {
            return translated;
        }

        Component entityName = maid.getName();
        if (hasVisibleText(entityName)) {
            return entityName.copy();
        }

        return Component.literal(maid.getModelId());
    }

    public static Component resolveDisplayName(String modelId) {
        if (modelId == null || modelId.isBlank()) {
            return Component.empty();
        }

        ResourceLocation id = ResourceLocation.tryParse(modelId);
        if (id == null) {
            return Component.literal(modelId);
        }

        String translationKey = "model." + id.getNamespace() + "." + id.getPath() + ".name";
        if (Language.getInstance().has(translationKey)) {
            return Component.translatable(translationKey);
        }
        return Component.literal(modelId);
    }

    public static Component resolveDisplayName(String modelId, String fallbackDisplayName, String ysmDisplayName) {
        if (ysmDisplayName != null && !ysmDisplayName.isBlank()) {
            return Component.literal(ysmDisplayName);
        }
        if (fallbackDisplayName != null && !fallbackDisplayName.isBlank()) {
            return Component.literal(fallbackDisplayName);
        }
        return resolveDisplayName(modelId);
    }

    public static Component resolveChatSafeDisplayName(EntityMaid maid) {
        return toChatSafe(resolveDisplayName(maid));
    }

    public static Component resolveChatSafeDisplayName(String modelId, String fallbackDisplayName) {
        Component base = fallbackDisplayName != null && !fallbackDisplayName.isBlank()
                ? Component.literal(fallbackDisplayName)
                : resolveDisplayName(modelId);
        return toChatSafe(base);
    }

    public static String resolvePlainDisplayName(EntityMaid maid) {
        return resolveDisplayName(maid).getString();
    }

    public static String resolvePlainDisplayName(String modelId, String fallbackDisplayName) {
        if (fallbackDisplayName != null && !fallbackDisplayName.isBlank()) {
            return fallbackDisplayName;
        }
        return resolveDisplayName(modelId).getString();
    }

    public static String resolvePlainDisplayName(String modelId, String fallbackDisplayName, String ysmDisplayName) {
        return resolveDisplayName(modelId, fallbackDisplayName, ysmDisplayName).getString();
    }

    private static Component toChatSafe(Component component) {
        return Component.literal(component.getString().replace(" ", "\u00A0"));
    }

    private static boolean hasVisibleText(Component component) {
        return component != null && !component.getString().isBlank();
    }
}
