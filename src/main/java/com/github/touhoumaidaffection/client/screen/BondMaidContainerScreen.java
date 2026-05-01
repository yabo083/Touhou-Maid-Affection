package com.github.touhoumaidaffection.client.screen;

import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.AbstractMaidContainerGui;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.settings.AIChatSettingsHubScreen;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.site.AvailableSites;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.touhoumaidaffection.ModConfig;
import com.github.touhoumaidaffection.bond.BondConfig;
import com.github.touhoumaidaffection.bond.ability.BondAbilityManager;
import com.github.touhoumaidaffection.bond.ability.IBondAbility;
import com.github.touhoumaidaffection.bond.service.MorningKissService;
import com.github.touhoumaidaffection.client.BondClientStateCache;
import com.github.touhoumaidaffection.client.RescueYsmActionConfig;
import com.github.touhoumaidaffection.client.YsmModelActionIndex;
import com.github.touhoumaidaffection.client.screen.component.BondGuiTokens;
import com.github.touhoumaidaffection.client.screen.component.BondModalPage;
import com.github.touhoumaidaffection.client.screen.page.BondAbilityPrimaryPage;
import com.github.touhoumaidaffection.client.screen.page.BondPrimaryPageHost;
import com.github.touhoumaidaffection.client.screen.page.BondSecondaryPage;
import com.github.touhoumaidaffection.client.screen.page.BondSecondaryPageHost;
import com.github.touhoumaidaffection.client.screen.page.BondSecondaryPageRegistry;
import com.github.touhoumaidaffection.client.screen.page.RescueActionSecondaryPage;
import com.github.touhoumaidaffection.inventory.BondContainer;
import com.github.touhoumaidaffection.network.BondActivateAbilityPayload;
import com.github.touhoumaidaffection.network.BondStateRequestPayload;
import com.github.touhoumaidaffection.util.PowerPointInventoryHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BondMaidContainerScreen extends AbstractMaidContainerGui<BondContainer> implements BondSecondaryPageHost, BondPrimaryPageHost {
    private static final float SECONDARY_PAGE_Z = 240.0F;
    private static final int PAGE_X_OFFSET = 80;
    private static final int PAGE_Y_OFFSET = 28;
    private static final int PAGE_WIDTH = 176;
    private static final int PAGE_HEIGHT = 137;

    private static final int PANEL_X_OFFSET = 86;
    private static final int PANEL_Y_OFFSET = 40;
    private static final int PANEL_WIDTH = 164;
    private static final int PANEL_HEIGHT = 114;

    private static final int ROW_START_Y = 9;
    private static final int ROW_HEIGHT = 24;
    private static final int ROW_SPACING = 24;
    private static final int BTN_WIDTH = 46;
    private static final int BTN_HEIGHT = BondGuiTokens.CONTROL_HEIGHT;
    private static final int SECONDARY_BUTTON_WIDTH = 40;
    private static final int SECONDARY_BUTTON_GAP = BondGuiTokens.SPACING_SM;

    private final EntityMaid maid;
    private final List<IBondAbility> abilities;
    private String cachedRescueActionModelId = "";
    private String cachedRescueActionTextureId = "";
    private Map<String, String> cachedRescueActionLabels = Map.of();
    private BondAbilityPrimaryPage primaryPage;
    private BondSecondaryPage secondaryPage;

    public BondMaidContainerScreen(BondContainer container, Inventory inv, Component titleIn) {
        super(container, inv, titleIn);
        this.imageHeight = 256;
        this.imageWidth = 256;
        this.maid = menu.getMaid();
        this.abilities = List.copyOf(BondAbilityManager.getAllAbilities());
        BondSecondaryPageRegistry.registerDefaults();
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
        PacketDistributor.sendToServer(new BondStateRequestPayload(maid.getUUID()));
        primaryPage = new BondAbilityPrimaryPage(
                this,
                leftPos + PANEL_X_OFFSET,
                topPos + PANEL_Y_OFFSET,
                PANEL_WIDTH,
                PANEL_HEIGHT,
                ROW_START_Y,
                ROW_HEIGHT,
                ROW_SPACING,
                BTN_WIDTH,
                SECONDARY_BUTTON_WIDTH,
                BTN_HEIGHT,
                SECONDARY_BUTTON_GAP
        );
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int x, int y) {
        super.renderBg(graphics, partialTicks, x, y);
        renderBondPageBackground(graphics);
    }

    @Override
    protected void renderAddition(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (!hasActiveSecondaryPage()) {
            renderBondPanel(graphics, mouseX, mouseY);
        }
        renderSecondaryPage(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderAdditionTransTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (hasActiveSecondaryPage()) {
            return;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (hasActiveSecondaryPage()) {
            if (isMouseOverAnyMenuSlot(mouseX, mouseY)) {
                return super.mouseClicked(mouseX, mouseY, button);
            }
            return handleSecondaryPageClick(mouseX, mouseY, button);
        }
        if (button == 0 && isMouseInsideBondPage(mouseX, mouseY) && primaryPage != null) {
            return primaryPage.mouseClicked(mouseX, mouseY);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (hasActiveSecondaryPage()) {
            List<Component> modalTooltip = getSecondaryPageTooltip(mouseX, mouseY);
            if (!modalTooltip.isEmpty()) {
                graphics.pose().pushPose();
                try {
                    graphics.pose().translate(0.0F, 0.0F, SECONDARY_PAGE_Z + 20.0F);
                    graphics.renderComponentTooltip(font, modalTooltip, mouseX, mouseY);
                } finally {
                    graphics.pose().popPose();
                }
            }
            return;
        }
        if (isMouseInsideBondPage(mouseX, mouseY)) {
            List<Component> tooltip = primaryPage == null ? List.of() : primaryPage.getTooltip(mouseX, mouseY);
            if (!tooltip.isEmpty()) {
                graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
            }
            return;
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (secondaryPage != null && secondaryPage.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        if (super.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        if (hasActiveSecondaryPage() || isMouseInsideBondPage(mouseX, mouseY)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (secondaryPage != null && secondaryPage.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        if (super.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        if (hasActiveSecondaryPage() || isMouseInsideBondPage(mouseX, mouseY)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (secondaryPage != null && secondaryPage.mouseScrolled(mouseX, mouseY, scrollY)) {
            return true;
        }
        if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        if (hasActiveSecondaryPage() || isMouseInsideBondPage(mouseX, mouseY)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (hasActiveSecondaryPage() && keyCode == 256) {
            if (secondaryPage != null && secondaryPage.onEscapePressed()) {
                return true;
            }
            closeSecondaryPage();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void renderBondPageBackground(GuiGraphics graphics) {
        int left = leftPos + PAGE_X_OFFSET;
        int top = topPos + PAGE_Y_OFFSET;
        int right = left + PAGE_WIDTH;
        int bottom = top + PAGE_HEIGHT;

        BondGuiTokens.drawFramedPanel(graphics, left, top, right, bottom, BondGuiTokens.COLOR_BG_PANEL);
        graphics.fill(left + 3, top + 3, right - 3, bottom - 3, 0xAA2B2228);
        graphics.fill(left + 6, top + 22, right - 6, bottom - 6, BondGuiTokens.COLOR_BG_ELEMENT);
        graphics.fill(left + 6, top + 22, right - 6, top + 23, BondGuiTokens.DIVIDER_COLOR);
        graphics.fill(left + 6, bottom - 7, right - 6, bottom - 6, 0x440F0A0D);
        graphics.fill(left + 6, top + 22, left + 7, bottom - 6, BondGuiTokens.DIVIDER_COLOR);
        graphics.fill(right - 7, top + 22, right - 6, bottom - 6, 0x440F0A0D);
        if (!hasActiveSecondaryPage()) {
            graphics.drawCenteredString(font, Component.translatable("bond.tab.title"), left + PAGE_WIDTH / 2, top + 8, BondGuiTokens.COLOR_TEXT_TITLE);
        }
    }

    private void renderBondPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        if (maid == null || primaryPage == null) {
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        try {
            primaryPage.render(graphics, mouseX, mouseY);
        } finally {
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
        }
    }

    private void renderSecondaryPage(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!hasActiveSecondaryPage()) {
            return;
        }
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(0.0F, 0.0F, SECONDARY_PAGE_Z);
            secondaryPage.render(graphics, mouseX, mouseY);
        } finally {
            graphics.pose().popPose();
        }
    }

    private boolean handleSecondaryPageClick(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return true;
        }
        return secondaryPage != null && secondaryPage.mouseClicked(mouseX, mouseY, button);
    }

    private List<Component> getSecondaryPageTooltip(int mouseX, int mouseY) {
        return secondaryPage == null ? List.of() : secondaryPage.getTooltip(mouseX, mouseY);
    }

    @Override
    public void openSecondaryPageForAbility(IBondAbility ability) {
        BondSecondaryPage nextPage = BondSecondaryPageRegistry.createPage(this, ability);
        if (nextPage != null) {
            closeSecondaryPage();
            secondaryPage = nextPage;
        }
    }

    @Override
    public void openEmergencyRescueActionPage() {
        if (maid == null || !isRescueActionConfigAvailable()) {
            return;
        }
        closeSecondaryPage();
        secondaryPage = new RescueActionSecondaryPage(this);
    }

    @Override
    public void openMimoAdapterSettings() {
        if (!isMimoAdapterAvailable()) {
            return;
        }
        Minecraft.getInstance().setScreen(AIChatSettingsHubScreen.openDefault(this, AvailableSites.LLM_SITES, AvailableSites.TTS_SITES, false));
    }

    @Override
    public void closeSecondaryPage() {
        if (secondaryPage != null) {
            secondaryPage.onClose();
            secondaryPage = null;
        }
        invalidateRescueActionLabelCache();
    }

    @Override
    public BondModalPage createModal(int width, int height, Component title) {
        return new BondModalPage(leftPos + PAGE_X_OFFSET, topPos + PAGE_Y_OFFSET, PAGE_WIDTH, PAGE_HEIGHT, width, height, title);
    }

    @Override
    public boolean isSecondaryPageUnlocked(IBondAbility ability) {
        return maid != null
                && ability != null
                && BondClientStateCache.isAbilityUnlocked(maid.getUUID(), ability.getId());
    }

    private boolean hasActiveSecondaryPage() {
        return secondaryPage != null;
    }

    private boolean isMouseInsideBondPage(double mouseX, double mouseY) {
        int left = leftPos + PAGE_X_OFFSET;
        int top = topPos + PAGE_Y_OFFSET;
        return mouseX >= left && mouseX < left + PAGE_WIDTH && mouseY >= top && mouseY < top + PAGE_HEIGHT;
    }

    private boolean isMouseInsidePanel(double mouseX, double mouseY) {
        int left = leftPos + PANEL_X_OFFSET;
        int top = topPos + PANEL_Y_OFFSET;
        return mouseX >= left && mouseX < left + PANEL_WIDTH && mouseY >= top && mouseY < top + PANEL_HEIGHT;
    }

    @Override
    public boolean isBondUnlocked() {
        return maid != null && maid.getFavorabilityManager().getLevel() >= BondConfig.DEFAULT_UNLOCK_LEVEL;
    }

    @Override
    public int getPowerPointCount() {
        return PowerPointInventoryHelper.countPowerPoints(Minecraft.getInstance().player);
    }

    @Override
    public Component getStatusText(IBondAbility ability, boolean unlocked, boolean abilityUnlocked, boolean enoughPowerPoint, boolean canUnlockNow, boolean canUseSecondary) {
        if (!unlocked) {
            return Component.translatable("bond.locked");
        }
        if (!abilityUnlocked) {
            if (!canUnlockNow) {
                return Component.translatable("bond.requirements_unmet");
            }
            if (!enoughPowerPoint) {
                return Component.translatable("bond.insufficient_power_point");
            }
            return Component.translatable("bond.unlock");
        }
        if (isEmergencyHealAbility(ability)) {
            if (isRescueActionConfigAvailable()) {
                return Component.translatable("bond.emergency_rescue.action.button");
            }
            return ability.getUnlockedButtonLabel();
        }
        if (ability.hasSecondaryAction()) {
            if (canUseSecondary) {
                return ability.getSecondaryActionButtonLabel();
            }
            return Component.translatable("bond.requirements_unmet");
        }
        return ability.getUnlockedButtonLabel();
    }

    @Override
    public boolean isMainButtonClickable(IBondAbility ability, boolean unlocked, boolean abilityUnlocked, boolean enoughPowerPoint, boolean canUnlockNow, boolean canUseSecondary) {
        if (!unlocked) {
            return false;
        }
        if (!abilityUnlocked) {
            return enoughPowerPoint && canUnlockNow;
        }
        if (isEmergencyHealAbility(ability)) {
            return isRescueActionConfigAvailable();
        }
        if (ability.hasSecondaryAction()) {
            return canUseSecondary;
        }
        return false;
    }

    @Override
    public boolean hasSecondaryPageButton(IBondAbility ability, boolean abilityUnlocked) {
        return abilityUnlocked && BondSecondaryPageRegistry.hasPage(this, ability);
    }

    @Override
    public Component getSecondaryPageButtonLabel(IBondAbility ability) {
        if ("lap_pillow".equals(ability.getId())) {
            return Component.translatable("bond.action.settings");
        }
        if (isEmergencyHealAbility(ability)) {
            return Component.translatable("bond.action.voice");
        }
        if (isMorningKissAbility(ability)) {
            return Component.translatable("bond.action.voice");
        }
        return Component.empty();
    }

    @Override
    public boolean isRandomGiftAbility(IBondAbility ability) {
        return "random_gift".equals(ability.getId());
    }

    @Override
    public boolean isMorningKissAbility(IBondAbility ability) {
        return "morning_kiss".equals(ability.getId());
    }

    @Override
    public boolean isEmergencyHealAbility(IBondAbility ability) {
        return "emergency_heal".equals(ability.getId());
    }

    @Override
    public String formatRemainingDuration(int totalSeconds) {
        long safeSeconds = Math.max(0L, totalSeconds);
        long minutes = safeSeconds / 60L;
        long seconds = safeSeconds % 60L;
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    public boolean isRescueActionConfigAvailable() {
        return maid != null && maid.isYsmModel() && !getRescueActionModelId().isBlank();
    }

    @Override
    public boolean isMimoAdapterAvailable() {
        return ModConfig.TMA_MIMO_ADAPTER_ENABLED.get();
    }

    @Override
    public String getRescueActionModelId() {
        if (maid == null) {
            return "";
        }
        String ysmModelId = maid.getYsmModelId();
        return ysmModelId == null ? "" : ysmModelId;
    }

    @Override
    public String getRescueActionTextureId() {
        if (maid == null) {
            return "";
        }
        String ysmTexture = maid.getYsmModelTexture();
        return ysmTexture == null ? "" : ysmTexture;
    }

    private String getCurrentSoundPackId() {
        if (maid == null) {
            return "";
        }
        String soundPackId = maid.getSoundPackId();
        return soundPackId == null ? "" : soundPackId;
    }

    @Override
    public String resolveSelectedRescueActionLabel(String actionId) {
        if (actionId == null || actionId.isBlank()) {
            return "";
        }
        ensureRescueActionLabelCache();
        return cachedRescueActionLabels.getOrDefault(actionId, actionId);
    }

    private void ensureRescueActionLabelCache() {
        String modelId = getRescueActionModelId();
        String textureId = getRescueActionTextureId();
        if (modelId.equals(cachedRescueActionModelId) && textureId.equals(cachedRescueActionTextureId)) {
            return;
        }

        LinkedHashMap<String, String> labels = new LinkedHashMap<>();
        for (YsmModelActionIndex.DetectedYsmAction action : YsmModelActionIndex.getActions(modelId, textureId)) {
            labels.put(action.actionId(), action.displayName());
        }
        cachedRescueActionModelId = modelId;
        cachedRescueActionTextureId = textureId;
        cachedRescueActionLabels = Map.copyOf(labels);
    }

    private void invalidateRescueActionLabelCache() {
        cachedRescueActionModelId = "";
        cachedRescueActionTextureId = "";
        cachedRescueActionLabels = Map.of();
    }

    private boolean isInsideRect(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private boolean isMouseOverAnyMenuSlot(double mouseX, double mouseY) {
        for (Slot slot : menu.slots) {
            if (slot != null && isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Font getFont() {
        return font;
    }

    @Override
    public EntityMaid getMaid() {
        return maid;
    }

    @Override
    public Player getLocalPlayer() {
        return Minecraft.getInstance().player;
    }

    @Override
    public List<IBondAbility> getAbilities() {
        return abilities;
    }

    @Override
    public void activateAbility(IBondAbility ability) {
        PacketDistributor.sendToServer(new BondActivateAbilityPayload(ability.getId(), maid.getUUID()));
    }
}
