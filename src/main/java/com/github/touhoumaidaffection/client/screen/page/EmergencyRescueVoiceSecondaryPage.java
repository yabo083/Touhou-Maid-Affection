package com.github.touhoumaidaffection.client.screen.page;

import com.github.touhoumaidaffection.bond.EmergencyRescueVoiceSettings;
import com.github.touhoumaidaffection.client.BondClientStateCache;
import com.github.touhoumaidaffection.client.EmergencyRescueCustomVoiceConfig;
import com.github.touhoumaidaffection.client.EmergencyRescueServerSoundSyncClient;
import com.github.touhoumaidaffection.client.EmergencyRescueSoundPlayer;
import com.github.touhoumaidaffection.client.RescueTlmVoiceIndex;
import com.github.touhoumaidaffection.client.screen.component.BondButtonRow;
import com.github.touhoumaidaffection.client.screen.component.BondDropdown;
import com.github.touhoumaidaffection.client.screen.component.BondModalPage;
import com.github.touhoumaidaffection.client.screen.component.BondScrollableList;
import com.github.touhoumaidaffection.network.RescueVoiceConfigPayload;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class EmergencyRescueVoiceSecondaryPage implements BondSecondaryPage {
    private static final int MODAL_WIDTH = 172;
    private static final int MODAL_HEIGHT = 178;
    private static final int DROPDOWN_HEIGHT = 16;
    private static final int DROPDOWN_ROW_HEIGHT = 16;
    private static final int DROPDOWN_VISIBLE_ROWS = 3;
    private static final int LIST_ROW_HEIGHT = 16;
    private static final int BUTTON_WIDTH = 46;
    private static final int BUTTON_HEIGHT = 16;
    private static final int BUTTON_GAP = 8;

    private final BondSecondaryPageHost host;
    private EmergencyRescueVoiceSettings workingSettings = EmergencyRescueVoiceSettings.DEFAULT;
    private List<SourceOption> sourceOptions = List.of();
    private List<TlmModeOption> modeOptions = List.of();
    private List<TlmListOption> selectionOptions = List.of();
    private List<CustomModeOption> customModeOptions = List.of();
    private List<FallbackOption> fallbackOptions = List.of();
    private List<FixedFileOption> fixedFileOptions = List.of();
    private BondDropdown<SourceOption> sourceDropdown;
    private BondDropdown<TlmModeOption> modeDropdown;
    private BondScrollableList<TlmListOption> selectionList;
    private BondDropdown<CustomModeOption> customModeDropdown;
    private BondDropdown<FallbackOption> fallbackDropdown;
    private BondScrollableList<FixedFileOption> fixedFileList;

    public EmergencyRescueVoiceSecondaryPage(BondSecondaryPageHost host) {
        this.host = host;
        initialize();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        Font font = host.getFont();
        BondModalPage modal = modal();
        modal.renderChrome(graphics, font);

        boolean sourceExpanded = sourceDropdown.isExpanded();
        boolean modeExpanded = modeDropdown.isExpanded();
        boolean customModeExpanded = customModeDropdown.isExpanded();
        boolean fallbackExpanded = fallbackDropdown.isExpanded();
        boolean anyDropdownExpanded = sourceExpanded || modeExpanded || customModeExpanded || fallbackExpanded;
        int contentMouseX = anyDropdownExpanded ? Integer.MIN_VALUE : mouseX;
        int contentMouseY = anyDropdownExpanded ? Integer.MIN_VALUE : mouseY;

        graphics.drawString(font, Component.translatable("bond.emergency_rescue.voice.source"), modal.left() + 10, modal.top() + 18, 0xFFD0D0D0, false);
        sourceDropdown.renderBase(graphics, font, sourceOptions, selectedSourceIndex(), mouseX, mouseY, this::renderSourceOption);

        if (workingSettings.sourceMode() == EmergencyRescueVoiceSettings.SourceMode.TLM_PACK) {
            Component packName = getSoundPackId().isBlank()
                    ? Component.translatable("bond.morning_kiss.voice.none")
                    : Component.literal(getSoundPackId());
            graphics.drawString(font, Component.translatable("bond.morning_kiss.voice.pack", packName), modal.left() + 10, modal.top() + 42, 0xFFE0E0E0, false);
            graphics.drawString(font, Component.translatable("bond.morning_kiss.voice.mode"), modal.left() + 10, modal.top() + 56, 0xFFD0D0D0, false);
            modeDropdown.renderBase(graphics, font, modeOptions, selectedModeIndex(), mouseX, mouseY, this::renderModeOption);

            int listLeft = modal.left() + 10;
            int listTop = modal.top() + 78;
            int listWidth = modal.width() - 20;
            int listHeight = 54;
            graphics.fill(listLeft, listTop, listLeft + listWidth, listTop + listHeight, 0xFF111111);
            if (!anyDropdownExpanded) {
                if (getSoundPackId().isBlank()) {
                    graphics.drawCenteredString(font, Component.translatable("bond.morning_kiss.voice.no_sound_pack"), listLeft + listWidth / 2, listTop + 24, 0xFFB0B0B0);
                } else if (workingSettings.tlmPlayMode() == EmergencyRescueVoiceSettings.TlmPlayMode.RANDOM_ALL) {
                    graphics.drawCenteredString(font, Component.translatable("bond.emergency_rescue.voice.tlm.random_all_hint"), listLeft + listWidth / 2, listTop + 24, 0xFFB0B0B0);
                } else if (selectionOptions.isEmpty()) {
                    graphics.drawCenteredString(font, Component.translatable("bond.emergency_rescue.voice.tlm.no_entries"), listLeft + listWidth / 2, listTop + 24, 0xFFB0B0B0);
                } else {
                    selectionList.clamp(selectionOptions);
                    selectionList.render(graphics, font, selectionOptions, contentMouseX, contentMouseY, this::renderSelectionOption);
                }
            }
        } else {
            int textX = modal.left() + 10;
            graphics.drawString(font, Component.translatable("bond.emergency_rescue.voice.custom.mode"), textX, modal.top() + 56, 0xFFD0D0D0, false);
            customModeDropdown.renderBase(graphics, font, customModeOptions, selectedCustomModeIndex(), mouseX, mouseY, this::renderCustomModeOption);

            graphics.drawString(font, Component.translatable("bond.emergency_rescue.voice.custom.fallback"), textX, modal.top() + 84, 0xFFD0D0D0, false);
            fallbackDropdown.renderBase(graphics, font, fallbackOptions, selectedFallbackIndex(), mouseX, mouseY, this::renderFallbackOption);

            graphics.drawString(font, Component.translatable("bond.emergency_rescue.voice.custom.fixed"), textX, modal.top() + 104, 0xFFD0D0D0, false);
            int listLeft = modal.left() + 10;
            int listTop = modal.top() + 114;
            int listWidth = modal.width() - 20;
            int listHeight = 24;
            graphics.fill(listLeft, listTop, listLeft + listWidth, listTop + listHeight, 0xFF111111);
            if (!anyDropdownExpanded) {
                if (workingSettings.customPlayMode() != EmergencyRescueVoiceSettings.CustomPlayMode.FIXED) {
                    graphics.drawCenteredString(font, Component.translatable("bond.emergency_rescue.voice.custom.fixed.hint"), listLeft + listWidth / 2, listTop + 3, 0xFFB0B0B0);
                } else if (fixedFileOptions.isEmpty()) {
                    graphics.drawCenteredString(font, Component.translatable("bond.emergency_rescue.voice.custom.fixed.none"), listLeft + listWidth / 2, listTop + 3, 0xFFB0B0B0);
                } else {
                    fixedFileList.clamp(fixedFileOptions);
                    fixedFileList.render(graphics, font, fixedFileOptions, contentMouseX, contentMouseY, this::renderFixedFileOption);
                }
            }

            graphics.drawString(font, Component.translatable("bond.emergency_rescue.voice.custom.tip2"), textX, modal.top() + 140, 0xFFB8B8B8, false);
            graphics.drawString(font, Component.translatable("bond.emergency_rescue.voice.custom.tip3"), textX, modal.top() + 150, 0xFF8AD8FF, false);
        }

        BondButtonRow.render(graphics, font, modal.left(), buttons(modal), contentMouseX, contentMouseY);
        sourceDropdown.renderOverlay(graphics, font, sourceOptions, selectedSourceIndex(), mouseX, mouseY, this::renderSourceOption);
        modeDropdown.renderOverlay(graphics, font, modeOptions, selectedModeIndex(), mouseX, mouseY, this::renderModeOption);
        customModeDropdown.renderOverlay(graphics, font, customModeOptions, selectedCustomModeIndex(), mouseX, mouseY, this::renderCustomModeOption);
        fallbackDropdown.renderOverlay(graphics, font, fallbackOptions, selectedFallbackIndex(), mouseX, mouseY, this::renderFallbackOption);
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

        BondDropdown.ClickResult sourceClick = sourceDropdown.mouseClicked(mouseX, mouseY, sourceOptions.size());
        if (sourceClick.handled()) {
            if (sourceClick.selectedIndex() >= 0 && sourceClick.selectedIndex() < sourceOptions.size()) {
                SourceOption sourceOption = sourceOptions.get(sourceClick.selectedIndex());
                workingSettings = new EmergencyRescueVoiceSettings(
                        sourceOption.mode(),
                        workingSettings.tlmPlayMode(),
                        workingSettings.tlmSelectedGroup(),
                        workingSettings.tlmSelectedClip(),
                        workingSettings.customPlayMode(),
                        workingSettings.fixedFile(),
                        workingSettings.useCommonFallback()
                );
                rebuildSelectionOptions();
                rebuildCustomFileOptions();
            }
            return true;
        }

        if (workingSettings.sourceMode() == EmergencyRescueVoiceSettings.SourceMode.TLM_PACK) {
            BondDropdown.ClickResult modeClick = modeDropdown.mouseClicked(mouseX, mouseY, modeOptions.size());
            if (modeClick.handled()) {
                if (modeClick.selectedIndex() >= 0 && modeClick.selectedIndex() < modeOptions.size()) {
                    TlmModeOption selectedMode = modeOptions.get(modeClick.selectedIndex());
                    workingSettings = new EmergencyRescueVoiceSettings(
                            workingSettings.sourceMode(),
                            selectedMode.mode(),
                            "",
                            "",
                            workingSettings.customPlayMode(),
                            workingSettings.fixedFile(),
                            workingSettings.useCommonFallback()
                    );
                    rebuildSelectionOptions();
                }
                return true;
            }
        } else {
            BondDropdown.ClickResult customModeClick = customModeDropdown.mouseClicked(mouseX, mouseY, customModeOptions.size());
            if (customModeClick.handled()) {
                if (customModeClick.selectedIndex() >= 0 && customModeClick.selectedIndex() < customModeOptions.size()) {
                    CustomModeOption selectedMode = customModeOptions.get(customModeClick.selectedIndex());
                    String fixedFile = workingSettings.fixedFile();
                    if (selectedMode.mode() == EmergencyRescueVoiceSettings.CustomPlayMode.FIXED && fixedFile.isBlank()) {
                        fixedFile = firstAvailableFixedFile();
                    }
                    workingSettings = new EmergencyRescueVoiceSettings(
                            workingSettings.sourceMode(),
                            workingSettings.tlmPlayMode(),
                            workingSettings.tlmSelectedGroup(),
                            workingSettings.tlmSelectedClip(),
                            selectedMode.mode(),
                            fixedFile,
                            workingSettings.useCommonFallback()
                    );
                    rebuildCustomFileOptions();
                }
                return true;
            }
            BondDropdown.ClickResult fallbackClick = fallbackDropdown.mouseClicked(mouseX, mouseY, fallbackOptions.size());
            if (fallbackClick.handled()) {
                if (fallbackClick.selectedIndex() >= 0 && fallbackClick.selectedIndex() < fallbackOptions.size()) {
                    FallbackOption selectedFallback = fallbackOptions.get(fallbackClick.selectedIndex());
                    workingSettings = new EmergencyRescueVoiceSettings(
                            workingSettings.sourceMode(),
                            workingSettings.tlmPlayMode(),
                            workingSettings.tlmSelectedGroup(),
                            workingSettings.tlmSelectedClip(),
                            workingSettings.customPlayMode(),
                            workingSettings.fixedFile(),
                            selectedFallback.enabled()
                    );
                }
                return true;
            }
        }

        String buttonId = BondButtonRow.click(buttons(modal), modal.left(), mouseX, mouseY);
        if (!buttonId.isEmpty()) {
            switch (buttonId) {
                case "save" -> {
                    EmergencyRescueVoiceSettings toSave = sanitizeForSave();
                    PacketDistributor.sendToServer(new RescueVoiceConfigPayload(
                            host.getMaid().getUUID(),
                            toSave.sourceMode().serializedName(),
                            toSave.tlmPlayMode().serializedName(),
                            toSave.tlmSelectedGroup(),
                            toSave.tlmSelectedClip(),
                            toSave.customPlayMode().serializedName(),
                            toSave.fixedFile(),
                            toSave.useCommonFallback()
                    ));
                    EmergencyRescueCustomVoiceConfig.saveMaidSettings(
                            EmergencyRescueCustomVoiceConfig.localMaidDir(host.getMaid().getUUID().toString(), host.getMaid().getName().getString()),
                            toSave
                    );
                    BondClientStateCache.updateEmergencyRescueVoiceSettings(host.getMaid().getUUID(), toSave);
                    EmergencyRescueSoundPlayer.invalidateCaches();
                    host.closeSecondaryPage();
                }
                case "clear" -> {
                    workingSettings = EmergencyRescueVoiceSettings.DEFAULT;
                    PacketDistributor.sendToServer(new RescueVoiceConfigPayload(
                            host.getMaid().getUUID(),
                            workingSettings.sourceMode().serializedName(),
                            workingSettings.tlmPlayMode().serializedName(),
                            "",
                            "",
                            workingSettings.customPlayMode().serializedName(),
                            "",
                            workingSettings.useCommonFallback()
                    ));
                    EmergencyRescueCustomVoiceConfig.saveMaidSettings(
                            EmergencyRescueCustomVoiceConfig.localMaidDir(host.getMaid().getUUID().toString(), host.getMaid().getName().getString()),
                            workingSettings
                    );
                    BondClientStateCache.updateEmergencyRescueVoiceSettings(host.getMaid().getUUID(), workingSettings);
                    EmergencyRescueSoundPlayer.invalidateCaches();
                    host.closeSecondaryPage();
                }
                case "cancel" -> host.closeSecondaryPage();
                default -> {
                }
            }
            return true;
        }

        if (workingSettings.sourceMode() == EmergencyRescueVoiceSettings.SourceMode.TLM_PACK
                && workingSettings.tlmPlayMode() != EmergencyRescueVoiceSettings.TlmPlayMode.RANDOM_ALL
                && selectionList != null) {
            int optionIndex = selectionList.getHoveredIndex(mouseX, mouseY, selectionOptions.size());
            if (optionIndex >= 0 && optionIndex < selectionOptions.size()) {
                TlmListOption option = selectionOptions.get(optionIndex);
                if (workingSettings.tlmPlayMode() == EmergencyRescueVoiceSettings.TlmPlayMode.RANDOM_GROUP && option.group()) {
                    workingSettings = new EmergencyRescueVoiceSettings(
                            workingSettings.sourceMode(),
                            workingSettings.tlmPlayMode(),
                            option.key(),
                            "",
                            workingSettings.customPlayMode(),
                            workingSettings.fixedFile(),
                            workingSettings.useCommonFallback()
                    );
                } else if (workingSettings.tlmPlayMode() == EmergencyRescueVoiceSettings.TlmPlayMode.SPECIFIC_CLIP && !option.group()) {
                    workingSettings = new EmergencyRescueVoiceSettings(
                            workingSettings.sourceMode(),
                            workingSettings.tlmPlayMode(),
                            "",
                            option.key(),
                            workingSettings.customPlayMode(),
                            workingSettings.fixedFile(),
                            workingSettings.useCommonFallback()
                    );
                }
            }
        } else if (workingSettings.sourceMode() == EmergencyRescueVoiceSettings.SourceMode.CUSTOM_FS
                && workingSettings.customPlayMode() == EmergencyRescueVoiceSettings.CustomPlayMode.FIXED
                && fixedFileList != null) {
            int optionIndex = fixedFileList.getHoveredIndex(mouseX, mouseY, fixedFileOptions.size());
            if (optionIndex >= 0 && optionIndex < fixedFileOptions.size()) {
                FixedFileOption option = fixedFileOptions.get(optionIndex);
                workingSettings = new EmergencyRescueVoiceSettings(
                        workingSettings.sourceMode(),
                        workingSettings.tlmPlayMode(),
                        workingSettings.tlmSelectedGroup(),
                        workingSettings.tlmSelectedClip(),
                        workingSettings.customPlayMode(),
                        option.fileName(),
                        workingSettings.useCommonFallback()
                );
            }
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (sourceDropdown.mouseScrolled(mouseX, mouseY, scrollY, sourceOptions.size())) {
            return true;
        }
        if (workingSettings.sourceMode() == EmergencyRescueVoiceSettings.SourceMode.TLM_PACK
                && modeDropdown.mouseScrolled(mouseX, mouseY, scrollY, modeOptions.size())) {
            return true;
        }
        if (workingSettings.sourceMode() == EmergencyRescueVoiceSettings.SourceMode.CUSTOM_FS) {
            if (customModeDropdown.mouseScrolled(mouseX, mouseY, scrollY, customModeOptions.size())) {
                return true;
            }
            if (fallbackDropdown.mouseScrolled(mouseX, mouseY, scrollY, fallbackOptions.size())) {
                return true;
            }
        }
        if (workingSettings.sourceMode() == EmergencyRescueVoiceSettings.SourceMode.CUSTOM_FS
                && workingSettings.customPlayMode() == EmergencyRescueVoiceSettings.CustomPlayMode.FIXED
                && fixedFileList != null
                && fixedFileList.scroll(mouseX, mouseY, scrollY, fixedFileOptions.size())) {
            return true;
        }
        return workingSettings.sourceMode() == EmergencyRescueVoiceSettings.SourceMode.TLM_PACK
                && workingSettings.tlmPlayMode() != EmergencyRescueVoiceSettings.TlmPlayMode.RANDOM_ALL
                && selectionList != null
                && selectionList.scroll(mouseX, mouseY, scrollY, selectionOptions.size());
    }

    @Override
    public List<Component> getTooltip(int mouseX, int mouseY) {
        if (sourceDropdown.isExpanded() || modeDropdown.isExpanded() || customModeDropdown.isExpanded() || fallbackDropdown.isExpanded()) {
            return List.of();
        }
        int sourceIndex = sourceDropdown.getHoveredIndex(mouseX, mouseY, sourceOptions.size());
        if (sourceIndex >= 0 && sourceIndex < sourceOptions.size()) {
            SourceOption option = sourceOptions.get(sourceIndex);
            return List.of(option.label(), option.detail());
        }
        if (workingSettings.sourceMode() == EmergencyRescueVoiceSettings.SourceMode.TLM_PACK) {
            int modeIndex = modeDropdown.getHoveredIndex(mouseX, mouseY, modeOptions.size());
            if (modeIndex >= 0 && modeIndex < modeOptions.size()) {
                TlmModeOption option = modeOptions.get(modeIndex);
                return List.of(option.label(), option.detail());
            }
            if (workingSettings.tlmPlayMode() != EmergencyRescueVoiceSettings.TlmPlayMode.RANDOM_ALL && selectionList != null) {
                int optionIndex = selectionList.getHoveredIndex(mouseX, mouseY, selectionOptions.size());
                if (optionIndex >= 0 && optionIndex < selectionOptions.size()) {
                    TlmListOption option = selectionOptions.get(optionIndex);
                    return option.detail().equals(Component.empty()) ? List.of(option.label()) : List.of(option.label(), option.detail());
                }
            }
        } else {
            int modeIndex = customModeDropdown.getHoveredIndex(mouseX, mouseY, customModeOptions.size());
            if (modeIndex >= 0 && modeIndex < customModeOptions.size()) {
                CustomModeOption option = customModeOptions.get(modeIndex);
                return List.of(option.label(), option.detail());
            }
            int fallbackIndex = fallbackDropdown.getHoveredIndex(mouseX, mouseY, fallbackOptions.size());
            if (fallbackIndex >= 0 && fallbackIndex < fallbackOptions.size()) {
                FallbackOption option = fallbackOptions.get(fallbackIndex);
                return List.of(option.label(), option.detail());
            }
            if (workingSettings.customPlayMode() == EmergencyRescueVoiceSettings.CustomPlayMode.FIXED && fixedFileList != null) {
                int fixedIndex = fixedFileList.getHoveredIndex(mouseX, mouseY, fixedFileOptions.size());
                if (fixedIndex >= 0 && fixedIndex < fixedFileOptions.size()) {
                    FixedFileOption option = fixedFileOptions.get(fixedIndex);
                    return option.detail().equals(Component.empty()) ? List.of(option.label()) : List.of(option.label(), option.detail());
                }
            }
        }
        return List.of();
    }

    private void initialize() {
        workingSettings = BondClientStateCache.getEmergencyRescueVoiceSettings(host.getMaid().getUUID());
        sourceOptions = List.of(
                new SourceOption(EmergencyRescueVoiceSettings.SourceMode.TLM_PACK, Component.translatable("bond.emergency_rescue.voice.source.tlm"), Component.translatable("bond.emergency_rescue.voice.source.tlm.desc")),
                new SourceOption(EmergencyRescueVoiceSettings.SourceMode.CUSTOM_FS, Component.translatable("bond.emergency_rescue.voice.source.custom"), Component.translatable("bond.emergency_rescue.voice.source.custom.desc"))
        );
        modeOptions = List.of(
                new TlmModeOption(EmergencyRescueVoiceSettings.TlmPlayMode.RANDOM_ALL, Component.translatable("bond.morning_kiss.voice.mode.random_all"), Component.translatable("bond.morning_kiss.voice.mode.random_all.desc")),
                new TlmModeOption(EmergencyRescueVoiceSettings.TlmPlayMode.RANDOM_GROUP, Component.translatable("bond.morning_kiss.voice.mode.random_group"), Component.translatable("bond.morning_kiss.voice.mode.random_group.desc")),
                new TlmModeOption(EmergencyRescueVoiceSettings.TlmPlayMode.SPECIFIC_CLIP, Component.translatable("bond.morning_kiss.voice.mode.specific_clip"), Component.translatable("bond.morning_kiss.voice.mode.specific_clip.desc"))
        );
        customModeOptions = List.of(
                new CustomModeOption(EmergencyRescueVoiceSettings.CustomPlayMode.RANDOM, Component.translatable("bond.emergency_rescue.voice.custom.mode.random"), Component.translatable("bond.emergency_rescue.voice.custom.mode.random.desc")),
                new CustomModeOption(EmergencyRescueVoiceSettings.CustomPlayMode.SEQUENTIAL, Component.translatable("bond.emergency_rescue.voice.custom.mode.sequential"), Component.translatable("bond.emergency_rescue.voice.custom.mode.sequential.desc")),
                new CustomModeOption(EmergencyRescueVoiceSettings.CustomPlayMode.FIXED, Component.translatable("bond.emergency_rescue.voice.custom.mode.fixed"), Component.translatable("bond.emergency_rescue.voice.custom.mode.fixed.desc"))
        );
        fallbackOptions = List.of(
                new FallbackOption(true, Component.translatable("bond.emergency_rescue.voice.custom.fallback.on"), Component.translatable("bond.emergency_rescue.voice.custom.fallback.on.desc")),
                new FallbackOption(false, Component.translatable("bond.emergency_rescue.voice.custom.fallback.off"), Component.translatable("bond.emergency_rescue.voice.custom.fallback.off.desc"))
        );

        BondModalPage modal = modal();
        sourceDropdown = new BondDropdown<>(modal.left() + 44, modal.top() + 15, modal.width() - 54, DROPDOWN_HEIGHT, DROPDOWN_ROW_HEIGHT, DROPDOWN_VISIBLE_ROWS);
        modeDropdown = new BondDropdown<>(modal.left() + 44, modal.top() + 53, modal.width() - 54, DROPDOWN_HEIGHT, DROPDOWN_ROW_HEIGHT, DROPDOWN_VISIBLE_ROWS);
        selectionList = new BondScrollableList<>(modal.left() + 10, modal.top() + 78, modal.width() - 20, 54, LIST_ROW_HEIGHT);
        customModeDropdown = new BondDropdown<>(modal.left() + 44, modal.top() + 53, modal.width() - 54, DROPDOWN_HEIGHT, DROPDOWN_ROW_HEIGHT, DROPDOWN_VISIBLE_ROWS);
        fallbackDropdown = new BondDropdown<>(modal.left() + 44, modal.top() + 81, modal.width() - 54, DROPDOWN_HEIGHT, DROPDOWN_ROW_HEIGHT, DROPDOWN_VISIBLE_ROWS);
        fixedFileList = new BondScrollableList<>(modal.left() + 10, modal.top() + 114, modal.width() - 20, 24, LIST_ROW_HEIGHT);
        rebuildSelectionOptions();
        rebuildCustomFileOptions();
    }

    private void rebuildSelectionOptions() {
        if (workingSettings.sourceMode() != EmergencyRescueVoiceSettings.SourceMode.TLM_PACK) {
            selectionOptions = List.of();
            return;
        }
        String soundPackId = getSoundPackId();
        if (soundPackId.isBlank()) {
            selectionOptions = List.of();
            return;
        }
        switch (workingSettings.tlmPlayMode()) {
            case RANDOM_ALL -> selectionOptions = List.of();
            case RANDOM_GROUP -> {
                List<TlmListOption> options = new ArrayList<>();
                for (RescueTlmVoiceIndex.VoiceGroup group : RescueTlmVoiceIndex.getGroups(soundPackId)) {
                    options.add(TlmListOption.group(group.key(), Component.literal(group.displayName()), Component.translatable("bond.morning_kiss.voice.group.entries", group.entryCount())));
                }
                selectionOptions = List.copyOf(options);
                if (workingSettings.tlmSelectedGroup().isBlank() || options.stream().noneMatch(option -> option.key().equals(workingSettings.tlmSelectedGroup()))) {
                    if (!options.isEmpty()) {
                        workingSettings = new EmergencyRescueVoiceSettings(
                                workingSettings.sourceMode(),
                                workingSettings.tlmPlayMode(),
                                options.getFirst().key(),
                                "",
                                workingSettings.customPlayMode(),
                                workingSettings.fixedFile(),
                                workingSettings.useCommonFallback()
                        );
                    }
                }
            }
            case SPECIFIC_CLIP -> {
                List<TlmListOption> options = new ArrayList<>();
                for (RescueTlmVoiceIndex.VoiceEntry entry : RescueTlmVoiceIndex.getEntries(soundPackId)) {
                    options.add(TlmListOption.clip(entry.clipKey(), Component.literal(entry.groupDisplayName() + " / " + entry.displayName()), entry.detail()));
                }
                selectionOptions = List.copyOf(options);
                if (workingSettings.tlmSelectedClip().isBlank() || options.stream().noneMatch(option -> option.key().equals(workingSettings.tlmSelectedClip()))) {
                    if (!options.isEmpty()) {
                        workingSettings = new EmergencyRescueVoiceSettings(
                                workingSettings.sourceMode(),
                                workingSettings.tlmPlayMode(),
                                "",
                                options.getFirst().key(),
                                workingSettings.customPlayMode(),
                                workingSettings.fixedFile(),
                                workingSettings.useCommonFallback()
                        );
                    }
                }
            }
        }
    }

    private void rebuildCustomFileOptions() {
        if (workingSettings.sourceMode() != EmergencyRescueVoiceSettings.SourceMode.CUSTOM_FS) {
            fixedFileOptions = List.of();
            return;
        }
        List<String> files = collectCustomFileNames();
        ArrayList<FixedFileOption> options = new ArrayList<>(files.size() + 2);
        options.add(new FixedFileOption("", Component.translatable("bond.emergency_rescue.voice.custom.fixed.auto"), Component.empty()));
        for (String file : files) {
            options.add(new FixedFileOption(file, Component.literal(file), Component.empty()));
        }
        if (!workingSettings.fixedFile().isBlank() && files.stream().noneMatch(name -> name.equalsIgnoreCase(workingSettings.fixedFile()))) {
            options.add(new FixedFileOption(
                    workingSettings.fixedFile(),
                    Component.translatable("bond.emergency_rescue.voice.custom.fixed.missing", workingSettings.fixedFile()),
                    Component.translatable("bond.emergency_rescue.voice.custom.fixed.missing", workingSettings.fixedFile())
            ));
        }
        fixedFileOptions = List.copyOf(options);
    }

    private List<String> collectCustomFileNames() {
        Set<String> output = new LinkedHashSet<>();
        Path localMaidDir = EmergencyRescueCustomVoiceConfig.localMaidDir(host.getMaid().getUUID().toString(), host.getMaid().getName().getString());
        addAudioFileNames(localMaidDir, output);
        addAudioFileNames(EmergencyRescueCustomVoiceConfig.localCommonDir(), output);

        String serverId = EmergencyRescueServerSoundSyncClient.getActiveServerId();
        addAudioFileNames(EmergencyRescueCustomVoiceConfig.syncedMaidDir(serverId, host.getMaid().getUUID().toString(), host.getMaid().getName().getString()), output);
        addAudioFileNames(EmergencyRescueCustomVoiceConfig.syncedCommonDir(serverId), output);

        return output.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    private void addAudioFileNames(Path directory, Set<String> output) {
        if (directory == null || !Files.isDirectory(directory)) {
            return;
        }
        try (var stream = Files.list(directory)) {
            stream.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.toLowerCase(Locale.ROOT).endsWith(".ogg"))
                    .sorted(Comparator.naturalOrder())
                    .forEach(output::add);
        } catch (IOException ignored) {
        }
    }

    private String firstAvailableFixedFile() {
        for (FixedFileOption option : fixedFileOptions) {
            if (!option.fileName().isBlank()) {
                return option.fileName();
            }
        }
        return "";
    }

    private EmergencyRescueVoiceSettings sanitizeForSave() {
        return switch (workingSettings.sourceMode()) {
            case TLM_PACK -> switch (workingSettings.tlmPlayMode()) {
                case RANDOM_ALL -> new EmergencyRescueVoiceSettings(
                        EmergencyRescueVoiceSettings.SourceMode.TLM_PACK,
                        EmergencyRescueVoiceSettings.TlmPlayMode.RANDOM_ALL,
                        "",
                        "",
                        workingSettings.customPlayMode(),
                        workingSettings.fixedFile(),
                        workingSettings.useCommonFallback()
                );
                case RANDOM_GROUP -> new EmergencyRescueVoiceSettings(
                        EmergencyRescueVoiceSettings.SourceMode.TLM_PACK,
                        EmergencyRescueVoiceSettings.TlmPlayMode.RANDOM_GROUP,
                        workingSettings.tlmSelectedGroup(),
                        "",
                        workingSettings.customPlayMode(),
                        workingSettings.fixedFile(),
                        workingSettings.useCommonFallback()
                );
                case SPECIFIC_CLIP -> new EmergencyRescueVoiceSettings(
                        EmergencyRescueVoiceSettings.SourceMode.TLM_PACK,
                        EmergencyRescueVoiceSettings.TlmPlayMode.SPECIFIC_CLIP,
                        "",
                        workingSettings.tlmSelectedClip(),
                        workingSettings.customPlayMode(),
                        workingSettings.fixedFile(),
                        workingSettings.useCommonFallback()
                );
            };
            case CUSTOM_FS -> new EmergencyRescueVoiceSettings(
                    EmergencyRescueVoiceSettings.SourceMode.CUSTOM_FS,
                    workingSettings.tlmPlayMode(),
                    workingSettings.tlmSelectedGroup(),
                    workingSettings.tlmSelectedClip(),
                    workingSettings.customPlayMode(),
                    workingSettings.fixedFile(),
                    workingSettings.useCommonFallback()
            );
        };
    }

    private void renderSourceOption(GuiGraphics graphics, Font font, SourceOption option, int index, int left, int top, int right, int height, boolean hovered, boolean selectedHeader) {
        if (!selectedHeader) {
            int fill = index == selectedSourceIndex() ? 0xFF4F4A32 : 0xFF2B2B2B;
            graphics.fill(left - 4, top - 2, right, top + height + 2, fill);
            if (hovered) {
                graphics.fill(left - 4, top - 2, right, top + height + 2, 0x22FFFFFF);
            }
        }
        graphics.drawString(font, option.label(), left, top + 1, index == selectedSourceIndex() ? 0xFFFFE08A : 0xFFE0E0E0, false);
    }

    private void renderModeOption(GuiGraphics graphics, Font font, TlmModeOption option, int index, int left, int top, int right, int height, boolean hovered, boolean selectedHeader) {
        if (!selectedHeader) {
            int fill = index == selectedModeIndex() ? 0xFF4F4A32 : 0xFF2B2B2B;
            graphics.fill(left - 4, top - 2, right, top + height + 2, fill);
            if (hovered) {
                graphics.fill(left - 4, top - 2, right, top + height + 2, 0x22FFFFFF);
            }
        }
        graphics.drawString(font, option.label(), left, top + 1, index == selectedModeIndex() ? 0xFFFFE08A : 0xFFE0E0E0, false);
    }

    private void renderCustomModeOption(GuiGraphics graphics, Font font, CustomModeOption option, int index, int left, int top, int right, int height, boolean hovered, boolean selectedHeader) {
        if (!selectedHeader) {
            int fill = index == selectedCustomModeIndex() ? 0xFF4F4A32 : 0xFF2B2B2B;
            graphics.fill(left - 4, top - 2, right, top + height + 2, fill);
            if (hovered) {
                graphics.fill(left - 4, top - 2, right, top + height + 2, 0x22FFFFFF);
            }
        }
        graphics.drawString(font, option.label(), left, top + 1, index == selectedCustomModeIndex() ? 0xFFFFE08A : 0xFFE0E0E0, false);
    }

    private void renderFallbackOption(GuiGraphics graphics, Font font, FallbackOption option, int index, int left, int top, int right, int height, boolean hovered, boolean selectedHeader) {
        if (!selectedHeader) {
            int fill = index == selectedFallbackIndex() ? 0xFF4F4A32 : 0xFF2B2B2B;
            graphics.fill(left - 4, top - 2, right, top + height + 2, fill);
            if (hovered) {
                graphics.fill(left - 4, top - 2, right, top + height + 2, 0x22FFFFFF);
            }
        }
        graphics.drawString(font, option.label(), left, top + 1, index == selectedFallbackIndex() ? 0xFFFFE08A : 0xFFE0E0E0, false);
    }

    private void renderSelectionOption(GuiGraphics graphics, Font font, TlmListOption option, int index, int left, int top, int right, int height, boolean hovered) {
        boolean selected = switch (workingSettings.tlmPlayMode()) {
            case RANDOM_GROUP -> option.group() && option.key().equals(workingSettings.tlmSelectedGroup());
            case SPECIFIC_CLIP -> !option.group() && option.key().equals(workingSettings.tlmSelectedClip());
            case RANDOM_ALL -> false;
        };
        int fill = selected ? 0xFF4F4A32 : 0xFF2B2B2B;
        graphics.fill(left, top, right, top + height, fill);
        if (hovered) {
            graphics.fill(left, top, right, top + height, 0x33FFFFFF);
        }
        graphics.drawString(font, option.label(), left + 4, top + 3, selected ? 0xFFFFE08A : 0xFFE0E0E0, false);
    }

    private void renderFixedFileOption(GuiGraphics graphics, Font font, FixedFileOption option, int index, int left, int top, int right, int height, boolean hovered) {
        boolean selected = option.fileName().equalsIgnoreCase(workingSettings.fixedFile());
        if (workingSettings.fixedFile().isBlank() && option.fileName().isBlank()) {
            selected = true;
        }
        int fill = selected ? 0xFF4F4A32 : 0xFF2B2B2B;
        graphics.fill(left, top, right, top + height, fill);
        if (hovered) {
            graphics.fill(left, top, right, top + height, 0x33FFFFFF);
        }
        graphics.drawString(font, option.label(), left + 4, top + 3, selected ? 0xFFFFE08A : 0xFFE0E0E0, false);
    }

    private int selectedSourceIndex() {
        for (int i = 0; i < sourceOptions.size(); i++) {
            if (sourceOptions.get(i).mode() == workingSettings.sourceMode()) {
                return i;
            }
        }
        return 0;
    }

    private int selectedModeIndex() {
        for (int i = 0; i < modeOptions.size(); i++) {
            if (modeOptions.get(i).mode() == workingSettings.tlmPlayMode()) {
                return i;
            }
        }
        return 0;
    }

    private int selectedCustomModeIndex() {
        for (int i = 0; i < customModeOptions.size(); i++) {
            if (customModeOptions.get(i).mode() == workingSettings.customPlayMode()) {
                return i;
            }
        }
        return 0;
    }

    private int selectedFallbackIndex() {
        for (int i = 0; i < fallbackOptions.size(); i++) {
            if (fallbackOptions.get(i).enabled() == workingSettings.useCommonFallback()) {
                return i;
            }
        }
        return 0;
    }

    private BondModalPage modal() {
        return host.createModal(MODAL_WIDTH, MODAL_HEIGHT, Component.translatable("bond.emergency_rescue.voice.title"));
    }

    private List<BondButtonRow.ButtonSpec> buttons(BondModalPage modal) {
        boolean saveEnabled = workingSettings.sourceMode() != EmergencyRescueVoiceSettings.SourceMode.TLM_PACK || !getSoundPackId().isBlank();
        return BondButtonRow.createCentered(modal.width(), modal.bottom() - 22, BUTTON_WIDTH, BUTTON_HEIGHT, BUTTON_GAP,
                new BondButtonRow.ButtonSpec(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("bond.morning_kiss.voice.save"), "save", saveEnabled),
                new BondButtonRow.ButtonSpec(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("bond.morning_kiss.voice.clear"), "clear", true),
                new BondButtonRow.ButtonSpec(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("gui.cancel"), "cancel", true)
        );
    }

    private String getSoundPackId() {
        return host.getMaid() == null || host.getMaid().getSoundPackId() == null ? "" : host.getMaid().getSoundPackId();
    }

    private record SourceOption(EmergencyRescueVoiceSettings.SourceMode mode, Component label, Component detail) {
    }

    private record TlmModeOption(EmergencyRescueVoiceSettings.TlmPlayMode mode, Component label, Component detail) {
    }

    private record TlmListOption(String key, Component label, Component detail, boolean group) {
        private static TlmListOption group(String key, Component label, Component detail) {
            return new TlmListOption(key, label, detail, true);
        }

        private static TlmListOption clip(String key, Component label, Component detail) {
            return new TlmListOption(key, label, detail, false);
        }
    }

    private record CustomModeOption(EmergencyRescueVoiceSettings.CustomPlayMode mode, Component label, Component detail) {
    }

    private record FallbackOption(boolean enabled, Component label, Component detail) {
    }

    private record FixedFileOption(String fileName, Component label, Component detail) {
    }
}
