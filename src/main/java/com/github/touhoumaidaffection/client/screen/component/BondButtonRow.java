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
            result.add(new ButtonSpec(x, y, buttonWidth, buttonHeight, spec.label(), spec.id(), spec.enabled()));
            x += buttonWidth + gap;
        }
        return result;
    }

    public static void render(GuiGraphics graphics, Font font, int baseLeft, List<ButtonSpec> buttons, int mouseX, int mouseY) {
        for (ButtonSpec button : buttons) {
            int x = baseLeft + button.x();
            int y = button.y();
            int fill = button.enabled() ? 0xFF414141 : 0xFF2B2B2B;
            graphics.fill(x, y, x + button.width(), y + button.height(), fill);
            graphics.fill(x + 1, y + 1, x + button.width() - 1, y + button.height() - 1, 0xFF1A1A1A);
            if (contains(button, baseLeft, mouseX, mouseY)) {
                graphics.fill(x, y, x + button.width(), y + button.height(), 0x33FFFFFF);
            }
            int color = button.enabled() ? 0xFFFFFFFF : 0xFF909090;
            graphics.drawCenteredString(font, button.label(), x + button.width() / 2, y + 3, color);
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

    public record ButtonSpec(int x, int y, int width, int height, Component label, String id, boolean enabled) {
    }
}
