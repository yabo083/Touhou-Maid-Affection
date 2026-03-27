package net.neoforged.neoforge.network.handling;

import net.minecraft.world.entity.player.Player;

public interface IPayloadContext {
    void enqueueWork(Runnable work);

    Player player();
}
