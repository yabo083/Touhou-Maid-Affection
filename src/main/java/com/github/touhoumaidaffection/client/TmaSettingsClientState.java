package com.github.touhoumaidaffection.client;

import com.github.touhoumaidaffection.bond.settings.TmaSettingsKeys;
import com.github.touhoumaidaffection.bond.settings.TmaSettingsWire;
import com.github.touhoumaidaffection.network.TmaSettingsRequestPayload;
import com.github.touhoumaidaffection.network.TmaSettingsStatePayload;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Client side cache of the last authoritative settings state pushed by the server.
 *
 * <p>Server-authoritative values are never written optimistically: {@link #set(String, String)}
 * only asks the server, and the UI refreshes when the resulting state push arrives through
 * {@link #applyState(TmaSettingsStatePayload)}.
 */
public final class TmaSettingsClientState {
    private static final Map<String, String> VALUES = new ConcurrentHashMap<>();
    private static final List<Runnable> LISTENERS = new CopyOnWriteArrayList<>();
    private static volatile boolean canEdit;
    private static volatile boolean received;

    private TmaSettingsClientState() {
    }

    /** Asks the server for the current state without requesting any change. */
    public static void requestSync() {
        PacketDistributor.sendToServer(new TmaSettingsRequestPayload(List.of()));
    }

    /** Asks the server to change one whitelisted key. The cached value updates on the state push. */
    public static void set(String key, String value) {
        PacketDistributor.sendToServer(new TmaSettingsRequestPayload(List.of(new TmaSettingsWire.Entry(key, value))));
    }

    /** Replaces the cache with a full authoritative state and notifies refresh listeners. */
    public static void applyState(TmaSettingsStatePayload payload) {
        VALUES.clear();
        for (TmaSettingsWire.Entry entry : payload.entries()) {
            VALUES.put(entry.key(), entry.value());
        }
        canEdit = payload.canEdit();
        received = true;
        for (Runnable listener : LISTENERS) {
            listener.run();
        }
    }

    public static boolean hasState() {
        return received;
    }

    /** Whether the local player may change server-authoritative settings (operator level 2). */
    public static boolean canEdit() {
        return canEdit;
    }

    public static String getValue(String key) {
        String value = VALUES.get(key);
        return value == null ? "" : value;
    }

    public static boolean getBoolean(String key, boolean fallback) {
        return TmaSettingsKeys.parseBoolean(VALUES.get(key)).orElse(fallback);
    }

    /** Registers a UI refresh callback invoked after every state push. */
    public static void addListener(Runnable listener) {
        if (listener != null) {
            LISTENERS.add(listener);
        }
    }

    public static void removeListener(Runnable listener) {
        LISTENERS.remove(listener);
    }
}