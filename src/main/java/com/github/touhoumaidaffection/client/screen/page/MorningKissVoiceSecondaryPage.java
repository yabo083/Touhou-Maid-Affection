package com.github.touhoumaidaffection.client.screen.page;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.github.touhoumaidaffection.bond.MorningKissVoiceSettings;
import com.github.touhoumaidaffection.client.BondClientStateCache;
import com.github.touhoumaidaffection.client.MorningKissVoiceIndex;
import com.github.touhoumaidaffection.client.screen.component.BondButtonRow;
import com.github.touhoumaidaffection.client.screen.component.BondDropdown;
import com.github.touhoumaidaffection.client.screen.component.BondGuiTokens;
import com.github.touhoumaidaffection.client.screen.component.BondModalPage;
import com.github.touhoumaidaffection.client.screen.component.BondScrollableList;
import com.github.touhoumaidaffection.network.MorningKissVoiceConfigPayload;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public final class MorningKissVoiceSecondaryPage implements BondSecondaryPage {
    private static final int MODAL_WIDTH = BondGuiTokens.SECONDARY_MODAL_WIDTH;
    private static final int MODAL_HEIGHT = BondGuiTokens.SECONDARY_MODAL_HEIGHT;
    private static final int DROPDOWN_HEIGHT = BondGuiTokens.CONTROL_HEIGHT;
    private static final int DROPDOWN_ROW_HEIGHT = BondGuiTokens.DROPDOWN_ROW_HEIGHT;
    private static final int DROPDOWN_VISIBLE_ROWS = 3;
    private static final int LIST_ROW_HEIGHT = BondGuiTokens.LIST_ROW_HEIGHT;
    private static final int BUTTON_HEIGHT = BondGuiTokens.CONTROL_HEIGHT;
    private static final int BUTTON_GAP = BondGuiTokens.SPACING_MD;

    private final BondSecondaryPageHost host;
    private MorningKissVoiceSettings workingSettings = MorningKissVoiceSettings.DEFAULT;
    private List<VoiceModeOption> modeOptions = List.of();
    private List<VoiceListOption> selectionOptions = List.of();
    private BondDropdown<VoiceModeOption> modeDropdown;
    private BondScrollableList<VoiceListOption> selectionList;

    public MorningKissVoiceSecondaryPage(BondSecondaryPageHost host) {
        this.host = host;
        initialize();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        Font font = host.getFont();
        BondModalPage modal = modal();
        modal.renderChrome(graphics, font);
        boolean dropdownExpanded = modeDropdown.isExpanded();
        int contentMouseX = dropdownExpanded ? Integer.MIN_VALUE : mouseX;
        int contentMouseY = dropdownExpanded ? Integer.MIN_VALUE : mouseY;
        int contentLeft = modal.contentLeft();
        int contentWidth = modal.contentRight() - contentLeft;
        int contentTop = modal.contentTop();

        Component packName = getSoundPackId().isBlank()
                ? Component.translatable("bond.morning_kiss.voice.none")
                : Component.literal(getSoundPackId());
        graphics.drawString(font, Component.translatable("bond.morning_kiss.voice.pack", packName), contentLeft, contentTop, BondGuiTokens.COLOR_TEXT_BODY, false);
        graphics.drawString(font, Component.translatable("bond.morning_kiss.voice.mode"), contentLeft, contentTop + 14, BondGuiTokens.COLOR_TEXT_BODY, false);
        modeDropdown.renderBase(graphics, font, modeOptions, selectedModeIndex(), mouseX, mouseY, this::renderModeOption);

        int listLeft = contentLeft;
        int listTop = contentTop + 36;
        int listWidth = contentWidth;
        int listHeight = Math.max(LIST_ROW_HEIGHT, modal.footerTop() - listTop - BondGuiTokens.SPACING_SM);
        graphics.fill(listLeft, listTop, listLeft + listWidth, listTop + listHeight, BondGuiTokens.COLOR_BG_ELEMENT);
        if (!dropdownExpanded) {
            if (getSoundPackId().isBlank()) {
                graphics.drawCenteredString(font, Component.translatable("bond.morning_kiss.voice.no_sound_pack"), listLeft + listWidth / 2, listTop + 12, BondGuiTokens.COLOR_TEXT_HINT);
            } else if (workingSettings.mode() == MorningKissVoiceSettings.Mode.RANDOM_ALL) {
                graphics.drawCenteredString(font, Component.translatable("bond.morning_kiss.voice.random_all_hint"), listLeft + listWidth / 2, listTop + 12, BondGuiTokens.COLOR_TEXT_HINT);
            } else if (selectionOptions.isEmpty()) {
                graphics.drawCenteredString(font, Component.translatable("bond.morning_kiss.voice.no_entries"), listLeft + listWidth / 2, listTop + 12, BondGuiTokens.COLOR_TEXT_HINT);
            } else {
                selectionList.clamp(selectionOptions);
                selectionList.render(graphics, font, selectionOptions, contentMouseX, contentMouseY, this::renderSelectionOption);
            }
        }

        BondButtonRow.render(graphics, font, modal.left(), buttons(modal), contentMouseX, contentMouseY);
        modeDropdown.renderOverlay(graphics, font, modeOptions, selectedModeIndex(), mouseX, mouseY, this::renderModeOption);
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

        BondDropdown.ClickResult modeClick = modeDropdown.mouseClicked(mouseX, mouseY, modeOptions.size());
        if (modeClick.handled()) {
            if (modeClick.selectedIndex() >= 0 && modeClick.selectedIndex() < modeOptions.size()) {
                VoiceModeOption selectedMode = modeOptions.get(modeClick.selectedIndex());
                workingSettings = MorningKissVoiceSettings.of(selectedMode.mode().serializedName(), "", "", getSoundPackId());
                rebuildSelectionOptions();
            }
            return true;
        }

        String buttonId = BondButtonRow.click(buttons(modal), modal.left(), mouseX, mouseY);
        if (!buttonId.isEmpty()) {
            switch (buttonId) {
                case "save" -> {
                    MorningKissVoiceSettings settingsToSave = sanitizeSettingsForSave();
                    TouhouMaidAffection.CHANNEL.sendToServer(new MorningKissVoiceConfigPayload(
                            host.getMaid().getUUID(),
                            settingsToSave.mode().serializedName(),
                            settingsToSave.selectedGroup(),
                            settingsToSave.selectedClip(),
                            settingsToSave.soundPackId()
                    ));
                    BondClientStateCache.updateMorningKissVoiceSettings(host.getMaid().getUUID(), settingsToSave);
                    host.closeSecondaryPage();
                }
                case "clear" -> {
                    workingSettings = MorningKissVoiceSettings.DEFAULT.withSoundPackId(getSoundPackId());
                    TouhouMaidAffection.CHANNEL.sendToServer(new MorningKissVoiceConfigPayload(host.getMaid().getUUID(), workingSettings.mode().serializedName(), "", "", getSoundPackId()));
                    BondClientStateCache.updateMorningKissVoiceSettings(host.getMaid().getUUID(), workingSettings);
                    host.closeSecondaryPage();
                }
                case "cancel" -> host.closeSecondaryPage();
                default -> {
                }
            }
            return true;
        }

        if (workingSettings.mode() != MorningKissVoiceSettings.Mode.RANDOM_ALL && selectionList != null) {
            int optionIndex = selectionList.getHoveredIndex(mouseX, mouseY, selectionOptions.size());
            if (optionIndex >= 0 && optionIndex < selectionOptions.size()) {
                VoiceListOption option = selectionOptions.get(optionIndex);
                if (workingSettings.mode() == MorningKissVoiceSettings.Mode.RANDOM_GROUP && option.group()) {
                    workingSettings = MorningKissVoiceSettings.of(workingSettings.mode().serializedName(), option.key(), "", getSoundPackId());
                } else if (workingSettings.mode() == MorningKissVoiceSettings.Mode.SPECIFIC_CLIP && !option.group()) {
                    workingSettings = MorningKissVoiceSettings.of(workingSettings.mode().serializedName(), "", option.key(), getSoundPackId());
                }
                return true;
            }
        }

        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (modeDropdown.mouseScrolled(mouseX, mouseY, scrollY, modeOptions.size())) {
            return true;
        }
        return workingSettings.mode() != MorningKissVoiceSettings.Mode.RANDOM_ALL
                && selectionList != null
                && selectionList.scroll(mouseX, mouseY, scrollY, selectionOptions.size());
    }

    @Override
    public boolean onEscapePressed() {
        if (modeDropdown.isExpanded()) {
            modeDropdown.collapse();
            return true;
        }
        return false;
    }

    @Override
    public List<Component> getTooltip(int mouseX, int mouseY) {
        if (modeDropdown.isExpanded()) {
            return List.of();
        }
        int modeIndex = modeDropdown.getHoveredIndex(mouseX, mouseY, modeOptions.size());
        if (modeIndex >= 0 && modeIndex < modeOptions.size()) {
            VoiceModeOption option = modeOptions.get(modeIndex);
            return List.of(option.label(), option.detail());
        }
        if (modeDropdown.contains(mouseX, mouseY, modeOptions.size())) {
            VoiceModeOption option = modeOptions.get(selectedModeIndex());
            return List.of(option.label(), option.detail());
        }
        if (workingSettings.mode() != MorningKissVoiceSettings.Mode.RANDOM_ALL && selectionList != null) {
            int optionIndex = selectionList.getHoveredIndex(mouseX, mouseY, selectionOptions.size());
            if (optionIndex >= 0 && optionIndex < selectionOptions.size()) {
                VoiceListOption option = selectionOptions.get(optionIndex);
                return option.detail().equals(Component.empty()) ? List.of(option.label()) : List.of(option.label(), option.detail());
            }
        }
        return List.of();
    }

    private void initialize() {
        workingSettings = BondClientStateCache.getMorningKissVoiceSettings(host.getMaid().getUUID()).withSoundPackId(getSoundPackId());
        modeOptions = List.of(
                new VoiceModeOption(MorningKissVoiceSettings.Mode.RANDOM_ALL, Component.translatable("bond.morning_kiss.voice.mode.random_all"), Component.translatable("bond.morning_kiss.voice.mode.random_all.desc")),
                new VoiceModeOption(MorningKissVoiceSettings.Mode.RANDOM_GROUP, Component.translatable("bond.morning_kiss.voice.mode.random_group"), Component.translatable("bond.morning_kiss.voice.mode.random_group.desc")),
                new VoiceModeOption(MorningKissVoiceSettings.Mode.SPECIFIC_CLIP, Component.translatable("bond.morning_kiss.voice.mode.specific_clip"), Component.translatable("bond.morning_kiss.voice.mode.specific_clip.desc"))
        );

        BondModalPage modal = modal();
        int contentLeft = modal.contentLeft();
        int contentTop = modal.contentTop();
        int listTop = contentTop + 36;
        int listHeight = Math.max(LIST_ROW_HEIGHT, modal.footerTop() - listTop - BondGuiTokens.SPACING_SM);
        modeDropdown = new BondDropdown<>(contentLeft + 34, contentTop + 12, modal.contentRight() - (contentLeft + 34), DROPDOWN_HEIGHT, DROPDOWN_ROW_HEIGHT, DROPDOWN_VISIBLE_ROWS);
        selectionList = new BondScrollableList<>(contentLeft, listTop, modal.contentRight() - contentLeft, listHeight, LIST_ROW_HEIGHT);
        rebuildSelectionOptions();
    }

    private void rebuildSelectionOptions() {
        String soundPackId = getSoundPackId();
        if (soundPackId.isBlank()) {
            selectionOptions = List.of();
            return;
        }
        switch (workingSettings.mode()) {
            case RANDOM_ALL -> selectionOptions = List.of();
            case RANDOM_GROUP -> {
                List<VoiceListOption> options = new ArrayList<>();
                for (MorningKissVoiceIndex.VoiceGroup group : MorningKissVoiceIndex.getGroups(soundPackId)) {
                    options.add(VoiceListOption.group(group.key(), Component.literal(group.displayName()), Component.translatable("bond.morning_kiss.voice.group.entries", group.entryCount())));
                }
                selectionOptions = List.copyOf(options);
                if (workingSettings.selectedGroup().isBlank() || options.stream().noneMatch(option -> option.key().equals(workingSettings.selectedGroup()))) {
                    if (!options.isEmpty()) {
                        workingSettings = MorningKissVoiceSettings.of(workingSettings.mode().serializedName(), options.get(0).key(), "", soundPackId);
                    }
                }
            }
            case SPECIFIC_CLIP -> {
                List<VoiceListOption> options = new ArrayList<>();
                for (MorningKissVoiceIndex.VoiceEntry entry : MorningKissVoiceIndex.getEntries(soundPackId)) {
                    options.add(VoiceListOption.clip(entry.clipKey(), Component.literal(entry.groupDisplayName() + " / " + entry.displayName()), entry.detail()));
                }
                selectionOptions = List.copyOf(options);
                if (workingSettings.selectedClip().isBlank() || options.stream().noneMatch(option -> option.key().equals(workingSettings.selectedClip()))) {
                    if (!options.isEmpty()) {
                        workingSettings = MorningKissVoiceSettings.of(workingSettings.mode().serializedName(), "", options.get(0).key(), soundPackId);
                    }
                }
            }
        }
    }

    private MorningKissVoiceSettings sanitizeSettingsForSave() {
        String soundPackId = getSoundPackId();
        return switch (workingSettings.mode()) {
            case RANDOM_ALL -> MorningKissVoiceSettings.of(workingSettings.mode().serializedName(), "", "", soundPackId);
            case RANDOM_GROUP -> MorningKissVoiceSettings.of(workingSettings.mode().serializedName(), workingSettings.selectedGroup(), "", soundPackId);
            case SPECIFIC_CLIP -> MorningKissVoiceSettings.of(workingSettings.mode().serializedName(), "", workingSettings.selectedClip(), soundPackId);
        };
    }

    private void renderModeOption(GuiGraphics graphics, Font font, VoiceModeOption option, int index, int left, int top, int right, int height, boolean hovered, boolean selectedHeader) {
        if (!selectedHeader) {
            BondGuiTokens.drawSelectableRow(graphics, left - 4, top - 2, right, top + height + 2, index == selectedModeIndex(), hovered);
        }
        graphics.drawString(font, option.label(), left, top + 1, index == selectedModeIndex() ? BondGuiTokens.COLOR_TEXT_SELECTED : BondGuiTokens.COLOR_TEXT_BODY, false);
    }

    private void renderSelectionOption(GuiGraphics graphics, Font font, VoiceListOption option, int index, int left, int top, int right, int height, boolean hovered) {
        boolean selected = switch (workingSettings.mode()) {
            case RANDOM_GROUP -> option.group() && option.key().equals(workingSettings.selectedGroup());
            case SPECIFIC_CLIP -> !option.group() && option.key().equals(workingSettings.selectedClip());
            case RANDOM_ALL -> false;
        };
        BondGuiTokens.drawSelectableRow(graphics, left, top, right, top + height, selected, hovered);
        graphics.drawString(font, option.label(), left + 4, top + 3, selected ? BondGuiTokens.COLOR_TEXT_SELECTED : BondGuiTokens.COLOR_TEXT_BODY, false);
    }

    private int selectedModeIndex() {
        for (int i = 0; i < modeOptions.size(); i++) {
            if (modeOptions.get(i).mode() == workingSettings.mode()) {
                return i;
            }
        }
        return 0;
    }

    private BondModalPage modal() {
        return host.createModal(MODAL_WIDTH, MODAL_HEIGHT, Component.translatable("bond.morning_kiss.voice.title"));
    }

    private List<BondButtonRow.ButtonSpec> buttons(BondModalPage modal) {
        boolean saveEnabled = !getSoundPackId().isBlank();
        return BondButtonRow.createCenteredUniform(host.getFont(), modal.width(), modal.footerButtonY(BUTTON_HEIGHT), BUTTON_HEIGHT, BUTTON_GAP, BondGuiTokens.BUTTON_HORIZONTAL_PADDING,
                new BondButtonRow.ButtonSpec(0, 0, BondGuiTokens.BUTTON_MIN_WIDTH, BUTTON_HEIGHT, Component.translatable("bond.morning_kiss.voice.save"), "save", saveEnabled, true),
                new BondButtonRow.ButtonSpec(0, 0, BondGuiTokens.BUTTON_MIN_WIDTH, BUTTON_HEIGHT, Component.translatable("bond.morning_kiss.voice.clear"), "clear", true),
                new BondButtonRow.ButtonSpec(0, 0, BondGuiTokens.BUTTON_MIN_WIDTH, BUTTON_HEIGHT, Component.translatable("gui.cancel"), "cancel", true)
        );
    }

    private String getSoundPackId() {
        return host.getMaid() == null || host.getMaid().getSoundPackId() == null ? "" : host.getMaid().getSoundPackId();
    }

    private record VoiceModeOption(MorningKissVoiceSettings.Mode mode, Component label, Component detail) {
    }

    private record VoiceListOption(String key, Component label, Component detail, boolean group) {
        private static VoiceListOption group(String key, Component label, Component detail) {
            return new VoiceListOption(key, label, detail, true);
        }

        private static VoiceListOption clip(String key, Component label, Component detail) {
            return new VoiceListOption(key, label, detail, false);
        }
    }
}
