package com.github.touhoumaidaffection.network;

import net.minecraft.world.entity.player.Player;

public interface IPayloadContext {
    void enqueueWork(Runnable work);

    Player player();
}
