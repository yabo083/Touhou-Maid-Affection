package com.github.touhoumaidaffection.network;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraftforge.network.NetworkEvent;

public final class ForgePayloadContext implements IPayloadContext {
    private final NetworkEvent.Context context;

    private ForgePayloadContext(NetworkEvent.Context context) {
        this.context = context;
    }

    public static ForgePayloadContext wrap(NetworkEvent.Context context) {
        return new ForgePayloadContext(context);
    }

    @Override
    public void enqueueWork(Runnable work) {
        context.enqueueWork(work);
    }

    @Override
    public Player player() {
        return context.getSender();
    }
}
