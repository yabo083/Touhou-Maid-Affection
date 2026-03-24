package com.github.touhoumaidaffection.client.screen.page;

import com.github.touhoumaidaffection.client.RescueYsmActionConfig;
import com.github.touhoumaidaffection.client.YsmModelActionIndex;
import com.github.touhoumaidaffection.client.screen.component.BondButtonRow;
import com.github.touhoumaidaffection.client.screen.component.BondGuiTokens;
import com.github.touhoumaidaffection.client.screen.component.BondModalPage;
import com.github.touhoumaidaffection.client.screen.component.BondScrollableList;
import com.github.touhoumaidaffection.network.RescueActionConfigPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public final class RescueActionSecondaryPage implements BondSecondaryPage {
    private static final int MODAL_WIDTH = BondGuiTokens.SECONDARY_MODAL_WIDTH;
    private static final int MODAL_HEIGHT = BondGuiTokens.SECONDARY_MODAL_HEIGHT;
    private static final int LIST_ROW_HEIGHT = BondGuiTokens.LIST_ROW_HEIGHT;
    private static final int BUTTON_HEIGHT = BondGuiTokens.CONTROL_HEIGHT;
    private static final int BUTTON_GAP = BondGuiTokens.SPACING_MD;

    private final BondSecondaryPageHost host;
    private List<RescueActionOption> actionOptions = List.of();
    private BondScrollableList<RescueActionOption> actionList;
    private String selectedActionId = "";

    public RescueActionSecondaryPage(BondSecondaryPageHost host) {
        this.host = host;
        initialize();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        Font font = host.getFont();
        BondModalPage modal = modal();
        modal.renderChrome(graphics, font);
        if (actionList == null) {
            initialize();
        }
        actionList.clamp(actionOptions);
        actionList.render(graphics, font, actionOptions, mouseX, mouseY, (g, f, entry, index, left, top, right, height, hovered) -> {
            boolean selected = entry.actionId().equals(selectedActionId) || (entry.actionId().isBlank() && selectedActionId.isBlank());
            BondGuiTokens.drawSelectableRow(g, left, top, right, top + height, selected, hovered);
            g.drawString(f, entry.label(), left + 4, top + 3, selected ? BondGuiTokens.COLOR_TEXT_SELECTED : BondGuiTokens.COLOR_TEXT_BODY, false);
        });
        if (actionOptions.size() <= 1) {
            graphics.drawCenteredString(font, Component.translatable("bond.emergency_rescue.action.none_detected"), modal.left() + modal.width() / 2, actionList.top() + 18, BondGuiTokens.COLOR_TEXT_HINT);
            graphics.drawCenteredString(font, Component.translatable("bond.emergency_rescue.action.none_detected.tip"), modal.left() + modal.width() / 2, actionList.top() + 34, BondGuiTokens.COLOR_TEXT_DISABLED);
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
        if (!buttonId.isEmpty()) {
            switch (buttonId) {
                case "save" -> {
                    RescueYsmActionConfig.setSelectedAction(getModelId(), getTextureId(), selectedActionId);
                    PacketDistributor.sendToServer(new RescueActionConfigPayload(host.getMaid().getUUID(), selectedActionId));
                    host.closeSecondaryPage();
                }
                case "clear" -> {
                    selectedActionId = "";
                    RescueYsmActionConfig.setSelectedAction(getModelId(), getTextureId(), "");
                    PacketDistributor.sendToServer(new RescueActionConfigPayload(host.getMaid().getUUID(), ""));
                    host.closeSecondaryPage();
                }
                case "cancel" -> host.closeSecondaryPage();
                default -> {
                }
            }
            return true;
        }

        if (actionList != null) {
            int hoveredIndex = actionList.getHoveredIndex(mouseX, mouseY, actionOptions.size());
            if (hoveredIndex >= 0 && hoveredIndex < actionOptions.size()) {
                selectedActionId = actionOptions.get(hoveredIndex).actionId();
            }
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        return actionList != null && actionList.scroll(mouseX, mouseY, scrollY, actionOptions.size());
    }

    @Override
    public List<Component> getTooltip(int mouseX, int mouseY) {
        if (actionList == null) {
            return List.of();
        }
        int hoveredIndex = actionList.getHoveredIndex(mouseX, mouseY, actionOptions.size());
        if (hoveredIndex < 0 || hoveredIndex >= actionOptions.size()) {
            return List.of();
        }
        RescueActionOption option = actionOptions.get(hoveredIndex);
        List<Component> lines = new ArrayList<>();
        lines.add(option.label());
        if (!option.detail().equals(Component.empty())) {
            lines.add(option.detail());
        }
        return lines;
    }

    private void initialize() {
        selectedActionId = RescueYsmActionConfig.getSelectedAction(getModelId(), getTextureId());
        List<RescueActionOption> options = new ArrayList<>();
        options.add(new RescueActionOption(
                "",
                Component.translatable("bond.emergency_rescue.action.default").copy().append(Component.literal(" (内置)").withStyle(ChatFormatting.DARK_GRAY)),
                Component.translatable("bond.emergency_rescue.action.default.tooltip")
        ));
        for (YsmModelActionIndex.DetectedYsmAction action : YsmModelActionIndex.getActions(getModelId(), getTextureId())) {
            options.add(new RescueActionOption(action.actionId(), Component.literal(action.displayName()), Component.literal(action.actionId()).withStyle(ChatFormatting.DARK_GRAY)));
        }
        actionOptions = List.copyOf(options);

        BondModalPage modal = modal();
        int listTop = modal.contentTop();
        int listHeight = Math.max(LIST_ROW_HEIGHT, modal.footerTop() - listTop - BondGuiTokens.SPACING_SM);
        actionList = new BondScrollableList<>(modal.contentLeft(), listTop, modal.contentRight() - modal.contentLeft(), listHeight, LIST_ROW_HEIGHT);
        actionList.clamp(actionOptions);
    }

    private BondModalPage modal() {
        return host.createModal(MODAL_WIDTH, MODAL_HEIGHT, Component.translatable("bond.emergency_rescue.action.title"));
    }

    private List<BondButtonRow.ButtonSpec> buttons(BondModalPage modal) {
        return BondButtonRow.createCenteredUniform(host.getFont(), modal.width(), modal.footerButtonY(BUTTON_HEIGHT), BUTTON_HEIGHT, BUTTON_GAP, BondGuiTokens.BUTTON_HORIZONTAL_PADDING,
                new BondButtonRow.ButtonSpec(0, 0, BondGuiTokens.BUTTON_MIN_WIDTH, BUTTON_HEIGHT, Component.translatable("bond.emergency_rescue.action.save"), "save", true, true),
                new BondButtonRow.ButtonSpec(0, 0, BondGuiTokens.BUTTON_MIN_WIDTH, BUTTON_HEIGHT, Component.translatable("bond.emergency_rescue.action.clear"), "clear", true),
                new BondButtonRow.ButtonSpec(0, 0, BondGuiTokens.BUTTON_MIN_WIDTH, BUTTON_HEIGHT, Component.translatable("gui.cancel"), "cancel", true)
        );
    }

    private String getModelId() {
        return host.getMaid() == null || host.getMaid().getYsmModelId() == null ? "" : host.getMaid().getYsmModelId();
    }

    private String getTextureId() {
        return host.getMaid() == null || host.getMaid().getYsmModelTexture() == null ? "" : host.getMaid().getYsmModelTexture();
    }

    private record RescueActionOption(String actionId, Component label, Component detail) {
    }
}
