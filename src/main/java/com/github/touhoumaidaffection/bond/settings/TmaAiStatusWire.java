package com.github.touhoumaidaffection.bond.settings;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure-logic codec for the Morning Kiss AI status channel.
 *
 * <p>Mirrors {@link TmaSettingsWire}: the byte-level transport (netty {@code ByteBuf}) lives in
 * {@code network/TmaAiStatusByteBuf}, while this class owns the record structure and the
 * encode/decode round trip so it stays unit-testable without any Minecraft class on the classpath.
 *
 * <p>The status is strictly read-only server data; the only mutating operation is a cache clear
 * request ({@link ClearRequest}) that reuses exactly the same service methods as the
 * {@code /tma morning_kiss clear_ai_cache} command.
 */
public final class TmaAiStatusWire {
    /** Hard cap on the number of maids carried by one status packet. */
    public static final int MAX_MAIDS = 64;
    /** Hard cap on the number of pools carried per maid. */
    public static final int MAX_POOLS = 8;
    /** Hard cap on every free string (names, languages, pool ids) carried by the channel. */
    public static final int MAX_STRING_LENGTH = 128;

    private TmaAiStatusWire() {
    }

    /** Minimal byte sink used to keep the codec transport agnostic. */
    public interface Sink {
        void writeInt(int value);

        void writeLong(long value);

        void writeBoolean(boolean value);

        void writeString(String value);
    }

    /** Minimal byte source used to keep the codec transport agnostic. */
    public interface Source {
        int readInt();

        long readLong();

        boolean readBoolean();

        String readString();
    }

    /** Scope of a cache clear request. */
    public enum ClearScope {
        /** Every cached line of every maid (requires operator permission level 2). */
        ALL,
        /** Every cached line of one maid. */
        MAID,
        /** Every cached line of one maid and one dialogue pool. */
        POOL;

        /** @return the scope matching {@code name} (case-insensitive), or {@code null}. */
        public static ClearScope byName(String name) {
            if (name == null) {
                return null;
            }
            for (ClearScope scope : values()) {
                if (scope.name().equalsIgnoreCase(name)) {
                    return scope;
                }
            }
            return null;
        }
    }

    /** Per-pool entry counts of one maid. */
    public record PoolStatus(String pool, int totalEntries, int voiceEntries) {
        public int textOnlyEntries() {
            return Math.max(0, totalEntries - voiceEntries);
        }
    }

    /** Per-maid entry counts shown on the status tab. */
    public record MaidStatus(String maidUuid, String name, int totalEntries, int target, List<PoolStatus> pools) {
        public int textOnlyEntries() {
            return Math.max(0, totalEntries - voiceEntries());
        }

        public int voiceEntries() {
            int total = 0;
            for (PoolStatus pool : pools) {
                total += pool.voiceEntries();
            }
            return total;
        }
    }

    /**
     * Full read-only Morning Kiss AI status of the requesting player.
     *
     * <p>{@code maids} only ever contains maids owned by the receiving player; the global counters
     * ({@code totalEntries}, {@code maidCount}, ...) describe the whole server cache so an operator
     * can still see the real load.
     */
    public record Status(
            boolean morningKissEnabled,
            boolean aiDialogueEnabled,
            boolean aiTtsEnabled,
            boolean immediateFallbackEnabled,
            String globalDisplayLanguage,
            String globalVoiceLanguage,
            String aiDialogueLanguage,
            String aiVoiceLanguage,
            int cacheTargetPerPool,
            int scanIntervalTicks,
            boolean consumeOnUse,
            int totalEntries,
            int voiceEntries,
            int maidCount,
            int inFlightRequests,
            long revision,
            boolean canClear,
            List<MaidStatus> maids
    ) {
        public Status {
            maids = sanitizeMaids(maids);
        }

        public int textOnlyEntries() {
            return Math.max(0, totalEntries - voiceEntries);
        }
    }

    /** Cache clear request: {@code maidUuid}/{@code pool} are only meaningful for the matching scope. */
    public record ClearRequest(ClearScope scope, String maidUuid, String pool) {
        public ClearRequest {
            scope = scope == null ? ClearScope.ALL : scope;
            maidUuid = maidUuid == null ? "" : maidUuid;
            pool = pool == null ? "" : pool;
        }
    }

    // ---- Status codec ----

    public static void writeStatus(Sink sink, Status status) {
        sink.writeBoolean(status.morningKissEnabled());
        sink.writeBoolean(status.aiDialogueEnabled());
        sink.writeBoolean(status.aiTtsEnabled());
        sink.writeBoolean(status.immediateFallbackEnabled());
        sink.writeString(clamp(status.globalDisplayLanguage()));
        sink.writeString(clamp(status.globalVoiceLanguage()));
        sink.writeString(clamp(status.aiDialogueLanguage()));
        sink.writeString(clamp(status.aiVoiceLanguage()));
        sink.writeInt(status.cacheTargetPerPool());
        sink.writeInt(status.scanIntervalTicks());
        sink.writeBoolean(status.consumeOnUse());
        sink.writeInt(status.totalEntries());
        sink.writeInt(status.voiceEntries());
        sink.writeInt(status.maidCount());
        sink.writeInt(status.inFlightRequests());
        sink.writeLong(status.revision());
        sink.writeBoolean(status.canClear());
        List<MaidStatus> maids = sanitizeMaids(status.maids());
        sink.writeInt(maids.size());
        for (MaidStatus maid : maids) {
            sink.writeString(clamp(maid.maidUuid()));
            sink.writeString(clamp(maid.name()));
            sink.writeInt(maid.totalEntries());
            sink.writeInt(maid.target());
            sink.writeInt(maid.pools().size());
            for (PoolStatus pool : maid.pools()) {
                sink.writeString(clamp(pool.pool()));
                sink.writeInt(pool.totalEntries());
                sink.writeInt(pool.voiceEntries());
            }
        }
    }

