package com.github.touhoumaidaffection.client.screen;

import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.AbstractMaidContainerGui;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitItems;
import com.github.touhoumaidaffection.bond.BondConfig;
import com.github.touhoumaidaffection.bond.ability.BondAbilityManager;
import com.github.touhoumaidaffection.bond.ability.IBondAbility;
import com.github.touhoumaidaffection.inventory.BondContainer;
import com.github.touhoumaidaffection.network.BondActivateAbilityPayload;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.ArrayList;
import java.util.List;

public class BondMaidContainerScreen extends AbstractMaidContainerGui<BondContainer> {
    private static final int PAGE_X_OFFSET = 80;
    private static final int PAGE_Y_OFFSET = 28;
    private static final int PAGE_WIDTH = 176;
    private static final int PAGE_HEIGHT = 137;

    private static final int PANEL_X_OFFSET = 86;
    private static final int PANEL_Y_OFFSET = 40;
    private static final int PANEL_WIDTH = 164;
    private static final int PANEL_HEIGHT = 114;

    private static final int ROW_START_Y = 9;
    private static final int ROW_HEIGHT = 18;
    private static final int ROW_SPACING = 18;
    private static final int BTN_WIDTH = 44;
    private static final int BTN_HEIGHT = 12;

    private final EntityMaid maid;
    private final List<IBondAbility> abilities;

    public BondMaidContainerScreen(BondContainer container, Inventory inv, Component titleIn) {
        super(container, inv, titleIn);
        this.imageHeight = 256;
        this.imageWidth = 256;
        this.maid = menu.getMaid();
        this.abilities = List.copyOf(BondAbilityManager.getAllAbilities());
    }

