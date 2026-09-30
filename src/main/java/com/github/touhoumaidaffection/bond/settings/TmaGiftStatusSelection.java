package com.github.touhoumaidaffection.bond.settings;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Screen-local selection over sequential bounded snapshots; never a global cache. */
public final class TmaGiftStatusSelection {
    public record Entry(TmaGiftStatusWire.Status page, TmaGiftStatusWire.MaidStatus maid, long receivedNanos) { }

    private final Map<String, Entry> entries = new LinkedHashMap<>();
    private List<Entry> options = List.of();
    private String selectedUuid;
    private int requestedPage;
    private int lastPage;
    private boolean loading;
    private boolean complete;
    private int revision;

    public void refresh() {
        entries.clear();
        options = List.of();
        requestedPage = 0;
        lastPage = 0;
        loading = true;
        complete = false;
        revision++;
    }

    /** Returns the next page to request, or -1 when finished/ignored. */
    public int accept(TmaGiftStatusWire.Status page, long receivedNanos) {
        if (!loading || page.page() > requestedPage) return -1;
        // A lower page is only a valid reply when the server clamps after removals.
        int currentLast = page.totalMaids() == 0 ? 0 : (page.totalMaids() - 1) / TmaGiftStatusWire.MAX_MAIDS;
        if (page.page() != requestedPage && page.page() != currentLast) return -1;
        if (requestedPage == 0) lastPage = currentLast;
        else lastPage = Math.min(lastPage, currentLast);
        entries.values().removeIf(entry -> entry.page().page() >= page.page());
        for (TmaGiftStatusWire.MaidStatus maid : page.maids()) {
            entries.put(maid.maidUuid(), new Entry(page, maid, receivedNanos));
        }
        options = List.copyOf(entries.values());
        if (selectedUuid == null && !options.isEmpty()) selectedUuid = options.get(0).maid().maidUuid();
        revision++;
        if (page.page() < lastPage) return ++requestedPage;
        loading = false;
        complete = true;
        if (!entries.containsKey(selectedUuid)) {
            selectedUuid = options.isEmpty() ? null : options.get(0).maid().maidUuid();
        }
        return -1;
    }

    public void fail() {
        loading = false;
        revision++;
    }

    public void clear() {
        entries.clear();
        options = List.of();
        selectedUuid = null;
        loading = false;
        complete = false;
        revision++;
    }

    public void select(String uuid) {
        if (entries.containsKey(uuid)) {
            selectedUuid = uuid;
            revision++;
        }
    }

    public Entry selected() { return entries.get(selectedUuid); }
    public String selectedUuid() { return selectedUuid; }
    public List<Entry> options() { return options; }
    public boolean loading() { return loading; }
    public boolean complete() { return complete; }
    public int revision() { return revision; }

    /** Aggregate the actual prepared items, preserving first appearance order. */
    public static List<TmaGiftStatusWire.Gift> preparedItems(TmaGiftStatusWire.MaidStatus maid) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (TmaGiftStatusWire.Gift gift : maid.gifts()) counts.merge(gift.itemId(), gift.count(), Math::addExact);
        List<TmaGiftStatusWire.Gift> result = new ArrayList<>(counts.size());
        counts.forEach((id, count) -> result.add(new TmaGiftStatusWire.Gift(id, count)));
        return List.copyOf(result);
    }

    public static long preparationSeconds(Entry entry, long nowNanos) {
        return TmaGiftStatusViewRules.remainingSeconds(entry.maid().nextReadyAtMs(),
                TmaGiftStatusViewRules.serverNow(entry.page().serverTimeMs(), elapsedMillis(entry, nowNanos)));
    }

    public static long cooldownSeconds(Entry entry, long nowNanos) {
        return Math.max(0L, entry.maid().deliveryCooldownSeconds() - elapsedMillis(entry, nowNanos) / 1000L);
    }

    public static long elapsedMillis(Entry entry, long nowNanos) {
        return TmaGiftStatusViewRules.elapsedMillis(entry.receivedNanos(), nowNanos);
    }
}
