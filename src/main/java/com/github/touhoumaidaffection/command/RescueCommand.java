package com.github.touhoumaidaffection.command;

import com.github.touhoumaidaffection.ModConfig;
import com.github.touhoumaidaffection.bond.BondData;
import com.github.touhoumaidaffection.bond.rescue.EmergencyRescueData;
import com.github.touhoumaidaffection.bond.rescue.EmergencyRescueService;
import com.github.touhoumaidaffection.bond.rescue.RescueSoundSyncService;
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
                        .executes(context -> executeSelfQuery(context.getSource()))
                        .then(Commands.literal("on")
                                .executes(context -> setEnabled(context.getSource(), true)))
                        .then(Commands.literal("off")
                                .executes(context -> setEnabled(context.getSource(), false)))
                        .then(Commands.literal("toggle")
                                .executes(context -> toggleEnabled(context.getSource())))
                        .then(Commands.literal("clear")
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> clearPoolAndResetUnlock(context.getSource())))
                        .then(Commands.literal("reset")
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> clearPoolAndResetUnlock(context.getSource())))
                        .then(Commands.literal("sound")
                                .then(Commands.literal("sync")
                                        .requires(source -> source.hasPermission(2))
                                        .executes(context -> forceSoundSync(context.getSource())))
                                .then(Commands.literal("reload")
                                        .executes(context -> reloadClientSoundConfig(context.getSource()))))));
    }

    private static int executeSelfQuery(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        EmergencyRescueService.refreshChargesIfNeeded(player);

        int current = EmergencyRescueData.getChargeCount(player);
        int max = EmergencyRescueData.getMaxChargeCount(player);
        long currentDay = EmergencyRescueService.getCurrentRescueDay(player);
        long lastReplenishDay = EmergencyRescueData.getLastReplenishDay(player);
        boolean refreshedToday = currentDay <= lastReplenishDay;
        boolean globalEnabled = ModConfig.BOND_EMERGENCY_RESCUE_ENABLED.get();
        boolean playerEnabled = EmergencyRescueData.isRescueEnabled(player);
        boolean effectiveEnabled = globalEnabled && playerEnabled;

        source.sendSuccess(() -> Component.translatable("command.touhou_maid_affection.rescue.header"), false);
        source.sendSuccess(() -> Component.translatable("command.touhou_maid_affection.rescue.charges", current, max), false);
        source.sendSuccess(() -> Component.translatable(
                globalEnabled
                        ? "command.touhou_maid_affection.rescue.global.on"
                        : "command.touhou_maid_affection.rescue.global.off"
        ), false);
        source.sendSuccess(() -> Component.translatable(
                playerEnabled
                        ? "command.touhou_maid_affection.rescue.personal.on"
                        : "command.touhou_maid_affection.rescue.personal.off"
        ), false);
        source.sendSuccess(() -> Component.translatable(
                effectiveEnabled
                        ? "command.touhou_maid_affection.rescue.effective.on"
                        : "command.touhou_maid_affection.rescue.effective.off"
        ), false);
        source.sendSuccess(() -> Component.translatable(
                refreshedToday
                        ? "command.touhou_maid_affection.rescue.refresh.tomorrow"
                        : "command.touhou_maid_affection.rescue.refresh.pending"
        ), false);
        return current;
    }

    private static int setEnabled(CommandSourceStack source, boolean enabled) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        EmergencyRescueData.setRescueEnabled(player, enabled);
        source.sendSuccess(() -> Component.translatable(
                enabled
                        ? "command.touhou_maid_affection.rescue.personal.set_on"
                        : "command.touhou_maid_affection.rescue.personal.set_off"
        ), false);
        return enabled ? 1 : 0;
    }

    private static int toggleEnabled(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        boolean next = !EmergencyRescueData.isRescueEnabled(player);
        EmergencyRescueData.setRescueEnabled(player, next);
        source.sendSuccess(() -> Component.translatable(
                next
                        ? "command.touhou_maid_affection.rescue.personal.set_on"
                        : "command.touhou_maid_affection.rescue.personal.set_off"
        ), false);
        return next ? 1 : 0;
    }

    private static int clearPoolAndResetUnlock(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        BondData bondData = BondData.of(player);
        int resetCount = bondData.resetAbilityForAllMaids("emergency_heal");
        EmergencyRescueData.clearPoolAndRegistration(player);
        source.sendSuccess(
                () -> Component.translatable("command.touhou_maid_affection.rescue.clear.done", resetCount),
                true
        );
        return resetCount;
    }

    private static int forceSoundSync(CommandSourceStack source) {
        if (source.getServer() == null) {
            return 0;
        }
        RescueSoundSyncService.forceResync(source.getServer());
        source.sendSuccess(() -> Component.translatable("command.touhou_maid_affection.rescue.sound.sync"), true);
        return 1;
    }

    private static int reloadClientSoundConfig(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        RescueSoundSyncService.requestClientReload(player, "command_reload");
        source.sendSuccess(() -> Component.translatable("command.touhou_maid_affection.rescue.sound.reload"), false);
        return 1;
    }
}

