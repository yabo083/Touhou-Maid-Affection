package com.github.touhoumaidaffection.client.screen;

import com.github.touhoumaidaffection.ModConfig;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.github.touhoumaidaffection.bond.settings.TmaSettingsKeys;
import com.github.touhoumaidaffection.client.TmaSettingsClientState;
import com.github.touhoumaidaffection.client.screen.component.BondDropdown;
import com.github.touhoumaidaffection.client.screen.component.BondGuiTokens;
import com.github.touhoumaidaffection.client.screen.component.BondModalPage;
import com.github.touhoumaidaffection.client.screen.component.BondSlider;
import com.github.touhoumaidaffection.util.SoundVolumeSettings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Global (maid independent) settings panel, opened as a standalone {@link Screen} on top of the
 * maid GUI.
 *
 * <p>It is deliberately not a {@code BondSecondaryPage}: it is entered from the maid GUI's
 * "settings" button via {@link net.minecraft.client.Minecraft#setScreen(Screen)} and closing it
 * (footer "done", ESC or a click on the dimmed area) restores the parent screen it was opened from.
 * Layout follows the reviewed mockup: a 300x188 modal with a 46px navigation rail (features / voice
 * / volume) and one section per tab. Feature switches and languages are server-authoritative and go
 * through the settings channel, while the volume sliders are pure client preferences written
 * straight into the local config. Every control applies instantly; the rail bottom hosts a
 * decorative rose vine whose stem base rests on the panel's bottom border (fully inside the panel,
 * so it is never clipped by the physical bottom of the screen). It never accepts mouse input.
 *
 * <p>Per-row status dots are derived without any protocol change: a request recorded in
 * {@link #pending} is resolved when the next authoritative state push arrives - a matching value
 * means "saved" (no dot), a differing value means "rejected" (red dot for a few seconds), and a
 * request that never comes back within {@link #PENDING_TIMEOUT_MILLIS} is treated as rejected.
 */
public final class TmaSettingsScreen extends Screen {
    // ---- Modal geometry ----
    private static final int MODAL_WIDTH = BondGuiTokens.SETTINGS_MODAL_WIDTH;
    private static final int MODAL_HEIGHT = BondGuiTokens.SETTINGS_MODAL_HEIGHT;

    // ---- Navigation rail ----
    private static final int NAV_WIDTH = 46;
    private static final int NAV_PADDING_Y = 8;
    private static final int NAV_TAB_HEIGHT = 20;
    private static final int NAV_TAB_GAP = 1;
    private static final int NAV_TAB_TEXT_LEFT = 8;
    private static final int NAV_SELECTED_BAR_WIDTH = 2;
    private static final int NAV_HOVER_BG = 0x14FFFFFF;
    /** On-screen size the vine is drawn at; the source texture is {@code NAV_VINE_TEXTURE_*}. */
    private static final int NAV_VINE_WIDTH = 44;
    private static final int NAV_VINE_HEIGHT = 55;
    /** High resolution source texture (132x165), blitted down to {@link #NAV_VINE_WIDTH} x {@link #NAV_VINE_HEIGHT}. */
    private static final int NAV_VINE_TEXTURE_WIDTH = 132;
    private static final int NAV_VINE_TEXTURE_HEIGHT = 165;

    // ---- Content layout ----
    private static final int CONTENT_PADDING = 8;
    private static final int SECTION_HEADER_HEIGHT = 15;
    private static final int ROW_GAP = 6;
    private static final int TOGGLE_ROW_HEIGHT = 18;
    private static final int LANGUAGE_ROW_HEIGHT = 20;
    private static final int SLIDER_ROW_HEIGHT = 18;

    // ---- Control geometry ----
    private static final int TOGGLE_WIDTH = 26;
    private static final int TOGGLE_HEIGHT = 13;
    private static final int TOGGLE_KNOB_SIZE = 9;
    private static final int TOGGLE_KNOB_ON_OFFSET = 14;
    private static final int DROPDOWN_WIDTH = 78;
    private static final int DROPDOWN_HEADER_HEIGHT = 20;
    private static final int DROPDOWN_ROW_HEIGHT = 12;
    private static final int DROPDOWN_MAX_VISIBLE_ROWS = 4;
    private static final int SLIDER_WIDTH = 88;
    private static final int SLIDER_HEIGHT = 13;
    private static final int STATUS_DOT_SIZE = 5;
    private static final int STATUS_DOT_GAP = 2;
    private static final int LABEL_CONTROL_GAP = 4;
    private static final int SCROLLBAR_WIDTH = 2;
    private static final int SCROLL_STEP = 10;

    // ---- Footer ----
    private static final int FOOTER_BUTTON_HEIGHT = 17;
    private static final int FOOTER_PADDING = 8;
    private static final int FOOTER_GAP = 4;
    private static final int TEXT_BUTTON_PADDING = 4;

    // ---- Status machine timing ----
    /** A request that gets no authoritative answer within this window is treated as rejected. */
    private static final long PENDING_TIMEOUT_MILLIS = 5000L;
    /** How long a rejected key keeps its red dot before the marker expires. */
    private static final long REJECTED_LINGER_MILLIS = 3000L;
    /** Blink period of the "request in flight" dot. */
    private static final long PENDING_BLINK_MILLIS = 500L;

    // ---- Values ----
    private static final String LANGUAGE_AUTO = "auto";
    private static final List<String> COMMON_LANGUAGES = List.of("zh_cn", "en_us", "ja_jp", "zh_tw", "ko_kr");
    private static final double VOLUME_STEP = 0.05D;

    private static final ResourceLocation ROSE_VINE =
            ResourceLocation.fromNamespaceAndPath(TouhouMaidAffection.MOD_ID, "textures/gui/rose_vine.png");

    private static final List<String> TOGGLE_KEYS = TmaSettingsKeys.keys().stream()
            .filter(key -> TmaSettingsKeys.typeOf(key) == TmaSettingsKeys.Type.BOOLEAN)
            .toList();
    private static final List<String> LANGUAGE_KEYS = TmaSettingsKeys.keys().stream()
            .filter(key -> TmaSettingsKeys.typeOf(key) == TmaSettingsKeys.Type.LANGUAGE)
            .toList();

    /** Client-only volume preferences; they never travel over the settings channel. */
    private static final List<VolumeSetting> VOLUME_SETTINGS = List.of(
            new VolumeSetting("kiss_sound", ModConfig.KISS_SOUND_VOLUME),
            new VolumeSetting("morning_kiss_voice", ModConfig.BOND_MORNING_KISS_VOICE_VOLUME),
            new VolumeSetting("emergency_rescue", ModConfig.BOND_EMERGENCY_RESCUE_VOLUME),
            new VolumeSetting("voice_preview", ModConfig.VOICE_PREVIEW_VOLUME)
    );

    private final Screen parent;
    private final Runnable refreshListener = this::refreshFromState;

    private final List<ToggleRow> toggles = new ArrayList<>();
    private final List<LanguageRow> languages = new ArrayList<>();
    private final List<SliderRow> sliders = new ArrayList<>();

    /** Server keys with a request in flight: key -> requested value + send timestamp. */
    private final Map<String, PendingRequest> pending = new ConcurrentHashMap<>();
    /** Server keys whose last request was rejected (or timed out): key -> marker expiry millis. */
    private final Map<String, Long> rejectedUntil = new ConcurrentHashMap<>();

    private BondModalPage modal;
    private int activeTab;
    private int contentHeight;
    private int scrollOffset;
    private boolean layoutDirty = true;
    private boolean volumesDirty;
    private boolean released;

    public TmaSettingsScreen(Screen parent) {
        super(Component.translatable("bond.settings.title"));
        this.parent = parent;
        buildRows();
        TmaSettingsClientState.addListener(refreshListener);
        // Always re-read: the cache may still hold the state of a previous world or server.
        TmaSettingsClientState.requestSync();
    }

    @Override
    protected void init() {
        super.init();
        modal = null;
        layoutDirty = true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        // The modal chrome paints the full-screen dim overlay (page == whole screen), so the vanilla
        // menu/blur background is intentionally skipped to keep a single dim layer.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        Font font = this.font;
        BondModalPage modal = modal();
        modal.renderChrome(graphics, font);
        renderTitleScope(graphics, font);
        ensureLayout();
        tickState();

        int viewportTop = contentTop();
        int viewportBottom = viewportBottom();
        graphics.enableScissor(contentLeft(), viewportTop, contentRight(), viewportBottom);
        try {
            renderContent(graphics, font, mouseX, mouseY);
        } finally {
            graphics.disableScissor();
        }

        renderScrollbar(graphics, viewportTop, viewportBottom);
        renderNav(graphics, font, mouseX, mouseY);
        // Dropdown overlays are drawn after every other element and are clamped to the screen, never
        // to the panel content viewport, so an expanded list is always fully visible.
        renderDropdownOverlays(graphics, font, mouseX, mouseY);
        renderFooter(graphics, font, mouseX, mouseY);
        // Drawn last so the vine lands on top of the rail chrome it decorates.
        renderVine(graphics);

        List<Component> tooltip = getTooltip(mouseX, mouseY);
        if (!tooltip.isEmpty()) {
            graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return true;
        }
        BondModalPage modal = modal();
        if (!modal.contains(mouseX, mouseY)) {
            closeToParent();
            return true;
        }
        ensureLayout();

        int tab = tabAt(mouseX, mouseY);
        if (tab >= 0) {
            for (LanguageRow row : languages) {
                row.dropdown.collapse();
            }
            if (tab != activeTab) {
                activeTab = tab;
                scrollOffset = 0;
                layoutDirty = true;
            }
            return true;
        }
        if (handleExpandedDropdown(mouseX, mouseY)) {
            return true;
        }
        if (handleFooterClick(mouseX, mouseY)) {
            return true;
        }
        if (!isInsideViewport(mouseX, mouseY)) {
            return true;
        }
        clickActiveTabRow(mouseX, mouseY);
        return true;
    }

    private void clickActiveTabRow(double mouseX, double mouseY) {
        switch (activeTab) {
            case 0 -> clickToggle(mouseX, mouseY);
            case 1 -> clickLanguage(mouseX, mouseY);
            default -> clickSlider(mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        for (SliderRow row : sliders) {
            if (row.slider.mouseReleased()) {
                saveVolumes();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button != 0) {
            return false;
        }
        for (SliderRow row : sliders) {
            if (row.slider.mouseDragged(mouseX, mouseY)) {
                applyVolume(row);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        ensureLayout();
        for (LanguageRow row : languages) {
            if (row.dropdown.mouseScrolled(mouseX, mouseY, scrollY, row.options.size())) {
                return true;
            }
        }
        if (!isInsideViewport(mouseX, mouseY) || maxScroll() <= 0 || scrollY == 0.0D) {
            return false;
        }
        int delta = scrollY > 0.0D ? -SCROLL_STEP : SCROLL_STEP;
        scrollOffset = Math.max(0, Math.min(maxScroll(), scrollOffset + delta));
        layoutDirty = true;
        return true;
    }

    @Override
    public void onClose() {
        closeToParent();
    }

    @Override
    public void removed() {
        release();
    }

    /** Releases every listener/resource this screen owns. Safe to call more than once. */
    private void release() {
        if (released) {
            return;
        }
        released = true;
        TmaSettingsClientState.removeListener(refreshListener);
        for (SliderRow row : sliders) {
            row.slider.mouseReleased();
        }
        saveVolumes();
    }

    /** Restores the screen that opened this panel (the maid GUI). */
    private void closeToParent() {
        release();
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    private List<Component> getTooltip(int mouseX, int mouseY) {
        ensureLayout();
        FooterLayout footer = footerLayout(this.font);
        if (hasActiveChanges()
                && within(mouseX, mouseY, footer.reloadLeft(), footer.reloadWidth(), footer.top(), FOOTER_BUTTON_HEIGHT)) {
            return List.of(Component.translatable("bond.settings.button.reload.tip"));
        }
        if (!isInsideViewport(mouseX, mouseY)) {
            return List.of();
        }
        switch (activeTab) {
            case 0 -> {
                for (ToggleRow row : toggles) {
                    if (containsToggle(row, mouseX, mouseY)) {
                        return rowTooltip(row.key, Component.translatable(currentToggleValue(row.key)
                                ? "bond.settings.toggle.tip.off"
                                : "bond.settings.toggle.tip.on").withStyle(ChatFormatting.GRAY));
                    }
                }
            }
            case 1 -> {
                for (LanguageRow row : languages) {
                    if (row.dropdown.contains(mouseX, mouseY, row.options.size())) {
                        return rowTooltip(row.key, Component.translatable("bond.settings.language.tip").withStyle(ChatFormatting.GRAY));
                    }
                }
            }
            default -> {
                for (SliderRow row : sliders) {
                    if (row.slider.contains(mouseX, mouseY)) {
                        return List.of(
                                Component.translatable(volumeLabelKey(row.setting.id())),
                                Component.translatable("bond.settings.volume.tip").withStyle(ChatFormatting.GRAY)
                        );
                    }
                }
            }
        }
        return List.of();
    }

    // ---- State wiring ----

    private void refreshFromState() {
        resolvePending();
        layoutDirty = true;
    }

    /** Compares every in-flight request against the freshly pushed authoritative values. */
    private void resolvePending() {
        if (pending.isEmpty()) {
            return;
        }
        long now = now();
        for (Map.Entry<String, PendingRequest> entry : pending.entrySet()) {
            PendingRequest request = entry.getValue();
            String pushed = TmaSettingsClientState.getValue(entry.getKey());
            if (request.value().equals(pushed)) {
                pending.remove(entry.getKey(), request);
            } else {
                pending.remove(entry.getKey(), request);
                rejectedUntil.put(entry.getKey(), now + REJECTED_LINGER_MILLIS);
            }
        }
    }

    /** Expires in-flight requests that never got an answer and drops stale rejection markers. */
    private void tickState() {
        long now = now();
        for (Map.Entry<String, PendingRequest> entry : pending.entrySet()) {
            if (now - entry.getValue().sentAt() >= PENDING_TIMEOUT_MILLIS) {
                if (pending.remove(entry.getKey(), entry.getValue())) {
                    rejectedUntil.put(entry.getKey(), now + REJECTED_LINGER_MILLIS);
                }
            }
        }
        rejectedUntil.entrySet().removeIf(entry -> now >= entry.getValue());
    }

    private void markPending(String key, String value) {
        pending.put(key, new PendingRequest(value, now()));
        rejectedUntil.remove(key);
    }

    private boolean hasActiveChanges() {
        return !pending.isEmpty() || !rejectedUntil.isEmpty();
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    private void buildRows() {
        int y = SECTION_HEADER_HEIGHT;
        for (String key : TOGGLE_KEYS) {
            toggles.add(new ToggleRow(key, y));
            y += TOGGLE_ROW_HEIGHT + ROW_GAP;
        }
        y = SECTION_HEADER_HEIGHT;
        for (String key : LANGUAGE_KEYS) {
            languages.add(new LanguageRow(key, new BondDropdown<>(
                    0,
                    0,
                    DROPDOWN_WIDTH,
                    DROPDOWN_HEADER_HEIGHT,
                    DROPDOWN_ROW_HEIGHT,
                    DROPDOWN_MAX_VISIBLE_ROWS
            ), y));
            y += LANGUAGE_ROW_HEIGHT + ROW_GAP;
        }
        y = SECTION_HEADER_HEIGHT;
        for (VolumeSetting setting : VOLUME_SETTINGS) {
            sliders.add(new SliderRow(setting, new BondSlider(
                    0,
                    0,
                    SLIDER_WIDTH,
                    SLIDER_HEIGHT,
                    SoundVolumeSettings.MIN_VOLUME,
                    SoundVolumeSettings.MAX_VOLUME,
                    VOLUME_STEP,
                    setting.configValue().get()
            ), y));
            y += SLIDER_ROW_HEIGHT + ROW_GAP;
        }
    }

    /** Recomputes absolute positions and the active tab height after scrolling or a state push. */
    private void ensureLayout() {
        if (!layoutDirty) {
            return;
        }
        int contentRight = contentRight();
        int rowTop = contentTop() - scrollOffset;
        for (LanguageRow row : languages) {
            row.options = buildLanguageOptions(row.key);
            row.selectedIndex = Math.max(0, row.options.indexOf(TmaSettingsClientState.getValue(row.key)));
            row.x = contentRight - DROPDOWN_WIDTH;
            int dropdownTop = rowTop + row.y;
            row.dropdown.setPosition(row.x, dropdownTop);
            // Flip the expanded list above the header when it would spill past the bottom of the
            // screen, so every option stays reachable (scrolling is preserved either way).
            int listHeight = row.dropdown.overlayHeight(row.options.size());
            boolean flipAbove = dropdownTop + DROPDOWN_HEADER_HEIGHT + listHeight > height
                    && dropdownTop - listHeight >= 0;
            row.dropdown.setOverlayAbove(flipAbove);
        }
        for (SliderRow row : sliders) {
            row.slider.setPosition(contentRight - SLIDER_WIDTH, rowTop + row.y);
        }
        contentHeight = contentHeight(activeTab);
        layoutDirty = false;
    }

    private int contentHeight(int tab) {
        return switch (tab) {
            case 0 -> SECTION_HEADER_HEIGHT + toggles.size() * (TOGGLE_ROW_HEIGHT + ROW_GAP);
            case 1 -> SECTION_HEADER_HEIGHT + languages.size() * (LANGUAGE_ROW_HEIGHT + ROW_GAP);
            default -> SECTION_HEADER_HEIGHT + sliders.size() * (SLIDER_ROW_HEIGHT + ROW_GAP);
        };
    }

    private boolean currentToggleValue(String key) {
        return TmaSettingsClientState.getBoolean(key, false);
    }

    private List<String> buildLanguageOptions(String key) {
        String current = TmaSettingsClientState.getValue(key);
        List<String> options = new ArrayList<>(COMMON_LANGUAGES.size() + 2);
        options.add(LANGUAGE_AUTO);
        if (!current.isBlank() && !options.contains(current)) {
            options.add(current);
        }
        for (String language : COMMON_LANGUAGES) {
            if (!options.contains(language)) {
                options.add(language);
            }
        }
        return List.copyOf(options);
    }

    private void applyVolume(SliderRow row) {
        row.setting.configValue().set(row.slider.value());
        volumesDirty = true;
    }

    private void saveVolumes() {
        if (!volumesDirty) {
            return;
        }
        ModConfig.SPEC.save();
        volumesDirty = false;
    }

    // ---- Rendering ----

    private void renderTitleScope(GuiGraphics graphics, Font font) {
        BondModalPage modal = modal();
        Component scope = Component.translatable("bond.settings.scope.global");
        int y = modal.top() + Math.max(2, (BondGuiTokens.MODAL_TITLE_HEIGHT - font.lineHeight) / 2);
        graphics.drawString(font, scope, modal.right() - 2 - CONTENT_PADDING - font.width(scope), y,
                BondGuiTokens.COLOR_TEXT_HINT, false);
    }

    private void renderContent(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        if (!TmaSettingsClientState.hasState()) {
            graphics.drawString(
                    font,
                    Component.translatable("bond.settings.status.loading"),
                    contentLeft(),
                    contentTop(),
                    BondGuiTokens.COLOR_TEXT_HINT,
                    false
            );
            return;
        }
        int rowTop = contentTop() - scrollOffset;
        switch (activeTab) {
            case 0 -> {
                renderSectionHeader(graphics, font, rowTop, "bond.settings.section.features", BondGuiTokens.TAG_SERVER);
                for (ToggleRow row : toggles) {
                    renderToggleRow(graphics, font, row, rowTop + row.y, mouseX, mouseY);
                }
            }
            case 1 -> {
                renderSectionHeader(graphics, font, rowTop, "bond.settings.section.languages", BondGuiTokens.TAG_SERVER);
                for (LanguageRow row : languages) {
                    renderLanguageRow(graphics, font, row, rowTop + row.y, mouseX, mouseY);
                }
            }
            default -> {
                renderSectionHeader(graphics, font, rowTop, "bond.settings.section.volumes", BondGuiTokens.TAG_LOCAL);
                for (SliderRow row : sliders) {
                    renderSliderRow(graphics, font, row, rowTop + row.y, mouseX, mouseY);
                }
            }
        }
    }

    private void renderSectionHeader(GuiGraphics graphics, Font font, int y, String labelKey, int tagColor) {
        int left = contentLeft();
        int right = contentRight();
        graphics.drawString(font, Component.translatable(labelKey), left, y, BondGuiTokens.COLOR_TEXT_HINT, false);
        Component tag = Component.translatable(tagColor == BondGuiTokens.TAG_LOCAL
                ? "bond.settings.tag.local"
                : "bond.settings.tag.server");
        graphics.drawString(font, tag, right - font.width(tag), y, tagColor, false);
        int lineY = y + font.lineHeight + 2;
        graphics.hLine(left, right - 1, lineY, BondGuiTokens.DIVIDER_COLOR);
    }

    private void renderToggleRow(GuiGraphics graphics, Font font, ToggleRow row, int rowTop, int mouseX, int mouseY) {
        int controlLeft = contentRight() - TOGGLE_WIDTH;
        int controlTop = rowTop + (TOGGLE_ROW_HEIGHT - TOGGLE_HEIGHT) / 2;
        int labelRight = controlLeft - STATUS_DOT_SIZE - STATUS_DOT_GAP - LABEL_CONTROL_GAP;
        drawRowLabels(graphics, font, row.key, rowTop, labelRight);
        boolean enabled = TmaSettingsClientState.canEdit();
        boolean hovered = mouseX >= controlLeft && mouseX < controlLeft + TOGGLE_WIDTH
                && mouseY >= controlTop && mouseY < controlTop + TOGGLE_HEIGHT;
        drawToggle(graphics, controlLeft, controlTop, currentToggleValue(row.key), enabled, hovered);
        drawStatusDot(graphics, controlLeft - STATUS_DOT_GAP - STATUS_DOT_SIZE,
                rowTop + (TOGGLE_ROW_HEIGHT - STATUS_DOT_SIZE) / 2, row.key);
    }

    private void renderLanguageRow(GuiGraphics graphics, Font font, LanguageRow row, int rowTop, int mouseX, int mouseY) {
        int labelRight = row.x - STATUS_DOT_SIZE - STATUS_DOT_GAP - LABEL_CONTROL_GAP;
        drawRowLabels(graphics, font, row.key, rowTop, labelRight);
        drawDropdownHeader(graphics, font, row, rowTop, mouseX, mouseY);
        drawStatusDot(graphics, row.x - STATUS_DOT_GAP - STATUS_DOT_SIZE,
                rowTop + (LANGUAGE_ROW_HEIGHT - STATUS_DOT_SIZE) / 2, row.key);
    }

    private void renderSliderRow(GuiGraphics graphics, Font font, SliderRow row, int rowTop, int mouseX, int mouseY) {
        int sliderLeft = contentRight() - SLIDER_WIDTH;
        int labelRight = sliderLeft - STATUS_DOT_SIZE - STATUS_DOT_GAP - LABEL_CONTROL_GAP;
        int maxWidth = Math.max(0, labelRight - contentLeft());
        Component label = Component.translatable(volumeLabelKey(row.setting.id()));
        graphics.drawString(font, font.plainSubstrByWidth(label.getString(), maxWidth),
                contentLeft(), rowTop, BondGuiTokens.COLOR_TEXT_BODY, false);
        row.slider.render(graphics, font, mouseX, mouseY);
        drawStatusDot(graphics, sliderLeft - STATUS_DOT_GAP - STATUS_DOT_SIZE,
                rowTop + (SLIDER_ROW_HEIGHT - STATUS_DOT_SIZE) / 2, row.setting.id());
    }

    private void drawRowLabels(GuiGraphics graphics, Font font, String key, int rowTop, int labelRight) {
        int maxWidth = Math.max(0, labelRight - contentLeft());
        Component label = Component.translatable(TmaSettingsKeys.labelKey(key));
        graphics.drawString(font, font.plainSubstrByWidth(label.getString(), maxWidth),
                contentLeft(), rowTop, BondGuiTokens.COLOR_TEXT_BODY, false);
        drawRowSubtitle(graphics, font, TmaSettingsKeys.subKey(key), rowTop + font.lineHeight, labelRight);
    }

    private void drawRowSubtitle(GuiGraphics graphics, Font font, String subKey, int y, int labelRight) {
        int maxWidth = Math.max(0, labelRight - contentLeft());
        if (maxWidth <= 0) {
            return;
        }
        Component sub = Component.translatable(subKey);
        graphics.drawString(font, font.plainSubstrByWidth(sub.getString(), maxWidth),
                contentLeft(), y, BondGuiTokens.COLOR_TEXT_HINT, false);
    }

    private void drawToggle(GuiGraphics graphics, int left, int top, boolean on, boolean enabled, boolean hovered) {
        int background = !enabled
                ? BondGuiTokens.TOGGLE_DISABLED_BG
                : on ? BondGuiTokens.TOGGLE_ON_TRACK : BondGuiTokens.TOGGLE_TRACK;
        int border = !enabled
                ? BondGuiTokens.TOGGLE_DISABLED_BORDER
                : on ? BondGuiTokens.TOGGLE_ON_BORDER
                : hovered ? BondGuiTokens.FIELD_BORDER_HOVER : BondGuiTokens.TOGGLE_TRACK_BORDER;
        int knob = !enabled
                ? BondGuiTokens.TOGGLE_DISABLED_KNOB
                : on ? BondGuiTokens.TOGGLE_ON_KNOB : BondGuiTokens.TOGGLE_KNOB;
        int right = left + TOGGLE_WIDTH;
        int bottom = top + TOGGLE_HEIGHT;
        graphics.fill(left, top, right, bottom, border);
        graphics.fill(left + 1, top + 1, right - 1, bottom - 1, background);
        int knobLeft = left + 1 + (on ? TOGGLE_KNOB_ON_OFFSET : 0);
        graphics.fill(knobLeft, top + 1, knobLeft + TOGGLE_KNOB_SIZE, top + 1 + TOGGLE_KNOB_SIZE, knob);
    }

    private void drawDropdownHeader(GuiGraphics graphics, Font font, LanguageRow row, int rowTop, int mouseX, int mouseY) {
        int left = row.x;
        int right = left + DROPDOWN_WIDTH;
        int bottom = rowTop + DROPDOWN_HEADER_HEIGHT;
        boolean expanded = row.dropdown.isExpanded();
        boolean hovered = mouseX >= left && mouseX < right && mouseY >= rowTop && mouseY < bottom;
        boolean enabled = TmaSettingsClientState.canEdit();
        int background;
        int border;
        int textColor;
        if (!enabled) {
            background = BondGuiTokens.TOGGLE_DISABLED_BG;
            border = BondGuiTokens.TOGGLE_DISABLED_BORDER;
            textColor = BondGuiTokens.COLOR_TEXT_DISABLED;
        } else if (expanded) {
            background = BondGuiTokens.TOGGLE_ON_TRACK;
            border = BondGuiTokens.TOGGLE_ON_BORDER;
            textColor = BondGuiTokens.TOGGLE_ON_KNOB;
        } else {
            background = hovered ? BondGuiTokens.FIELD_HOVER : BondGuiTokens.FIELD_BG;
            border = hovered ? BondGuiTokens.FIELD_BORDER_HOVER : BondGuiTokens.FIELD_BORDER;
            textColor = BondGuiTokens.COLOR_TEXT_BODY;
        }
        graphics.fill(left, rowTop, right, bottom, border);
        graphics.fill(left + 1, rowTop + 1, right - 1, bottom - 1, background);

        String value = row.selectedIndex >= 0 && row.selectedIndex < row.options.size()
                ? row.options.get(row.selectedIndex)
                : "";
        int textY = rowTop + Math.max(1, (DROPDOWN_HEADER_HEIGHT - font.lineHeight) / 2);
        graphics.drawString(font, font.plainSubstrByWidth(value, DROPDOWN_WIDTH - 16), left + 5, textY, textColor, false);
        graphics.drawString(font, expanded ? "▲" : "▼", right - 10, textY,
                expanded ? BondGuiTokens.TOGGLE_ON_KNOB : BondGuiTokens.COLOR_TEXT_HINT, false);
    }

    private void drawStatusDot(GuiGraphics graphics, int x, int y, String key) {
        long now = now();
        Long rejected = rejectedUntil.get(key);
        if (rejected != null && now < rejected) {
            graphics.fill(x, y, x + STATUS_DOT_SIZE, y + STATUS_DOT_SIZE, BondGuiTokens.COLOR_ERROR);
            return;
        }
        if (pending.containsKey(key) && (now / PENDING_BLINK_MILLIS) % 2L == 0L) {
            graphics.fill(x, y, x + STATUS_DOT_SIZE, y + STATUS_DOT_SIZE, BondGuiTokens.COLOR_TEXT_HINT);
        }
    }

    private void renderNav(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        int navLeft = navLeft();
        int navRight = navRight();
        int navTop = navTop();
        int navBottom = navBottom();
        graphics.vLine(navRight, navTop, navBottom - 1, BondGuiTokens.DIVIDER_COLOR);

        int tabTop = navTop + NAV_PADDING_Y;
        for (int index = 0; index < 3; index++) {
            int y = tabTop + index * (NAV_TAB_HEIGHT + NAV_TAB_GAP);
            boolean selected = index == activeTab;
            boolean hovered = mouseX >= navLeft && mouseX < navRight && mouseY >= y && mouseY < y + NAV_TAB_HEIGHT;
            if (selected) {
                graphics.fill(navLeft, y, navRight, y + NAV_TAB_HEIGHT, BondGuiTokens.NAV_SELECTED_BG);
                graphics.fill(navLeft, y, navLeft + NAV_SELECTED_BAR_WIDTH, y + NAV_TAB_HEIGHT, BondGuiTokens.COLOR_ACCENT);
            } else if (hovered) {
                graphics.fill(navLeft, y, navRight, y + NAV_TAB_HEIGHT, NAV_HOVER_BG);
            }
            int color = selected ? BondGuiTokens.NAV_SELECTED_TEXT
                    : hovered ? BondGuiTokens.COLOR_TEXT_BODY : BondGuiTokens.COLOR_TEXT_HINT;
            graphics.drawString(font, tabLabel(index), navLeft + NAV_TAB_TEXT_LEFT,
                    y + (NAV_TAB_HEIGHT - font.lineHeight) / 2, color, false);
        }
    }

    /**
     * Decorative rose vine anchored to the panel's bottom edge: its stem base rests on the bottom
     * border and it is horizontally centred inside the navigation rail
     * ({@link #NAV_WIDTH} - {@link #NAV_VINE_WIDTH} = 2px, 1px per side).
     *
     * <p>Anchoring the top edge to the footer button instead would push the stem below the panel
     * (the button's top edge is only {@code MODAL_FOOTER_HEIGHT - (MODAL_FOOTER_HEIGHT -
     * FOOTER_BUTTON_HEIGHT) / 2} px above the bottom border, while the vine is
     * {@link #NAV_VINE_HEIGHT} px tall), which reads as a decoration escaping the frame. Keeping the
     * whole vine inside also means it can never be clipped by the physical bottom of the screen at
     * small GUI heights.
     *
     * <p>The high resolution source texture ({@link #NAV_VINE_TEXTURE_WIDTH} x
     * {@link #NAV_VINE_TEXTURE_HEIGHT}) is blitted down to {@link #NAV_VINE_WIDTH} x
     * {@link #NAV_VINE_HEIGHT}, i.e. 1:1 at GUI scale 3, with no colour quantisation. The vine never
     * receives mouse input.
     */
    private void renderVine(GuiGraphics graphics) {
        int vineLeft = navLeft() + (NAV_WIDTH - NAV_VINE_WIDTH) / 2;
        int vineTop = modal().bottom() - NAV_VINE_HEIGHT;
        graphics.blit(ROSE_VINE, vineLeft, vineTop, NAV_VINE_WIDTH, NAV_VINE_HEIGHT,
                0, 0, NAV_VINE_TEXTURE_WIDTH, NAV_VINE_TEXTURE_HEIGHT,
                NAV_VINE_TEXTURE_WIDTH, NAV_VINE_TEXTURE_HEIGHT);
    }

    private void renderScrollbar(GuiGraphics graphics, int viewportTop, int viewportBottom) {
        int maxScroll = maxScroll();
        if (maxScroll <= 0) {
            return;
        }
        BondModalPage modal = modal();
        int trackRight = modal.right() - 2;
        int trackLeft = trackRight - SCROLLBAR_WIDTH;
        graphics.fill(trackLeft, viewportTop, trackRight, viewportBottom, BondGuiTokens.COLOR_BG_ELEMENT);
        int viewportHeight = viewportBottom - viewportTop;
        int thumbHeight = Math.max(6, viewportHeight * viewportHeight / contentHeight);
        int thumbTop = viewportTop + (viewportHeight - thumbHeight) * scrollOffset / maxScroll;
        graphics.fill(trackLeft, thumbTop, trackRight, thumbTop + thumbHeight, BondGuiTokens.COLOR_ACCENT);
    }

    private void renderFooter(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        FooterLayout layout = footerLayout(font);
        renderFooterStatus(graphics, font, layout);
        boolean hovered = mouseX >= layout.doneLeft() && mouseX < layout.doneLeft() + layout.doneWidth()
                && mouseY >= layout.top() && mouseY < layout.top() + FOOTER_BUTTON_HEIGHT;
        graphics.fill(
                layout.doneLeft(),
                layout.top(),
                layout.doneLeft() + layout.doneWidth(),
                layout.top() + FOOTER_BUTTON_HEIGHT,
                hovered ? BondGuiTokens.PRIMARY_BUTTON_HOVER_BG : BondGuiTokens.PRIMARY_BUTTON_BG
        );
        Component done = Component.translatable("bond.settings.button.done");
        graphics.drawCenteredString(
                font,
                done,
                layout.doneLeft() + layout.doneWidth() / 2,
                layout.top() + (FOOTER_BUTTON_HEIGHT - font.lineHeight) / 2,
                BondGuiTokens.COLOR_TEXT_TITLE
        );

        if (!hasActiveChanges()) {
            return;
        }
        boolean reloadHovered = mouseX >= layout.reloadLeft() && mouseX < layout.reloadLeft() + layout.reloadWidth()
                && mouseY >= layout.top() && mouseY < layout.top() + FOOTER_BUTTON_HEIGHT;
        if (reloadHovered) {
            graphics.fill(layout.reloadLeft(), layout.top(), layout.reloadLeft() + layout.reloadWidth(),
                    layout.top() + FOOTER_BUTTON_HEIGHT, BondGuiTokens.HOVER_OVERLAY);
        }
        graphics.drawString(
                font,
                Component.translatable("bond.settings.button.reload"),
                layout.reloadLeft() + TEXT_BUTTON_PADDING,
                layout.top() + (FOOTER_BUTTON_HEIGHT - font.lineHeight) / 2,
                BondGuiTokens.COLOR_TEXT_BODY,
                false
        );
    }

    private void renderFooterStatus(GuiGraphics graphics, Font font, FooterLayout layout) {
        Component hint;
        int color;
        if (!TmaSettingsClientState.hasState()) {
            hint = Component.translatable("bond.settings.status.loading");
            color = BondGuiTokens.COLOR_TEXT_HINT;
        } else if (!TmaSettingsClientState.canEdit()) {
            hint = Component.translatable("bond.settings.status.readonly");
            color = BondGuiTokens.COLOR_WARNING;
        } else {
            return;
        }
        int maxWidth = Math.max(0, layout.reloadLeft() - CONTENT_PADDING - contentLeft());
        if (maxWidth <= 0) {
            return;
        }
        graphics.drawString(
                font,
                font.plainSubstrByWidth(hint.getString(), maxWidth),
                contentLeft(),
                layout.top() + (FOOTER_BUTTON_HEIGHT - font.lineHeight) / 2,
                color,
                false
        );
    }

    private void renderDropdownOverlays(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        for (LanguageRow row : languages) {
            if (!row.dropdown.isExpanded()) {
                continue;
            }
            // No panel-content scissor here: BondDropdown clamps/flips the list inside the screen so
            // every entry stays visible and the hit test matches the drawn position.
            row.dropdown.renderOverlay(graphics, font, row.options, row.selectedIndex, mouseX, mouseY,
                    TmaSettingsScreen::renderDropdownEntry);
        }
    }

    private static void renderDropdownEntry(GuiGraphics graphics, Font font, String item, int index,
                                            int left, int top, int right, int height, boolean hovered, boolean selectedHeader) {
        int color = hovered && !selectedHeader ? BondGuiTokens.COLOR_TEXT_TITLE : BondGuiTokens.COLOR_TEXT_BODY;
        graphics.drawString(font, item, left, top, color, false);
    }

    // ---- Interaction ----

    private boolean handleExpandedDropdown(double mouseX, double mouseY) {
        for (LanguageRow row : languages) {
            if (!row.dropdown.isExpanded()) {
                continue;
            }
            BondDropdown.ClickResult result = row.dropdown.mouseClicked(mouseX, mouseY, row.options.size());
            if (result.handled() && result.selectedIndex() >= 0 && TmaSettingsClientState.canEdit()) {
                applyServerValue(row.key, row.options.get(result.selectedIndex()));
            }
            return result.handled();
        }
        return false;
    }

    private boolean handleFooterClick(double mouseX, double mouseY) {
        FooterLayout layout = footerLayout(this.font);
        if (mouseY < layout.top() || mouseY >= layout.top() + FOOTER_BUTTON_HEIGHT) {
            return false;
        }
        if (mouseX >= layout.doneLeft() && mouseX < layout.doneLeft() + layout.doneWidth()) {
            closeToParent();
            return true;
        }
        if (hasActiveChanges()
                && mouseX >= layout.reloadLeft() && mouseX < layout.reloadLeft() + layout.reloadWidth()) {
            reloadFromServer();
            return true;
        }
        return false;
    }

    /** Clears the local request markers and asks the server for the authoritative state again. */
    private void reloadFromServer() {
        pending.clear();
        rejectedUntil.clear();
        TmaSettingsClientState.requestSync();
    }

    private void clickToggle(double mouseX, double mouseY) {
        for (ToggleRow row : toggles) {
            if (!containsToggle(row, mouseX, mouseY)) {
                continue;
            }
            if (TmaSettingsClientState.canEdit()) {
                applyServerValue(row.key, currentToggleValue(row.key) ? "false" : "true");
            }
            return;
        }
    }

    private void clickLanguage(double mouseX, double mouseY) {
        if (!TmaSettingsClientState.canEdit()) {
            return;
        }
        for (LanguageRow row : languages) {
            BondDropdown.ClickResult result = row.dropdown.mouseClicked(mouseX, mouseY, row.options.size());
            if (!result.handled()) {
                continue;
            }
            if (result.selectedIndex() >= 0) {
                applyServerValue(row.key, row.options.get(result.selectedIndex()));
            }
            return;
        }
    }

    private void clickSlider(double mouseX, double mouseY) {
        for (SliderRow row : sliders) {
            if (row.slider.mousePressed(mouseX, mouseY)) {
                applyVolume(row);
                return;
            }
        }
    }

    private void applyServerValue(String key, String value) {
        markPending(key, value);
        TmaSettingsClientState.set(key, value);
    }

    private boolean containsToggle(ToggleRow row, double mouseX, double mouseY) {
        int left = contentRight() - TOGGLE_WIDTH;
        int top = contentTop() - scrollOffset + row.y + (TOGGLE_ROW_HEIGHT - TOGGLE_HEIGHT) / 2;
        return mouseX >= left && mouseX < contentRight()
                && mouseY >= top && mouseY < top + TOGGLE_HEIGHT;
    }

    private boolean isInsideViewport(double mouseX, double mouseY) {
        return mouseX >= contentLeft() && mouseX < contentRight()
                && mouseY >= contentTop() && mouseY < viewportBottom();
    }

    private int tabAt(double mouseX, double mouseY) {
        if (mouseX < navLeft() || mouseX >= navRight()) {
            return -1;
        }
        int tabTop = navTop() + NAV_PADDING_Y;
        for (int index = 0; index < 3; index++) {
            int y = tabTop + index * (NAV_TAB_HEIGHT + NAV_TAB_GAP);
            if (mouseY >= y && mouseY < y + NAV_TAB_HEIGHT) {
                return index;
            }
        }
        return -1;
    }

    private static boolean within(double mouseX, double mouseY, int left, int width, int top, int height) {
        return mouseX >= left && mouseX < left + width && mouseY >= top && mouseY < top + height;
    }

    private List<Component> rowTooltip(String key, Component detail) {
        List<Component> tooltip = new ArrayList<>(3);
        tooltip.add(Component.translatable(TmaSettingsKeys.labelKey(key)));
        Long rejected = rejectedUntil.get(key);
        if (rejected != null && now() < rejected) {
            tooltip.add(Component.translatable("bond.settings.status.rejected").withStyle(ChatFormatting.RED));
        } else if (pending.containsKey(key)) {
            tooltip.add(Component.translatable("bond.settings.status.pending").withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(detail);
        tooltip.add(permissionTip());
        return tooltip;
    }

    private Component permissionTip() {
        return Component.translatable(TmaSettingsClientState.canEdit()
                ? "bond.settings.permission.granted"
                : "bond.settings.permission.required").withStyle(TmaSettingsClientState.canEdit()
                ? ChatFormatting.GREEN
                : ChatFormatting.RED);
    }

    private Component tabLabel(int index) {
        return Component.translatable(switch (index) {
            case 0 -> "bond.settings.nav.features";
            case 1 -> "bond.settings.nav.voice";
            default -> "bond.settings.nav.volume";
        });
    }

    // ---- Geometry helpers ----

    private BondModalPage modal() {
        if (modal == null) {
            modal = new BondModalPage(0, 0, width, height, MODAL_WIDTH, MODAL_HEIGHT,
                    Component.translatable("bond.settings.title"));
        }
        return modal;
    }

    private int navLeft() {
        return modal().left() + 2;
    }

    private int navRight() {
        return navLeft() + NAV_WIDTH;
    }

    private int navTop() {
        return modal().top() + BondGuiTokens.MODAL_TITLE_HEIGHT;
    }

    private int navBottom() {
        return modal().footerTop();
    }

    private int contentLeft() {
        return navRight() + CONTENT_PADDING;
    }

    private int contentRight() {
        return modal().right() - 2 - CONTENT_PADDING;
    }

    private int contentTop() {
        return modal().top() + BondGuiTokens.MODAL_TITLE_HEIGHT + CONTENT_PADDING;
    }

    private int viewportBottom() {
        return modal().footerTop();
    }

    private FooterLayout footerLayout(Font font) {
        BondModalPage modal = modal();
        Component done = Component.translatable("bond.settings.button.done");
        int doneWidth = Math.max(BondGuiTokens.BUTTON_MIN_WIDTH, font.width(done) + BondGuiTokens.BUTTON_HORIZONTAL_PADDING * 2);
        int top = modal.footerTop() + (BondGuiTokens.MODAL_FOOTER_HEIGHT - FOOTER_BUTTON_HEIGHT) / 2;
        int doneLeft = modal.right() - 2 - FOOTER_PADDING - doneWidth;
        Component reload = Component.translatable("bond.settings.button.reload");
        int reloadWidth = font.width(reload) + TEXT_BUTTON_PADDING * 2;
        int reloadLeft = doneLeft - FOOTER_GAP - reloadWidth;
        return new FooterLayout(doneLeft, doneWidth, reloadLeft, reloadWidth, top);
    }

    private int maxScroll() {
        return Math.max(0, contentHeight - (viewportBottom() - contentTop()));
    }

    private static String volumeLabelKey(String id) {
        return "bond.settings.volume." + id;
    }

    private record FooterLayout(int doneLeft, int doneWidth, int reloadLeft, int reloadWidth, int top) {
    }

    private record PendingRequest(String value, long sentAt) {
    }

    private static final class ToggleRow {
        private final String key;
        private final int y;

        private ToggleRow(String key, int y) {
            this.key = key;
            this.y = y;
        }
    }

    private static final class LanguageRow {
        private final String key;
        private final BondDropdown<String> dropdown;
        private final int y;
        private List<String> options = List.of();
        private int selectedIndex;
        private int x;

        private LanguageRow(String key, BondDropdown<String> dropdown, int y) {
            this.key = key;
            this.dropdown = dropdown;
            this.y = y;
        }
    }

    private static final class SliderRow {
        private final VolumeSetting setting;
        private final BondSlider slider;
        private final int y;

        private SliderRow(VolumeSetting setting, BondSlider slider, int y) {
            this.setting = setting;
            this.slider = slider;
            this.y = y;
        }
    }

    private record VolumeSetting(String id, ForgeConfigSpec.DoubleValue configValue) {
    }
}