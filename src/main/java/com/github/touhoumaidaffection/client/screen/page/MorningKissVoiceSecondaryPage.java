package com.github.touhoumaidaffection.client.screen.page;

import com.github.touhoumaidaffection.bond.MorningKissVoiceSettings;
import com.github.touhoumaidaffection.bond.VoicePoolIds;
import com.github.touhoumaidaffection.bond.VoicePoolSelection;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.github.touhoumaidaffection.client.BondClientStateCache;
import com.github.touhoumaidaffection.client.BondKeyMappings;
import com.github.touhoumaidaffection.client.MorningKissVoiceIndex;
import com.github.touhoumaidaffection.client.VoicePreviewPlayback;
import com.github.touhoumaidaffection.client.screen.component.BondButtonRow;
import com.github.touhoumaidaffection.client.screen.component.BondGuiTokens;
import com.github.touhoumaidaffection.client.screen.component.BondModalPage;
import com.github.touhoumaidaffection.client.screen.component.BondVoicePoolList;
import com.github.touhoumaidaffection.network.MorningKissVoiceConfigPayload;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class MorningKissVoiceSecondaryPage implements BondSecondaryPage {
    private static final int MODAL_WIDTH = 166;
    private static final int MODAL_HEIGHT = 132;
    private static final int LIST_ROW_HEIGHT = 14;
    private static final int BUTTON_HEIGHT = 17;
    private static final int HEADER_BUTTON_HEIGHT = 13;
    private static final int BUTTON_GAP = 3;

    private final BondSecondaryPageHost host;
    private MorningKissVoiceSettings.Mode playMode = MorningKissVoiceSettings.Mode.RANDOM_ALL;
    private final Set<String> selectedIds = new LinkedHashSet<>();
    private List<BondVoicePoolList.Entry> voiceEntries = List.of();
    private BondVoicePoolList voiceList;

    public MorningKissVoiceSecondaryPage(BondSecondaryPageHost host) {
        this.host = host;
        initialize();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        Font font = host.getFont();
        BondModalPage modal = modal();
        modal.renderChrome(graphics, font);

        int contentLeft = modal.contentLeft();
        int contentTop = modal.contentTop();
        int contentWidth = modal.contentRight() - contentLeft;
        Component packName = getSoundPackId().isBlank()
                ? Component.translatable("bond.morning_kiss.voice.none")
                : Component.literal(getSoundPackId());
        Component packLine = Component.translatable("bond.morning_kiss.voice.pack", packName);
        graphics.drawString(font, font.plainSubstrByWidth(packLine.getString(), contentWidth - 48), contentLeft, contentTop, BondGuiTokens.COLOR_TEXT_BODY, false);
        BondButtonRow.render(graphics, font, modal.left(), headerButtons(modal), mouseX, mouseY);

        int buttonY = modal.footerButtonY(BUTTON_HEIGHT);
        int listTop = contentTop + 16;
        int listHeight = Math.max(LIST_ROW_HEIGHT * 4, buttonY - listTop - 3);
        if (voiceEntries.isEmpty()) {
            BondGuiTokens.drawFramedPanel(graphics, contentLeft, listTop, contentLeft + contentWidth, listTop + listHeight, BondGuiTokens.COLOR_BG_ELEMENT);
            graphics.drawCenteredString(font, Component.translatable("bond.voice_pool.no_entries"), contentLeft + contentWidth / 2, listTop + 12, BondGuiTokens.COLOR_TEXT_HINT);
        } else {
            voiceList.render(graphics, font, voiceEntries, selectedIds, mouseX, mouseY);
        }

        BondButtonRow.render(graphics, font, modal.left(), buttons(modal), mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1 || BondKeyMappings.VOICE_PREVIEW.matchesMouse(button)) {
            return true;
        }
        if (button != 0) {
            return true;
        }
        BondModalPage modal = modal();
        if (!modal.contains(mouseX, mouseY)) {
            host.closeSecondaryPage();
            return true;
        }

        String buttonId = BondButtonRow.click(buttons(modal), modal.left(), mouseX, mouseY);
        if (buttonId.isEmpty()) {
            buttonId = BondButtonRow.click(headerButtons(modal), modal.left(), mouseX, mouseY);
        }
        if (!buttonId.isEmpty()) {
            switch (buttonId) {
                case "mode" -> playMode = nextMode(playMode);
                case "toggle_all" -> toggleAll();
                case "save" -> saveAndClose(false);
                case "cancel" -> host.closeSecondaryPage();
                default -> {
                }
            }
            return true;
        }

        int optionIndex = voiceList.getHoveredIndex(mouseX, mouseY, voiceEntries.size());
        if (optionIndex >= 0 && optionIndex < voiceEntries.size()) {
            String id = voiceEntries.get(optionIndex).id();
            if (!selectedIds.remove(id)) {
                selectedIds.add(id);
            }
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 1 || BondKeyMappings.VOICE_PREVIEW.matchesMouse(button)) {
            return previewHoveredVoice(mouseX, mouseY, button);
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        return voiceList.scroll(mouseX, mouseY, scrollY, voiceEntries.size());
    }

    @Override
    public boolean previewSelectedVoice() {
        return previewEntryId(firstSelectedOrFirstEntryId());
    }

    @Override
    public List<Component> getTooltip(int mouseX, int mouseY) {
        int optionIndex = voiceList.getHoveredIndex(mouseX, mouseY, voiceEntries.size());
        if (optionIndex >= 0 && optionIndex < voiceEntries.size()) {
            BondVoicePoolList.Entry entry = voiceEntries.get(optionIndex);
            return entry.detail().equals(Component.empty()) ? List.of(entry.label()) : List.of(entry.label(), entry.detail());
        }
        return List.of();
    }

    private void initialize() {
        MorningKissVoiceSettings current = BondClientStateCache.getMorningKissVoiceSettings(host.getMaid().getUUID()).withSoundPackId(getSoundPackId());
        playMode = current.mode() == MorningKissVoiceSettings.Mode.SPECIFIC_CLIP
                ? MorningKissVoiceSettings.Mode.RANDOM_ALL
                : current.mode();
        voiceEntries = buildEntries();
        selectedIds.clear();
        selectedIds.addAll(VoicePoolSelection.initialSelection(current.selectedVoiceIds(), defaultSelectedIds(), allEntryIds()));

        BondModalPage modal = modal();
        int contentLeft = modal.contentLeft();
        int contentTop = modal.contentTop();
        int buttonY = modal.footerButtonY(BUTTON_HEIGHT);
        int listTop = contentTop + 16;
        int listHeight = Math.max(LIST_ROW_HEIGHT * 4, buttonY - listTop - 3);
        voiceList = new BondVoicePoolList(contentLeft, listTop, modal.contentRight() - contentLeft, listHeight, LIST_ROW_HEIGHT);
    }

    private List<BondVoicePoolList.Entry> buildEntries() {
        List<BondVoicePoolList.Entry> entries = new ArrayList<>();
        boolean includeBasePool = VoicePoolSelection.shouldIncludeBasePool(
                BondClientStateCache.getMorningKissDataPackVoiceMode(host.getMaid().getUUID()),
                BondClientStateCache.getMorningKissDataPackVoiceFiles(host.getMaid().getUUID())
        );
        if (includeBasePool) {
            entries.add(new BondVoicePoolList.Entry(
                    VoicePoolIds.BUILTIN_MORNING_KISS,
                    Component.translatable("bond.voice_pool.builtin_morning_kiss"),
                    Component.translatable("bond.voice_pool.source.builtin"),
                    Component.translatable("bond.voice_pool.builtin_morning_kiss.desc")
            ));
        }
        for (String fileName : BondClientStateCache.getMorningKissDataPackVoiceFiles(host.getMaid().getUUID())) {
            entries.add(new BondVoicePoolList.Entry(
                    VoicePoolIds.dataPack(fileName),
                    Component.literal(fileName),
                    Component.translatable("bond.voice_pool.source.datapack"),
                    Component.literal("morning_kiss/voices/" + fileName)
            ));
        }
        String soundPackId = getSoundPackId();
        if (includeBasePool && !soundPackId.isBlank()) {
            for (MorningKissVoiceIndex.VoiceEntry entry : MorningKissVoiceIndex.getEntries(soundPackId)) {
                entries.add(new BondVoicePoolList.Entry(
                        VoicePoolIds.tlm(entry.clipKey()),
                        Component.literal(entry.groupDisplayName() + " / " + entry.displayName()),
                        Component.translatable("bond.voice_pool.source.tlm"),
                        entry.detail()
                ));
            }
        }
        return List.copyOf(entries);
    }

    private List<String> defaultSelectedIds() {
        if (!VoicePoolSelection.shouldIncludeBasePool(
                BondClientStateCache.getMorningKissDataPackVoiceMode(host.getMaid().getUUID()),
                BondClientStateCache.getMorningKissDataPackVoiceFiles(host.getMaid().getUUID())
        )) {
            return BondClientStateCache.getMorningKissDataPackVoiceFiles(host.getMaid().getUUID()).stream()
                    .map(VoicePoolIds::dataPack)
                    .toList();
        }
        ArrayList<String> ids = new ArrayList<>();
        ids.add(VoicePoolIds.BUILTIN_MORNING_KISS);
        ids.addAll(BondClientStateCache.getMorningKissDataPackVoiceFiles(host.getMaid().getUUID()).stream()
                .map(VoicePoolIds::dataPack)
                .toList());
        return ids;
    }

    private void saveAndClose(boolean resetToDefault) {
        List<String> savedIds = resetToDefault ? List.of() : selectedIds.stream().toList();
        MorningKissVoiceSettings settings = MorningKissVoiceSettings.of(playMode.serializedName(), "", "", getSoundPackId(), savedIds);
        PacketDistributor.sendToServer(new MorningKissVoiceConfigPayload(
                host.getMaid().getUUID(),
                settings.mode().serializedName(),
                settings.selectedGroup(),
                settings.selectedClip(),
                settings.soundPackId(),
                settings.selectedVoiceIds()
        ));
        BondClientStateCache.updateMorningKissVoiceSettings(host.getMaid().getUUID(), settings);
        host.closeSecondaryPage();
    }

    private MorningKissVoiceSettings.Mode nextMode(MorningKissVoiceSettings.Mode current) {
        return switch (current) {
            case RANDOM_ALL -> MorningKissVoiceSettings.Mode.RANDOM_GROUP;
            case RANDOM_GROUP -> MorningKissVoiceSettings.Mode.RANDOM_ALL;
            case SPECIFIC_CLIP -> MorningKissVoiceSettings.Mode.RANDOM_ALL;
        };
    }

    private Component playModeLabel() {
        return switch (playMode) {
            case RANDOM_ALL -> Component.translatable("bond.voice_pool.mode.random");
            case RANDOM_GROUP -> Component.translatable("bond.voice_pool.mode.sequential");
            case SPECIFIC_CLIP -> Component.translatable("bond.voice_pool.mode.random");
        };
    }

    private BondModalPage modal() {
        return host.createModal(MODAL_WIDTH, MODAL_HEIGHT, Component.translatable("bond.morning_kiss.voice.title"));
    }

    private List<BondButtonRow.ButtonSpec> buttons(BondModalPage modal) {
        return BondButtonRow.createCenteredUniform(host.getFont(), modal.width(), modal.footerButtonY(BUTTON_HEIGHT), BUTTON_HEIGHT, BUTTON_GAP, BondGuiTokens.BUTTON_HORIZONTAL_PADDING,
                new BondButtonRow.ButtonSpec(0, 0, 42, BUTTON_HEIGHT, Component.translatable("bond.morning_kiss.voice.save"), "save", !selectedIds.isEmpty(), true),
                new BondButtonRow.ButtonSpec(0, 0, 42, BUTTON_HEIGHT, playModeLabel(), "mode", true),
                new BondButtonRow.ButtonSpec(0, 0, 42, BUTTON_HEIGHT, Component.translatable("gui.cancel"), "cancel", true)
        );
    }

    private List<BondButtonRow.ButtonSpec> headerButtons(BondModalPage modal) {
        int y = modal.contentTop() - 1;
        int right = modal.contentRight() - modal.left();
        return List.of(
                new BondButtonRow.ButtonSpec(right - 42, y, 42, HEADER_BUTTON_HEIGHT, toggleAllLabel(), "toggle_all", !voiceEntries.isEmpty())
        );
    }

    private List<String> allEntryIds() {
        return voiceEntries.stream().map(BondVoicePoolList.Entry::id).toList();
    }

    private void toggleAll() {
        if (selectedIds.size() == voiceEntries.size() && !voiceEntries.isEmpty()) {
            selectedIds.clear();
            return;
        }
        selectedIds.clear();
        selectedIds.addAll(allEntryIds());
    }

    private Component toggleAllLabel() {
        return selectedIds.size() == voiceEntries.size() && !voiceEntries.isEmpty()
                ? Component.translatable("bond.voice_pool.select_none")
                : Component.translatable("bond.voice_pool.select_all");
    }

    private boolean previewHoveredVoice(double mouseX, double mouseY, int button) {
        int optionIndex = voiceList.getHoveredIndex(mouseX, mouseY, voiceEntries.size());
        if (optionIndex < 0 || optionIndex >= voiceEntries.size()) {
            TouhouMaidAffection.LOGGER.info("Morning kiss voice preview skipped: button={}, mouse=({}, {}), hovered={}, entries={}",
                    button, mouseX, mouseY, optionIndex, voiceEntries.size());
            return true;
        }
        String id = voiceEntries.get(optionIndex).id();
        TouhouMaidAffection.LOGGER.info("Morning kiss voice preview requested: button={}, index={}, id={}", button, optionIndex, id);
        previewEntryId(id);
        return true;
    }

    private boolean previewEntryId(String id) {
        return VoicePreviewPlayback.playMorningKiss(host.getMaid(), id);
    }

    private String firstSelectedOrFirstEntryId() {
        if (!selectedIds.isEmpty()) {
            return selectedIds.iterator().next();
        }
        return voiceEntries.isEmpty() ? "" : voiceEntries.get(0).id();
    }

    private String getSoundPackId() {
        return host.getMaid() == null || host.getMaid().getSoundPackId() == null ? "" : host.getMaid().getSoundPackId();
    }
}
