package com.github.touhoumaidaffection.client.screen.page;

import com.github.touhoumaidaffection.bond.EmergencyRescueVoiceSettings;
import com.github.touhoumaidaffection.bond.VoicePoolIds;
import com.github.touhoumaidaffection.bond.VoicePoolSelection;
import com.github.touhoumaidaffection.client.BondClientStateCache;
import com.github.touhoumaidaffection.client.EmergencyRescueSoundPlayer;
import com.github.touhoumaidaffection.client.RescueTlmVoiceIndex;
import com.github.touhoumaidaffection.client.screen.component.BondButtonRow;
import com.github.touhoumaidaffection.client.screen.component.BondGuiTokens;
import com.github.touhoumaidaffection.client.screen.component.BondModalPage;
import com.github.touhoumaidaffection.client.screen.component.BondVoicePoolList;
import com.github.touhoumaidaffection.network.RescueVoiceConfigPayload;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class EmergencyRescueVoiceSecondaryPage implements BondSecondaryPage {
    private static final int MODAL_WIDTH = 166;
    private static final int MODAL_HEIGHT = 132;
    private static final int LIST_ROW_HEIGHT = 14;
    private static final int BUTTON_HEIGHT = 17;
    private static final int HEADER_BUTTON_HEIGHT = 13;
    private static final int BUTTON_GAP = 3;

    private final BondSecondaryPageHost host;
    private EmergencyRescueVoiceSettings.CustomPlayMode playMode = EmergencyRescueVoiceSettings.CustomPlayMode.RANDOM;
    private final Set<String> selectedIds = new LinkedHashSet<>();
    private List<BondVoicePoolList.Entry> voiceEntries = List.of();
    private BondVoicePoolList voiceList;

    public EmergencyRescueVoiceSecondaryPage(BondSecondaryPageHost host) {
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
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        return voiceList.scroll(mouseX, mouseY, scrollY, voiceEntries.size());
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
        EmergencyRescueVoiceSettings current = BondClientStateCache.getEmergencyRescueVoiceSettings(host.getMaid().getUUID());
        playMode = current.customPlayMode() == EmergencyRescueVoiceSettings.CustomPlayMode.FIXED
                ? EmergencyRescueVoiceSettings.CustomPlayMode.RANDOM
                : current.customPlayMode();
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
                BondClientStateCache.getRescueDataPackVoiceMode(host.getMaid().getUUID()),
                BondClientStateCache.getRescueDataPackVoiceFiles(host.getMaid().getUUID())
        );
        for (String fileName : BondClientStateCache.getRescueDataPackVoiceFiles(host.getMaid().getUUID())) {
            entries.add(new BondVoicePoolList.Entry(
                    VoicePoolIds.dataPack(fileName),
                    Component.literal(fileName),
                    Component.translatable("bond.voice_pool.source.datapack"),
                    Component.literal("emergency_rescue/voices/" + fileName)
            ));
        }
        String soundPackId = getSoundPackId();
        if (includeBasePool && !soundPackId.isBlank()) {
            for (RescueTlmVoiceIndex.VoiceEntry entry : RescueTlmVoiceIndex.getEntries(soundPackId)) {
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
                BondClientStateCache.getRescueDataPackVoiceMode(host.getMaid().getUUID()),
                BondClientStateCache.getRescueDataPackVoiceFiles(host.getMaid().getUUID())
        )) {
            return BondClientStateCache.getRescueDataPackVoiceFiles(host.getMaid().getUUID()).stream()
                    .map(VoicePoolIds::dataPack)
                    .toList();
        }
        ArrayList<String> ids = new ArrayList<>();
        String soundPackId = getSoundPackId();
        if (!soundPackId.isBlank()) {
            ids.addAll(RescueTlmVoiceIndex.getEntries(soundPackId).stream()
                    .map(entry -> VoicePoolIds.tlm(entry.clipKey()))
                    .toList());
        }
        ids.addAll(BondClientStateCache.getRescueDataPackVoiceFiles(host.getMaid().getUUID()).stream()
                .map(VoicePoolIds::dataPack)
                .toList());
        return ids;
    }

    private void saveAndClose(boolean resetToDefault) {
        List<String> savedIds = resetToDefault ? List.of() : selectedIds.stream().toList();
        EmergencyRescueVoiceSettings settings = new EmergencyRescueVoiceSettings(
                EmergencyRescueVoiceSettings.SourceMode.TLM_PACK,
                EmergencyRescueVoiceSettings.TlmPlayMode.RANDOM_ALL,
                "",
                "",
                playMode,
                "",
                true,
                savedIds
        );
        PacketDistributor.sendToServer(new RescueVoiceConfigPayload(
                host.getMaid().getUUID(),
                settings.sourceMode().serializedName(),
                settings.tlmPlayMode().serializedName(),
                settings.tlmSelectedGroup(),
                settings.tlmSelectedClip(),
                settings.customPlayMode().serializedName(),
                settings.fixedFile(),
                settings.useCommonFallback(),
                settings.selectedVoiceIds()
        ));
        BondClientStateCache.updateEmergencyRescueVoiceSettings(host.getMaid().getUUID(), settings);
        EmergencyRescueSoundPlayer.invalidateCaches();
        host.closeSecondaryPage();
    }

    private EmergencyRescueVoiceSettings.CustomPlayMode nextMode(EmergencyRescueVoiceSettings.CustomPlayMode current) {
        return switch (current) {
            case RANDOM -> EmergencyRescueVoiceSettings.CustomPlayMode.SEQUENTIAL;
            case SEQUENTIAL -> EmergencyRescueVoiceSettings.CustomPlayMode.RANDOM;
            case FIXED -> EmergencyRescueVoiceSettings.CustomPlayMode.RANDOM;
        };
    }

    private Component playModeLabel() {
        return switch (playMode) {
            case RANDOM -> Component.translatable("bond.voice_pool.mode.random");
            case SEQUENTIAL -> Component.translatable("bond.voice_pool.mode.sequential");
            case FIXED -> Component.translatable("bond.voice_pool.mode.random");
        };
    }

    private BondModalPage modal() {
        return host.createModal(MODAL_WIDTH, MODAL_HEIGHT, Component.translatable("bond.emergency_rescue.voice.title"));
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

    private String getSoundPackId() {
        return host.getMaid() == null || host.getMaid().getSoundPackId() == null ? "" : host.getMaid().getSoundPackId();
    }
}
