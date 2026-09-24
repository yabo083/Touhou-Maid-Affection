package com.github.touhoumaidaffection.client;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.github.touhoumaidaffection.bond.settings.TmaAiStatusWire;
import com.github.touhoumaidaffection.network.TmaAiCacheClearPayload;
import com.github.touhoumaidaffection.network.TmaAiStatusPayload;
import com.github.touhoumaidaffection.network.TmaAiStatusRequestPayload;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Client side cache of the last read-only Morning Kiss AI status pushed by the server.
 *
 * <p>Nothing here is optimistic: {@link #requestSync()} only asks the server, and the UI refreshes
 * when the resulting {@link TmaAiStatusPayload} arrives through {@link #applyStatus(TmaAiStatusPayload)}.
 * Clear requests follow the same rule and simply trigger a fresh status push.
 */
public final class TmaAiStatusClientState {
    private static final List<Runnable> LISTENERS = new CopyOnWriteArrayList<>();
    private static volatile TmaAiStatusWire.Status status;

    private TmaAiStatusClientState() {
    }

    /** Asks the server for the current status without requesting any change. */
    public static void requestSync() {
        TouhouMaidAffection.CHANNEL.sendToServer(new TmaAiStatusRequestPayload());
    }

    /** Asks the server to clear the whole Morning Kiss AI cache (operator only, server enforced). */
    public static void clearAll() {
        send(new TmaAiStatusWire.ClearRequest(TmaAiStatusWire.ClearScope.ALL, "", ""));
    }

    /** Asks the server to clear every cached line of one maid (operator only, server enforced). */
    public static void clearMaid(String maidUuid) {
        send(new TmaAiStatusWire.ClearRequest(TmaAiStatusWire.ClearScope.MAID, maidUuid, ""));
    }

    /** Asks the server to clear one dialogue pool of one maid (operator only, server enforced). */
    public static void clearPool(String maidUuid, String pool) {
        send(new TmaAiStatusWire.ClearRequest(TmaAiStatusWire.ClearScope.POOL, maidUuid, pool));
    }

    private static void send(TmaAiStatusWire.ClearRequest request) {
        TouhouMaidAffection.CHANNEL.sendToServer(new TmaAiCacheClearPayload(request));
    }

    /** Replaces the cache with the authoritative status and notifies refresh listeners. */
    public static void applyStatus(TmaAiStatusPayload payload) {
        status = payload.status();
        for (Runnable listener : LISTENERS) {
            listener.run();
        }
    }

    public static boolean hasStatus() {
        return status != null;
    }

    /** @return the last pushed status, or {@code null} before the first push. */
    public static TmaAiStatusWire.Status get() {
        return status;
    }

    /** Registers a UI refresh callback invoked after every status push. */
    public static void addListener(Runnable listener) {
        if (listener != null) {
            LISTENERS.add(listener);
        }
    }

    public static void removeListener(Runnable listener) {
        LISTENERS.remove(listener);
    }
}