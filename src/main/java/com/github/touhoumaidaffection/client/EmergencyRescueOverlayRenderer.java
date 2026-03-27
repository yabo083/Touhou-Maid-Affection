package com.github.touhoumaidaffection.client;

import com.github.tartaricacid.touhoulittlemaid.compat.ysm.event.UpdateRemoteStructEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.compat.ysm.event.YsmMaidClientTickEvent;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import com.github.touhoumaidaffection.ModConfig;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.github.touhoumaidaffection.network.MaidRescuePopPayload;
import com.github.touhoumaidaffection.util.MaidDisplayNameResolver;
import com.github.touhoumaidaffection.ysm.YSMActionBridge;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import org.joml.Quaternionf;

import static com.github.tartaricacid.touhoulittlemaid.util.EntityCacheUtil.clearMaidDataResidue;

@EventBusSubscriber(modid = TouhouMaidAffection.MOD_ID, value = Dist.CLIENT)
public final class EmergencyRescueOverlayRenderer {
    private static final int DURATION_TICKS = 40;
    private static ActiveOverlay activeOverlay;

    private EmergencyRescueOverlayRenderer() {
    }

    public static boolean show(MaidRescuePopPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (minecraft.player == null || level == null) {
            return false;
        }

        try {
            EntityMaid maid = buildOverlayMaid(level, payload);
            clearMaidDataResidue(maid, true);
            maid.setNoAi(true);
            maid.setSilent(true);
            maid.setInvulnerable(true);
            maid.tickCount = (int) level.getGameTime();
            resetOverlayYsmRuntimeState(maid);
            String selectedAction = resolveSelectedAction(payload);
            boolean hasCustomAction = !selectedAction.isBlank();
            syncYsmState(maid);
            if (hasCustomAction) {
                YSMActionBridge.playIfAvailable(maid, selectedAction);
            }
            TouhouMaidAffection.LOGGER.debug(
                    "Emergency rescue overlay created: maidUuid={}, ysmLoaded={}, ysmModel={}, rescueActionId='{}', appliedAction='{}', rouletteAnim='{}', roulettePlaying={}",
                    payload.maidUuid(),
                    com.github.touhoumaidaffection.ysm.YSMCompatibility.isYSMLoaded(),
                    maid.isYsmModel(),
                    payload.rescueActionId(),
                    selectedAction,
                    maid.rouletteAnim,
                    maid.rouletteAnimPlaying
            );
            activeOverlay = new ActiveOverlay(
                    maid,
                    hasCustomAction,
                    DURATION_TICKS,
                    false
            );
            return true;
        } catch (Exception ex) {
            TouhouMaidAffection.LOGGER.warn("Failed to create rescue overlay maid, falling back to totem animation.", ex);
            activeOverlay = null;
            return false;
        }
    }

    public static Component getResolvedDisplayName(MaidRescuePopPayload payload) {
        return MaidDisplayNameResolver.resolveDisplayName(
                payload.maidModelId(),
                payload.maidDisplayName(),
                payload.ysmDisplayName()
        );
    }

    public static String getPlainDisplayName(MaidRescuePopPayload payload) {
        return MaidDisplayNameResolver.resolvePlainDisplayName(
                payload.maidModelId(),
                payload.maidDisplayName(),
                payload.ysmDisplayName()
        );
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (activeOverlay == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || activeOverlay.remainingTicks() <= 0) {
            clearOverlay();
            return;
        }
        tickOverlayMaid(activeOverlay);
        activeOverlay = activeOverlay.tickDown();
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (activeOverlay == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getWindow() == null) {
            clearOverlay();
            return;
        }

        GuiGraphics gui = event.getGuiGraphics();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        float progress = activeOverlay.progress(event.getPartialTick());
        float scaleFactor = getScale(progress, activeOverlay.hasCustomAction());
        float rise = getRise(progress, activeOverlay.hasCustomAction());
        float sway = getSway(progress, activeOverlay.hasCustomAction());
        float screenRotation = getScreenRotation(progress, activeOverlay.hasCustomAction());

        int centerX = width / 2;
        int centerY = height / 2;
        int scale = Math.max(1, Math.round(52 * scaleFactor));

        applyEntityPose(activeOverlay.maid(), progress, activeOverlay.hasCustomAction());

        gui.pose().pushPose();
        try {
            float translatedCenterX = centerX + sway;
            float translatedCenterY = centerY - rise;
            int halfWidth = Math.max(72, Math.round(scale * 2.3f));
            int halfHeight = Math.max(108, Math.round(scale * 2.8f));
            gui.pose().translate(translatedCenterX, translatedCenterY, 0.0f);
            gui.pose().mulPose(Axis.ZP.rotationDegrees(screenRotation));
            gui.pose().translate(-translatedCenterX, -translatedCenterY, 0.0f);
            EntityRenderState state = EntityRenderState.capture(activeOverlay.maid());
            float configuredXOffset = (float) ModConfig.BOND_EMERGENCY_RESCUE_VIEW_X_ROT_OFFSET.get().doubleValue();
            float configuredOffset = (float) ModConfig.BOND_EMERGENCY_RESCUE_VIEW_Y_ROT_OFFSET.get().doubleValue();
            float configuredZOffset = (float) ModConfig.BOND_EMERGENCY_RESCUE_VIEW_Z_ROT_OFFSET.get().doubleValue();
            Quaternionf bodyRotation = new Quaternionf()
                    .rotateZ((float) Math.PI);
            bodyRotation.rotateX(configuredXOffset * Mth.DEG_TO_RAD);
            bodyRotation.rotateY(configuredOffset * Mth.DEG_TO_RAD);
            bodyRotation.rotateZ(configuredZOffset * Mth.DEG_TO_RAD);
            Quaternionf cameraRotation = new Quaternionf()
                    .rotateX(activeOverlay.hasCustomAction() ? 0.0f : -10.0f * Mth.DEG_TO_RAD);
            InventoryScreen.renderEntityInInventory(
                    gui,
                    Math.round(translatedCenterX),
                    Math.round(translatedCenterY + scale * 0.95f),
                    scale,
                    bodyRotation,
                    cameraRotation,
                    activeOverlay.maid()
            );
            state.restore(activeOverlay.maid());
        } finally {
            gui.pose().popPose();
        }
    }

