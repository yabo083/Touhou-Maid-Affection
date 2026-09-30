package com.github.touhoumaidaffection.client.screen;

import java.util.ArrayList;
import java.util.List;

/** Content-relative geometry shared by status rendering, scrolling and pointer handling. */
final class TmaStatusLayout {
    static final int SUBSECTION_INDENT = 4;
    static final int FIELD_INDENT = 8;
    static final int FEATURE_HEIGHT = 18;
    static final int SELECTOR_TOP = FEATURE_HEIGHT;
    static final int SELECTOR_HEIGHT = 18;
    static final int GIFT_ROW_HEIGHT = 14;
    static final int GIFT_DETAIL_TOP = SELECTOR_TOP + SELECTOR_HEIGHT + 4;
    static final int GIFT_ROWS = 6;
    static final int GIFT_HEIGHT = GIFT_DETAIL_TOP + GIFT_ROWS * GIFT_ROW_HEIGHT + 8;
    private static final int ROW_GAP = 6;
    private static final int SUBSECTION_GAP = 4;

    enum Kind {
        FEATURE(FEATURE_HEIGHT, 0), SUBSECTION(15, SUBSECTION_INDENT),
        FIELD(13, FIELD_INDENT), MAID(18, FIELD_INDENT), MESSAGE(13, FIELD_INDENT);

        final int height;
        final int indent;

        Kind(int height, int indent) {
            this.height = height;
            this.indent = indent;
        }
    }

    enum Section {
        GIFTS("bond.settings.gifts.title", 0),
        AI("bond.settings.status.section.morning_kiss", 0),
        SWITCHES("bond.settings.status.section.switches", 4),
        LANGUAGES("bond.settings.status.section.languages", 2),
        CACHE_POLICY("bond.settings.status.section.cache_policy", 3),
        CACHE_STATS("bond.settings.status.section.cache_stats", 2),
        MAIDS("bond.settings.status.section.maids", 0);

        final String key;
        final int fields;

        Section(String key, int fields) {
            this.key = key;
            this.fields = fields;
        }
    }

    record Row(Kind kind, Section section, int index, int y) {
        int height() { return kind.height; }
        int left(int contentLeft) { return contentLeft + kind.indent; }
        int controlTop(int contentTop, int controlHeight) {
            return contentTop + y + (height() - controlHeight) / 2;
        }
    }

    private final List<Row> rows;
    private final int height;

    TmaStatusLayout(boolean aiReady, int maidCount) {
        List<Row> rows = new ArrayList<>();
        rows.add(new Row(Kind.FEATURE, Section.GIFTS, 0, 0));
        rows.add(new Row(Kind.FEATURE, Section.AI, 0, GIFT_HEIGHT));
        int y = GIFT_HEIGHT + FEATURE_HEIGHT;
        if (!aiReady) {
            rows.add(new Row(Kind.MESSAGE, Section.AI, 0, y));
            y += Kind.MESSAGE.height + ROW_GAP;
        } else {
            for (Section section : Section.values()) {
                if (section == Section.GIFTS || section == Section.AI) continue;
                y += SUBSECTION_GAP;
                rows.add(new Row(Kind.SUBSECTION, section, 0, y));
                y += Kind.SUBSECTION.height;
                int count = section == Section.MAIDS ? maidCount : section.fields;
                Kind kind = section == Section.MAIDS ? Kind.MAID : Kind.FIELD;
                if (section == Section.MAIDS && count == 0) {
                    rows.add(new Row(Kind.MESSAGE, section, 0, y));
                    y += Kind.MESSAGE.height + ROW_GAP;
                }
                for (int index = 0; index < count; index++) {
                    rows.add(new Row(kind, section, index, y));
                    y += kind.height + ROW_GAP;
                }
            }
        }
        this.rows = List.copyOf(rows);
        height = y;
    }

    List<Row> rows() { return rows; }
    int height() { return height; }

    static int giftDetailTop(int contentTop, int index) {
        return contentTop + GIFT_DETAIL_TOP + index * GIFT_ROW_HEIGHT;
    }

    static int giftDetailIndex(int contentTop, double mouseY) {
        double offset = mouseY - giftDetailTop(contentTop, 0);
        return offset < 0 || offset >= GIFT_ROWS * GIFT_ROW_HEIGHT ? -1 : (int) offset / GIFT_ROW_HEIGHT;
    }

    static boolean selectorContainsY(int contentTop, double mouseY) {
        return mouseY >= contentTop + SELECTOR_TOP && mouseY < contentTop + SELECTOR_TOP + SELECTOR_HEIGHT;
    }
}
