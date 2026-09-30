package com.github.touhoumaidaffection.client.screen.component;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class BondGuiText {
    private BondGuiText() {
    }

    public static void drawFittedLabel(GuiGraphics graphics, Font font, Component label,
                                       int x, int y, int width, int height, int color) {
        // Include the one-pixel shadow in the horizontal footprint.
        int textWidth = font.width(label) + 1;
        float scale = BondTextFit.scale(textWidth, font.lineHeight, width - 6, height - 4);
        if (scale <= 0.0F) {
            return;
        }
        float textX = x + (width - textWidth * scale) / 2.0F;
        float textY = y + (height - font.lineHeight * scale) / 2.0F;
        if (scale == 1.0F) {
            graphics.drawString(font, label, Math.round(textX), Math.round(textY), color, true);
            return;
        }
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(textX, textY, 0.0F);
            graphics.pose().scale(scale, scale, 1.0F);
            graphics.drawString(font, label, 0, 0, color, true);
        } finally {
            graphics.pose().popPose();
        }
    }
}
