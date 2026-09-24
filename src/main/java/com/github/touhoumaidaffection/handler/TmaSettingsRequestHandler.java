package com.github.touhoumaidaffection.handler;

import com.github.touhoumaidaffection.ModConfig;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.github.touhoumaidaffection.bond.settings.TmaSettingsKeys;
import com.github.touhoumaidaffection.bond.settings.TmaSettingsResolver;
import com.github.touhoumaidaffection.bond.settings.TmaSettingsWire;
import com.github.touhoumaidaffection.network.TmaSettingsRequestPayload;
import com.github.touhoumaidaffection.network.TmaSettingsStatePayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;
import java.util.Optional;

/**
 * Server side of the settings channel.
 *
 * <p>Permission model: every player may read the effective state, only operators (permission level
 * {@value #REQUIRED_PERMISSION_LEVEL}) may change it. Updates are validated as a whole packet and
 * applied all-or-nothing so the client can never observe a half-applied configuration.
 */
public final class TmaSettingsRequestHandler {
    /** Matches the vanilla operator level used by the rescue commands. */
    private static final int REQUIRED_PERMISSION_LEVEL = 2;

    private TmaSettingsRequestHandler() {
    }

    public static void handle(TmaSettingsRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            boolean canEdit = player.hasPermissions(REQUIRED_PERMISSION_LEVEL);
            List<TmaSettingsWire.Entry> requested = payload.entries();
            if (!requested.isEmpty()) {
                if (!canEdit) {
                    TouhouMaidAffection.LOGGER.warn(
                            "[TMA Settings] Rejected settings update from player={} (requires permission level {})",
                            player.getScoreboardName(),
                            REQUIRED_PERMISSION_LEVEL
                    );
                } else {
                    apply(player, requested);
                }
            }
            sendState(player, canEdit);
        });
    }

    private static void apply(ServerPlayer player, List<TmaSettingsWire.Entry> requested) {
        Optional<List<TmaSettingsWire.Entry>> normalized = TmaSettingsKeys.normalizeAll(requested);
        if (normalized.isEmpty()) {
            TouhouMaidAffection.LOGGER.warn(
                    "[TMA Settings] Rejected settings update from player={} entries={} (unknown key or invalid value; the whole packet was dropped)",
                    player.getScoreboardName(),
                    requested
            );
            return;
        }

        for (TmaSettingsWire.Entry entry : normalized.get()) {
            String oldValue = TmaSettingsResolver.read(entry.key());
            TmaSettingsResolver.write(entry.key(), entry.value());
            TouhouMaidAffection.LOGGER.info(
                    "[TMA Settings] player={} key={} old={} new={}",
                    player.getScoreboardName(),
                    entry.key(),
                    oldValue,
                    entry.value()
            );
        }
        ModConfig.SPEC.save();
    }

    private static void sendState(ServerPlayer player, boolean canEdit) {
        PacketDistributor.sendToPlayer(player, new TmaSettingsStatePayload(TmaSettingsResolver.snapshot(), canEdit));
    }
}