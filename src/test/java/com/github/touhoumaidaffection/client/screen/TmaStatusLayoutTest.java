package com.github.touhoumaidaffection.client.screen;

import com.github.touhoumaidaffection.bond.settings.TmaGiftStatusViewRules;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TmaStatusLayoutTest {
    @Test
    void loadingKeepsBothFeatureParentsAndScopesItsMessageUnderAi() {
        TmaStatusLayout loading = new TmaStatusLayout(false, 0);
        TmaStatusLayout ready = new TmaStatusLayout(true, 0);
        List<TmaStatusLayout.Row> features = loading.rows().stream()
                .filter(row -> row.kind() == TmaStatusLayout.Kind.FEATURE).toList();
        assertEquals(List.of(TmaStatusLayout.Section.GIFTS, TmaStatusLayout.Section.AI),
                features.stream().map(TmaStatusLayout.Row::section).toList());
        assertEquals(features, ready.rows().stream()
                .filter(row -> row.kind() == TmaStatusLayout.Kind.FEATURE).toList());
        assertEquals(features.get(0).left(30), features.get(1).left(30));
        TmaStatusLayout.Row message = loading.rows().get(2);
        assertEquals(TmaStatusLayout.Kind.MESSAGE, message.kind());
        assertEquals(TmaStatusLayout.Section.AI, message.section());
        assertTrue(message.y() >= features.get(1).y() + features.get(1).height());
        assertTrue(message.left(30) > features.get(1).left(30));
        assertTrue(message.y() + message.height() <= loading.height());
    }

    @Test
    void everyAiFieldBelongsToItsPrecedingSubsectionWithoutOverlapping() {
        TmaStatusLayout layout = new TmaStatusLayout(true, 3);
        TmaStatusLayout.Row subsection = null;
        int previousBottom = 0;
        for (TmaStatusLayout.Row row : layout.rows()) {
            assertTrue(row.y() >= previousBottom);
            previousBottom = row.y() + row.height();
            if (row.kind() == TmaStatusLayout.Kind.SUBSECTION) subsection = row;
            if (row.kind() == TmaStatusLayout.Kind.FIELD || row.kind() == TmaStatusLayout.Kind.MAID) {
                assertNotNull(subsection);
                assertEquals(subsection.section(), row.section());
                assertTrue(row.left(0) > subsection.left(0));
            }
        }
        assertTrue(previousBottom <= layout.height());
        assertEquals(List.of(0, 1, 2), layout.rows().stream()
                .filter(row -> row.kind() == TmaStatusLayout.Kind.MAID)
                .map(TmaStatusLayout.Row::index).toList());
        TmaStatusLayout empty = new TmaStatusLayout(true, 0);
        TmaStatusLayout.Row last = empty.rows().get(empty.rows().size() - 1);
        assertEquals(TmaStatusLayout.Section.MAIDS, last.section());
        assertEquals(TmaStatusLayout.Kind.MESSAGE, last.kind());
    }

    @Test
    void giftSelectorAndItemHitRowsFollowTheSameScrolledGeometry() {
        for (int top : new int[]{40, 0, -90}) {
            int selectorTop = top + TmaStatusLayout.SELECTOR_TOP;
            assertFalse(TmaStatusLayout.selectorContainsY(top, selectorTop - 0.1));
            assertTrue(TmaStatusLayout.selectorContainsY(top, selectorTop));
            assertFalse(TmaStatusLayout.selectorContainsY(top, selectorTop + TmaStatusLayout.SELECTOR_HEIGHT));
            int first = TmaStatusLayout.giftDetailTop(top, 0);
            assertTrue(first >= selectorTop + TmaStatusLayout.SELECTOR_HEIGHT);
            assertEquals(-1, TmaStatusLayout.giftDetailIndex(top, first - 0.1));
            for (int index = 0; index < TmaStatusLayout.GIFT_ROWS; index++) {
                int rowTop = TmaStatusLayout.giftDetailTop(top, index);
                assertEquals(index, TmaStatusLayout.giftDetailIndex(top, rowTop));
                assertEquals(index, TmaStatusLayout.giftDetailIndex(top, rowTop + TmaStatusLayout.GIFT_ROW_HEIGHT - 0.1));
            }
            assertEquals(-1, TmaStatusLayout.giftDetailIndex(top,
                    first + TmaStatusLayout.GIFT_ROWS * TmaStatusLayout.GIFT_ROW_HEIGHT));
            assertTrue(first + TmaStatusLayout.GIFT_ROWS * TmaStatusLayout.GIFT_ROW_HEIGHT <= top + TmaStatusLayout.GIFT_HEIGHT);
        }
    }

    @Test
    void finalMaidControlRemainsReachableAndShrinkingSnapshotClampsScroll() {
        TmaStatusLayout ready = new TmaStatusLayout(true, 24);
        int viewportHeight = 170;
        int scroll = TmaGiftStatusViewRules.clampScroll(Integer.MAX_VALUE, ready.height(), viewportHeight);
        TmaStatusLayout.Row last = ready.rows().get(ready.rows().size() - 1);
        int controlHeight = 14;
        int controlTop = last.controlTop(-scroll, controlHeight);
        assertTrue(controlTop >= 0);
        assertTrue(controlTop + controlHeight <= viewportHeight);
        assertTrue(controlTop >= last.y() - scroll);
        assertTrue(controlTop + controlHeight <= last.y() - scroll + last.height());
        TmaStatusLayout loading = new TmaStatusLayout(false, 0);
        int clamped = TmaGiftStatusViewRules.clampScroll(scroll, loading.height(), viewportHeight);
        assertEquals(Math.max(0, loading.height() - viewportHeight), clamped);
        assertTrue(clamped < scroll);
    }
}
