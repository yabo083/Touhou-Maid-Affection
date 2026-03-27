package com.github.touhoumaidaffection.bond.rescue;

import com.github.touhoumaidaffection.ModCapabilities;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EmergencyRescueCapabilityProvider implements ICapabilitySerializable<CompoundTag> {
    public static final ResourceLocation ID = new ResourceLocation(TouhouMaidAffection.MOD_ID, "emergency_rescue");

    private final EmergencyRescueAttachment data = new EmergencyRescueAttachment();
    private final LazyOptional<EmergencyRescueAttachment> optional = LazyOptional.of(() -> data);

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ModCapabilities.EMERGENCY_RESCUE) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return data.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        data.deserializeNBT(nbt == null ? new CompoundTag() : nbt);
    }

    public void invalidate() {
        optional.invalidate();
    }
}