    private static String resolveSelectedAction(MaidRescuePopPayload payload) {
        if (payload.rescueActionId() != null && !payload.rescueActionId().isBlank()) {
            return payload.rescueActionId().trim();
        }
        if (payload.ysmModelId().isBlank()) {
            return "";
        }
        return RescueYsmActionConfig.getSelectedAction(payload.ysmModelId(), payload.ysmModelTexture());
    }

    private static void clearOverlay() {
        if (activeOverlay != null) {
            activeOverlay.maid().stopRouletteAnim();
        }
        activeOverlay = null;
    }

    private static void tickOverlayMaid(ActiveOverlay overlay) {
        EntityMaid maid = overlay.maid();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            maid.tickCount = (int) minecraft.level.getGameTime();
        } else {
            maid.tickCount++;
        }
        if (maid.isYsmModel()) {
            if (!overlay.tickLogEmitted()) {
                TouhouMaidAffection.LOGGER.debug(
                        "Emergency rescue overlay tick start: maidUuid={}, rouletteAnim='{}', roulettePlaying={}, roamingVars={}, remainingTicks={}",
                        maid.getUUID(),
                        maid.rouletteAnim,
                        maid.rouletteAnimPlaying,
                        maid.roamingVars.size(),
                        overlay.remainingTicks()
                );
            }
            MinecraftForge.EVENT_BUS.post(new YsmMaidClientTickEvent(maid));
        }
    }

    private static void syncYsmState(EntityMaid maid) {
        if (!maid.isYsmModel()) {
            return;
        }
        MinecraftForge.EVENT_BUS.post(new UpdateRemoteStructEvent(maid, new Object2FloatOpenHashMap<>(maid.roamingVars)));
    }

    private static void resetOverlayYsmRuntimeState(EntityMaid maid) {
        if (!maid.isYsmModel()) {
            return;
        }
        maid.rouletteAnimPlaying = false;
        maid.rouletteAnim = "empty";
        maid.rouletteAnimDirty = false;
        maid.roamingVarsUpdateFlag = 0;
    }

    private static void applyEntityPose(EntityMaid maid, float progress, boolean hasCustomAction) {
        float headSway = hasCustomAction ? 0.0f : Mth.sin(progress * (float) Math.PI * 1.5f) * 1.5f;
        float pitch = hasCustomAction ? -4.0f : -10.0f + Mth.sin(progress * (float) Math.PI) * 3.0f;
        maid.setYRot(0.0f);
        maid.yRotO = 0.0f;
        maid.setYBodyRot(0.0f);
        maid.yBodyRotO = 0.0f;
        maid.setYHeadRot(headSway);
        maid.yHeadRotO = maid.getYHeadRot();
        maid.setXRot(pitch);
        maid.xRotO = pitch;
    }

    private static float getScale(float progress, boolean hasCustomAction) {
        if (hasCustomAction) {
            if (progress < 0.18f) {
                float local = progress / 0.18f;
                return 0.5f + easeOutBack(local) * 0.62f;
            }
            if (progress < 0.82f) {
                float local = (progress - 0.18f) / 0.64f;
                return 1.12f - Mth.sin(local * (float) Math.PI) * 0.04f;
            }
            float local = (progress - 0.82f) / 0.18f;
            return 1.08f - easeInCubic(local) * 0.96f;
        }
        if (progress < 0.16f) {
            float local = progress / 0.16f;
            return 0.2f + easeOutBack(local) * 0.96f;
        }
        if (progress < 0.72f) {
            float local = (progress - 0.16f) / 0.56f;
            return 1.16f - Mth.sin(local * (float) Math.PI) * 0.06f;
        }
        float local = (progress - 0.72f) / 0.28f;
        return 1.1f - easeInCubic(local) * 1.04f;
    }

    private static float getScreenRotation(float progress, boolean hasCustomAction) {
        if (hasCustomAction) {
            if (progress < 0.2f) {
                float local = progress / 0.2f;
                return 18.0f - easeOutCubic(local) * 20.0f;
            }
            if (progress < 0.82f) {
                float local = (progress - 0.2f) / 0.62f;
                return -2.0f + Mth.sin(local * (float) Math.PI * 2.0f) * 2.0f;
            }
            float local = (progress - 0.82f) / 0.18f;
            return easeInCubic(local) * 14.0f;
        }
        if (progress < 0.18f) {
            float local = progress / 0.18f;
            return 95.0f - easeOutCubic(local) * 115.0f;
        }
        if (progress < 0.68f) {
            float local = (progress - 0.18f) / 0.5f;
            return -20.0f + Mth.sin(local * (float) Math.PI) * 8.0f;
        }
        float local = (progress - 0.68f) / 0.32f;
        return -20.0f + easeInCubic(local) * 78.0f;
    }

    private static float getRise(float progress, boolean hasCustomAction) {
        if (hasCustomAction) {
            if (progress < 0.18f) {
                float local = progress / 0.18f;
                return 22.0f - easeOutCubic(local) * 18.0f;
            }
            if (progress < 0.82f) {
                float local = (progress - 0.18f) / 0.64f;
                return 4.0f + Mth.sin(local * (float) Math.PI * 1.5f) * 1.2f;
            }
            float local = (progress - 0.82f) / 0.18f;
            return 4.0f + easeInCubic(local) * 22.0f;
        }
        if (progress < 0.18f) {
            float local = progress / 0.18f;
            return 30.0f - easeOutCubic(local) * 26.0f;
        }
        if (progress < 0.68f) {
            float local = (progress - 0.18f) / 0.5f;
            return 4.0f + Mth.sin(local * (float) Math.PI * 2.0f) * 1.6f;
        }
        float local = (progress - 0.68f) / 0.32f;
        return 4.0f + easeInCubic(local) * 34.0f;
    }

    private static float getSway(float progress, boolean hasCustomAction) {
        float amplitude = hasCustomAction ? 2.0f : 4.5f;
        float frequency = hasCustomAction ? 2.5f : 3.0f;
        return Mth.sin(progress * (float) Math.PI * frequency) * amplitude;
    }

    private static EntityMaid buildOverlayMaid(ClientLevel level, MaidRescuePopPayload payload) {
        EntityMaid maid = new EntityMaid(level);
        maid.setModelId(payload.maidModelId());
        applyPayloadYsmIdentity(maid, payload);
        return maid;
    }

    private static float easeOutCubic(float x) {
        float inv = 1.0f - x;
        return 1.0f - inv * inv * inv;
    }

    private static float easeInCubic(float x) {
        return x * x * x;
    }

    private static float easeOutBack(float x) {
        float c1 = 1.70158f;
        float c3 = c1 + 1.0f;
        float p = x - 1.0f;
        return 1.0f + c3 * p * p * p + c1 * p * p;
    }

    private static void applyPayloadYsmIdentity(EntityMaid maid, MaidRescuePopPayload payload) {
        if (!payload.ysmModelId().isBlank() && !payload.ysmModelTexture().isBlank()) {
            maid.setIsYsmModel(true);
            Component ysmName = payload.ysmDisplayName().isBlank()
                    ? Component.empty()
                    : Component.literal(payload.ysmDisplayName());
            maid.setYsmModel(payload.ysmModelId(), payload.ysmModelTexture(), ysmName);
            return;
        }
        maid.setIsYsmModel(false);
    }

    private record ActiveOverlay(EntityMaid maid, boolean hasCustomAction, int remainingTicks, boolean tickLogEmitted) {
        private ActiveOverlay tickDown() {
            return new ActiveOverlay(maid, hasCustomAction, remainingTicks - 1, true);
        }

        private float progress(float partialTick) {
            float remaining = Math.max(0.0f, remainingTicks - partialTick);
            return 1.0f - remaining / DURATION_TICKS;
        }
    }

    private record EntityRenderState(
            float yRot,
            float yRotO,
            float yBodyRot,
            float yBodyRotO,
            float yHeadRot,
            float yHeadRotO,
            float xRot,
            float xRotO
    ) {
        private static EntityRenderState capture(EntityMaid maid) {
            return new EntityRenderState(
                    maid.getYRot(),
                    maid.yRotO,
                    maid.yBodyRot,
                    maid.yBodyRotO,
                    maid.getYHeadRot(),
                    maid.yHeadRotO,
                    maid.getXRot(),
                    maid.xRotO
            );
        }

        private void restore(EntityMaid maid) {
            maid.setYRot(yRot);
            maid.yRotO = yRotO;
            maid.setYBodyRot(yBodyRot);
            maid.yBodyRotO = yBodyRotO;
            maid.setYHeadRot(yHeadRot);
            maid.yHeadRotO = yHeadRotO;
            maid.setXRot(xRot);
            maid.xRotO = xRotO;
        }
    }
}
