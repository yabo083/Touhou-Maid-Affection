package com.github.touhoumaidaffection.client;

import com.github.tartaricacid.touhoulittlemaid.api.event.client.MaidContainerGuiEvent;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.AbstractMaidContainerGui;
import com.github.tartaricacid.touhoulittlemaid.client.gui.widget.button.MaidTabButton;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.github.touhoumaidaffection.bond.BondConfig;
import com.github.touhoumaidaffection.client.screen.BondMaidContainerScreen;
import com.github.touhoumaidaffection.inventory.BondContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = TouhouMaidAffection.MOD_ID, value = Dist.CLIENT)
public final class BondMaidGuiTabHandler {
    private static final String TAB_BUTTON_NAME = "bond_tab";
    private static final int TAB_X_OFFSET = 194;
    private static final int TAB_Y_OFFSET = 5;
    private static final int TAB_ICON_U = 207;

    private static final ResourceLocation BOND_TAB_ICON =
            ResourceLocation.fromNamespaceAndPath(TouhouMaidAffection.MOD_ID, "textures/gui/bond_tab_icon.png");

    private BondMaidGuiTabHandler() {
    }

    @SubscribeEvent
    public static void onInit(MaidContainerGuiEvent.Init event) {
        AbstractMaidContainerGui<?> gui = event.getGui();
        EntityMaid maid = gui.getMaid();
        if (maid == null) {
            return;
        }

        boolean unlocked = isBondUnlocked(maid);
        if (gui instanceof BondMaidContainerScreen) {
            BondTabButton tabButton = new BondTabButton(
                    event.getLeftPos() + TAB_X_OFFSET,
                    event.getTopPos() + TAB_Y_OFFSET,
                    TAB_ICON_U,
                    button -> { },
                    true
            );
            tabButton.active = false;
            event.addButton(TAB_BUTTON_NAME, tabButton);
            return;
        }

        BondTabButton tabButton = new BondTabButton(
                event.getLeftPos() + TAB_X_OFFSET,
                event.getTopPos() + TAB_Y_OFFSET,
                TAB_ICON_U,
                button -> openBondTab(gui, maid),
                unlocked
        );
        event.addButton(TAB_BUTTON_NAME, tabButton);
    }

    private static void openBondTab(AbstractMaidContainerGui<?> gui, EntityMaid maid) {
        if (!isBondUnlocked(maid)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.setScreen(new BondMaidContainerScreen(
                    new BondContainer(0, minecraft.player.getInventory(), maid.getId()),
                    minecraft.player.getInventory(),
                    gui.getTitle()
            ));
        }
    }

    private static boolean isBondUnlocked(EntityMaid maid) {
        return maid.getFavorabilityManager().getLevel() >= BondConfig.DEFAULT_UNLOCK_LEVEL;
    }

    private static final class BondTabButton extends MaidTabButton {
        private final boolean unlocked;

        private BondTabButton(int x, int y, int left, OnPress onPress, boolean unlocked) {
            super(x, y, left, "bond", onPress);
            this.unlocked = unlocked;
        }

        @Override
        public void renderWidget(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            if (!unlocked) {
                graphics.setColor(0.45f, 0.45f, 0.45f, 1.0f);
                graphics.blit(BOND_TAB_ICON, getX() + 4, getY() + 6, 0, 0, 16, 16, 16, 16);
                graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
                if (isHovered()) {
                    graphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, 0x18000000);
                }
                return;
            }

            super.renderWidget(graphics, mouseX, mouseY, partialTicks);
            graphics.blit(BOND_TAB_ICON, getX() + 4, getY() + 6, 0, 0, 16, 16, 16, 16);
        }

        @Override
        public boolean isTooltipHovered() {
            return this.isHovered();
        }

        @Override
        public void renderTooltip(net.minecraft.client.gui.GuiGraphics graphics, Minecraft mc, int mouseX, int mouseY) {
            if (unlocked) {
                super.renderTooltip(graphics, mc, mouseX, mouseY);
                return;
            }
            graphics.renderComponentTooltip(
                    mc.font,
                    java.util.List.of(
                            Component.translatable("gui.touhou_little_maid.button.bond"),
                            Component.translatable("bond.unlock.requirement", BondConfig.DEFAULT_UNLOCK_LEVEL)
                    ),
                    mouseX,
                    mouseY
            );
        }
    }
}