    public static Status readStatus(Source source) {
        boolean morningKissEnabled = source.readBoolean();
        boolean aiDialogueEnabled = source.readBoolean();
        boolean aiTtsEnabled = source.readBoolean();
        boolean immediateFallbackEnabled = source.readBoolean();
        String globalDisplayLanguage = readString(source);
        String globalVoiceLanguage = readString(source);
        String aiDialogueLanguage = readString(source);
        String aiVoiceLanguage = readString(source);
        int cacheTargetPerPool = source.readInt();
        int scanIntervalTicks = source.readInt();
        boolean consumeOnUse = source.readBoolean();
        int totalEntries = source.readInt();
        int voiceEntries = source.readInt();
        int maidCount = source.readInt();
        int inFlightRequests = source.readInt();
        long revision = source.readLong();
        boolean canClear = source.readBoolean();
        int maidCountWire = clampCount(source.readInt(), MAX_MAIDS);
        List<MaidStatus> maids = new ArrayList<>(maidCountWire);
        for (int index = 0; index < maidCountWire; index++) {
            String maidUuid = readString(source);
            String name = readString(source);
            int maidTotal = source.readInt();
            int target = source.readInt();
            int poolCount = clampCount(source.readInt(), MAX_POOLS);
            List<PoolStatus> pools = new ArrayList<>(poolCount);
            for (int poolIndex = 0; poolIndex < poolCount; poolIndex++) {
                pools.add(new PoolStatus(readString(source), source.readInt(), source.readInt()));
            }
            maids.add(new MaidStatus(maidUuid, name, maidTotal, target, List.copyOf(pools)));
        }
        return new Status(
                morningKissEnabled,
                aiDialogueEnabled,
                aiTtsEnabled,
                immediateFallbackEnabled,
                globalDisplayLanguage,
                globalVoiceLanguage,
                aiDialogueLanguage,
                aiVoiceLanguage,
                cacheTargetPerPool,
                scanIntervalTicks,
                consumeOnUse,
                totalEntries,
                voiceEntries,
                maidCount,
                inFlightRequests,
                revision,
                canClear,
                List.copyOf(maids)
        );
    }

    // ---- Clear request codec ----

    public static void writeClear(Sink sink, ClearRequest request) {
        ClearRequest safe = request == null ? new ClearRequest(ClearScope.ALL, "", "") : request;
        sink.writeString(safe.scope().name());
        sink.writeString(clamp(safe.maidUuid()));
        sink.writeString(clamp(safe.pool()));
    }

    public static ClearRequest readClear(Source source) {
        ClearScope scope = ClearScope.byName(readString(source));
        String maidUuid = readString(source);
        String pool = readString(source);
        return new ClearRequest(scope, maidUuid, pool);
    }

    /** Drops {@code null} entries and truncates the maid/pool lists to the protocol limits. */
    public static List<MaidStatus> sanitizeMaids(List<MaidStatus> maids) {
        if (maids == null || maids.isEmpty()) {
            return List.of();
        }
        List<MaidStatus> result = new ArrayList<>(Math.min(maids.size(), MAX_MAIDS));
        for (MaidStatus maid : maids) {
            if (result.size() >= MAX_MAIDS) {
                break;
            }
            if (maid == null) {
                continue;
            }
            result.add(new MaidStatus(
                    clamp(maid.maidUuid()),
                    clamp(maid.name()),
                    maid.totalEntries(),
                    maid.target(),
                    sanitizePools(maid.pools())
            ));
        }
        return List.copyOf(result);
    }

    private static List<PoolStatus> sanitizePools(List<PoolStatus> pools) {
        if (pools == null || pools.isEmpty()) {
            return List.of();
        }
        List<PoolStatus> result = new ArrayList<>(Math.min(pools.size(), MAX_POOLS));
        for (PoolStatus pool : pools) {
            if (result.size() >= MAX_POOLS) {
                break;
            }
            if (pool == null) {
                continue;
            }
            result.add(new PoolStatus(clamp(pool.pool()), pool.totalEntries(), pool.voiceEntries()));
        }
        return List.copyOf(result);
    }

    private static String readString(Source source) {
        String value = source.readString();
        return value == null ? "" : clamp(value);
    }

    private static int clampCount(int count, int max) {
        return Math.max(0, Math.min(count, max));
    }

    private static String clamp(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= MAX_STRING_LENGTH ? value : value.substring(0, MAX_STRING_LENGTH);
    }
}