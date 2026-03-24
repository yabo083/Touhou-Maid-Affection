package com.github.touhoumaidaffection.client.screen.component;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class BondButtonRow {
    private BondButtonRow() {
    }

    public static List<ButtonSpec> createCentered(int totalWidth, int y, int buttonWidth, int buttonHeight, int gap, ButtonSpec... specs) {
        List<ButtonSpec> result = new ArrayList<>();
        int x = (totalWidth - (specs.length * buttonWidth + Math.max(0, specs.length - 1) * gap)) / 2;
        for (ButtonSpec spec : specs) {
            result.add(new ButtonSpec(x, y, buttonWidth, buttonHeight, spec.label(), spec.id(), spec.enabled(), spec.primary()));
            x += buttonWidth + gap;
        }
        return result;
    }

    public static List<ButtonSpec> createCenteredUniform(Font font, int totalWidth, int y, int buttonHeight, int gap, int horizontalPadding, ButtonSpec... specs) {
        int width = BondGuiTokens.BUTTON_MIN_WIDTH;
        for (ButtonSpec spec : specs) {
            width = Math.max(width, font.width(spec.label()) + horizontalPadding * 2);
        }
        return createCentered(totalWidth, y, width, buttonHeight, gap, specs);
    }

    public static void render(GuiGraphics graphics, Font font, int baseLeft, List<ButtonSpec> buttons, int mouseX, int mouseY) {
        for (ButtonSpec button : buttons) {
            int x = baseLeft + button.x();
            int y = button.y();
            boolean hovered = contains(button, baseLeft, mouseX, mouseY);

            int innerBorder = borderColor(button, hovered);
            int background = backgroundColor(button, hovered);
            BondGuiTokens.drawFramedPanelWithInnerBorder(graphics, x, y, x + button.width(), y + button.height(), background, innerBorder);
            if (hovered && button.enabled()) {
                graphics.fill(x + 2, y + 2, x + button.width() - 2, y + button.height() - 2, BondGuiTokens.HOVER_OVERLAY);
            }

            int color = textColor(button);
            int textY = y + Math.max(1, (button.height() - font.lineHeight) / 2);
            graphics.drawCenteredString(font, button.label(), x + button.width() / 2, textY, color);
        }
    }

    public static String click(List<ButtonSpec> buttons, int baseLeft, double mouseX, double mouseY) {
        for (ButtonSpec button : buttons) {
            if (button.enabled() && contains(button, baseLeft, mouseX, mouseY)) {
                return button.id();
            }
        }
        return "";
    }

    private static boolean contains(ButtonSpec button, int baseLeft, double mouseX, double mouseY) {
        int x = baseLeft + button.x();
        return mouseX >= x && mouseX < x + button.width() && mouseY >= button.y() && mouseY < button.y() + button.height();
    }

    private static int backgroundColor(ButtonSpec button, boolean hovered) {
        if (!button.enabled()) {
            return BondGuiTokens.STATE_DISABLED_BG;
        }
        if (button.primary()) {
            return hovered ? BondGuiTokens.PRIMARY_BUTTON_HOVER_BG : BondGuiTokens.PRIMARY_BUTTON_BG;
        }
        return hovered ? BondGuiTokens.STATE_HOVER_BG : BondGuiTokens.STATE_DEFAULT_BG;
    }

    private static int borderColor(ButtonSpec button, boolean hovered) {
        if (!button.enabled()) {
            return BondGuiTokens.STATE_DISABLED_BORDER;
        }
        return hovered ? BondGuiTokens.STATE_HOVER_BORDER : BondGuiTokens.STATE_DEFAULT_BORDER;
    }

    private static int textColor(ButtonSpec button) {
        if (!button.enabled()) {
            return BondGuiTokens.COLOR_TEXT_DISABLED;
        }
        return button.primary() ? BondGuiTokens.COLOR_TEXT_TITLE : BondGuiTokens.COLOR_TEXT_BODY;
    }

    public record ButtonSpec(int x, int y, int width, int height, Component label, String id, boolean enabled, boolean primary) {
        public ButtonSpec(int x, int y, int width, int height, Component label, String id, boolean enabled) {
            this(x, y, width, height, label, id, enabled, false);
        }
    }
}
