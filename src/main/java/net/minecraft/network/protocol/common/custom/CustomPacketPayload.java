package net.minecraft.network.protocol.common.custom;

import net.minecraft.resources.ResourceLocation;

public interface CustomPacketPayload {
    Type<? extends CustomPacketPayload> type();

    final class Type<T extends CustomPacketPayload> {
        private final ResourceLocation id;

        public Type(ResourceLocation id) {
            this.id = id;
        }

        public ResourceLocation id() {
            return id;
        }
    }
}
