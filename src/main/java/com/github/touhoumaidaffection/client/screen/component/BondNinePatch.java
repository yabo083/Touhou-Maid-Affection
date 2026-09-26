package com.github.touhoumaidaffection.client.screen.component;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure geometry for a nine-patch (nine-slice) blit, split out of {@link BondGuiArt} so the slice
 * maths stays unit-testable without a live {@code GuiGraphics}.
 *
 * <p>The source slices are given in texture pixels and the target coordinates in GUI pixels: this
 * mod bakes its artwork at {@code scale} texture pixels per logical GUI pixel (3, i.e. GUI scale 3,
 * the same convention as {@code textures/gui/settings_panel.png}), so a fixed-size corner stays
 * exactly as authored while the centre slices stretch to the requested frame size.
 */
public final class BondNinePatch {
    private BondNinePatch() {
    }

    /**
     * One non-empty rectangle of the patch.
     *
     * @param x       destination left, in GUI pixels
     * @param y       destination top, in GUI pixels
     * @param width   destination width, in GUI pixels
     * @param height  destination height, in GUI pixels
     * @param u       source left, in texture pixels
     * @param v       source top, in texture pixels
     * @param uWidth  source width, in texture pixels
     * @param vHeight source height, in texture pixels
     */
    public record Slice(int x, int y, int width, int height, int u, int v, int uWidth, int vHeight) {
    }

    /**
     * Splits a nine-patch blit into its non-empty slices, in reading order (top-left, top-centre,
     * top-right, middle-left, centre, middle-right, bottom-left, bottom-centre, bottom-right).
     * Zero-sized slices - a frame whose only content is its borders, for example - are omitted.
     */
    public static List<Slice> slices(int x, int y, int width, int height,
                                     int sourceLeft, int sourceTop, int sourceRight, int sourceBottom,
                                     int sourceWidth, int sourceHeight, int scale) {
        List<Slice> result = new ArrayList<>(9);
        if (width <= 0 || height <= 0 || scale <= 0) {
            return result;
        }
        int left = sourceLeft / scale;
        int top = sourceTop / scale;
        int right = sourceRight / scale;
        int bottom = sourceBottom / scale;
        int centreSourceWidth = Math.max(0, sourceWidth - sourceLeft - sourceRight);
        int centreSourceHeight = Math.max(0, sourceHeight - sourceTop - sourceBottom);
        int centreWidth = Math.max(0, width - left - right);
        int centreHeight = Math.max(0, height - top - bottom);

        int[] destinationX = {x, x + left, x + left + centreWidth};
        int[] destinationY = {y, y + top, y + top + centreHeight};
        int[] destinationWidth = {left, centreWidth, right};
        int[] destinationHeight = {top, centreHeight, bottom};
        int[] sourceX = {0, sourceLeft, sourceLeft + centreSourceWidth};
        int[] sourceY = {0, sourceTop, sourceTop + centreSourceHeight};
        int[] sourceWidths = {sourceLeft, centreSourceWidth, sourceRight};
        int[] sourceHeights = {sourceTop, centreSourceHeight, sourceBottom};

        for (int row = 0; row < 3; row++) {
            if (destinationHeight[row] <= 0 || sourceHeights[row] <= 0) {
                continue;
            }
            for (int column = 0; column < 3; column++) {
                if (destinationWidth[column] <= 0 || sourceWidths[column] <= 0) {
                    continue;
                }
                result.add(new Slice(
                        destinationX[column],
                        destinationY[row],
                        destinationWidth[column],
                        destinationHeight[row],
                        sourceX[column],
                        sourceY[row],
                        sourceWidths[column],
                        sourceHeights[row]
                ));
            }
        }
        return result;
    }
}