    @Override
    protected void init() {
        if (maid == null || !isBondUnlocked()) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null) {
                minecraft.player.closeContainer();
            }
            return;
        }
        super.init();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int x, int y) {
        super.renderBg(graphics, partialTicks, x, y);
        renderBondPageBackground(graphics);
    }

    @Override
    protected void renderAddition(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        renderBondPanel(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderAdditionTransTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        List<Component> tooltip = getTooltipAt(mouseX, mouseY);
        if (!tooltip.isEmpty()) {
            graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isMouseInsideBondPage(mouseX, mouseY)) {
            return handleAbilityClick(mouseX, mouseY);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (isMouseInsideBondPage(mouseX, mouseY)) {
            List<Component> tooltip = getTooltipAt(mouseX, mouseY);
            if (!tooltip.isEmpty()) {
                graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
            }
            return;
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    private void renderBondPageBackground(GuiGraphics graphics) {
        int left = leftPos + PAGE_X_OFFSET;
        int top = topPos + PAGE_Y_OFFSET;
        int right = left + PAGE_WIDTH;
        int bottom = top + PAGE_HEIGHT;

        graphics.fill(left, top, right, bottom, 0xFFBEBEBE);
        graphics.fill(left + 1, top + 1, right - 1, bottom - 1, 0xFF4D4D4D);
        graphics.fill(left + 3, top + 3, right - 3, bottom - 3, 0xFF2A2A2A);
        graphics.fill(left + 6, top + 22, right - 6, bottom - 6, 0xFF1D1D1D);
        graphics.fill(left + 6, top + 22, right - 6, top + 23, 0xFF707070);
        graphics.fill(left + 6, bottom - 7, right - 6, bottom - 6, 0xFF101010);
        graphics.fill(left + 6, top + 22, left + 7, bottom - 6, 0xFF707070);
        graphics.fill(right - 7, top + 22, right - 6, bottom - 6, 0xFF101010);
        graphics.drawCenteredString(font, Component.translatable("bond.tab.title"), left + PAGE_WIDTH / 2, top + 8, 0xFFFFFF);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (isMouseInsideBondPage(mouseX, mouseY)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isMouseInsideBondPage(mouseX, mouseY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isMouseInsideBondPage(mouseX, mouseY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean isMouseInsideBondPage(double mouseX, double mouseY) {
        int left = leftPos + PAGE_X_OFFSET;
        int top = topPos + PAGE_Y_OFFSET;
        return mouseX >= left && mouseX < left + PAGE_WIDTH && mouseY >= top && mouseY < top + PAGE_HEIGHT;
    }

    private void renderBondPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        if (maid == null) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        int left = leftPos + PANEL_X_OFFSET;
        int top = topPos + PANEL_Y_OFFSET;
        int right = left + PANEL_WIDTH;
        int bottom = top + PANEL_HEIGHT;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        try {
            int powerPoints = getPowerPointCount();
            boolean unlocked = isBondUnlocked();

            graphics.enableScissor(left + 1, top + 1, right - 1, bottom - 1);
            try {
                for (int i = 0; i < abilities.size(); i++) {
                    int rowY = top + ROW_START_Y + i * ROW_SPACING;
                    if (rowY + ROW_HEIGHT > bottom - 2) {
                        break;
                    }
                    renderAbilityRow(graphics, font, abilities.get(i), powerPoints, unlocked, left + 2, rowY, mouseX, mouseY);
                }
            } finally {
                graphics.disableScissor();
            }
        } finally {
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
        }
    }

    private void renderAbilityRow(GuiGraphics graphics, Font font, IBondAbility ability, int powerPoints, boolean unlocked,
                                  int x, int y, int mouseX, int mouseY) {
        int rowRight = leftPos + PANEL_X_OFFSET + PANEL_WIDTH - 4;
        int rowBottom = y + ROW_HEIGHT - 1;
        graphics.fill(x, y, rowRight, rowBottom, 0x7F2F2F2F);

        boolean enoughPowerPoint = powerPoints >= ability.getPowerPointCost();
        Component status = getStatusText(unlocked, enoughPowerPoint);

        MutableComponent title = ability.getDisplayName().copy();
        if (!unlocked) {
            title.withStyle(ChatFormatting.RED);
        }
        graphics.drawString(font, title, x + 4, y + 2, 0xFFE0E0E0, false);
        graphics.drawString(font, Component.translatable("bond.power_point_cost", ability.getPowerPointCost()), x + 4, y + 10, 0xFF7AD5FF, false);

        int buttonX = rowRight - BTN_WIDTH - 4;
        int buttonY = y + 3;
        int buttonColor = unlocked && enoughPowerPoint ? 0xFF4B4B4B : 0xFF2E2E2E;
        graphics.fill(buttonX, buttonY, buttonX + BTN_WIDTH, buttonY + BTN_HEIGHT, buttonColor);
        graphics.fill(buttonX + 1, buttonY + 1, buttonX + BTN_WIDTH - 1, buttonY + BTN_HEIGHT - 1, 0xFF1F1F1F);

        int statusColor = unlocked && enoughPowerPoint ? 0xFF79F079 : 0xFFFFC970;
        int statusX = buttonX + (BTN_WIDTH - font.width(status)) / 2;
        graphics.drawString(font, status, statusX, buttonY + 2, statusColor, false);

        if (mouseX >= buttonX && mouseX < buttonX + BTN_WIDTH && mouseY >= buttonY && mouseY < buttonY + BTN_HEIGHT) {
            graphics.fill(buttonX, buttonY, buttonX + BTN_WIDTH, buttonY + BTN_HEIGHT, 0x33FFFFFF);
        }
    }

    private boolean handleAbilityClick(double mouseX, double mouseY) {
        if (maid == null || !isMouseInsidePanel(mouseX, mouseY)) {
            return false;
        }
        int powerPoints = getPowerPointCount();
        boolean unlocked = isBondUnlocked();
        int rowRight = leftPos + PANEL_X_OFFSET + PANEL_WIDTH - 4;
        int buttonX = rowRight - BTN_WIDTH - 4;

        for (int i = 0; i < abilities.size(); i++) {
            int rowY = topPos + PANEL_Y_OFFSET + ROW_START_Y + i * ROW_SPACING;
            if (rowY + ROW_HEIGHT > topPos + PANEL_Y_OFFSET + PANEL_HEIGHT - 2) {
                break;
            }
            int buttonY = rowY + 3;
            if (mouseX >= buttonX && mouseX < buttonX + BTN_WIDTH && mouseY >= buttonY && mouseY < buttonY + BTN_HEIGHT) {
                IBondAbility ability = abilities.get(i);
                if (unlocked && powerPoints >= ability.getPowerPointCost()) {
                    PacketDistributor.sendToServer(new BondActivateAbilityPayload(ability.getId(), maid.getUUID()));
                }
                return true;
            }
        }
        return true;
    }

    private List<Component> getTooltipAt(int mouseX, int mouseY) {
        if (!isMouseInsidePanel(mouseX, mouseY) || maid == null) {
            return List.of();
        }
        boolean unlocked = isBondUnlocked();
        int powerPoints = getPowerPointCount();
        for (int i = 0; i < abilities.size(); i++) {
            int rowY = topPos + PANEL_Y_OFFSET + ROW_START_Y + i * ROW_SPACING;
            int rowBottom = rowY + ROW_HEIGHT - 1;
            if (mouseY < rowY || mouseY > rowBottom) {
                continue;
            }
            IBondAbility ability = abilities.get(i);
            boolean enoughPowerPoint = powerPoints >= ability.getPowerPointCost();
            List<Component> result = new ArrayList<>();
            result.add(ability.getDisplayName());
            result.add(ability.getDescription().copy().withStyle(ChatFormatting.GRAY));
            result.add(Component.translatable("bond.power_point_cost", ability.getPowerPointCost()).withStyle(ChatFormatting.AQUA));
            result.add(getStatusText(unlocked, enoughPowerPoint).copy().withStyle(enoughPowerPoint ? ChatFormatting.GREEN : ChatFormatting.RED));
            return result;
        }
        return List.of();
    }

    private boolean isMouseInsidePanel(double mouseX, double mouseY) {
        int left = leftPos + PANEL_X_OFFSET;
        int top = topPos + PANEL_Y_OFFSET;
        return mouseX >= left && mouseX < left + PANEL_WIDTH && mouseY >= top && mouseY < top + PANEL_HEIGHT;
    }

    private boolean isBondUnlocked() {
        return maid != null && maid.getFavorabilityManager().getLevel() >= BondConfig.DEFAULT_UNLOCK_LEVEL;
    }

    private int getPowerPointCount() {
        if (Minecraft.getInstance().player == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < Minecraft.getInstance().player.getInventory().getContainerSize(); i++) {
            ItemStack stack = Minecraft.getInstance().player.getInventory().getItem(i);
            if (stack.is(InitItems.POWER_POINT.get())) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private Component getStatusText(boolean unlocked, boolean enoughPowerPoint) {
        if (!unlocked) {
            return Component.translatable("bond.locked");
        }
        if (!enoughPowerPoint) {
            return Component.translatable("bond.insufficient_power_point");
        }
        return Component.translatable("bond.activate");
    }
}
