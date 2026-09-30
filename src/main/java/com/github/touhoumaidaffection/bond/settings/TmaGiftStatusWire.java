package com.github.touhoumaidaffection.bond.settings;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Bounded, read-only, owner-scoped gift status protocol. */
public final class TmaGiftStatusWire {
    public static final int MAX_MAIDS = 16;
    public static final int MAX_GIFTS = 64;
    public static final int MAX_STRING_LENGTH = 256;
    public static final int MAX_PAGE = Integer.MAX_VALUE / MAX_MAIDS;

    private TmaGiftStatusWire() { }

    public interface Sink {
        void writeInt(int value);
        void writeLong(long value);
        void writeBoolean(boolean value);
        void writeString(String value);
    }

    public interface Source {
        int readInt();
        long readLong();
        boolean readBoolean();
        String readString();
    }

    public enum DeliveryState {
        DISABLED, UNLOADED, OTHER_DIMENSION, TOO_FAR, LAP_PILLOW, COOLDOWN,
        PREPARING, POOL_EMPTY, APPROACHING, READY
    }

    public record Gift(String itemId, int count) {
        public Gift {
            itemId = checkedString(itemId);
            bounded(count, 1, Integer.MAX_VALUE);
        }
    }

    public record MaidStatus(String maidUuid, String name, int queued, int preparedCount,
                             List<Gift> gifts, long nextReadyAtMs, long lastDeliveryAtMs,
                             String lastGiftId, DeliveryState state, int deliveryCooldownSeconds) {
        public MaidStatus {
            maidUuid = checkedString(maidUuid);
            name = checkedString(name);
            lastGiftId = checkedString(lastGiftId);
            bounded(queued, 0, MAX_GIFTS);
            bounded(preparedCount, 0, MAX_GIFTS);
            bounded(gifts.size(), 0, MAX_GIFTS);
            gifts = List.copyOf(gifts);
            Objects.requireNonNull(state);
            bounded(deliveryCooldownSeconds, 0, Integer.MAX_VALUE);
        }
    }

    public record Status(long serverTimeMs, boolean enabled, boolean curatedPoolOnly,
                         boolean includeModItems, int candidateCount, int intervalMinutes,
                         int maxQueued, int page, int totalMaids, List<MaidStatus> maids) {
        public Status {
            bounded(candidateCount, 0, Integer.MAX_VALUE);
            bounded(intervalMinutes, 1, 1440);
            bounded(maxQueued, 1, MAX_GIFTS);
            validatePage(page);
            bounded(totalMaids, 0, Integer.MAX_VALUE);
            bounded(maids.size(), 0, MAX_MAIDS);
            maids = List.copyOf(maids);
        }
    }

    public static int validatePage(int page) {
        return bounded(page, 0, MAX_PAGE);
    }

    public static void writeStatus(Sink sink, Status status) {
        sink.writeLong(status.serverTimeMs());
        sink.writeBoolean(status.enabled());
        sink.writeBoolean(status.curatedPoolOnly());
        sink.writeBoolean(status.includeModItems());
        sink.writeInt(status.candidateCount());
        sink.writeInt(status.intervalMinutes());
        sink.writeInt(status.maxQueued());
        sink.writeInt(status.page());
        sink.writeInt(status.totalMaids());
        sink.writeInt(status.maids().size());
        for (MaidStatus maid : status.maids()) {
            sink.writeString(maid.maidUuid());
            sink.writeString(maid.name());
            sink.writeInt(maid.queued());
            sink.writeInt(maid.preparedCount());
            sink.writeInt(maid.gifts().size());
            for (Gift gift : maid.gifts()) {
                sink.writeString(gift.itemId());
                sink.writeInt(gift.count());
            }
            sink.writeLong(maid.nextReadyAtMs());
            sink.writeLong(maid.lastDeliveryAtMs());
            sink.writeString(maid.lastGiftId());
            sink.writeInt(maid.state().ordinal());
            sink.writeInt(maid.deliveryCooldownSeconds());
        }
    }

    public static Status readStatus(Source source) {
        long now = source.readLong();
        boolean enabled = source.readBoolean();
        boolean curated = source.readBoolean();
        boolean mods = source.readBoolean();
        int candidates = source.readInt();
        int interval = source.readInt();
        int cap = source.readInt();
        int page = validatePage(source.readInt());
        int total = source.readInt();
        int count = bounded(source.readInt(), 0, MAX_MAIDS);
        List<MaidStatus> maids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String uuid = checkedString(source.readString());
            String name = checkedString(source.readString());
            int queued = source.readInt();
            int prepared = source.readInt();
            int giftCount = bounded(source.readInt(), 0, MAX_GIFTS);
            List<Gift> gifts = new ArrayList<>(giftCount);
            for (int g = 0; g < giftCount; g++) {
                gifts.add(new Gift(source.readString(), source.readInt()));
            }
            long next = source.readLong();
            long last = source.readLong();
            String lastId = checkedString(source.readString());
            DeliveryState state = DeliveryState.values()[bounded(source.readInt(), 0, DeliveryState.values().length - 1)];
            maids.add(new MaidStatus(uuid, name, queued, prepared, gifts, next, last, lastId,
                    state, source.readInt()));
        }
        return new Status(now, enabled, curated, mods, candidates, interval, cap, page, total, maids);
    }

    private static String checkedString(String value) {
        Objects.requireNonNull(value);
        bounded(value.length(), 0, MAX_STRING_LENGTH);
        return value;
    }

    private static int bounded(int value, int min, int max) {
        if (value < min || value > max) {
            throw new IllegalArgumentException("Gift status value outside bounds: " + value);
        }
        return value;
    }
}
