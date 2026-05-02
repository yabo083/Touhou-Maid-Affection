package com.github.touhoumaidaffection.client.screen.page;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

public interface BondSecondaryPage {
    void render(GuiGraphics graphics, int mouseX, int mouseY);

    boolean mouseClicked(double mouseX, double mouseY, int button);

    default boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    default boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return false;
    }

    default boolean onEscapePressed() {
        return false;
    }

    default boolean previewSelectedVoice() {
        return false;
    }

    boolean mouseScrolled(double mouseX, double mouseY, double scrollY);

    List<Component> getTooltip(int mouseX, int mouseY);

    default void onClose() {
    }
}
