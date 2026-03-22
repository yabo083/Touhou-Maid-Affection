package com.github.touhoumaidaffection;

import com.github.touhoumaidaffection.bond.rescue.EmergencyRescueAttachment;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, TouhouMaidAffection.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<EmergencyRescueAttachment>> EMERGENCY_RESCUE =
            ATTACHMENTS.register("emergency_rescue",
                    () -> AttachmentType.serializable(EmergencyRescueAttachment::new)
                            .copyOnDeath()
                            .build());

    private ModAttachments() {
    }
}
