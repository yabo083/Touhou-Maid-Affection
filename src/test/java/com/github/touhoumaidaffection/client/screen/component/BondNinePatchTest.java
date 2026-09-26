package com.github.touhoumaidaffection.client.screen.component;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Geometry contract of the nine-patch used for the bond tab's secondary-page modal frame: the
 * corners must keep their authored size, the centre must absorb every extra pixel, and a frame too
 * small to hold a centre slice must degrade to just its borders instead of producing negative
 * rectangles.
 */
class BondNinePatchTest {
    // textures/gui/bond_modal.png: 6 | 2 | 6 logical columns and 22 | 2 | 6 logical rows, baked
    // at 3 texture pixels per logical GUI pixel.
    private static final int SLICE_LEFT = 6 * 3;
    private static final int SLICE_TOP = 22 * 3;
    private static final int SLICE_RIGHT = 6 * 3;
    private static final int SLICE_BOTTOM = 6 * 3;
    private static final int TEXTURE_WIDTH = SLICE_LEFT + 2 * 3 + SLICE_RIGHT;
    private static final int TEXTURE_HEIGHT = SLICE_TOP + 2 * 3 + SLICE_BOTTOM;
    private static final int SCALE = 3;

    private static List<BondNinePatch.Slice> slices(int width, int height) {
        return BondNinePatch.slices(100, 50, width, height,
                SLICE_LEFT, SLICE_TOP, SLICE_RIGHT, SLICE_BOTTOM,
                TEXTURE_WIDTH, TEXTURE_HEIGHT, SCALE);
    }

    @Test
    void nineSlicesCoverTheSplitPageModal() {
        List<BondNinePatch.Slice> result = slices(BondGuiTokens.SECONDARY_MODAL_WIDTH, BondGuiTokens.SECONDARY_MODAL_HEIGHT);
        assertEquals(9, result.size());

        // corners keep their authored size, the centre absorbs the rest
        assertEquals(new BondNinePatch.Slice(100, 50, 6, 22, 0, 0, 18, 66), result.get(0));
        assertEquals(new BondNinePatch.Slice(106, 50, 160, 22, 18, 0, 6, 66), result.get(1));
        assertEquals(new BondNinePatch.Slice(266, 50, 6, 22, 24, 0, 18, 66), result.get(2));
        assertEquals(new BondNinePatch.Slice(100, 72, 6, 122, 0, 66, 18, 6), result.get(3));
        assertEquals(new BondNinePatch.Slice(106, 72, 160, 122, 18, 66, 6, 6), result.get(4));
        assertEquals(new BondNinePatch.Slice(266, 72, 6, 122, 24, 66, 18, 6), result.get(5));
        assertEquals(new BondNinePatch.Slice(100, 194, 6, 6, 0, 72, 18, 18), result.get(6));
        assertEquals(new BondNinePatch.Slice(106, 194, 160, 6, 18, 72, 6, 18), result.get(7));
        assertEquals(new BondNinePatch.Slice(266, 194, 6, 6, 24, 72, 18, 18), result.get(8));
    }

    @Test
    void sliceRowsAndColumnsCoverTheRequestedFrameExactly() {
        int width = 166;
        int height = 132;
        List<BondNinePatch.Slice> result = slices(width, height);

        // top row spans the full width, middle row spans the full height
        assertEquals(0, result.get(0).x() - 100);
        assertEquals(width, result.get(2).x() + result.get(2).width() - 100);
        assertEquals(0, result.get(0).y() - 50);
        assertEquals(height, result.get(6).y() + result.get(6).height() - 50);
        // the voice-page modal (166x132) must reuse the same corners as the split-page one (172x150)
        assertEquals(new BondNinePatch.Slice(100, 50, 6, 22, 0, 0, 18, 66), result.get(0));
        assertEquals(new BondNinePatch.Slice(260, 50, 6, 22, 24, 0, 18, 66), result.get(2));
        assertEquals(new BondNinePatch.Slice(106, 72, 154, 104, 18, 66, 6, 6), result.get(4));
    }

    @Test
    void frameSqueezedBelowItsBordersDropsTheEmptyCentreSlices() {
        List<BondNinePatch.Slice> squeezed = slices(4, 10);
        assertEquals(4, squeezed.size(), "both centre slices collapse, leaving the four corners");
        assertTrue(squeezed.stream().allMatch(slice -> slice.width() > 0 && slice.height() > 0),
                "no slice may have a non-positive destination size");
        assertTrue(squeezed.stream().allMatch(slice -> slice.uWidth() > 0 && slice.vHeight() > 0),
                "no slice may have a non-positive source size");

        assertEquals(6, slices(4, 150).size(), "a narrow frame keeps three rows but only two columns");
        assertEquals(6, slices(172, 10).size(), "a short frame keeps three columns but only two rows");
    }

    @Test
    void nonPositiveFrameOrScaleDrawsNothing() {
        assertTrue(slices(0, 150).isEmpty());
        assertTrue(slices(172, -1).isEmpty());
        assertTrue(BondNinePatch.slices(0, 0, 172, 150,
                SLICE_LEFT, SLICE_TOP, SLICE_RIGHT, SLICE_BOTTOM,
                TEXTURE_WIDTH, TEXTURE_HEIGHT, 0).isEmpty());
    }
}
