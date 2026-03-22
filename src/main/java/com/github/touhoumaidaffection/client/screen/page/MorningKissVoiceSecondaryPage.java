package com.github.touhoumaidaffection.client.screen.page;

import com.github.touhoumaidaffection.bond.MorningKissVoiceSettings;
import com.github.touhoumaidaffection.client.BondClientStateCache;
import com.github.touhoumaidaffection.client.MorningKissVoiceIndex;
import com.github.touhoumaidaffection.client.screen.component.BondButtonRow;
import com.github.touhoumaidaffection.client.screen.component.BondDropdown;
import com.github.touhoumaidaffection.client.screen.component.BondModalPage;
import com.github.touhoumaidaffection.client.screen.component.BondScrollableList;
import com.github.touhoumaidaffection.network.MorningKissVoiceConfigPayload;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public final class MorningKissVoiceSecondaryPage implements BondSecondaryPage {
    private static final int MODAL_WIDTH = 172;
    private static final int MODAL_HEIGHT = 126;
    private static final int DROPDOWN_HEIGHT = 16;
    private static final int DROPDOWN_ROW_HEIGHT = 16;
    private static final int DROPDOWN_VISIBLE_ROWS = 3;
    private static final int LIST_ROW_HEIGHT = 16;
    private static final int BUTTON_WIDTH = 46;
    private static final int BUTTON_HEIGHT = 16;
    private static final int BUTTON_GAP = 8;

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

        Component packName = getSoundPackId().isBlank()
                ? Component.translatable("bond.morning_kiss.voice.none")
                : Component.literal(getSoundPackId());
        graphics.drawString(font, Component.translatable("bond.morning_kiss.voice.pack", packName), modal.left() + 10, modal.top() + 18, 0xFFE0E0E0, false);
        graphics.drawString(font, Component.translatable("bond.morning_kiss.voice.mode"), modal.left() + 10, modal.top() + 32, 0xFFD0D0D0, false);
        modeDropdown.renderBase(graphics, font, modeOptions, selectedModeIndex(), mouseX, mouseY, this::renderModeOption);

        int listLeft = modal.left() + 10;
        int listTop = modal.top() + 54;
        int listWidth = modal.width() - 20;
        int listHeight = 32;
        graphics.fill(listLeft, listTop, listLeft + listWidth, listTop + listHeight, 0xFF111111);
        if (!dropdownExpanded) {
            if (getSoundPackId().isBlank()) {
                graphics.drawCenteredString(font, Component.translatable("bond.morning_kiss.voice.no_sound_pack"), listLeft + listWidth / 2, listTop + 12, 0xFFB0B0B0);
            } else if (workingSettings.mode() == MorningKissVoiceSettings.Mode.RANDOM_ALL) {
                graphics.drawCenteredString(font, Component.translatable("bond.morning_kiss.voice.random_all_hint"), listLeft + listWidth / 2, listTop + 12, 0xFFB0B0B0);
            } else if (selectionOptions.isEmpty()) {
                graphics.drawCenteredString(font, Component.translatable("bond.morning_kiss.voice.no_entries"), listLeft + listWidth / 2, listTop + 12, 0xFFB0B0B0);
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
                    PacketDistributor.sendToServer(new MorningKissVoiceConfigPayload(
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
                    PacketDistributor.sendToServer(new MorningKissVoiceConfigPayload(host.getMaid().getUUID(), workingSettings.mode().serializedName(), "", "", getSoundPackId()));
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
        modeDropdown = new BondDropdown<>(modal.left() + 44, modal.top() + 29, modal.width() - 54, DROPDOWN_HEIGHT, DROPDOWN_ROW_HEIGHT, DROPDOWN_VISIBLE_ROWS);
        selectionList = new BondScrollableList<>(modal.left() + 10, modal.top() + 54, modal.width() - 20, 32, LIST_ROW_HEIGHT);
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
                        workingSettings = MorningKissVoiceSettings.of(workingSettings.mode().serializedName(), options.getFirst().key(), "", soundPackId);
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
                        workingSettings = MorningKissVoiceSettings.of(workingSettings.mode().serializedName(), "", options.getFirst().key(), soundPackId);
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
            int fill = index == selectedModeIndex() ? 0xFF4F4A32 : 0xFF2B2B2B;
            graphics.fill(left - 4, top - 2, right, top + height + 2, fill);
            if (hovered) {
                graphics.fill(left - 4, top - 2, right, top + height + 2, 0x22FFFFFF);
            }
        }
        graphics.drawString(font, option.label(), left, top + 1, index == selectedModeIndex() ? 0xFFFFE08A : 0xFFE0E0E0, false);
    }

    private void renderSelectionOption(GuiGraphics graphics, Font font, VoiceListOption option, int index, int left, int top, int right, int height, boolean hovered) {
        boolean selected = switch (workingSettings.mode()) {
            case RANDOM_GROUP -> option.group() && option.key().equals(workingSettings.selectedGroup());
            case SPECIFIC_CLIP -> !option.group() && option.key().equals(workingSettings.selectedClip());
            case RANDOM_ALL -> false;
        };
        int fill = selected ? 0xFF4F4A32 : 0xFF2B2B2B;
        graphics.fill(left, top, right, top + height, fill);
        if (hovered) {
            graphics.fill(left, top, right, top + height, 0x33FFFFFF);
        }
        graphics.drawString(font, option.label(), left + 4, top + 3, selected ? 0xFFFFE08A : 0xFFE0E0E0, false);
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
        return BondButtonRow.createCentered(modal.width(), modal.bottom() - 22, BUTTON_WIDTH, BUTTON_HEIGHT, BUTTON_GAP,
                new BondButtonRow.ButtonSpec(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("bond.morning_kiss.voice.save"), "save", saveEnabled),
                new BondButtonRow.ButtonSpec(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("bond.morning_kiss.voice.clear"), "clear", true),
                new BondButtonRow.ButtonSpec(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("gui.cancel"), "cancel", true)
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
