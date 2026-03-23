package com.github.touhoumaidaffection.client;

import com.github.touhoumaidaffection.bond.lap.LapPillowAnchorEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public final class LapPillowAnchorRenderer extends EntityRenderer<LapPillowAnchorEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    public LapPillowAnchorRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(LapPillowAnchorEntity entity) {
        return TEXTURE;
    }
}
