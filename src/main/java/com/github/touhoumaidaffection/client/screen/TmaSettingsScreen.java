package com.github.touhoumaidaffection.client.screen;

import com.github.tartaricacid.touhoulittlemaid.ai.manager.site.AvailableSites;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.settings.AIChatSettingsHubScreen;
import com.github.touhoumaidaffection.ModConfig;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.github.touhoumaidaffection.bond.settings.TmaAiStatusWire;
import com.github.touhoumaidaffection.bond.settings.TmaMaidLabels;
import com.github.touhoumaidaffection.bond.settings.TmaSettingsKeys;
import com.github.touhoumaidaffection.client.TmaAiStatusClientState;
import com.github.touhoumaidaffection.client.TmaSettingsClientState;
import com.github.touhoumaidaffection.client.screen.component.BondDropdown;
import com.github.touhoumaidaffection.client.screen.component.BondGuiTokens;
import com.github.touhoumaidaffection.client.screen.component.BondModalPage;
import com.github.touhoumaidaffection.client.screen.component.BondNumberField;
import com.github.touhoumaidaffection.client.screen.component.BondPromptBox;
import com.github.touhoumaidaffection.client.screen.component.BondSlider;
import com.github.touhoumaidaffection.util.SoundVolumeSettings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Global (maid independent) settings panel, opened as a standalone {@link Screen} on top of the
 * maid GUI.
 *
 * <p>It is deliberately not a {@code BondSecondaryPage}: it is entered from the maid GUI's
 * "settings" button via {@link net.minecraft.client.Minecraft#setScreen(Screen)} and closing it
 * (footer "done", ESC or a click on the dimmed area) restores the parent screen it was opened from.
 * Layout follows the reviewed mockup: a 340x230 modal with a 46px navigation rail (status / features
 * / voice / volume, status first and selected by default) and one section per tab. Feature switches,
 * the cache policy and the languages are server-authoritative and go through the settings channel,
 * the status tab is a read-only view fed by the AI status channel, while the volume sliders are pure
 * client preferences written straight into the local config. Every control applies instantly; the
 * rail bottom hosts a decorative rose vine whose stem base rests on the panel's bottom border
 * (fully inside the panel, so it is never clipped by the physical bottom of the screen). It never
 * accepts mouse input.
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
    /** Height of a cache-policy numeric row (label + subtitle, like a toggle row). */
    private static final int NUMBER_ROW_HEIGHT = 18;
    private static final int LANGUAGE_ROW_HEIGHT = 20;
    private static final int SLIDER_ROW_HEIGHT = 18;
    /** Number of tabs in the navigation rail. */
    private static final int TAB_COUNT = 4;

    // ---- Control geometry ----
    private static final int TOGGLE_WIDTH = 26;
    private static final int TOGGLE_HEIGHT = 13;
    private static final int TOGGLE_KNOB_SIZE = 9;
    private static final int TOGGLE_KNOB_ON_OFFSET = 14;
    private static final int DROPDOWN_WIDTH = 78;
    private static final int DROPDOWN_HEADER_HEIGHT = 17;
    private static final int DROPDOWN_ROW_HEIGHT = 12;
    private static final int DROPDOWN_MAX_VISIBLE_ROWS = 4;
    private static final int SLIDER_WIDTH = 88;
    private static final int SLIDER_HEIGHT = 13;
    private static final int STATUS_DOT_SIZE = 5;
    private static final int STATUS_DOT_GAP = 2;
    private static final int LABEL_CONTROL_GAP = 4;
    private static final int SCROLLBAR_WIDTH = 2;
    private static final int SCROLL_STEP = 10;

    // ---- Voice tab (prompt template + AI site) ----
    /**
     * Height of the dialogue editor: {@code 9 * lines + 4}, i.e. exactly five text lines plus the
     * widget's inner padding. Sizing it on that grid means the sixth line starts on the scissor
     * border instead of being sliced in half, and
     * {@link com.github.touhoumaidaffection.client.screen.component.BondPromptBox} keeps the scroll
     * offset on the same grid while the text overflows.
     */
    private static final int PROMPT_BOX_HEIGHT = 49;
    private static final int PROMPT_LABEL_BLOCK = 18;
    /** Inner padding of the vanilla multi-line editor; the counter is inset by the same amount. */
    private static final int PROMPT_INNER_PADDING = 4;
    /** Height of the full-width placeholder legend row below the prompt box. */
    private static final int PROMPT_ROW_HEIGHT = 14;
    private static final int TEXT_BUTTON_HEIGHT = 14;
    private static final int SITE_ROW_HEIGHT = 18;

    // ---- Status tab ----
    private static final int STATUS_KV_HEIGHT = 13;
    private static final int STATUS_MAID_HEIGHT = 18;
    private static final int STATUS_ROW_GAP = 6;
    private static final int STATUS_CLEAR_BUTTON_WIDTH = 26;
    /** Width of a compact cache-policy number field, matching the language dropdown column. */
    private static final int NUMBER_FIELD_WIDTH = DROPDOWN_WIDTH;
    /** Space the maid name column keeps when the pool summary would otherwise swallow the row. */
    private static final int MIN_MAID_NAME_WIDTH = 60;

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
    private static final String PROMPT_KEY = TmaSettingsKeys.MORNING_KISS_TEXT_PROMPT;
    private static final List<String> COMMON_LANGUAGES = List.of("zh_cn", "en_us", "ja_jp", "zh_tw", "ko_kr");
    private static final double VOLUME_STEP = 0.05D;

    private static final ResourceLocation ROSE_VINE =
            ResourceLocation.fromNamespaceAndPath(TouhouMaidAffection.MOD_ID, "textures/gui/rose_vine.png");

    private static final List<String> TOGGLE_KEYS = TmaSettingsKeys.keys().stream()
            .filter(key -> TmaSettingsKeys.typeOf(key) == TmaSettingsKeys.Type.BOOLEAN)
            // "Consume on use" belongs to the cache-policy section of the same tab, so it is
            // rendered there and excluded here to keep exactly one control per key.
            .filter(key -> !TmaSettingsKeys.MORNING_KISS_CACHE_CONSUME_ON_USE.equals(key))
            .toList();
    private static final List<String> LANGUAGE_KEYS = TmaSettingsKeys.keys().stream()
            .filter(key -> TmaSettingsKeys.typeOf(key) == TmaSettingsKeys.Type.LANGUAGE)
            .toList();
    /** Integer cache-policy keys of the features tab, in whitelist order. */
    private static final List<String> CACHE_NUMBER_KEYS = TmaSettingsKeys.keys().stream()
            .filter(key -> TmaSettingsKeys.typeOf(key) == TmaSettingsKeys.Type.INT)
            .toList();
    /** Unit suffix of each cache-policy integer field, e.g. {@code t} for the scan interval. */
    private static final Map<String, String> CACHE_NUMBER_UNITS = Map.of(
            TmaSettingsKeys.MORNING_KISS_CACHE_TARGET_PER_POOL, "",
            TmaSettingsKeys.MORNING_KISS_CACHE_SCAN_INTERVAL_TICKS, "t");

    /** Client-only volume preferences; they never travel over the settings channel. */
    private static final List<VolumeSetting> VOLUME_SETTINGS = List.of(
            new VolumeSetting("kiss_sound", ModConfig.KISS_SOUND_VOLUME),
            new VolumeSetting("morning_kiss_voice", ModConfig.BOND_MORNING_KISS_VOICE_VOLUME),
            new VolumeSetting("emergency_rescue", ModConfig.BOND_EMERGENCY_RESCUE_VOLUME),
            new VolumeSetting("voice_preview", ModConfig.VOICE_PREVIEW_VOLUME)
    );

    private final Screen parent;
    private final Runnable refreshListener = this::refreshFromState;
    private final Runnable statusListener = this::onStatusPushed;

    private final List<ToggleRow> toggles = new ArrayList<>();
    private final List<LanguageRow> languages = new ArrayList<>();
    private final List<SliderRow> sliders = new ArrayList<>();
    /** Status tab rows, rebuilt on every layout pass from the pushed status. */
    private final List<StatusRow> statusRows = new ArrayList<>();
    /** Cache-policy rows of the features tab, positioned once in {@link #buildRows()}. */
    private final List<NumberRow> cacheNumberRows = new ArrayList<>();
    /** Compact integer editors of the features tab, keyed by the server key. */
    private final Map<String, BondNumberField> cacheNumberFields = new HashMap<>();

    /** Server keys with a request in flight: key -> requested value + send timestamp. */
    private final Map<String, PendingRequest> pending = new ConcurrentHashMap<>();
    /** Server keys whose last request was rejected (or timed out): key -> marker expiry millis. */
    private final Map<String, Long> rejectedUntil = new ConcurrentHashMap<>();

    private BondModalPage modal;
    /** Index of the visible tab; 0 is the read-only status tab, which is selected by default. */
    private int activeTab;
    private int contentHeight;
    private int scrollOffset;
    private boolean layoutDirty = true;
    private boolean volumesDirty;
    private boolean released;

    // ---- Features tab state ----
    /** The "consume on use" switch, rendered inside the cache-policy section of the features tab. */
    private ToggleRow cacheConsumeRow;
    private int cachePolicySectionY;
    private int featuresContentHeight;

    // ---- Voice tab state ----
    private BondPromptBox promptBox;
    private int voicePromptSectionY;
    private int voicePromptLabelY;
    private int voicePromptBoxY;
    private int voicePromptRowY;
    private int voiceSiteSectionY;
    private int voiceSiteRowY;
    private int voiceContentHeight;
    /** Whether a status request is already in flight, so opening the tab does not spam the server. */
    private boolean statusRequested;

    public TmaSettingsScreen(Screen parent) {
        super(Component.translatable("bond.settings.title"));
        this.parent = parent;
        buildRows();
        TmaSettingsClientState.addListener(refreshListener);
        TmaAiStatusClientState.addListener(statusListener);
        // Always re-read: the cache may still hold the state of a previous world or server.
        TmaSettingsClientState.requestSync();
    }

    @Override
    protected void init() {
        super.init();
        modal = null;
        promptBox = null;
        layoutDirty = true;
        // The status tab is selected by default, so its read-only feed has to be requested on open.
        requestStatusIfNeeded();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
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
            blurPrompt();
            blurNumberFields();
            for (LanguageRow row : languages) {
                row.dropdown.collapse();
            }
            if (tab != activeTab) {
                activeTab = tab;
                scrollOffset = 0;
                layoutDirty = true;
                requestStatusIfNeeded();
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
            blurPrompt();
            blurNumberFields();
            return true;
        }
        clickActiveTabRow(mouseX, mouseY);
        return true;
    }

    private void clickActiveTabRow(double mouseX, double mouseY) {
        switch (activeTab) {
            case 0 -> {
                // The status tab is read-only; only the per-maid "clear" buttons accept clicks.
                blurPrompt();
                blurNumberFields();
                clickStatus(mouseX, mouseY);
            }
            case 1 -> {
                blurPrompt();
                clickFeatures(mouseX, mouseY);
            }
            case 2 -> {
                if (!clickPromptBox(mouseX, mouseY)) {
                    blurPrompt();
                    if (!clickVoiceExtras(mouseX, mouseY)) {
                        clickLanguage(mouseX, mouseY);
                    }
                }
            }
            default -> {
                blurPrompt();
                clickSlider(mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (activeTab == 2 && promptBox != null && promptBox.isFocused()
                && promptBox.keyPressed(keyCode, scanCode, modifiers)) {
            clampPromptLength();
            return true;
        }
        BondNumberField field = activeTab == 1 ? focusedNumberField() : null;
        if (field != null) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                field.blur();
                return true;
            }
            if (field.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (activeTab == 2 && promptBox != null && promptBox.isFocused()
                && promptBox.charTyped(codePoint, modifiers)) {
            clampPromptLength();
            return true;
        }
        BondNumberField field = activeTab == 1 ? focusedNumberField() : null;
        if (field != null && field.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        if (promptBox != null && promptBox.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        BondNumberField field = focusedNumberField();
        if (field != null && field.mouseReleased(mouseX, mouseY, button)) {
            return true;
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
        if (promptBox != null && promptBox.isFocused() && promptBox.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        BondNumberField field = focusedNumberField();
        if (field != null && field.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
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
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        ensureLayout();
        // A wheel event inside the dialogue editor scrolls the editor's own text and is consumed
        // here, so the panel behind it never scrolls at the same time (the two never move together).
        if (activeTab == 2 && promptBox != null && promptBox.isMouseOver(mouseX, mouseY)) {
            promptBox.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
            return true;
        }
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
        blurPrompt();
        blurNumberFields();
        TmaSettingsClientState.removeListener(refreshListener);
        TmaAiStatusClientState.removeListener(statusListener);
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
        for (FooterButton button : footer.buttons()) {
            if (within(mouseX, mouseY, button.left(), button.width(), footer.top(), FOOTER_BUTTON_HEIGHT)) {
                return List.of(Component.translatable(button.labelKey() + ".tip"));
            }
        }
        if (!isInsideViewport(mouseX, mouseY)) {
            return List.of();
        }
        switch (activeTab) {
            case 0 -> {
                // Read-only tab: the only interactive elements are the per-maid "clear" buttons.
                if (statusClearHovered(mouseX, mouseY) != null) {
                    return List.of(Component.translatable("bond.settings.status.clear.tip"));
                }
            }
            case 1 -> {
                for (ToggleRow row : toggles) {
                    if (containsToggle(row, mouseX, mouseY)) {
                        return toggleTooltip(row.key);
                    }
                }
                if (cacheConsumeRow != null && containsToggle(cacheConsumeRow, mouseX, mouseY)) {
                    return toggleTooltip(cacheConsumeRow.key);
                }
                for (NumberRow row : cacheNumberRows) {
                    BondNumberField field = cacheNumberFields.get(row.key());
                    if (field != null && field.contains(mouseX, mouseY)) {
                        return rowTooltip(row.key(), Component.translatable(TmaSettingsKeys.labelKey(row.key())),
                                Component.translatable("bond.settings.number.tip").withStyle(ChatFormatting.GRAY));
                    }
                }
            }
            case 2 -> {
                for (LanguageRow row : languages) {
                    if (row.dropdown.contains(mouseX, mouseY, row.options.size())) {
                        return rowTooltip(row.key, Component.translatable("bond.settings.language.tip").withStyle(ChatFormatting.GRAY));
                    }
                }
                if (promptBox != null && promptBox.isMouseOver(mouseX, mouseY)) {
                    return rowTooltip(PROMPT_KEY, Component.translatable("bond.settings.prompt.tip").withStyle(ChatFormatting.GRAY));
                }
                if (within(mouseX, mouseY, contentRight() - promptResetWidth(this.font), promptResetWidth(this.font),
                        contentTop() - scrollOffset + voicePromptLabelY + (PROMPT_LABEL_BLOCK - TEXT_BUTTON_HEIGHT) / 2,
                        TEXT_BUTTON_HEIGHT)) {
                    return List.of(Component.translatable("bond.settings.prompt.reset.tip"));
                }
                if (within(mouseX, mouseY, siteButtonLeft(this.font), siteButtonWidth(this.font),
                        contentTop() - scrollOffset + voiceSiteRowY, TEXT_BUTTON_HEIGHT)) {
                    return List.of(Component.translatable("bond.settings.site.open.tip"));
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

    /** Tooltip of one of the editable feature switches (capsule toggle + pending marker). */
    private List<Component> toggleTooltip(String key) {
        return rowTooltip(key, Component.translatable(currentToggleValue(key)
                ? "bond.settings.toggle.tip.off"
                : "bond.settings.toggle.tip.on").withStyle(ChatFormatting.GRAY));
    }

    // ---- State wiring ----

    private void refreshFromState() {
        resolvePending();
        layoutDirty = true;
    }

    /** Called after every AI status push; just re-lays out the status tab. */
    private void onStatusPushed() {
        statusRequested = false;
        layoutDirty = true;
    }

    /** Asks for the AI status the first time the status tab becomes active (or after a failure). */
    private void requestStatusIfNeeded() {
        if (activeTab != 0 || statusRequested) {
            return;
        }
        statusRequested = true;
        TmaAiStatusClientState.requestSync();
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
        // Features tab, cache-policy section: two integer fields plus the "consume on use" switch.
        // "Consume on use" is deliberately not part of TOGGLE_KEYS, so it exists exactly once.
        //
        // Every tab now breathes with the widened spacing constants and simply scrolls when its
        // content is taller than the viewport (MODAL_HEIGHT 230 - MODAL_TITLE_HEIGHT 24 -
        // CONTENT_PADDING 8 - MODAL_FOOTER_HEIGHT 30 = 168px of visible content).
        //   header 15 + 8 * (TOGGLE_ROW_HEIGHT 18 + ROW_GAP 6)               = 207
        // + cache header 15 + 2 * (NUMBER_ROW_HEIGHT 18 + ROW_GAP 6)         = 270
        // + consume row (TOGGLE_ROW_HEIGHT 18 + ROW_GAP 6)                   = 294
        cachePolicySectionY = y;
        y += SECTION_HEADER_HEIGHT;
        for (String key : CACHE_NUMBER_KEYS) {
            cacheNumberRows.add(new NumberRow(key, y));
            y += NUMBER_ROW_HEIGHT + ROW_GAP;
        }
        cacheConsumeRow = new ToggleRow(TmaSettingsKeys.MORNING_KISS_CACHE_CONSUME_ON_USE, y);
        y += TOGGLE_ROW_HEIGHT + ROW_GAP;
        featuresContentHeight = y;

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
        // Voice tab: prompt template editor + "open TLM AI settings" jump button.
        //   languages header 15 + 2 * (LANGUAGE_ROW_HEIGHT 20 + ROW_GAP 6)      =  67
        // + prompt header 15                                                     =  82
        // + prompt label row (PROMPT_LABEL_BLOCK 18, hosts the "restore default" button) = 100
        // + prompt box PROMPT_BOX_HEIGHT 49                                      = 149
        // + ROW_GAP 6 + legend row PROMPT_ROW_HEIGHT 14 + ROW_GAP 6              = 175
        // + site header 15 + site row SITE_ROW_HEIGHT 18                         = 208
        // The legend row spans the full content width (no button on it), so the placeholder legend
        // is never truncated.
        voicePromptSectionY = y;
        y += SECTION_HEADER_HEIGHT;
        voicePromptLabelY = y;
        y += PROMPT_LABEL_BLOCK;
        voicePromptBoxY = y;
        y += PROMPT_BOX_HEIGHT;
        // The placeholder legend owns the whole row under the editor; "restore default" sits on the
        // label row above so the legend never has to share its width.
        voicePromptRowY = y + ROW_GAP;
        y = voicePromptRowY + PROMPT_ROW_HEIGHT + ROW_GAP;
        voiceSiteSectionY = y;
        y += SECTION_HEADER_HEIGHT;
        voiceSiteRowY = y;
        y += SITE_ROW_HEIGHT;
        voiceContentHeight = y;
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
        // The status tab owns its own read-only rows, so it is laid out before the shared language
        // dropdowns of the voice tab are positioned.
        buildStatusRows();
        for (LanguageRow row : languages) {
            row.options = buildLanguageOptions(row.key);
            row.selectedIndex = Math.max(0, row.options.indexOf(
                    TmaSettingsKeys.languageForDisplay(TmaSettingsClientState.getValue(row.key))));
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
        ensurePromptBox();
        if (promptBox != null) {
            promptBox.setX(contentLeft());
            promptBox.setY(rowTop + voicePromptBoxY);
            promptBox.setWidth(Math.max(20, contentRight - contentLeft()));
            syncPromptBox();
        }
        ensureCacheNumberFields(rowTop);
        contentHeight = contentHeight(activeTab);
        layoutDirty = false;
    }

    /** Creates and positions the compact integer editors of the features tab. */
    private void ensureCacheNumberFields(int rowTop) {
        for (NumberRow row : cacheNumberRows) {
            int[] bounds = TmaSettingsKeys.intBounds(row.key());
            if (bounds == null) {
                continue;
            }
            BondNumberField field = cacheNumberFields.get(row.key());
            if (field == null) {
                field = new BondNumberField(this.font, bounds[0], bounds[1],
                        CACHE_NUMBER_UNITS.getOrDefault(row.key(), ""), currentIntValue(row.key(), bounds[0]));
                field.setResponder(value -> applyServerValue(row.key(), Integer.toString(value)));
                cacheNumberFields.put(row.key(), field);
            }
            field.setBounds(contentRight() - NUMBER_FIELD_WIDTH,
                    rowTop + row.y() + (NUMBER_ROW_HEIGHT - BondNumberField.HEIGHT) / 2,
                    NUMBER_FIELD_WIDTH);
            field.setValue(currentIntValue(row.key(), field.value()));
            field.setEditable(TmaSettingsClientState.canEdit());
        }
    }

    /** Authoritative integer value of a whitelisted key, clamped into its own bounds. */
    private int currentIntValue(String key, int fallback) {
        int[] bounds = TmaSettingsKeys.intBounds(key);
        if (bounds == null) {
            return fallback;
        }
        Optional<String> normalized = TmaSettingsKeys.normalizeInt(key, TmaSettingsClientState.getValue(key));
        if (normalized.isPresent()) {
            return Integer.parseInt(normalized.get());
        }
        TmaAiStatusWire.Status status = TmaAiStatusClientState.get();
        if (status != null) {
            int fromStatus = TmaSettingsKeys.MORNING_KISS_CACHE_TARGET_PER_POOL.equals(key)
                    ? status.cacheTargetPerPool()
                    : status.scanIntervalTicks();
            return Math.max(bounds[0], Math.min(bounds[1], fromStatus));
        }
        return fallback;
    }

    private int contentHeight(int tab) {
        return switch (tab) {
            case 0 -> Math.max(STATUS_KV_HEIGHT, statusRows.isEmpty()
                    ? STATUS_KV_HEIGHT + STATUS_ROW_GAP
                    : statusRows.get(statusRows.size() - 1).y() + statusRows.get(statusRows.size() - 1).height()
                    + STATUS_ROW_GAP);
            case 1 -> featuresContentHeight;
            case 2 -> voiceContentHeight;
            default -> SECTION_HEADER_HEIGHT + sliders.size() * (SLIDER_ROW_HEIGHT + ROW_GAP);
        };
    }

    private void buildStatusRows() {
        statusRows.clear();
        TmaAiStatusWire.Status status = TmaAiStatusClientState.get();
        if (status == null) {
            return;
        }
        int y = 0;
        y = addStatusHeader(y, "bond.settings.status.section.switches");
        y = addStatusKv(y, tr("bond.settings.status.switch.morning_kiss"), onOff(status.morningKissEnabled()));
        y = addStatusKv(y, tr("bond.settings.status.switch.ai_dialogue"), onOff(status.aiDialogueEnabled()));
        y = addStatusKv(y, tr("bond.settings.status.switch.ai_tts"), onOff(status.aiTtsEnabled()));
        y = addStatusKv(y, tr("bond.settings.status.switch.fallback"), onOff(status.immediateFallbackEnabled()));
        y = addStatusHeader(y, "bond.settings.status.section.languages");
        y = addStatusKv(y, tr("bond.settings.status.language.display"),
                languageValue(status.globalDisplayLanguage(), "bond.settings.status.language.auto.display"));
        y = addStatusKv(y, tr("bond.settings.status.language.voice"),
                languageValue(status.globalVoiceLanguage(), "bond.settings.status.language.auto.voice"));
        y = addStatusHeader(y, "bond.settings.status.section.cache_policy");
        y = addStatusKv(y, tr("bond.settings.status.cache_policy.target"),
                literal(String.valueOf(status.cacheTargetPerPool())));
        y = addStatusKv(y, tr("bond.settings.status.cache_policy.scan"),
                literal(status.scanIntervalTicks() + "t"));
        y = addStatusKv(y, tr("bond.settings.status.cache_policy.consume"), onOff(status.consumeOnUse()));
        y = addStatusHeader(y, "bond.settings.status.section.cache_stats");
        y = addStatusKv(y, tr("bond.settings.status.cache.entries"),
                literal(status.totalEntries() + " (" + status.voiceEntries() + " / " + status.textOnlyEntries() + ")"));
        y = addStatusKv(y, tr("bond.settings.status.cache.runtime"),
                literal(status.maidCount() + " / " + status.inFlightRequests() + " / " + status.revision()));
        y = addStatusHeader(y, "bond.settings.status.section.maids");
        List<TmaAiStatusWire.MaidStatus> maids = status.maids();
        List<String> maidLabels = TmaMaidLabels.displayLabels(maids);
        for (int index = 0; index < maids.size(); index++) {
            TmaAiStatusWire.MaidStatus maid = maids.get(index);
            int totalTarget = maid.target() * Math.max(1, maid.pools().size());
            statusRows.add(new StatusRow(StatusRow.Kind.MAID, y, STATUS_MAID_HEIGHT, null,
                    literal(maidLabels.get(index)),
                    literal(poolSummary(maid) + " · " + maid.totalEntries() + "/" + totalTarget),
                    maid.maidUuid()));
            y += STATUS_MAID_HEIGHT + STATUS_ROW_GAP;
        }
    }

    private int addStatusHeader(int y, String headerKey) {
        statusRows.add(new StatusRow(StatusRow.Kind.HEADER, y, SECTION_HEADER_HEIGHT, headerKey, null, null, null));
        return y + SECTION_HEADER_HEIGHT;
    }

    private int addStatusKv(int y, Component label, Component value) {
        statusRows.add(new StatusRow(StatusRow.Kind.KV, y, STATUS_KV_HEIGHT, null, label, value, null));
        return y + STATUS_KV_HEIGHT + STATUS_ROW_GAP;
    }

    private static Component onOff(boolean on) {
        return Component.translatable(on ? "bond.settings.status.on" : "bond.settings.status.off");
    }

    private static Component languageValue(String language, String autoKey) {
        if (language == null || language.isBlank() || LANGUAGE_AUTO.equalsIgnoreCase(language.trim())) {
            return Component.translatable(autoKey);
        }
        return Component.literal(language);
    }

    private static String poolSummary(TmaAiStatusWire.MaidStatus maid) {
        StringBuilder summary = new StringBuilder();
        for (TmaAiStatusWire.PoolStatus pool : maid.pools()) {
            if (summary.length() > 0) {
                summary.append(" · ");
            }
            summary.append(Component.translatable("bond.settings.status.pool." + pool.pool()).getString())
                    .append(' ')
                    .append(pool.totalEntries());
        }
        return summary.toString();
    }

    private static Component tr(String key) {
        return Component.translatable(key);
    }

    private static Component literal(String value) {
        return Component.literal(value == null ? "" : value);
    }

    private void ensurePromptBox() {
        if (promptBox != null) {
            return;
        }
        promptBox = new BondPromptBox(
                this.font,
                contentLeft(),
                0,
                Math.max(20, contentRight() - contentLeft()),
                PROMPT_BOX_HEIGHT,
                Component.translatable("bond.settings.prompt.placeholder"),
                Component.translatable("bond.settings.prompt.label")
        );
        // No vanilla character limit: MultiLineEditBox would draw its counter below the box where it
        // collides with the legend row. The screen draws the counter inside and clamps the length.
        promptBox.setCharacterLimit(Integer.MAX_VALUE);
        promptBox.setValue(TmaSettingsClientState.getValue(PROMPT_KEY));
    }

    /**
     * Mirrors the authoritative prompt into the editor, but never while the player is typing:
     * server pushes must not clobber an in-progress edit (the same contract as the pending machine).
     */
    private void syncPromptBox() {
        if (promptBox == null || promptBox.isFocused()) {
            return;
        }
        String authoritative = TmaSettingsClientState.getValue(PROMPT_KEY);
        if (!authoritative.equals(promptBox.getValue())) {
            promptBox.setValue(authoritative);
        }
    }

    /** Submits the editor content (if it changed) and drops focus. */
    private void blurPrompt() {
        if (promptBox == null || !promptBox.isFocused()) {
            return;
        }
        promptBox.setFocused(false);
        submitPrompt();
    }

    private void submitPrompt() {
        if (promptBox == null) {
            return;
        }
        clampPromptLength();
        String value = promptBox.getValue();
        if (value.equals(TmaSettingsClientState.getValue(PROMPT_KEY))) {
            return;
        }
        // An empty template means "restore the built-in default"; the client resolves the default
        // locally so the pending marker can match the server's pushed value.
        applyServerValue(PROMPT_KEY, value.isEmpty() ? promptDefault() : value);
    }

    /**
     * Keeps the editor within {@link TmaSettingsKeys#MAX_TEXT_LENGTH}.
     *
     * <p>The editor runs without a vanilla character limit (see {@link #ensurePromptBox()}), so the
     * cap is enforced here after every key/char event and once more before submitting.</p>
     */
    private void clampPromptLength() {
        if (promptBox == null) {
            return;
        }
        String value = promptBox.getValue();
        if (value.length() > TmaSettingsKeys.MAX_TEXT_LENGTH) {
            promptBox.setValue(value.substring(0, TmaSettingsKeys.MAX_TEXT_LENGTH));
        }
    }

    private String promptDefault() {
        String defaultValue = ModConfig.BOND_MORNING_KISS_AI_DIALOGUE_PROMPT.getDefault();
        return defaultValue == null ? "" : defaultValue;
    }

    private boolean clickPromptBox(double mouseX, double mouseY) {
        if (promptBox == null || activeTab != 2) {
            return false;
        }
        if (!promptBox.mouseClicked(mouseX, mouseY, 0)) {
            return false;
        }
        promptBox.setFocused(true);
        return true;
    }

    private boolean currentToggleValue(String key) {
        return TmaSettingsClientState.getBoolean(key, false);
    }

    private List<String> buildLanguageOptions(String key) {
        String current = TmaSettingsKeys.languageForDisplay(TmaSettingsClientState.getValue(key));
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
            case 0 -> renderStatusTab(graphics, font, rowTop, mouseX, mouseY);
            case 1 -> {
                renderSectionHeader(graphics, font, rowTop, "bond.settings.section.features", BondGuiTokens.TAG_SERVER);
                for (ToggleRow row : toggles) {
                    renderToggleRow(graphics, font, row, rowTop + row.y, mouseX, mouseY);
                }
                renderCachePolicySection(graphics, font, rowTop, mouseX, mouseY);
            }
            case 2 -> {
                renderSectionHeader(graphics, font, rowTop, "bond.settings.section.languages", BondGuiTokens.TAG_SERVER);
                for (LanguageRow row : languages) {
                    renderLanguageRow(graphics, font, row, rowTop + row.y, mouseX, mouseY);
                }
                renderPromptSection(graphics, font, rowTop, mouseX, mouseY);
                renderSiteSection(graphics, font, rowTop, mouseX, mouseY);
            }
            default -> {
                renderSectionHeader(graphics, font, rowTop, "bond.settings.section.volumes", BondGuiTokens.TAG_LOCAL);
                for (SliderRow row : sliders) {
                    renderSliderRow(graphics, font, row, rowTop + row.y, mouseX, mouseY);
                }
            }
        }
    }

    /** Cache-policy section of the features tab: two integer fields plus the "consume on use" switch. */
    private void renderCachePolicySection(GuiGraphics graphics, Font font, int rowTop, int mouseX, int mouseY) {
        renderSectionHeader(graphics, font, rowTop + cachePolicySectionY,
                "bond.settings.section.cache_policy", BondGuiTokens.TAG_SERVER);
        for (NumberRow row : cacheNumberRows) {
            renderNumberRow(graphics, font, row, rowTop + row.y(), mouseX, mouseY);
        }
        if (cacheConsumeRow != null) {
            renderToggleRow(graphics, font, cacheConsumeRow, rowTop + cacheConsumeRow.y, mouseX, mouseY);
        }
    }

    /** One cache-policy numeric row: label + subtitle on the left, compact integer field on the right. */
    private void renderNumberRow(GuiGraphics graphics, Font font, NumberRow row, int rowTop, int mouseX, int mouseY) {
        BondNumberField field = cacheNumberFields.get(row.key());
        int controlLeft = field == null ? contentRight() - NUMBER_FIELD_WIDTH : field.left();
        drawRowLabels(graphics, font, row.key(), rowTop, controlLeft - LABEL_CONTROL_GAP);
        if (field != null) {
            field.render(graphics, mouseX, mouseY, TmaSettingsClientState.canEdit());
        }
    }

    private void renderPromptSection(GuiGraphics graphics, Font font, int rowTop, int mouseX, int mouseY) {
        renderSectionHeader(graphics, font, rowTop + voicePromptSectionY, "bond.settings.section.prompt", BondGuiTokens.TAG_SERVER);
        int resetWidth = promptResetWidth(font);
        int resetLeft = contentRight() - resetWidth;
        int labelTop = rowTop + voicePromptLabelY;
        // The label block keeps its subtitle; "restore default" is right-aligned on that same row so
        // the legend below can use the whole content width and is never truncated.
        drawLabelBlock(graphics, font, "bond.settings.prompt.label", "bond.settings.prompt.sub",
                labelTop, resetLeft - STATUS_DOT_GAP - LABEL_CONTROL_GAP);
        int resetTop = labelTop + (PROMPT_LABEL_BLOCK - TEXT_BUTTON_HEIGHT) / 2;
        boolean resetHovered = within(mouseX, mouseY, resetLeft, resetWidth, resetTop, TEXT_BUTTON_HEIGHT);
        drawTextButton(graphics, font, Component.translatable("bond.settings.prompt.reset"), resetLeft, resetTop,
                resetWidth, resetHovered, TmaSettingsClientState.canEdit());
        if (promptBox != null) {
            promptBox.render(graphics, mouseX, mouseY, 0.0F);
        }
        drawPromptCounter(graphics, font, rowTop);
        Component legend = Component.translatable("bond.settings.prompt.legend");
        graphics.drawString(font, font.plainSubstrByWidth(legend.getString(), contentRight() - contentLeft()),
                contentLeft(), rowTop + voicePromptRowY + (PROMPT_ROW_HEIGHT - font.lineHeight) / 2,
                BondGuiTokens.COLOR_TEXT_HINT, false);
    }

    /**
     * Draws the character counter inside the editor's bottom-right corner.
     *
     * <p>The vanilla {@code MultiLineEditBox} paints its counter below the box (at {@code y + height + 4}),
     * which collides with the legend row; the editor therefore runs without a vanilla character limit
     * and this screen owns both the counter and the length cap.</p>
     */
    private void drawPromptCounter(GuiGraphics graphics, Font font, int rowTop) {
        if (promptBox == null) {
            return;
        }
        Component counter = Component.translatable("gui.multiLineEditBox.character_limit",
                promptBox.getValue().length(), TmaSettingsKeys.MAX_TEXT_LENGTH);
        int boxRight = contentRight();
        int boxBottom = rowTop + voicePromptBoxY + PROMPT_BOX_HEIGHT;
        graphics.drawString(font, counter, boxRight - PROMPT_INNER_PADDING - font.width(counter),
                boxBottom - PROMPT_INNER_PADDING - font.lineHeight, BondGuiTokens.COLOR_TEXT_HINT, false);
    }

    private void renderSiteSection(GuiGraphics graphics, Font font, int rowTop, int mouseX, int mouseY) {
        renderSectionHeader(graphics, font, rowTop + voiceSiteSectionY, "bond.settings.section.site", BondGuiTokens.TAG_LOCAL);
        int labelTop = rowTop + voiceSiteRowY;
        int buttonWidth = siteButtonWidth(font);
        int buttonLeft = siteButtonLeft(font);
        drawLabelBlock(graphics, font, "bond.settings.site.label", "bond.settings.site.sub",
                labelTop, buttonLeft - STATUS_DOT_GAP - LABEL_CONTROL_GAP);
        int buttonTop = labelTop + (SITE_ROW_HEIGHT - TEXT_BUTTON_HEIGHT) / 2;
        boolean hovered = within(mouseX, mouseY, buttonLeft, buttonWidth, buttonTop, TEXT_BUTTON_HEIGHT);
        drawTextButton(graphics, font, Component.translatable("bond.settings.site.open"), buttonLeft, buttonTop,
                buttonWidth, hovered);
    }

    private void renderStatusTab(GuiGraphics graphics, Font font, int rowTop, int mouseX, int mouseY) {
        if (!TmaAiStatusClientState.hasStatus()) {
            graphics.drawString(font, Component.translatable("bond.settings.status.loading"),
                    contentLeft(), rowTop, BondGuiTokens.COLOR_TEXT_HINT, false);
            return;
        }
        for (StatusRow row : statusRows) {
            int y = rowTop + row.y();
            switch (row.kind()) {
                case HEADER -> renderSectionHeader(graphics, font, y, row.key(), BondGuiTokens.TAG_SERVER);
                case KV -> drawKvRow(graphics, font, y, row.label(), row.value());
                case MAID -> renderMaidRow(graphics, font, y, row, mouseX, mouseY);
            }
        }
    }

    /**
     * Key/value row of the read-only status sections.
     *
     * <p>The value is right-aligned and the label is clipped to the space left of it, so a long
     * value (for example {@code 24 (18 / 6)}) can never be overpainted by the label.
     */
    private void drawKvRow(GuiGraphics graphics, Font font, int y, Component label, Component value) {
        int right = contentRight();
        int valueLeft = right;
        if (value != null) {
            String clippedValue = clip(font, value, Math.max(0, right - contentLeft() - LABEL_CONTROL_GAP));
            valueLeft = right - font.width(clippedValue);
            graphics.drawString(font, clippedValue, valueLeft, y, BondGuiTokens.HIGHLIGHT_TEXT, false);
        }
        int labelWidth = Math.max(0, valueLeft - LABEL_CONTROL_GAP - contentLeft());
        graphics.drawString(font, clip(font, label, labelWidth), contentLeft(), y,
                BondGuiTokens.COLOR_TEXT_BODY, false);
    }

    /**
     * Maid row of the "by maid" section: the name takes the left column, the pool summary is
     * right-aligned in front of the "clear" button and both are clipped with an ellipsis so neither
     * can slide under the button.
     */
    private void renderMaidRow(GuiGraphics graphics, Font font, int y, StatusRow row, int mouseX, int mouseY) {
        int buttonLeft = contentRight() - STATUS_CLEAR_BUTTON_WIDTH;
        int labelRight = buttonLeft - STATUS_DOT_GAP - LABEL_CONTROL_GAP;
        int available = Math.max(0, labelRight - contentLeft());
        Component detail = row.value();
        int detailLeft = labelRight;
        if (detail != null) {
            // The count column keeps its natural width (up to everything but the name column) so the
            // per-pool numbers stay readable; the name column then takes whatever is left.
            String clippedDetail = clip(font, detail, Math.max(0, available - MIN_MAID_NAME_WIDTH));
            detailLeft = labelRight - font.width(clippedDetail);
            graphics.drawString(font, clippedDetail, detailLeft, y, BondGuiTokens.COLOR_TEXT_HINT, false);
        }
        int nameWidth = Math.max(0, detailLeft - LABEL_CONTROL_GAP - contentLeft());
        graphics.drawString(font, clip(font, row.label(), nameWidth), contentLeft(), y,
                BondGuiTokens.COLOR_TEXT_BODY, false);

        boolean canClear = TmaAiStatusClientState.get() != null && TmaAiStatusClientState.get().canClear();
        int buttonTop = y + (STATUS_MAID_HEIGHT - TEXT_BUTTON_HEIGHT) / 2;
        boolean hovered = canClear && within(mouseX, mouseY, buttonLeft, STATUS_CLEAR_BUTTON_WIDTH, buttonTop, TEXT_BUTTON_HEIGHT);
        drawTextButton(graphics, font, Component.translatable("bond.settings.status.clear"), buttonLeft, buttonTop,
                STATUS_CLEAR_BUTTON_WIDTH, hovered, canClear);
    }

    /**
     * Clips plain text to {@code maxWidth} pixels, appending {@code …} when anything was dropped.
     *
     * <p>Used by every row of this panel that has to share its width with a control or a
     * right-aligned value, so text can never run underneath the widget next to it.
     */
    private static String clip(Font font, Component text, int maxWidth) {
        if (text == null || maxWidth <= 0) {
            return "";
        }
        String raw = text.getString();
        if (font.width(raw) <= maxWidth) {
            return raw;
        }
        String ellipsis = "…";
        return font.plainSubstrByWidth(raw, Math.max(0, maxWidth - font.width(ellipsis))) + ellipsis;
    }

    private void drawTextButton(GuiGraphics graphics, Font font, Component label, int left, int top, int width, boolean hovered) {
        drawTextButton(graphics, font, label, left, top, width, hovered, true);
    }

    private void drawTextButton(GuiGraphics graphics, Font font, Component label, int left, int top, int width,
                                boolean hovered, boolean enabled) {
        int background = !enabled
                ? BondGuiTokens.TOGGLE_DISABLED_BG
                : hovered ? BondGuiTokens.PRIMARY_BUTTON_HOVER_BG : BondGuiTokens.PRIMARY_BUTTON_BG;
        int border = enabled ? BondGuiTokens.FIELD_BORDER : BondGuiTokens.TOGGLE_DISABLED_BORDER;
        graphics.fill(left, top, left + width, top + TEXT_BUTTON_HEIGHT, border);
        graphics.fill(left + 1, top + 1, left + width - 1, top + TEXT_BUTTON_HEIGHT - 1, background);
        int color = enabled ? BondGuiTokens.COLOR_TEXT_TITLE : BondGuiTokens.COLOR_TEXT_DISABLED;
        graphics.drawCenteredString(font, label, left + width / 2,
                top + (TEXT_BUTTON_HEIGHT - font.lineHeight) / 2, color);
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

    /** Draws a label + hint subtitle block using explicit translation keys (voice tab extras). */
    private void drawLabelBlock(GuiGraphics graphics, Font font, String labelKey, String subKey, int rowTop, int labelRight) {
        int maxWidth = Math.max(0, labelRight - contentLeft());
        Component label = Component.translatable(labelKey);
        graphics.drawString(font, font.plainSubstrByWidth(label.getString(), maxWidth),
                contentLeft(), rowTop, BondGuiTokens.COLOR_TEXT_BODY, false);
        drawRowSubtitle(graphics, font, subKey, rowTop + font.lineHeight, labelRight);
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
        for (int index = 0; index < TAB_COUNT; index++) {
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

        for (FooterButton button : layout.buttons()) {
            boolean buttonHovered = mouseX >= button.left() && mouseX < button.left() + button.width()
                    && mouseY >= layout.top() && mouseY < layout.top() + FOOTER_BUTTON_HEIGHT;
            if (buttonHovered) {
                graphics.fill(button.left(), layout.top(), button.left() + button.width(),
                        layout.top() + FOOTER_BUTTON_HEIGHT, BondGuiTokens.HOVER_OVERLAY);
            }
            graphics.drawString(
                    font,
                    Component.translatable(button.labelKey()),
                    button.left() + TEXT_BUTTON_PADDING,
                    layout.top() + (FOOTER_BUTTON_HEIGHT - font.lineHeight) / 2,
                    BondGuiTokens.COLOR_TEXT_BODY,
                    false
            );
        }
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
        int hintRight = layout.buttons().isEmpty()
                ? layout.doneLeft() - FOOTER_GAP
                : layout.buttons().get(0).left() - FOOTER_GAP;
        int maxWidth = Math.max(0, hintRight - CONTENT_PADDING - contentLeft());
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
        for (FooterButton button : layout.buttons()) {
            if (mouseX < button.left() || mouseX >= button.left() + button.width()) {
                continue;
            }
            switch (button.action()) {
                case RELOAD -> reloadFromServer();
                case REFRESH -> refreshStatus();
                case CLEAR_ALL -> TmaAiStatusClientState.clearAll();
                default -> {
                }
            }
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

    /** Re-requests the AI status; the push lands through {@link #onStatusPushed()}. */
    private void refreshStatus() {
        statusRequested = true;
        TmaAiStatusClientState.requestSync();
    }

    private boolean clickVoiceExtras(double mouseX, double mouseY) {
        int resetTop = contentTop() - scrollOffset + voicePromptLabelY + (PROMPT_LABEL_BLOCK - TEXT_BUTTON_HEIGHT) / 2;
        int resetWidth = promptResetWidth(this.font);
        if (within(mouseX, mouseY, contentRight() - resetWidth, resetWidth, resetTop, TEXT_BUTTON_HEIGHT)) {
            if (TmaSettingsClientState.canEdit()) {
                applyServerValue(PROMPT_KEY, promptDefault());
            }
            return true;
        }
        int buttonTop = contentTop() - scrollOffset + voiceSiteRowY + (SITE_ROW_HEIGHT - TEXT_BUTTON_HEIGHT) / 2;
        if (within(mouseX, mouseY, siteButtonLeft(this.font), siteButtonWidth(this.font), buttonTop, TEXT_BUTTON_HEIGHT)) {
            openAiSettings();
            return true;
        }
        return false;
    }

    /** Opens Touhou Little Maid's native AI settings hub, returning to this panel when closed. */
    private void openAiSettings() {
        if (minecraft == null) {
            return;
        }
        minecraft.setScreen(AIChatSettingsHubScreen.openDefault(
                this,
                AvailableSites.LLM_SITES,
                AvailableSites.TTS_SITES,
                false
        ));
    }

    /** The status tab is read-only: only the per-maid "clear" buttons react to a click. */
    private void clickStatus(double mouseX, double mouseY) {
        clickStatusClear(mouseX, mouseY);
    }

    /** Features tab: the cache-policy numeric fields, then every capsule switch. */
    private void clickFeatures(double mouseX, double mouseY) {
        if (clickCacheNumber(mouseX, mouseY)) {
            return;
        }
        blurNumberFields();
        if (cacheConsumeRow != null && containsToggle(cacheConsumeRow, mouseX, mouseY)) {
            if (TmaSettingsClientState.canEdit()) {
                applyServerValue(cacheConsumeRow.key,
                        currentToggleValue(cacheConsumeRow.key) ? "false" : "true");
            }
            return;
        }
        clickToggle(mouseX, mouseY);
    }

    /** Focuses the clicked numeric field (the value is committed when it loses focus again). */
    private boolean clickCacheNumber(double mouseX, double mouseY) {
        for (BondNumberField field : cacheNumberFields.values()) {
            if (field.mouseClicked(mouseX, mouseY, 0)) {
                return true;
            }
        }
        return false;
    }

    private void blurNumberFields() {
        for (BondNumberField field : cacheNumberFields.values()) {
            field.blur();
        }
    }

    private BondNumberField focusedNumberField() {
        for (BondNumberField field : cacheNumberFields.values()) {
            if (field.isFocused()) {
                return field;
            }
        }
        return null;
    }

    private boolean clickStatusClear(double mouseX, double mouseY) {
        String maidUuid = statusClearHovered(mouseX, mouseY);
        if (maidUuid == null) {
            return false;
        }
        TmaAiStatusClientState.clearMaid(maidUuid);
        return true;
    }

    /** @return the maid uuid whose "clear" button is hovered, or {@code null}. */
    private String statusClearHovered(double mouseX, double mouseY) {
        TmaAiStatusWire.Status status = TmaAiStatusClientState.get();
        if (status == null || !status.canClear()) {
            return null;
        }
        int rowTop = contentTop() - scrollOffset;
        int buttonLeft = contentRight() - STATUS_CLEAR_BUTTON_WIDTH;
        for (StatusRow row : statusRows) {
            if (row.kind() != StatusRow.Kind.MAID) {
                continue;
            }
            int y = rowTop + row.y();
            int buttonTop = y + (STATUS_MAID_HEIGHT - TEXT_BUTTON_HEIGHT) / 2;
            if (within(mouseX, mouseY, buttonLeft, STATUS_CLEAR_BUTTON_WIDTH, buttonTop, TEXT_BUTTON_HEIGHT)) {
                return row.maidUuid();
            }
        }
        return null;
    }

    private int promptResetWidth(Font font) {
        return textButtonWidth(font, "bond.settings.prompt.reset");
    }

    private int siteButtonWidth(Font font) {
        return textButtonWidth(font, "bond.settings.site.open");
    }

    /** Shared width of a {@link #drawTextButton} control (centred label plus even side padding). */
    private static int textButtonWidth(Font font, String labelKey) {
        return font.width(Component.translatable(labelKey)) + TEXT_BUTTON_PADDING * 2 + 8;
    }

    private int siteButtonLeft(Font font) {
        return contentRight() - siteButtonWidth(font);
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
        for (int index = 0; index < TAB_COUNT; index++) {
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
        return rowTooltip(key, Component.translatable(TmaSettingsKeys.labelKey(key)), detail);
    }

    private List<Component> rowTooltip(String key, Component label, Component detail) {
        List<Component> tooltip = new ArrayList<>(3);
        tooltip.add(label);
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
            case 0 -> "bond.settings.nav.status";
            case 1 -> "bond.settings.nav.features";
            case 2 -> "bond.settings.nav.voice";
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

        List<FooterAction> actions = footerActions();
        List<FooterButton> buttons = new ArrayList<>(actions.size());
        int right = doneLeft - FOOTER_GAP;
        for (FooterAction action : actions) {
            int width = font.width(Component.translatable(action.labelKey())) + TEXT_BUTTON_PADDING * 2;
            buttons.add(new FooterButton(right - width, width, action.labelKey(), action));
            right -= width + FOOTER_GAP;
        }
        return new FooterLayout(doneLeft, doneWidth, List.copyOf(buttons), top);
    }

    /** Left-side footer buttons: reload while changes are pending, refresh/clear on the status tab. */
    private List<FooterAction> footerActions() {
        if (activeTab == 0) {
            List<FooterAction> actions = new ArrayList<>(2);
            actions.add(FooterAction.REFRESH);
            if (TmaAiStatusClientState.get() != null && TmaAiStatusClientState.get().canClear()) {
                actions.add(FooterAction.CLEAR_ALL);
            }
            return actions;
        }
        return hasActiveChanges() ? List.of(FooterAction.RELOAD) : List.of();
    }

    private int maxScroll() {
        return Math.max(0, contentHeight - (viewportBottom() - contentTop()));
    }

    private static String volumeLabelKey(String id) {
        return "bond.settings.volume." + id;
    }

    private enum FooterAction {
        RELOAD("bond.settings.button.reload"),
        REFRESH("bond.settings.button.refresh"),
        CLEAR_ALL("bond.settings.button.clear_all");

        private final String labelKey;

        FooterAction(String labelKey) {
            this.labelKey = labelKey;
        }

        private String labelKey() {
            return labelKey;
        }
    }

    private record FooterButton(int left, int width, String labelKey, FooterAction action) {
    }

    private record FooterLayout(int doneLeft, int doneWidth, List<FooterButton> buttons, int top) {
    }

    private record PendingRequest(String value, long sentAt) {
    }

    /**
     * One rendered row of the status tab; {@code y} is relative to the top of the content area.
     *
     * <p>The status tab is read-only, so {@code key} is only used by a {@link Kind#HEADER} row (its
     * translation key); {@code label} / {@code value} carry the text of a {@link Kind#KV} row and
     * {@code maidUuid} identifies the {@link Kind#MAID} row a "clear" button belongs to.
     */
    private record StatusRow(Kind kind, int y, int height, String key, Component label, Component value,
                             String maidUuid) {
        private enum Kind {
            HEADER,
            KV,
            MAID
        }
    }

    /** One cache-policy numeric row of the features tab; {@code y} is relative to the content top. */
    private record NumberRow(String key, int y) {
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

    private record VolumeSetting(String id, ModConfigSpec.DoubleValue configValue) {
    }
}