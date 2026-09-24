package com.github.touhoumaidaffection.client.screen.page;

import com.github.touhoumaidaffection.ModConfig;
import com.github.touhoumaidaffection.bond.settings.TmaSettingsKeys;
import com.github.touhoumaidaffection.client.TmaSettingsClientState;
import com.github.touhoumaidaffection.client.screen.component.BondButtonRow;
import com.github.touhoumaidaffection.client.screen.component.BondDropdown;
import com.github.touhoumaidaffection.client.screen.component.BondGuiTokens;
import com.github.touhoumaidaffection.client.screen.component.BondModalPage;
import com.github.touhoumaidaffection.client.screen.component.BondSlider;
import com.github.touhoumaidaffection.util.SoundVolumeSettings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * Global (maid independent) settings panel.
 *
 * <p>Three sections: feature switches and languages are server-authoritative and therefore go
 * through the settings channel, while the volume sliders are pure client preferences written
 * straight into the local config. All geometry lives in the constants below so the panel can be
 * restyled without touching the interaction logic.
 */
public final class SettingsSecondaryPage implements BondSecondaryPage {
    // ---- Modal geometry ----
    private static final int MODAL_WIDTH = BondGuiTokens.SECONDARY_MODAL_WIDTH;
    private static final int MODAL_HEIGHT = BondGuiTokens.SECONDARY_MODAL_HEIGHT;
    private static final int FOOTER_BUTTON_HEIGHT = 17;
    private static final int FOOTER_BUTTON_GAP = 3;
    private static final int STATUS_LINE_Y = 0;

    // ---- Section layout ----
    private static final int SECTION_HEADER_HEIGHT = 11;
    private static final int SECTION_GAP = 5;
    private static final int TOGGLE_ROW_HEIGHT = 12;
    private static final int LANGUAGE_ROW_HEIGHT = 25;
    private static final int LANGUAGE_LABEL_HEIGHT = 11;
    private static final int SLIDER_ROW_HEIGHT = 13;
    private static final int SCROLL_STEP = 10;
    private static final int SCROLLBAR_WIDTH = 2;

    // ---- Control geometry ----
    private static final int DROPDOWN_WIDTH = 96;
    private static final int DROPDOWN_HEADER_HEIGHT = 14;
    private static final int DROPDOWN_ROW_HEIGHT = 12;
    private static final int DROPDOWN_MAX_VISIBLE_ROWS = 4;
    private static final int SLIDER_LABEL_WIDTH = 68;
    private static final int SLIDER_VALUE_WIDTH = 26;

    // ---- Values ----
    private static final String LANGUAGE_AUTO = "auto";
    private static final List<String> COMMON_LANGUAGES = List.of("zh_cn", "en_us", "ja_jp", "zh_tw", "ko_kr");
    private static final double VOLUME_STEP = 0.05D;

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

    private final BondSecondaryPageHost host;
    private final Runnable refreshListener = this::refreshFromState;

    private final List<HeaderRow> headers = new ArrayList<>();
    private final List<ToggleRow> toggles = new ArrayList<>();
    private final List<LanguageRow> languages = new ArrayList<>();
    private final List<SliderRow> sliders = new ArrayList<>();

    private List<BondButtonRow.ButtonSpec> toggleButtons = List.of();
    private BondModalPage modal;
    private int contentHeight;
    private int scrollOffset;
    private boolean layoutDirty = true;
    private boolean volumesDirty;

