package com.github.touhoumaidaffection.bond.settings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Pure-logic helper that turns the raw {@code (uuid, name)} pairs of the read-only "by maid" list
 * into display labels that stay distinguishable when several maids share the same name.
 *
 * <p>Rules:
 * <ul>
 *   <li>a name that occurs exactly once is returned unchanged (no noise added);</li>
 *   <li>every row whose name occurs two or more times gets {@code " #"} plus the first four
 *       characters of its uuid in lower case appended, e.g. {@code 精灵酒狐 #a1b2};</li>
 *   <li>an empty/blank name falls back to {@code 未知女仆} and is always disambiguated (a blank
 *       name can never tell two rows apart), e.g. {@code 未知女仆 #a1b2}.</li>
 * </ul>
 *
 * <p>Names are compared after trimming, so {@code "芙兰"} and {@code "芙兰 "} count as the same
 * name; the original (untrimmed) name is still what gets rendered. The returned list always has
 * the same size and order as the input, so callers can pair a label back with its maid by index.
 *
 * <p>No Minecraft type is referenced, so this stays unit-testable on a plain JUnit classpath.
 */
public final class TmaMaidLabels {
    /** Wording used when a maid has no usable name. */
    public static final String UNKNOWN_NAME = "未知女仆";
    /** Number of uuid characters kept as the disambiguation suffix. */
    public static final int SHORT_ID_LENGTH = 4;

    private TmaMaidLabels() {
    }

    /**
     * Builds one display label per maid, in the same order as {@code maids}.
     *
     * @param maids the maids of the "by maid" list (may be {@code null}); {@code null} entries are
     *              rendered as {@code 未知女仆}
     * @return an immutable list of labels, one per input maid (empty for a {@code null}/empty input)
     */
    public static List<String> displayLabels(List<TmaAiStatusWire.MaidStatus> maids) {
        if (maids == null || maids.isEmpty()) {
            return List.of();
        }
        Map<String, Integer> nameCounts = new HashMap<>();
        for (TmaAiStatusWire.MaidStatus maid : maids) {
            nameCounts.merge(groupKey(maid), 1, Integer::sum);
        }
        List<String> labels = new ArrayList<>(maids.size());
        for (TmaAiStatusWire.MaidStatus maid : maids) {
            String base = baseLabel(maid);
            String shortId = shortId(maid);
            boolean ambiguous = base.equals(UNKNOWN_NAME) || nameCounts.get(groupKey(maid)) > 1;
            labels.add(ambiguous && !shortId.isEmpty() ? base + " #" + shortId : base);
        }
        return List.copyOf(labels);
    }

    /** @return the name used to group rows; trimmed, and blank names collapse onto {@link #UNKNOWN_NAME}. */
    private static String groupKey(TmaAiStatusWire.MaidStatus maid) {
        String base = baseLabel(maid);
        return base.equals(UNKNOWN_NAME) ? UNKNOWN_NAME : base.trim();
    }

    /** @return the row's name, or {@link #UNKNOWN_NAME} when it is missing/blank. */
    private static String baseLabel(TmaAiStatusWire.MaidStatus maid) {
        if (maid == null) {
            return UNKNOWN_NAME;
        }
        String name = maid.name();
        if (name == null || name.trim().isEmpty()) {
            return UNKNOWN_NAME;
        }
        return name;
    }

    /** @return the first {@link #SHORT_ID_LENGTH} characters of the uuid in lower case, or "". */
    private static String shortId(TmaAiStatusWire.MaidStatus maid) {
        if (maid == null) {
            return "";
        }
        String uuid = maid.maidUuid();
        if (uuid == null) {
            return "";
        }
        String hex = uuid.replace("-", "").trim().toLowerCase(Locale.ROOT);
        return hex.length() <= SHORT_ID_LENGTH ? hex : hex.substring(0, SHORT_ID_LENGTH);
    }
}