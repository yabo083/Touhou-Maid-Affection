package com.github.touhoumaidaffection.client;

import com.github.touhoumaidaffection.ModConfig;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(modid = TouhouMaidAffection.MOD_ID, value = Dist.CLIENT)
public class KissFovHandler {

    private static long zoomStartTime = -1;
    private static long outStartTime = -1;
    private static int zoomInTicks;
    private static int zoomOutTicks;

    // Camera angle forcing
    private static int targetMaidId = -1;
    private static float startYaw;
    private static float startPitch;
    private static float targetYaw;
    private static float targetPitch;

    // Camera position dolly (GMOD-style move toward the maid's face, first person only)
    private static Vec3 startPos;
    private static Vec3 targetPos;

    /**
     * Trigger a GMOD-style kiss camera: position dolly toward the maid's face
     * (first person) + camera snap to her face.
     */
    public static void trigger(int maidEntityId, boolean isCarriedKiss) {
        if (isCarriedKiss) {
            // Carried kiss: no camera animation and no maid animation. The player keeps
            // full camera control and the maid stays in her default carried pose; only
            // the kiss particles play (queued separately by KissClientHandler). Cancel
            // any in-flight standing-kiss state too.
            zoomStartTime = -1;
            outStartTime = -1;
            targetMaidId = -1;
            startPos = null;
            targetPos = null;
            return;
        }
        zoomStartTime = System.currentTimeMillis();
        outStartTime = -1;
        zoomInTicks = ModConfig.FOV_ZOOM_IN_TICKS.get();
        zoomOutTicks = ModConfig.FOV_ZOOM_OUT_TICKS.get();
        targetMaidId = maidEntityId;

        // Capture current camera angles and calculate target
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null) {
            startYaw = mc.player.getYRot();
            startPitch = mc.player.getXRot();
            startPos = mc.player.getEyePosition();

            Entity maid = mc.level.getEntity(maidEntityId);
            if (maid != null) {
                Vec3 eyePos = startPos;
                // Base aim: TLM's standing eye position sits at 85% of the maid's height, which
                // lands on the neck/mouth for tall-head models; the tuned offset raises it to
                // the face.
                Vec3 standingTarget = maid.getEyePosition().add(0.0, ModConfig.KISS_CAMERA_FACE_OFFSET_Y.get(), 0.0);
                Vec3 targetLookPos = standingTarget;
                boolean followAim = false;
                // A maid can render low (sitting pose/animation, chair seat, etc.) while her
                // entity pose stays STANDING, so getEyePosition() is useless there and no
                // entity-state check can catch it. If the player is aiming at a point well
                // below the standing face, follow the crosshair point on her instead — the
                // kiss lands exactly where the player is looking (her visible face).
                if (mc.hitResult instanceof EntityHitResult entityHit && entityHit.getEntity() == maid) {
                    Vec3 hit = entityHit.getLocation();
                    if (hit.y < standingTarget.y - 0.3) {
                        followAim = true;
                        targetLookPos = new Vec3(maid.getX(), hit.y + ModConfig.KISS_CAMERA_SITTING_FACE_OFFSET_Y.get(), maid.getZ());
                    }
                }
                TouhouMaidAffection.LOGGER.info("[KissDebug] maid={} pose={} hitY={} standingTargetY={} followAim={} sittingOffsetConfig={}",
                        maid.getId(),
                        (maid instanceof LivingEntity le ? le.getPose() : "non-living"),
                        (mc.hitResult instanceof EntityHitResult ehr && ehr.getEntity() == maid ? ehr.getLocation().y : "no-hit"),
                        standingTarget.y,
                        followAim,
                        ModConfig.KISS_CAMERA_SITTING_FACE_OFFSET_Y.get());
                Vec3 diff = targetLookPos.subtract(eyePos);
                double dist = diff.horizontalDistance();
                targetYaw = (float) (Mth.atan2(diff.z, diff.x) * Mth.RAD_TO_DEG) - 90.0F;
                targetPitch = (float) -(Mth.atan2(diff.y, dist) * Mth.RAD_TO_DEG);

                // Dolly target: end up KISS_CAMERA_GAP blocks in front of the maid's face,
                // along the line from the player's eye to the face, so the close-up
                // frames the face without clipping into the model.
                Vec3 dir = diff.lengthSqr() < 1.0E-6 ? mc.player.getLookAngle().normalize() : diff.normalize();
                targetPos = targetLookPos.subtract(dir.scale(ModConfig.KISS_CAMERA_GAP.get()));
            } else {
                targetYaw = startYaw;
                targetPitch = startPitch;
                targetPos = startPos;
            }
        }
    }

    /**
     * Whether the kiss camera-dolly is currently running. Used to suppress the
     * first-person hand model so it does not float in front of the maid's face.
     */
    public static boolean isDollyActive() {
        // Hide the first-person hand whenever the kiss dolly runs (standing or carried),
        // so it does not float in front of the maid's face.
        return ModConfig.KISS_CAMERA_MOVE_ENABLED.get() && getAnimFactor() >= 0;
    }

    /**
     * The camera position for the current kiss frame, interpolated from the player's
     * eye toward the maid's face. Returns null when the dolly is not active.
     * Applied by CameraKissMixin in first-person view only.
     */
    public static Vec3 getDollyPosition() {
        if (!ModConfig.KISS_CAMERA_MOVE_ENABLED.get()) {
            return null;
        }
        float factor = getAnimFactor();
        if (factor < 0 || startPos == null || targetPos == null) {
            return null;
        }
        return startPos.lerp(targetPos, factor);
    }

    /**
     * Make the kissed maid turn to face the player for the whole kiss window, so the
     * dolly camera arrives at her face instead of the back of her head.
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (targetMaidId < 0) {
            return;
        }
        if (getAnimFactor() < 0) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        Entity maid = mc.level.getEntity(targetMaidId);
        if (!(maid instanceof LivingEntity livingMaid)) {
            return;
        }
        Vec3 toPlayer = mc.player.getEyePosition().subtract(maid.getEyePosition());
        double horizontalDist = toPlayer.horizontalDistance();
        if (horizontalDist < 1.0E-6) {
            return;
        }
        float yaw = (float) (Mth.atan2(toPlayer.z, toPlayer.x) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = (float) -(Mth.atan2(toPlayer.y, horizontalDist) * Mth.RAD_TO_DEG);
        livingMaid.setYBodyRot(yaw);
        livingMaid.setYHeadRot(yaw);
        livingMaid.setXRot(pitch);
    }

    /**
     * Calculate the current animation progress factor (0 to 1 to 0).
     * The camera dollies in over the zoom-in window, stays at the maid's face while
     * the kiss key is held, and returns once the key is released. Returns -1 when
     * the animation is not active.
     */
    private static float getAnimFactor() {
        if (zoomStartTime < 0) return -1f;

        long now = System.currentTimeMillis();
        long elapsed = now - zoomStartTime;
        float inMs = zoomInTicks * 50f;
        float outMs = zoomOutTicks * 50f;

        if (elapsed < inMs) {
            outStartTime = -1;
            return smoothstep(elapsed / inMs);
        }

        // Hold phase: keep the camera at the face while the kiss key is held down.
        if (KissKeyMappings.KISS_MAID.isDown()) {
            outStartTime = -1;
            return 1.0f;
        }

        // Key released: start the return phase (and finish it after outMs).
        if (outStartTime < 0) {
            outStartTime = now;
        }
        float outElapsed = now - outStartTime;
        if (outElapsed >= outMs) {
            zoomStartTime = -1;
            outStartTime = -1;
            targetMaidId = -1;
            startPos = null;
            targetPos = null;
            return -1f;
        }
        return 1.0f - smoothstep(outElapsed / outMs);
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        float factor = getAnimFactor();
        if (factor < 0 || targetMaidId < 0) return;

        // Smoothly interpolate camera angles toward the maid's face
        float lerpedYaw = Mth.rotLerp(factor, startYaw, targetYaw);
        float lerpedPitch = Mth.lerp(factor, startPitch, targetPitch);

        event.setYaw(lerpedYaw);
        event.setPitch(lerpedPitch);
    }

    private static float smoothstep(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3f - 2f * t);
    }
}
