package com.github.touhoumaidaffection;

import com.github.touhoumaidaffection.bond.lap.LapPillowAnchorEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

public final class ModEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, TouhouMaidAffection.MOD_ID);

    public static final RegistryObject<EntityType<LapPillowAnchorEntity>> LAP_PILLOW_ANCHOR =
            ENTITY_TYPES.register("lap_pillow_anchor",
                    () -> EntityType.Builder.<LapPillowAnchorEntity>of(LapPillowAnchorEntity::new, MobCategory.MISC)
                            .sized(0.2F, 0.2F)
                            .clientTrackingRange(8)
                            .updateInterval(1)
                            .build("lap_pillow_anchor"));

    private ModEntityTypes() {
    }
}
