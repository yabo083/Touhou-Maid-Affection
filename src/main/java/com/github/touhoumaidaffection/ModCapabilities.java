package com.github.touhoumaidaffection;

import com.github.touhoumaidaffection.bond.rescue.EmergencyRescueAttachment;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public final class ModCapabilities {
    public static final Capability<EmergencyRescueAttachment> EMERGENCY_RESCUE =
            CapabilityManager.get(new CapabilityToken<>() {
            });

    private ModCapabilities() {
    }
}
