package com.github.touhoumaidaffection.client;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.touhoumaidaffection.ModConfig;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@EventBusSubscriber(modid = TouhouMaidAffection.MOD_ID, value = Dist.CLIENT)
public class KissParticleEffectManager {
    private static final List<KissParticleEffectInstance> ACTIVE_EFFECTS = new ArrayList<>();
    private static final double TAU = Math.PI * 2.0;

    public static void queueKissEffect(Entity maid, Entity player) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }

        int favorabilityLevel = 0;
        if (maid instanceof EntityMaid entityMaid) {
            favorabilityLevel = entityMaid.getFavorabilityManager().getLevel();
        }

        int totalLogicalParticles = ModConfig.PARTICLE_COUNT_MIN.get() + level.random.nextInt(ModConfig.PARTICLE_COUNT_EXTRA.get() + 1);
        ACTIVE_EFFECTS.add(new KissParticleEffectInstance(level.getGameTime(), maid.getId(), player.getId(), totalLogicalParticles, favorabilityLevel));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            ACTIVE_EFFECTS.clear();
            return;
        }

        long currentTick = level.getGameTime();
        Iterator<KissParticleEffectInstance> iterator = ACTIVE_EFFECTS.iterator();
        while (iterator.hasNext()) {
            KissParticleEffectInstance effect = iterator.next();
            if (effect.isFinished(currentTick)) {
                iterator.remove();
                continue;
            }
            if (!effect.emitPendingBursts(level, currentTick)) {
                iterator.remove();
            }
        }
    }

    private static final class KissParticleEffectInstance {
        private final long startTick;
        private final int maidEntityId;
        private final int playerEntityId;
        private final int[] burstCounts;
        private final int phaseBursts;
        private final int phaseIntervalTicks;
        private final int favorabilityLevel;
        private final ModConfig.ParticleShapeMode shapeMode;
        private final ModConfig.ParticlePhaseRamp phaseRamp;
        private final ModConfig.ParticleAccentType accentType;
        private final ModConfig.ParticleAccentColorMode accentColorMode;
        private final double offsetY;
        private final double spreadRadius;
        private final double forwardOffset;
        private final double avoidViewStrength;
        private final double upwardSpeed;
        private final double radialSpeed;
        private final double swirlSpeed;
        private final int clusterCopies;
        private final double clusterJitter;
        private final boolean accentEnabled;
        private final double accentChance;
        private final boolean favorabilityColorAccent;
        private final Rgba solidAccent;
        private final Rgba gradientStartAccent;
        private final Rgba gradientEndAccent;
        private int nextBurstIndex;

        private KissParticleEffectInstance(long startTick, int maidEntityId, int playerEntityId, int logicalTotalCount, int favorabilityLevel) {
            this.startTick = startTick;
            this.maidEntityId = maidEntityId;
            this.playerEntityId = playerEntityId;
            this.phaseBursts = ModConfig.PARTICLE_PHASE_BURSTS.get();
            this.phaseIntervalTicks = ModConfig.PARTICLE_PHASE_INTERVAL_TICKS.get();
            this.favorabilityLevel = favorabilityLevel;
            this.shapeMode = ModConfig.getParticleShapeMode();
            this.phaseRamp = ModConfig.getParticlePhaseRamp();
            this.accentType = ModConfig.getParticleAccentType();
            this.accentColorMode = ModConfig.getParticleAccentColorMode();
            this.offsetY = ModConfig.PARTICLE_OFFSET_Y.get();
            this.spreadRadius = ModConfig.PARTICLE_SPREAD_RADIUS.get();
            this.forwardOffset = ModConfig.PARTICLE_FORWARD_OFFSET.get();
            this.avoidViewStrength = ModConfig.PARTICLE_AVOID_VIEW_STRENGTH.get();
            this.upwardSpeed = ModConfig.PARTICLE_UPWARD_SPEED.get();
            this.radialSpeed = ModConfig.PARTICLE_RADIAL_SPEED.get();
            this.swirlSpeed = ModConfig.PARTICLE_SWIRL_SPEED.get();
            this.clusterCopies = ModConfig.PARTICLE_CLUSTER_COPIES.get();
            this.clusterJitter = ModConfig.PARTICLE_CLUSTER_JITTER.get();
            this.accentEnabled = ModConfig.PARTICLE_ACCENT_ENABLED.get();
            this.accentChance = ModConfig.PARTICLE_ACCENT_CHANCE.get();
            this.favorabilityColorAccent = ModConfig.PARTICLE_FAVORABILITY_COLOR_ACCENT.get();
            this.solidAccent = new Rgba(
                    ModConfig.PARTICLE_ACCENT_SOLID_R.get(),
                    ModConfig.PARTICLE_ACCENT_SOLID_G.get(),
                    ModConfig.PARTICLE_ACCENT_SOLID_B.get(),
                    ModConfig.PARTICLE_ACCENT_SOLID_A.get()
            );
            this.gradientStartAccent = new Rgba(
                    ModConfig.PARTICLE_ACCENT_GRADIENT_START_R.get(),
                    ModConfig.PARTICLE_ACCENT_GRADIENT_START_G.get(),
                    ModConfig.PARTICLE_ACCENT_GRADIENT_START_B.get(),
                    ModConfig.PARTICLE_ACCENT_GRADIENT_START_A.get()
            );
            this.gradientEndAccent = new Rgba(
                    ModConfig.PARTICLE_ACCENT_GRADIENT_END_R.get(),
                    ModConfig.PARTICLE_ACCENT_GRADIENT_END_G.get(),
                    ModConfig.PARTICLE_ACCENT_GRADIENT_END_B.get(),
                    ModConfig.PARTICLE_ACCENT_GRADIENT_END_A.get()
            );
            this.burstCounts = distributeToBursts(logicalTotalCount, phaseBursts, phaseRamp);
            this.nextBurstIndex = 0;
        }

        private boolean emitPendingBursts(ClientLevel level, long currentTick) {
            Entity maid = level.getEntity(maidEntityId);
            Entity player = level.getEntity(playerEntityId);
            if (maid == null || player == null) {
                return false;
            }

            long elapsed = Math.max(0, currentTick - startTick);
            while (nextBurstIndex < phaseBursts && elapsed >= (long) nextBurstIndex * phaseIntervalTicks) {
                emitSingleBurst(level, maid, player, nextBurstIndex, burstCounts[nextBurstIndex]);
                nextBurstIndex++;
            }
            return true;
        }

        private boolean isFinished(long currentTick) {
            if (nextBurstIndex < phaseBursts) {
                return false;
            }
            long elapsed = Math.max(0, currentTick - startTick);
            return elapsed > (long) phaseBursts * phaseIntervalTicks + 6L;
        }

        private void emitSingleBurst(ClientLevel level, Entity maid, Entity player, int burstIndex, int logicalCount) {
            if (logicalCount <= 0) {
                return;
            }

            Vec3 playerEye = player.getEyePosition();
            Vec3 maidEye = maid.getEyePosition();
            Vec3 axisForward = getHorizontalForward(playerEye, maidEye, player.getLookAngle());
            Vec3 axisRight = new Vec3(-axisForward.z, 0.0, axisForward.x);

            double sideSign = Math.signum(maidEye.subtract(playerEye).dot(axisRight));
            if (sideSign == 0) {
                sideSign = 1.0;
            }

            Vec3 anchor = playerEye.add(maidEye).scale(0.5)
                    .add(0.0, offsetY + avoidViewStrength * 0.15, 0.0)
                    .add(axisForward.scale(forwardOffset))
                    .add(axisRight.scale(sideSign * avoidViewStrength * spreadRadius * 0.5));

            double phaseProgress = phaseBursts <= 1 ? 1.0 : (double) burstIndex / (double) (phaseBursts - 1);

            for (int i = 0; i < logicalCount; i++) {
                double angle = level.random.nextDouble() * TAU;
                double radius = sampleRadius(level, phaseProgress);
                if (shapeMode == ModConfig.ParticleShapeMode.SPIRAL) {
                    angle += phaseProgress * TAU * 0.75;
                }

                Vec3 radialDir = axisRight.scale(Math.cos(angle)).add(axisForward.scale(Math.sin(angle)));
                Vec3 tangentDir = axisRight.scale(-Math.sin(angle)).add(axisForward.scale(Math.cos(angle)));

                Vec3 spawnPos = anchor
                        .add(radialDir.scale(radius))
                        .add(0.0, level.random.nextDouble() * 0.1, 0.0);

                double up = upwardSpeed * (0.8 + level.random.nextDouble() * 0.4);
                double radial = radialSpeed * (0.8 + level.random.nextDouble() * 0.4);
                double swirl = swirlSpeed * 0.02 * (0.8 + level.random.nextDouble() * 0.4);
                Vec3 velocity = radialDir.scale(radial).add(tangentDir.scale(swirl)).add(0.0, up, 0.0);

                spawnHeartCluster(level, spawnPos, velocity);
                spawnAccent(level, spawnPos, velocity.scale(0.75), phaseProgress);
            }
        }

        private double sampleRadius(ClientLevel level, double phaseProgress) {
            return switch (shapeMode) {
                case RING -> spreadRadius * (0.85 + level.random.nextDouble() * 0.3);
                case SPIRAL -> spreadRadius * Mth.clamp(0.15 + phaseProgress * 0.75 + level.random.nextDouble() * 0.15, 0.0, 1.2);
                case HALO -> spreadRadius * Math.sqrt(level.random.nextDouble());
            };
        }

        private void spawnHeartCluster(ClientLevel level, Vec3 pos, Vec3 velocity) {
            int copies = Math.max(0, clusterCopies);
            for (int i = 0; i <= copies; i++) {
                Vec3 jitter = randomJitter(level, clusterJitter);
                level.addParticle(
                        ParticleTypes.HEART,
                        pos.x + jitter.x,
                        pos.y + jitter.y,
                        pos.z + jitter.z,
                        velocity.x,
                        velocity.y,
                        velocity.z
                );
            }
        }

        private void spawnAccent(ClientLevel level, Vec3 pos, Vec3 velocity, double phaseProgress) {
            Rgba accentRgba = getAccentRgba(phaseProgress);
            double alphaFactor = accentRgba.alpha01();
            if (!accentEnabled || accentType == ModConfig.ParticleAccentType.NONE || level.random.nextDouble() > (accentChance * alphaFactor)) {
                return;
            }

            ParticleOptions accent = switch (accentType) {
                case GLOW -> ParticleTypes.GLOW;
                case DUST -> new DustParticleOptions(accentRgba.toVec3f(), accentRgba.toDustScale());
                case NONE -> null;
            };
            if (accent == null) {
                return;
            }
            level.addParticle(accent, pos.x, pos.y, pos.z, velocity.x * 0.6, velocity.y * 0.8, velocity.z * 0.6);
        }

        private Rgba getAccentRgba(double phaseProgress) {
            if (accentColorMode == ModConfig.ParticleAccentColorMode.SOLID) {
                return solidAccent;
            }
            if (accentColorMode == ModConfig.ParticleAccentColorMode.GRADIENT) {
                return gradientStartAccent.lerp(gradientEndAccent, phaseProgress);
            }
            if (!favorabilityColorAccent) {
                return new Rgba(255, 166, 191, 220);
            }
            return switch (Mth.clamp(favorabilityLevel, 0, 3)) {
                case 0 -> new Rgba(255, 191, 214, 210);
                case 1 -> new Rgba(255, 168, 204, 220);
                case 2 -> new Rgba(255, 143, 184, 230);
                default -> new Rgba(255, 189, 115, 240);
            };
        }
    }

    private record Rgba(int r, int g, int b, int a) {
        private Rgba {
            r = Mth.clamp(r, 0, 255);
            g = Mth.clamp(g, 0, 255);
            b = Mth.clamp(b, 0, 255);
            a = Mth.clamp(a, 0, 255);
        }

        private Vector3f toVec3f() {
            float alpha = (float) alpha01();
            float boost = 0.65f + 0.35f * alpha;
            return new Vector3f(
                    (r / 255.0f) * boost,
                    (g / 255.0f) * boost,
                    (b / 255.0f) * boost
            );
        }

        private float toDustScale() {
            // Dust supports RGB + size, no direct alpha; map alpha to size for visual "opacity" feeling.
            return 0.65f + (float) alpha01() * 0.55f;
        }

        private double alpha01() {
            return a / 255.0;
        }

        private Rgba lerp(Rgba other, double t) {
            t = Mth.clamp(t, 0.0, 1.0);
            int nr = (int) Math.round(Mth.lerp(t, r, other.r));
            int ng = (int) Math.round(Mth.lerp(t, g, other.g));
            int nb = (int) Math.round(Mth.lerp(t, b, other.b));
            int na = (int) Math.round(Mth.lerp(t, a, other.a));
            return new Rgba(nr, ng, nb, na);
        }
    }

    private static Vec3 getHorizontalForward(Vec3 playerEye, Vec3 maidEye, Vec3 fallbackLook) {
        Vec3 axis = maidEye.subtract(playerEye);
        axis = new Vec3(axis.x, 0.0, axis.z);
        if (axis.lengthSqr() < 1.0E-6) {
            axis = new Vec3(fallbackLook.x, 0.0, fallbackLook.z);
        }
        if (axis.lengthSqr() < 1.0E-6) {
            return new Vec3(0.0, 0.0, 1.0);
        }
        return axis.normalize();
    }

    private static Vec3 randomJitter(ClientLevel level, double maxRadius) {
        if (maxRadius <= 0.0) {
            return Vec3.ZERO;
        }
        return new Vec3(
                (level.random.nextDouble() - 0.5) * 2.0 * maxRadius,
                (level.random.nextDouble() - 0.5) * 2.0 * maxRadius,
                (level.random.nextDouble() - 0.5) * 2.0 * maxRadius
        );
    }

    private static int[] distributeToBursts(int total, int bursts, ModConfig.ParticlePhaseRamp ramp) {
        int[] result = new int[Math.max(1, bursts)];
        if (total <= 0) {
            return result;
        }

        double[] weights = new double[result.length];
        double sum = 0.0;
        for (int i = 0; i < result.length; i++) {
            double w = ramp == ModConfig.ParticlePhaseRamp.UNIFORM ? 1.0 : (i + 1);
            weights[i] = w;
            sum += w;
        }

        int assigned = 0;
        for (int i = 0; i < result.length; i++) {
            result[i] = (int) Math.floor(total * (weights[i] / sum));
            assigned += result[i];
        }

        int rest = total - assigned;
        for (int i = result.length - 1; i >= 0 && rest > 0; i--) {
            result[i]++;
            rest--;
        }
        return result;
    }
}
