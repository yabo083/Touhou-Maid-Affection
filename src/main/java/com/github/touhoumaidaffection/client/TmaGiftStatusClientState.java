package com.github.touhoumaidaffection.client;

import com.github.touhoumaidaffection.bond.settings.TmaGiftStatusViewRules;
import com.github.touhoumaidaffection.bond.settings.TmaGiftStatusSelection;
import com.github.touhoumaidaffection.bond.settings.TmaGiftStatusWire;
import com.github.touhoumaidaffection.client.screen.TmaSettingsScreen;
import com.github.touhoumaidaffection.network.TmaGiftStatusPayload;
import com.github.touhoumaidaffection.network.TmaGiftStatusRequestPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;

/** Screen-owned snapshot: never reused across screen openings, worlds, or players. */
public final class TmaGiftStatusClientState {
    private final Object connection;
    private final Object level;
    private final Object player;
    private final TmaGiftStatusSelection selection = new TmaGiftStatusSelection();
    private long requestedNanos;

    public TmaGiftStatusClientState() {
        Minecraft mc = Minecraft.getInstance();
        connection = mc.getConnection();
        level = mc.level;
        player = mc.player;
    }

    public boolean valid() {
        Minecraft mc = Minecraft.getInstance();
        return connection != null && connection == mc.getConnection() && level != null
                && level == mc.level && player != null && player == mc.player;
    }

    public void refresh() {
        if (!valid() || selection().loading()) return;
        selection.refresh();
        requestPage(0);
    }

    private void requestPage(int page) {
        requestedNanos = System.nanoTime();
        PacketDistributor.sendToServer(new TmaGiftStatusRequestPayload(page));
    }

    public TmaGiftStatusSelection selection() {
        if (!valid()) {
            if (selection.loading() || selection.complete() || !selection.options().isEmpty()
                    || selection.selectedUuid() != null) selection.clear();
        } else if (selection.loading()
                && TmaGiftStatusViewRules.elapsedMillis(requestedNanos, System.nanoTime()) >= 10_000L) {
            selection.fail();
        }
        return selection;
    }

    public static void applyStatus(TmaGiftStatusPayload payload, Object sourceConnection) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof TmaSettingsScreen screen) {
            screen.acceptGiftStatus(payload.status(), sourceConnection);
        }
    }

    public void accept(TmaGiftStatusWire.Status received, Object sourceConnection) {
        if (!valid() || sourceConnection != connection || !selection().loading()) return;
        int nextPage = selection.accept(received, System.nanoTime());
        if (nextPage >= 0) requestPage(nextPage);
    }
}
