package com.github.touhoumaidaffection.command;

import com.github.touhoumaidaffection.bond.rescue.EmergencyRescueData;
import com.github.touhoumaidaffection.bond.rescue.EmergencyHealListener;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = com.github.touhoumaidaffection.TouhouMaidAffection.MOD_ID)
public final class RescueCommand {
    private RescueCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tma")
                .then(Commands.literal("rescue")
                        .executes(context -> executeSelfQuery(context.getSource()))));
    }

    private static int executeSelfQuery(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int current = EmergencyRescueData.getChargeCount(player);
        int max = EmergencyRescueData.getMaxChargeCount(player);
        long currentDay = EmergencyHealListener.getCurrentRescueDay(player);
        long lastReplenishDay = EmergencyRescueData.getLastReplenishDay(player);
        boolean refreshedToday = currentDay <= lastReplenishDay;

        source.sendSuccess(() -> Component.translatable("command.touhou_maid_affection.rescue.header"), false);
        source.sendSuccess(() -> Component.translatable("command.touhou_maid_affection.rescue.charges", current, max), false);
        source.sendSuccess(() -> Component.translatable(
                refreshedToday
                        ? "command.touhou_maid_affection.rescue.refresh.tomorrow"
                        : "command.touhou_maid_affection.rescue.refresh.pending"
        ), false);
        return current;
    }
}
