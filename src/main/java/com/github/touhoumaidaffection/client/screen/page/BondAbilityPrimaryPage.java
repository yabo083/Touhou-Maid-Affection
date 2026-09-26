package com.github.touhoumaidaffection.client.screen.page;

import com.github.touhoumaidaffection.bond.ability.IBondAbility;
import com.github.touhoumaidaffection.client.BondClientStateCache;
import com.github.touhoumaidaffection.client.RescueYsmActionConfig;
import com.github.touhoumaidaffection.client.screen.component.BondAbilityListPanel;
import com.github.touhoumaidaffection.client.screen.component.BondAbilityRowLayout;
import com.github.touhoumaidaffection.client.screen.component.BondGuiArt;
import com.github.touhoumaidaffection.client.screen.component.BondGuiTokens;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public final class BondAbilityPrimaryPage {
    private final BondPrimaryPageHost host;
    private final BondAbilityListPanel listPanel;
    private final int rowHeight;
    private final int buttonWidth;
    private final int secondaryButtonWidth;
    private final int buttonHeight;
    private final int secondaryButtonGap;
    private final int panelX;
    private final int panelWidth;
    private final int settingsButtonX;
    private final int settingsButtonY;
    private static final int SETTINGS_BUTTON_WIDTH = 50;
    private static final int SETTINGS_BUTTON_HEIGHT = 12;

    public BondAbilityPrimaryPage(BondPrimaryPageHost host,
                                  int panelX,
                                  int panelY,
                                  int panelWidth,
                                  int panelHeight,
                                  int rowStartY,
                                  int rowHeight,
                                  int rowSpacing,
                                  int buttonWidth,
                                  int secondaryButtonWidth,
                                  int buttonHeight,
                                  int secondaryButtonGap) {
        this.host = host;
        this.listPanel = new BondAbilityListPanel(panelX, panelY, panelWidth, panelHeight, rowStartY, rowHeight, rowSpacing);
        this.rowHeight = rowHeight;
        this.buttonWidth = buttonWidth;
        this.secondaryButtonWidth = secondaryButtonWidth;
        this.buttonHeight = buttonHeight;
        this.secondaryButtonGap = secondaryButtonGap;
        this.panelX = panelX;
        this.panelWidth = panelWidth;
        // Sits in the top-right slot of the panel frame (where the removed AI button used to be),
        // kept inside the bond page frame so the screen's click routing reaches this button.
        this.settingsButtonX = panelX + panelWidth - SETTINGS_BUTTON_WIDTH - 2;
        this.settingsButtonY = panelY - SETTINGS_BUTTON_HEIGHT - 3;
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        if (host.getMaid() == null) {
            return;
        }
        Font font = host.getFont();
        Player player = host.getLocalPlayer();
        int powerPoints = host.getPowerPointCount();
        boolean unlocked = host.isBondUnlocked();
        int rowLeft = panelX + 2;

        renderSettingsButton(graphics, font, mouseX, mouseY);

        listPanel.renderViewport(graphics, () -> {
            int visible = listPanel.getVisibleRowCount();
            List<IBondAbility> abilities = host.getAbilities();
            for (int i = 0; i < Math.min(abilities.size(), visible); i++) {
                int rowY = listPanel.getRowTop(i);
                renderAbilityRow(graphics, font, abilities.get(i), player, powerPoints, unlocked, rowLeft, rowY, mouseX, mouseY);
            }
        });
    }

    public boolean mouseClicked(double mouseX, double mouseY) {
        if (host.getMaid() == null) {
            return false;
        }
        if (isSettingsButtonHovered(mouseX, mouseY)) {
            host.openSettingsPage();
            return true;
        }
        if (!listPanel.contains(mouseX, mouseY)) {
            return false;
        }
        List<IBondAbility> abilities = host.getAbilities();
        int index = listPanel.getRowIndexAt(mouseX, mouseY, abilities.size());
        if (index < 0 || index >= abilities.size()) {
            return true;
        }

        Player player = host.getLocalPlayer();
        int powerPoints = host.getPowerPointCount();
        boolean unlocked = host.isBondUnlocked();
        IBondAbility ability = abilities.get(index);
        boolean abilityUnlocked = BondClientStateCache.isAbilityUnlocked(host.getMaid().getUUID(), ability.getId());
        boolean enoughPowerPoint = powerPoints >= ability.getPowerPointCost();
        boolean canUnlockNow = player != null && ability.canUnlock(player, host.getMaid());
        boolean canUseSecondary = player != null && abilityUnlocked && ability.hasSecondaryAction() && ability.canPerformSecondaryAction(player, host.getMaid());
        boolean hasSecondaryButton = host.hasSecondaryPageButton(ability, abilityUnlocked);

        BondAbilityRowLayout layout = createLayout(index, hasSecondaryButton);
        if (hasSecondaryButton && layout.containsSecondaryButton(mouseX, mouseY, secondaryButtonWidth, buttonHeight)) {
            host.openSecondaryPageForAbility(ability);
            return true;
        }
        if (layout.containsMainButton(mouseX, mouseY, buttonWidth, buttonHeight)) {
            boolean clickable = host.isMainButtonClickable(
                    ability, unlocked, abilityUnlocked, enoughPowerPoint, canUnlockNow, canUseSecondary);
            if (clickable) {
                if (host.isEmergencyHealAbility(ability) && abilityUnlocked && host.isRescueActionConfigAvailable()) {
                    host.openEmergencyRescueActionPage();
                } else {
                    host.activateAbility(ability);
                }
            } else if (player != null) {
                Component reason = host.getStatusText(
                        ability, unlocked, abilityUnlocked, enoughPowerPoint, canUnlockNow, canUseSecondary);
                player.displayClientMessage(Component.translatable("bond.unlock_click_blocked", reason), true);
            }
        }
        return true;
    }

    public List<Component> getTooltip(int mouseX, int mouseY) {
        if (host.getMaid() == null) {
            return List.of();
        }
        if (isSettingsButtonHovered(mouseX, mouseY)) {
            return List.of(
                    Component.translatable("bond.settings.title"),
                    Component.translatable("bond.settings.entry.tip").withStyle(ChatFormatting.GRAY)
            );
        }
        if (!listPanel.contains(mouseX, mouseY)) {
            return List.of();
        }
        List<IBondAbility> abilities = host.getAbilities();
        int index = listPanel.getRowIndexAt(mouseX, mouseY, abilities.size());
        if (index < 0 || index >= abilities.size()) {
            return List.of();
        }

        Player player = host.getLocalPlayer();
        int powerPoints = host.getPowerPointCount();
        boolean unlocked = host.isBondUnlocked();
        IBondAbility ability = abilities.get(index);
        boolean abilityUnlocked = BondClientStateCache.isAbilityUnlocked(host.getMaid().getUUID(), ability.getId());
        boolean enoughPowerPoint = powerPoints >= ability.getPowerPointCost();
        boolean canUnlockNow = player != null && ability.canUnlock(player, host.getMaid());
        boolean canUseSecondary = player != null && abilityUnlocked && ability.hasSecondaryAction() && ability.canPerformSecondaryAction(player, host.getMaid());
        boolean hasSecondaryButton = host.hasSecondaryPageButton(ability, abilityUnlocked);
        BondAbilityRowLayout layout = createLayout(index, hasSecondaryButton);

        List<Component> result = new ArrayList<>();
        result.add(ability.getDisplayName());
        if (host.isRandomGiftAbility(ability) && abilityUnlocked) {
            result.add(Component.translatable("bond.random_gift.desc.auto").withStyle(ChatFormatting.GRAY));
            result.add(Component.translatable(
                    "bond.random_gift.status.queue",
                    BondClientStateCache.getQueuedGiftCount(host.getMaid().getUUID()),
                    BondClientStateCache.getMaxQueuedGiftCount(host.getMaid().getUUID())
            ).withStyle(ChatFormatting.AQUA));
            int nextGiftReadySeconds = BondClientStateCache.getNextGiftReadySeconds(host.getMaid().getUUID());
            if (nextGiftReadySeconds > 0) {
                result.add(Component.translatable("bond.random_gift.status.next", host.formatRemainingDuration(nextGiftReadySeconds)).withStyle(ChatFormatting.YELLOW));
            }
        } else {
            result.add(getAbilityDescription(ability).copy().withStyle(ChatFormatting.GRAY));
        }
        if (host.isMorningKissAbility(ability)) {
            result.add(Component.translatable("bond.morning_kiss.tooltip.favorability", com.github.touhoumaidaffection.ModConfig.BOND_MORNING_KISS_REQUIRED_FAVORABILITY.get()).withStyle(ChatFormatting.GRAY));
            result.add(Component.translatable("bond.morning_kiss.tooltip.time", com.github.touhoumaidaffection.bond.service.MorningKissService.getAllowedTimeRangesText()).withStyle(ChatFormatting.GRAY));
            result.add(Component.translatable("bond.morning_kiss.tooltip.kisses", com.github.touhoumaidaffection.bond.service.MorningKissService.getKissCountRangeText()).withStyle(ChatFormatting.GRAY));
            result.add(Component.translatable("bond.morning_kiss.tooltip.buff").withStyle(ChatFormatting.GRAY));
            if (hasSecondaryButton && layout.containsSecondaryButton(mouseX, mouseY, secondaryButtonWidth, buttonHeight)) {
                result.add(Component.translatable("bond.morning_kiss.voice.tip").withStyle(ChatFormatting.DARK_AQUA));
            }
        }
        if (host.isEmergencyHealAbility(ability) && abilityUnlocked && hasSecondaryButton) {
            result.add(Component.translatable(
                    "bond.emergency_rescue.voice.selected_source",
                    Component.translatable("bond.emergency_rescue.voice.source.tlm")
            ).withStyle(ChatFormatting.GRAY));
            if (host.isRescueActionConfigAvailable()) {
                result.add(Component.translatable("bond.emergency_rescue.action.selected", resolveRescueActionLabel()).withStyle(ChatFormatting.GRAY));
                if (layout.containsMainButton(mouseX, mouseY, buttonWidth, buttonHeight)) {
                    result.add(Component.translatable("bond.emergency_rescue.action.tip").withStyle(ChatFormatting.DARK_AQUA));
                }
            }
            if (layout.containsSecondaryButton(mouseX, mouseY, secondaryButtonWidth, buttonHeight)) {
                result.add(Component.translatable("bond.emergency_rescue.voice.tip").withStyle(ChatFormatting.DARK_AQUA));
            }
        }
        if (!abilityUnlocked) {
            result.add(Component.translatable("bond.power_point_cost", ability.getPowerPointCost()).withStyle(ChatFormatting.AQUA));
            result.add(Component.translatable("bond.power_point_inventory", powerPoints).withStyle(ChatFormatting.AQUA));
            result.add(Component.translatable("bond.power_point_item_hint").withStyle(ChatFormatting.DARK_GRAY));
        }
        result.add(host.getStatusText(ability, unlocked, abilityUnlocked, enoughPowerPoint, canUnlockNow, canUseSecondary)
                .copy()
                .withStyle(host.isMainButtonClickable(ability, unlocked, abilityUnlocked, enoughPowerPoint, canUnlockNow, canUseSecondary)
                        ? ChatFormatting.GREEN
                        : ChatFormatting.RED));
        return result;
    }

    public boolean contains(double mouseX, double mouseY) {
        return listPanel.contains(mouseX, mouseY);
    }

    private void renderAbilityRow(GuiGraphics graphics, Font font, IBondAbility ability, Player player, int powerPoints, boolean unlocked,
                                  int x, int y, int mouseX, int mouseY) {
        boolean abilityUnlocked = BondClientStateCache.isAbilityUnlocked(host.getMaid().getUUID(), ability.getId());
        boolean enoughPowerPoint = powerPoints >= ability.getPowerPointCost();
        boolean canUnlockNow = player != null && ability.canUnlock(player, host.getMaid());
        boolean canUseSecondary = player != null && abilityUnlocked && ability.hasSecondaryAction() && ability.canPerformSecondaryAction(player, host.getMaid());
        boolean hasSecondaryButton = host.hasSecondaryPageButton(ability, abilityUnlocked);
        Component status = host.getStatusText(ability, unlocked, abilityUnlocked, enoughPowerPoint, canUnlockNow, canUseSecondary);

        BondAbilityRowLayout row = createLayout(y, x, hasSecondaryButton);
        BondGuiArt.drawRowPlate(graphics, row.rowLeft(), row.rowTop(), row.rowRight(), row.rowBottom());
        // unlock lamp: lit once the skill is unlocked, dim while it is locked
        BondGuiArt.drawRowStar(graphics, row.rowLeft() + 2, row.rowTop() + 3, abilityUnlocked);

        MutableComponent title = ability.getDisplayName().copy();
        if (!unlocked) {
            title.withStyle(ChatFormatting.RED);
        }
        Component titleLine = Component.literal(font.plainSubstrByWidth(title.getString(), Math.max(8, row.textRight() - row.textLeft())));
        graphics.drawString(font, titleLine, row.textLeft(), y + 2, BondGuiTokens.COLOR_TEXT_BODY, false);

        Component secondaryText;
        if (host.isRandomGiftAbility(ability) && abilityUnlocked) {
            secondaryText = Component.translatable(
                    "bond.random_gift.status.queue",
                    BondClientStateCache.getQueuedGiftCount(host.getMaid().getUUID()),
                    BondClientStateCache.getMaxQueuedGiftCount(host.getMaid().getUUID())
            );
        } else if (!abilityUnlocked) {
            secondaryText = Component.translatable("bond.power_point_cost", ability.getPowerPointCost());
        } else if (host.isEmergencyHealAbility(ability) && hasSecondaryButton) {
            if (host.isRescueActionConfigAvailable()) {
                secondaryText = Component.translatable("bond.emergency_rescue.action.selected_compact", resolveRescueActionLabel());
            } else {
                secondaryText = Component.translatable(
                        "bond.emergency_rescue.voice.selected_source_compact",
                        Component.translatable("bond.emergency_rescue.voice.source.tlm")
                );
            }
        } else {
            secondaryText = getAbilityDescription(ability);
        }
        Component detailLine = Component.literal(font.plainSubstrByWidth(secondaryText.getString(), Math.max(8, row.textRight() - row.textLeft())));
        graphics.drawString(
                font,
                detailLine,
                row.textLeft(),
                y + 13,
                host.isRandomGiftAbility(ability) && abilityUnlocked
                        ? BondGuiTokens.COLOR_SUCCESS
                        : (abilityUnlocked ? BondGuiTokens.COLOR_TEXT_HINT : BondGuiTokens.COLOR_TEXT_SELECTED),
                false
        );

        if (hasSecondaryButton) {
            renderActionButton(graphics, font, row.secondaryButtonX(), row.buttonY(), secondaryButtonWidth, buttonHeight,
                    host.getSecondaryPageButtonLabel(ability), true, mouseX, mouseY, BondGuiTokens.COLOR_ACCENT, false);
        }

        boolean clickable = host.isMainButtonClickable(ability, unlocked, abilityUnlocked, enoughPowerPoint, canUnlockNow, canUseSecondary);
        int statusColor = clickable ? BondGuiTokens.COLOR_SUCCESS : BondGuiTokens.COLOR_WARNING;
        renderActionButton(graphics, font, row.mainButtonX(), row.buttonY(), buttonWidth, buttonHeight, status, clickable, mouseX, mouseY, statusColor, true);
    }

    private void renderActionButton(GuiGraphics graphics, Font font, int x, int y, int width, int height, Component label,
                                    boolean enabled, int mouseX, int mouseY, int textColor, boolean primary) {
        boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        BondGuiArt.BondButtonStyle style;
        if (!enabled) {
            style = BondGuiArt.BondButtonStyle.DISABLED;
        } else if (primary) {
            style = hovered ? BondGuiArt.BondButtonStyle.PRIMARY_HOVER : BondGuiArt.BondButtonStyle.PRIMARY;
        } else {
            style = hovered ? BondGuiArt.BondButtonStyle.HOVER : BondGuiArt.BondButtonStyle.DEFAULT;
        }

        BondGuiArt.drawButton(graphics, x, y, x + width, y + height, style);
        int color = enabled ? textColor : BondGuiTokens.COLOR_TEXT_DISABLED;
        int textY = y + Math.max(1, (height - font.lineHeight) / 2);
        graphics.drawCenteredString(font, label, x + width / 2, textY, color);
    }

    private void renderSettingsButton(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        renderActionButton(
                graphics,
                font,
                settingsButtonX,
                settingsButtonY,
                SETTINGS_BUTTON_WIDTH,
                SETTINGS_BUTTON_HEIGHT,
                Component.translatable("bond.settings.entry"),
                true,
                mouseX,
                mouseY,
                BondGuiTokens.COLOR_TEXT_SELECTED,
                false
        );
    }

    private boolean isSettingsButtonHovered(double mouseX, double mouseY) {
        return mouseX >= settingsButtonX
                && mouseX < settingsButtonX + SETTINGS_BUTTON_WIDTH
                && mouseY >= settingsButtonY
                && mouseY < settingsButtonY + SETTINGS_BUTTON_HEIGHT;
    }

    private BondAbilityRowLayout createLayout(int index, boolean hasSecondaryButton) {
        return createLayout(listPanel.getRowTop(index), panelX + 2, hasSecondaryButton);
    }

    private BondAbilityRowLayout createLayout(int rowY, int rowLeft, boolean hasSecondaryButton) {
        int rowRight = panelX + panelWidth - 4;
        return BondAbilityRowLayout.create(
                rowLeft,
                rowY,
                rowRight,
                rowHeight,
                buttonWidth,
                secondaryButtonWidth,
                buttonHeight,
                secondaryButtonGap,
                hasSecondaryButton
        );
    }

    private Component resolveRescueActionLabel() {
        String selectedActionId = RescueYsmActionConfig.getSelectedAction(host.getRescueActionModelId(), host.getRescueActionTextureId());
        if (selectedActionId.isBlank()) {
            return Component.translatable("bond.emergency_rescue.action.none");
        }
        return Component.literal(host.resolveSelectedRescueActionLabel(selectedActionId));
    }

    private Component getAbilityDescription(IBondAbility ability) {
        if ("lap_pillow".equals(ability.getId())) {
            return Component.translatable("bond.ability.lap.desc", com.github.touhoumaidaffection.client.BondKeyMappings.LAP_PILLOW.getTranslatedKeyMessage());
        }
        return ability.getDescription();
    }
}