    public SettingsSecondaryPage(BondSecondaryPageHost host) {
        this.host = host;
        buildRows();
        TmaSettingsClientState.addListener(refreshListener);
        // Always re-read: the cache may still hold the state of a previous world or server.
        TmaSettingsClientState.requestSync();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        Font font = host.getFont();
        modal = createModal();
        BondModalPage modal = this.modal;
        modal.renderChrome(graphics, font);
        ensureLayout();

        int viewportTop = modal.contentTop();
        int viewportBottom = modal.footerTop();
        graphics.enableScissor(modal.contentLeft(), viewportTop, modal.contentRight(), viewportBottom);
        try {
            renderContent(graphics, font, mouseX, mouseY);
        } finally {
            graphics.disableScissor();
        }

        renderScrollbar(graphics, viewportTop, viewportBottom);
        renderDropdownOverlays(graphics, font, mouseX, mouseY, modal);
        renderFooter(graphics, font, modal, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return true;
        }
        BondModalPage modal = modal();
        if (!modal.contains(mouseX, mouseY)) {
            host.closeSecondaryPage();
            return true;
        }
        ensureLayout();

        if (handleExpandedDropdown(mouseX, mouseY)) {
            return true;
        }
        String buttonId = BondButtonRow.click(footerButtons(modal), modal.left(), mouseX, mouseY);
        if (!buttonId.isEmpty()) {
            switch (buttonId) {
                case "refresh" -> TmaSettingsClientState.requestSync();
                case "close" -> host.closeSecondaryPage();
                default -> {
                }
            }
            return true;
        }
        if (!isInsideViewport(mouseX, mouseY)) {
            return true;
        }
        if (clickToggle(mouseX, mouseY) || clickLanguage(mouseX, mouseY) || clickSlider(mouseX, mouseY)) {
            return true;
        }
        return true;
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
    public List<Component> getTooltip(int mouseX, int mouseY) {
        ensureLayout();
        for (LanguageRow row : languages) {
            if (isInsideViewport(mouseX, mouseY) && row.dropdown.contains(mouseX, mouseY, row.options.size())) {
                return List.of(
                        Component.translatable(TmaSettingsKeys.labelKey(row.key)),
                        Component.translatable("bond.settings.language.tip").withStyle(ChatFormatting.GRAY),
                        permissionTip()
                );
            }
        }
        for (ToggleRow row : toggles) {
            if (isInsideViewport(mouseX, mouseY) && containsToggle(row, mouseX, mouseY)) {
                return List.of(
                        Component.translatable(TmaSettingsKeys.labelKey(row.key)),
                        Component.translatable(currentToggleValue(row.key)
                                ? "bond.settings.toggle.tip.off"
                                : "bond.settings.toggle.tip.on").withStyle(ChatFormatting.GRAY),
                        permissionTip()
                );
            }
        }
        for (SliderRow row : sliders) {
            if (isInsideViewport(mouseX, mouseY) && row.slider.contains(mouseX, mouseY)) {
                return List.of(
                        Component.translatable(volumeLabelKey(row.setting.id())),
                        Component.translatable("bond.settings.volume.tip").withStyle(ChatFormatting.GRAY)
                );
            }
        }
        return List.of();
    }

    @Override
    public void onClose() {
        TmaSettingsClientState.removeListener(refreshListener);
        for (SliderRow row : sliders) {
            row.slider.mouseReleased();
        }
        saveVolumes();
    }

    // ---- State wiring ----

    private void refreshFromState() {
        layoutDirty = true;
    }

    private void buildRows() {
        int y = 0;
        headers.add(new HeaderRow(Component.translatable("bond.settings.section.features"), y));
        y += SECTION_HEADER_HEIGHT;
        for (String key : TOGGLE_KEYS) {
            toggles.add(new ToggleRow(key, y));
            y += TOGGLE_ROW_HEIGHT;
        }
        y += SECTION_GAP;

        headers.add(new HeaderRow(Component.translatable("bond.settings.section.languages"), y));
        y += SECTION_HEADER_HEIGHT;
        for (String key : LANGUAGE_KEYS) {
            languages.add(new LanguageRow(key, new BondDropdown<>(
                    0,
                    0,
                    DROPDOWN_WIDTH,
                    DROPDOWN_HEADER_HEIGHT,
                    DROPDOWN_ROW_HEIGHT,
                    DROPDOWN_MAX_VISIBLE_ROWS
            ), y));
            y += LANGUAGE_ROW_HEIGHT;
        }
        y += SECTION_GAP;

        headers.add(new HeaderRow(Component.translatable("bond.settings.section.volumes"), y));
        y += SECTION_HEADER_HEIGHT;
        for (VolumeSetting setting : VOLUME_SETTINGS) {
            sliders.add(new SliderRow(setting, new BondSlider(
                    0,
                    0,
                    0,
                    SLIDER_ROW_HEIGHT,
                    SLIDER_LABEL_WIDTH,
                    SLIDER_VALUE_WIDTH,
                    SoundVolumeSettings.MIN_VOLUME,
                    SoundVolumeSettings.MAX_VOLUME,
                    VOLUME_STEP,
                    setting.configValue().get()
            ), y));
            y += SLIDER_ROW_HEIGHT;
        }
        contentHeight = y;
    }

    /** Recomputes absolute positions and the cached button specs after scrolling or a state push. */
    private void ensureLayout() {
        if (!layoutDirty) {
            return;
        }
        BondModalPage modal = modal();
        int contentLeft = modal.contentLeft();
        int contentRight = modal.contentRight();
        int rowTop = modal.contentTop() - scrollOffset;

        List<BondButtonRow.ButtonSpec> specs = new ArrayList<>(toggles.size());
        for (ToggleRow row : toggles) {
            specs.add(new BondButtonRow.ButtonSpec(
                    0,
                    rowTop + row.y,
                    contentRight - contentLeft,
                    TOGGLE_ROW_HEIGHT,
                    toggleLabel(row.key),
                    row.key,
                    TmaSettingsClientState.canEdit(),
                    currentToggleValue(row.key)
            ));
        }
        toggleButtons = List.copyOf(specs);

        for (LanguageRow row : languages) {
            row.options = buildLanguageOptions(row.key);
            row.selectedIndex = Math.max(0, row.options.indexOf(TmaSettingsClientState.getValue(row.key)));
            row.dropdown.setPosition(contentRight - DROPDOWN_WIDTH, rowTop + row.y + LANGUAGE_LABEL_HEIGHT);
        }

        for (SliderRow row : sliders) {
            row.slider.setPosition(contentLeft, rowTop + row.y);
            row.slider.setSize(contentRight - contentLeft);
        }
        layoutDirty = false;
    }

    private Component toggleLabel(String key) {
        return Component.translatable(
                "bond.settings.toggle.format",
                Component.translatable(TmaSettingsKeys.labelKey(key)),
                Component.translatable(currentToggleValue(key) ? "bond.settings.value.on" : "bond.settings.value.off")
        );
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

    private void renderContent(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        BondModalPage modal = modal();
        int rowTop = modal.contentTop() - scrollOffset;
        for (HeaderRow header : headers) {
            graphics.drawString(
                    font,
                    header.label,
                    modal.contentLeft(),
                    rowTop + header.y,
                    BondGuiTokens.COLOR_TEXT_SELECTED,
                    false
            );
        }
        BondButtonRow.render(graphics, font, modal.contentLeft() - scrollOffset, toggleButtons, mouseX, mouseY);
        for (LanguageRow row : languages) {
            graphics.drawString(
                    font,
                    Component.translatable(TmaSettingsKeys.labelKey(row.key)),
                    modal.contentLeft(),
                    rowTop + row.y,
                    BondGuiTokens.COLOR_TEXT_BODY,
                    false
            );
            row.dropdown.renderBase(graphics, font, row.options, row.selectedIndex, mouseX, mouseY, SettingsSecondaryPage::renderDropdownEntry);
        }
        for (SliderRow row : sliders) {
            row.slider.render(graphics, font, Component.translatable(volumeLabelKey(row.setting.id())), mouseX, mouseY);
        }
    }

    private void renderDropdownOverlays(GuiGraphics graphics, Font font, int mouseX, int mouseY, BondModalPage modal) {
        boolean anyExpanded = false;
        for (LanguageRow row : languages) {
            if (row.dropdown.isExpanded()) {
                anyExpanded = true;
                break;
            }
        }
        if (!anyExpanded) {
            return;
        }
        graphics.enableScissor(modal.left() + 2, modal.contentTop(), modal.right() - 2, modal.bottom() - 2);
        try {
            for (LanguageRow row : languages) {
                row.dropdown.renderOverlay(graphics, font, row.options, row.selectedIndex, mouseX, mouseY, SettingsSecondaryPage::renderDropdownEntry);
            }
        } finally {
            graphics.disableScissor();
        }
    }

    private void renderScrollbar(GuiGraphics graphics, int viewportTop, int viewportBottom) {
        int maxScroll = maxScroll();
        if (maxScroll <= 0) {
            return;
        }
        BondModalPage modal = modal();
        int trackLeft = modal.contentRight() - SCROLLBAR_WIDTH;
        graphics.fill(trackLeft, viewportTop, modal.contentRight(), viewportBottom, BondGuiTokens.COLOR_BG_ELEMENT);
        int viewportHeight = viewportBottom - viewportTop;
        int thumbHeight = Math.max(6, viewportHeight * viewportHeight / contentHeight);
        int thumbTop = viewportTop + (viewportHeight - thumbHeight) * scrollOffset / maxScroll;
        graphics.fill(trackLeft, thumbTop, modal.contentRight(), thumbTop + thumbHeight, BondGuiTokens.COLOR_ACCENT);
    }

    private void renderFooter(GuiGraphics graphics, Font font, BondModalPage modal, int mouseX, int mouseY) {
        Component status = statusLine();
        String text = font.plainSubstrByWidth(status.getString(), modal.contentRight() - modal.contentLeft());
        graphics.drawString(font, text, modal.contentLeft(), modal.footerTop() + STATUS_LINE_Y, statusColor(), false);
        BondButtonRow.render(graphics, font, modal.left(), footerButtons(modal), mouseX, mouseY);
    }

    private Component statusLine() {
        if (!TmaSettingsClientState.hasState()) {
            return Component.translatable("bond.settings.status.loading");
        }
        return Component.translatable(TmaSettingsClientState.canEdit()
                ? "bond.settings.status.editable"
                : "bond.settings.status.readonly");
    }

    private int statusColor() {
        if (!TmaSettingsClientState.hasState()) {
            return BondGuiTokens.COLOR_TEXT_HINT;
        }
        return TmaSettingsClientState.canEdit() ? BondGuiTokens.COLOR_SUCCESS : BondGuiTokens.COLOR_WARNING;
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
            if (result.handled() && result.selectedIndex() >= 0) {
                if (TmaSettingsClientState.canEdit()) {
                    TmaSettingsClientState.set(row.key, row.options.get(result.selectedIndex()));
                }
            }
            return result.handled();
        }
        return false;
    }

    private boolean clickToggle(double mouseX, double mouseY) {
        for (ToggleRow row : toggles) {
            if (!containsToggle(row, mouseX, mouseY)) {
                continue;
            }
            if (TmaSettingsClientState.canEdit()) {
                TmaSettingsClientState.set(row.key, currentToggleValue(row.key) ? "false" : "true");
            }
            return true;
        }
        return false;
    }

    private boolean clickLanguage(double mouseX, double mouseY) {
        if (!TmaSettingsClientState.canEdit()) {
            return false;
        }
        for (LanguageRow row : languages) {
            BondDropdown.ClickResult result = row.dropdown.mouseClicked(mouseX, mouseY, row.options.size());
            if (!result.handled()) {
                continue;
            }
            if (result.selectedIndex() >= 0) {
                TmaSettingsClientState.set(row.key, row.options.get(result.selectedIndex()));
            }
            return true;
        }
        return false;
    }

    private boolean clickSlider(double mouseX, double mouseY) {
        for (SliderRow row : sliders) {
            if (row.slider.mousePressed(mouseX, mouseY)) {
                applyVolume(row);
                return true;
            }
        }
        return false;
    }

    private boolean containsToggle(ToggleRow row, double mouseX, double mouseY) {
        BondModalPage modal = modal();
        int left = modal.contentLeft() - scrollOffset;
        int top = modal.contentTop() - scrollOffset + row.y;
        return mouseX >= left && mouseX < left + (modal.contentRight() - modal.contentLeft())
                && mouseY >= top && mouseY < top + TOGGLE_ROW_HEIGHT;
    }

    private boolean isInsideViewport(double mouseX, double mouseY) {
        BondModalPage modal = modal();
        return mouseX >= modal.contentLeft() && mouseX < modal.contentRight()
                && mouseY >= modal.contentTop() && mouseY < modal.footerTop();
    }

    private Component permissionTip() {
        return Component.translatable(TmaSettingsClientState.canEdit()
                ? "bond.settings.permission.granted"
                : "bond.settings.permission.required").withStyle(TmaSettingsClientState.canEdit()
                ? ChatFormatting.GREEN
                : ChatFormatting.RED);
    }

    // ---- Geometry helpers ----

    private BondModalPage modal() {
        if (modal == null) {
            modal = createModal();
        }
        return modal;
    }

    private BondModalPage createModal() {
        return host.createModal(MODAL_WIDTH, MODAL_HEIGHT, Component.translatable("bond.settings.title"));
    }

    private int maxScroll() {
        return Math.max(0, contentHeight - viewportHeight());
    }

    private int viewportHeight() {
        BondModalPage modal = modal();
        return modal.footerTop() - modal.contentTop();
    }

    private List<BondButtonRow.ButtonSpec> footerButtons(BondModalPage modal) {
        return BondButtonRow.createCenteredUniform(
                host.getFont(),
                modal.width(),
                modal.footerButtonY(FOOTER_BUTTON_HEIGHT),
                FOOTER_BUTTON_HEIGHT,
                FOOTER_BUTTON_GAP,
                BondGuiTokens.BUTTON_HORIZONTAL_PADDING,
                new BondButtonRow.ButtonSpec(0, 0, 40, FOOTER_BUTTON_HEIGHT, Component.translatable("bond.settings.button.refresh"), "refresh", true),
                new BondButtonRow.ButtonSpec(0, 0, 40, FOOTER_BUTTON_HEIGHT, Component.translatable("bond.settings.button.close"), "close", true)
        );
    }

    private static String volumeLabelKey(String id) {
        return "bond.settings.volume." + id;
    }

    private record HeaderRow(Component label, int y) {
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