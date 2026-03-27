package com.github.touhoumaidaffection.mixin;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(EntityMaid.class)
public interface EntityMaidAccessorMixin {
    @Invoker(value = "m_5677_", remap = false)
    Component touhou_maid_affection$invokeGetTypeName();
}
