package com.github.touhoumaidaffection.client.screen.component;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Texture-backed plates for the bond tab.
 *
 * <p>Every plate that used to be painted with {@code GuiGraphics#fill} - the tab page, the ability
 * row, the secondary-page modal frame and the inset list plates - now comes from a PNG under
 * {@code assets/touhou_maid_affection/textures/gui}, so a resource pack restyles the whole tab
 * without touching code. This is the same contract as the global settings panel
 * ({@code textures/gui/settings_panel.png}): the artwork is baked at {@link #TEXTURE_SCALE} texture
 * pixels per logical GUI pixel (three, i.e. GUI scale 3) and blitted 1:1, so the shipped art is
 * never resampled.
 *
 * <p>The geometry is deliberately unchanged from the hand-drawn version: the page still occupies
 * the same 176x137 rect, the row plate the same 158x23 fill box and the modal frame keeps its
 * {@link BondGuiTokens#MODAL_TITLE_HEIGHT} title band. Dropdowns, sliders, toggles and all text
 * stay drawn by code; the button faces live in the atlas described by {@link BondButtonStyle}.
 *
 * <p>The shipped PNGs are a placeholder drawn in classic vanilla GUI idiom (black outline, light
 * top/left bevel, dark bottom/right bevel, recessed content slots) and are meant to be replaced
 * with hand-drawn art.
 */
public final class BondGuiArt {
    private BondGuiArt() {
    }

    /** Texture pixels per logical GUI pixel baked into every bond plate. */
    private static final int TEXTURE_SCALE = 3;

    // ---- Bond page plate (176x137 logical) ----
    private static final ResourceLocation BOND_PAGE = texture("bond_page.png");
    private static final int PAGE_TEXTURE_WIDTH = 528;
    private static final int PAGE_TEXTURE_HEIGHT = 411;

    // ---- Ability row plate (158x23 logical: the row pitch is 24, the layout's fill box is 23) ----
    private static final ResourceLocation BOND_ROW = texture("bond_row.png");
    private static final int ROW_TEXTURE_WIDTH = 474;
    private static final int ROW_TEXTURE_HEIGHT = 69;

    // ---- Secondary-page modal frame (nine-patch, 6|2|6 columns and 22|2|6 rows) ----
    private static final ResourceLocation BOND_MODAL = texture("bond_modal.png");
    private static final int MODAL_SLICE_SIDE = 6 * TEXTURE_SCALE;
    /** Top slice: the title band plus its separator row, kept in sync with the token. */
    private static final int MODAL_SLICE_TOP = (BondGuiTokens.MODAL_TITLE_HEIGHT + 1) * TEXTURE_SCALE;
    private static final int MODAL_SLICE_BOTTOM = 6 * TEXTURE_SCALE;
    private static final int MODAL_TEXTURE_WIDTH = MODAL_SLICE_SIDE * 2 + 2 * TEXTURE_SCALE;
    private static final int MODAL_TEXTURE_HEIGHT = MODAL_SLICE_TOP + 2 * TEXTURE_SCALE + MODAL_SLICE_BOTTOM;

    // ---- Inset plate for list boxes and the pose grid (nine-patch, 3|2|3 slices) ----
    private static final ResourceLocation BOND_INSET = texture("bond_inset.png");
    private static final int INSET_SLICE = 3 * TEXTURE_SCALE;
    private static final int INSET_TEXTURE_SIZE = INSET_SLICE * 2 + 2 * TEXTURE_SCALE;

    // ---- Button atlas (nine-patch: one 8x8 logical cell per state, stacked vertically) ----
    private static final ResourceLocation BOND_BUTTON = texture("bond_button.png");
    private static final int BUTTON_SLICE = 3 * TEXTURE_SCALE;
    private static final int BUTTON_CELL = 8 * TEXTURE_SCALE;
    private static final int BUTTON_TEXTURE_WIDTH = BUTTON_CELL;
    private static final int BUTTON_TEXTURE_HEIGHT = BUTTON_CELL * 5;

    // ---- Ability-row star: the unlock lamp on the left of every skill row ----
    // Icons in this mod are drawn at 1 texture pixel per logical pixel (like bond_tab_icon.png), so a
    // 16x16 file covers 16x16 logical pixels; only the plates are baked at TEXTURE_SCALE.
    private static final ResourceLocation BOND_STAR_LIT = texture("bond_star_lit.png");
    private static final ResourceLocation BOND_STAR_DIM = texture("bond_star_dim.png");
    private static final int STAR_SIZE = 16;

    /** Row of {@link #BOND_BUTTON} to draw; the order must match the atlas. */
    public enum BondButtonStyle {
        DEFAULT,
        HOVER,
        PRIMARY,
        PRIMARY_HOVER,
        DISABLED
    }

    private static ResourceLocation texture(String fileName) {
        return ResourceLocation.fromNamespaceAndPath(TouhouMaidAffection.MOD_ID, "textures/gui/" + fileName);
    }

    /**
     * Paints the star at the left of an ability row: gold and glowing once the skill is unlocked, a
     * dark unlit four-point shape while it is still locked. Drawn from code rather than baked into
     * {@code bond_row.png} because the state changes per row; see {@link #STAR_SIZE}.
     */
    public static void drawRowStar(GuiGraphics graphics, int left, int top, boolean lit) {
        beginTranslucentPass();
        graphics.blit(lit ? BOND_STAR_LIT : BOND_STAR_DIM,
                left, top, STAR_SIZE, STAR_SIZE,
                0.0F, 0.0F, STAR_SIZE, STAR_SIZE, STAR_SIZE, STAR_SIZE);
    }

    /** Largest first, so a label keeps its full size whenever it fits. */
    private static final float[] LABEL_SCALES = {1.0F, 0.9F, 0.82F, 0.74F, 0.66F, 0.6F};

    /**
     * Draws a button label centred inside its button, scaled down in fixed steps until it fits the
     * inner box. Minecraft's font has no smaller size and the bond buttons are deliberately small
     * (42x16 for a status such as {@code 自动赠礼中}, 50x12 for the settings entry), so without this
     * the text spills over the frame. Steps rather than an exact fit keep every button in a list at
     * the same text size.
     */
    public static void drawFittedLabel(GuiGraphics graphics, Font font, Component label,
                                       int x, int y, int width, int height, int color) {
        int innerWidth = Math.max(1, width - 4);
        int innerHeight = Math.max(1, height - 4);
        float scale = LABEL_SCALES[LABEL_SCALES.length - 1];
        for (float candidate : LABEL_SCALES) {
            if (font.width(label) * candidate <= innerWidth && font.lineHeight * candidate <= innerHeight) {
                scale = candidate;
                break;
            }
        }
        // last resort for a label no step can hold: cut it to the widened text budget
        String text = font.plainSubstrByWidth(label.getString(), Math.max(1, (int) (innerWidth / scale)));
        graphics.pose().pushPose();
        graphics.pose().translate(x + width / 2.0F, y + height / 2.0F, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawCenteredString(font, text, 0, -font.lineHeight / 2, color);
        graphics.pose().popPose();
    }

    /**
     * Paints a button face from {@link #BOND_BUTTON}, replacing the old
     * {@code drawFramedPanelWithInnerBorder} + hover-overlay pair - the hover highlight is baked into
     * the hover rows, so callers only pick a state. Nine-patched, so every button size used by the
     * pages (the ability rows' 46x20 and 40x20 pair, the 17x13 header buttons, the footer buttons and
     * the 50x12 settings entry) reuses one texture.
     *
     * <p>The cell's borders are three logical pixels per side, so a button must be at least 6x6
     * logical pixels; anything smaller would drop its centre slice and lose the middle of the frame.
     */
    public static void drawButton(GuiGraphics graphics, int left, int top, int right, int bottom, BondButtonStyle style) {
        int rowOffset = style.ordinal() * BUTTON_CELL;
        for (BondNinePatch.Slice slice : BondNinePatch.slices(
                left, top, right - left, bottom - top,
                BUTTON_SLICE, BUTTON_SLICE, BUTTON_SLICE, BUTTON_SLICE,
                BUTTON_CELL, BUTTON_CELL, TEXTURE_SCALE)) {
            beginTranslucentPass();
            graphics.blit(
                    BOND_BUTTON,
                    slice.x(), slice.y(), slice.width(), slice.height(),
                    slice.u(), slice.v() + rowOffset, slice.uWidth(), slice.vHeight(),
                    BUTTON_TEXTURE_WIDTH, BUTTON_TEXTURE_HEIGHT
            );
        }
    }

    /**
     * Paints the bond page plate: frame, bevels, title bar, header / footer / side separators and
     * the recessed content area. The localized title is text and therefore still drawn by the
     * caller on top.
     */
    public static void drawPageBackground(GuiGraphics graphics, int left, int top, int width, int height) {
        blitWhole(graphics, BOND_PAGE, left, top, width, height, PAGE_TEXTURE_WIDTH, PAGE_TEXTURE_HEIGHT);
    }

    /**
     * Paints one ability row plate, replacing the old flat {@code fill} of the same rect. The shipped
     * artwork is exactly the 158x23 row box, so it lands 1:1.
     */
    public static void drawRowPlate(GuiGraphics graphics, int left, int top, int right, int bottom) {
        blitWhole(graphics, BOND_ROW, left, top, right - left, bottom - top, ROW_TEXTURE_WIDTH, ROW_TEXTURE_HEIGHT);
    }

    /**
     * Paints the secondary-page modal frame - outer frame, body and the header separator - as a
     * nine-patch, so every modal size ({@link BondGuiTokens#SECONDARY_MODAL_WIDTH} x
     * {@link BondGuiTokens#SECONDARY_MODAL_HEIGHT} for the split pages, the slightly smaller voice
     * pages, and any future size) reuses one texture. The dimming overlay and the title text stay in
     * {@link BondModalPage}, since both are code concerns (a translucent fill and a localized string).
     */
    public static void drawModalChrome(GuiGraphics graphics, int left, int top, int width, int height) {
        drawNinePatch(graphics, BOND_MODAL, left, top, width, height,
                MODAL_SLICE_SIDE, MODAL_SLICE_TOP, MODAL_SLICE_SIDE, MODAL_SLICE_BOTTOM,
                MODAL_TEXTURE_WIDTH, MODAL_TEXTURE_HEIGHT);
    }

    /**
     * Paints a recessed list/grid plate inside a secondary page, replacing the old
     * {@code drawFramedPanel(..., COLOR_BG_ELEMENT)} fills. Nine-patched, so the voice-pool lists,
     * the rescue-action list, the lap-pillow panel and the pose grid all share one texture.
     */
    public static void drawInsetPanel(GuiGraphics graphics, int left, int top, int right, int bottom) {
        drawNinePatch(graphics, BOND_INSET, left, top, right - left, bottom - top,
                INSET_SLICE, INSET_SLICE, INSET_SLICE, INSET_SLICE,
                INSET_TEXTURE_SIZE, INSET_TEXTURE_SIZE);
    }

    private static void drawNinePatch(GuiGraphics graphics, ResourceLocation texture,
                                      int left, int top, int width, int height,
                                      int sliceLeft, int sliceTop, int sliceRight, int sliceBottom,
                                      int textureWidth, int textureHeight) {
        for (BondNinePatch.Slice slice : BondNinePatch.slices(
                left, top, width, height,
                sliceLeft, sliceTop, sliceRight, sliceBottom,
                textureWidth, textureHeight, TEXTURE_SCALE)) {
            beginTranslucentPass();
            graphics.blit(
                    texture,
                    slice.x(), slice.y(), slice.width(), slice.height(),
                    slice.u(), slice.v(), slice.uWidth(), slice.vHeight(),
                    textureWidth, textureHeight
            );
        }
    }

    private static void blitWhole(GuiGraphics graphics, ResourceLocation texture,
                                  int left, int top, int width, int height,
                                  int textureWidth, int textureHeight) {
        if (width <= 0 || height <= 0) {
            return;
        }
        beginTranslucentPass();
        graphics.blit(texture, left, top, width, height,
                0.0F, 0.0F, textureWidth, textureHeight, textureWidth, textureHeight);
    }

    /**
     * A texture blit honours whatever blend state happens to be current, and finishing a
     * {@code GuiGraphics} render-type batch - the fills the ability buttons are drawn with, just
     * above the rows - clears that state. Without re-arming it here the translucent ability-row
     * plate (black at 80/255 alpha) is written straight to the framebuffer as opaque black.
     */
    private static void beginTranslucentPass() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }
}
