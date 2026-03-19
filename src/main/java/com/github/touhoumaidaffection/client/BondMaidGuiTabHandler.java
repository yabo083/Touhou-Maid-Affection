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

        if (gui instanceof BondMaidContainerScreen) {
            BondTabButton tabButton = new BondTabButton(
                    event.getLeftPos() + TAB_X_OFFSET,
                    event.getTopPos() + TAB_Y_OFFSET,
                    TAB_ICON_U,
                    button -> {
                    }
            );
            tabButton.active = false;
            event.addButton(TAB_BUTTON_NAME, tabButton);
            return;
        }

        BondTabButton tabButton = new BondTabButton(
                event.getLeftPos() + TAB_X_OFFSET,
                event.getTopPos() + TAB_Y_OFFSET,
                TAB_ICON_U,
                button -> openBondTab(gui, maid)
        );
        tabButton.active = isBondUnlocked(maid);
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
        private BondTabButton(int x, int y, int left, OnPress onPress) {
            super(x, y, left, "bond", onPress);
        }

        @Override
        public void renderWidget(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            super.renderWidget(graphics, mouseX, mouseY, partialTicks);
            graphics.blit(BOND_TAB_ICON, getX() + 4, getY() + 6, 0, 0, 16, 16, 16, 16);
        }
    }
}
