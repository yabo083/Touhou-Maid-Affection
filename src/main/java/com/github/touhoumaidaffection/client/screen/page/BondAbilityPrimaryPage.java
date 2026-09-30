package com.github.touhoumaidaffection.client.screen.page;

import com.github.touhoumaidaffection.bond.ability.IBondAbility;
import com.github.touhoumaidaffection.client.BondClientStateCache;
import com.github.touhoumaidaffection.client.RescueYsmActionConfig;
import com.github.touhoumaidaffection.client.screen.component.BondAbilityListPanel;
import com.github.touhoumaidaffection.client.screen.component.BondAbilityRowLayout;
import com.github.touhoumaidaffection.client.screen.component.BondGuiText;
import com.github.touhoumaidaffection.client.screen.component.BondHeaderLayout;
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
    private final BondHeaderLayout header;
    private final Component title = Component.translatable("bond.tab.title");

    public BondAbilityPrimaryPage(BondPrimaryPageHost host,
                                  BondHeaderLayout header,
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
        this.header = header;
        this.listPanel = new BondAbilityListPanel(panelX, panelY, panelWidth, panelHeight, rowStartY, rowHeight, rowSpacing);
        this.rowHeight = rowHeight;
        this.buttonWidth = buttonWidth;
        this.secondaryButtonWidth = secondaryButtonWidth;
        this.buttonHeight = buttonHeight;
        this.secondaryButtonGap = secondaryButtonGap;
        this.panelX = panelX;
        this.panelWidth = panelWidth;
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

        renderHeader(graphics, font, mouseX, mouseY);

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
        if (header.settings().contains(mouseX, mouseY)) {
            if (host.isBondUnlocked()) {
                host.openSettingsPage();
            }
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
        if (header.settings().contains(mouseX, mouseY)) {
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
        graphics.fill(row.rowLeft(), row.rowTop(), row.rowRight(), row.rowBottom(), BondGuiTokens.COLOR_BG_ELEMENT);

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
        int border;
        int background;
        if (!enabled) {
            border = BondGuiTokens.STATE_DISABLED_BORDER;
            background = BondGuiTokens.STATE_DISABLED_BG;
        } else {
            border = hovered ? BondGuiTokens.STATE_HOVER_BORDER : BondGuiTokens.STATE_DEFAULT_BORDER;
            if (primary) {
                background = hovered ? BondGuiTokens.PRIMARY_BUTTON_HOVER_BG : BondGuiTokens.PRIMARY_BUTTON_BG;
            } else {
                background = hovered ? BondGuiTokens.STATE_HOVER_BG : BondGuiTokens.STATE_DEFAULT_BG;
            }
        }

        BondGuiTokens.drawFramedPanelWithInnerBorder(graphics, x, y, x + width, y + height, background, border);
        if (hovered && enabled) {
            graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, BondGuiTokens.HOVER_OVERLAY);
        }
        int color = enabled ? textColor : BondGuiTokens.COLOR_TEXT_DISABLED;
        BondGuiText.drawFittedLabel(graphics, font, label, x, y, width, height, color);
    }

    private void renderHeader(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        BondHeaderLayout.Rect titleBounds = header.title();
        BondGuiText.drawFittedLabel(graphics, font, title, titleBounds.x(), titleBounds.y(),
                titleBounds.width(), titleBounds.height(), BondGuiTokens.COLOR_TEXT_TITLE);

        BondHeaderLayout.Rect gear = header.settings();
        int color = !host.isBondUnlocked() ? BondGuiTokens.COLOR_TEXT_DISABLED
                : gear.contains(mouseX, mouseY) ? BondGuiTokens.COLOR_ACCENT : BondGuiTokens.COLOR_TEXT_HINT;
        int x = gear.x();
        int y = gear.y();
        // Twelve-pixel gear: transparent center, four rim segments and eight teeth.
        graphics.fill(x + 3, y + 2, x + 9, y + 4, color);
        graphics.fill(x + 3, y + 8, x + 9, y + 10, color);
        graphics.fill(x + 2, y + 4, x + 4, y + 8, color);
        graphics.fill(x + 8, y + 4, x + 10, y + 8, color);
        graphics.fill(x + 5, y, x + 7, y + 2, color);
        graphics.fill(x + 5, y + 10, x + 7, y + 12, color);
        graphics.fill(x, y + 5, x + 2, y + 7, color);
        graphics.fill(x + 10, y + 5, x + 12, y + 7, color);
        graphics.fill(x + 1, y + 1, x + 3, y + 3, color);
        graphics.fill(x + 9, y + 1, x + 11, y + 3, color);
        graphics.fill(x + 1, y + 9, x + 3, y + 11, color);
        graphics.fill(x + 9, y + 9, x + 11, y + 11, color);
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
