package com.github.touhoumaidaffection.client.screen.page;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

public interface BondSecondaryPage {
    void render(GuiGraphics graphics, int mouseX, int mouseY);

    boolean mouseClicked(double mouseX, double mouseY, int button);

    boolean mouseScrolled(double mouseX, double mouseY, double scrollY);

    List<Component> getTooltip(int mouseX, int mouseY);

    default void onClose() {
    }
}
