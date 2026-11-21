package com.github.touhoumaidaffection.compat;

import net.minecraft.world.entity.player.Player;

public interface IPayloadContext {
    void enqueueWork(Runnable work);

    Player player();
}
