package com.github.touhoumaidaffection;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraft.util.Mth;

import java.util.Locale;

public class ModConfig {
    public static final ForgeConfigSpec SPEC;

    // Kiss cooldown (tiered by favorability level)
    public static final ForgeConfigSpec.IntValue COOLDOWN_LEVEL_0;
    public static final ForgeConfigSpec.IntValue COOLDOWN_LEVEL_1;
    public static final ForgeConfigSpec.IntValue COOLDOWN_LEVEL_2;
    public static final ForgeConfigSpec.IntValue COOLDOWN_LEVEL_3;

    // Favorability
    public static final ForgeConfigSpec.IntValue FAVORABILITY_POINTS;
    public static final ForgeConfigSpec.IntValue FAVORABILITY_COOLDOWN;

    // Buff
    public static final ForgeConfigSpec.BooleanValue BUFF_ENABLED;
    public static final ForgeConfigSpec.IntValue BUFF_KISS_THRESHOLD;
    public static final ForgeConfigSpec.IntValue BUFF_KISS_WINDOW;
    public static final ForgeConfigSpec.IntValue BUFF_DURATION;
    public static final ForgeConfigSpec.IntValue BUFF_AMPLIFIER_LEVEL_0;
    public static final ForgeConfigSpec.IntValue BUFF_AMPLIFIER_LEVEL_1;
    public static final ForgeConfigSpec.IntValue BUFF_AMPLIFIER_LEVEL_2;
    public static final ForgeConfigSpec.IntValue BUFF_AMPLIFIER_LEVEL_3;

    // Particles
    public static final ForgeConfigSpec.IntValue PARTICLE_COUNT_MIN;
    public static final ForgeConfigSpec.IntValue PARTICLE_COUNT_EXTRA;
    public static final ForgeConfigSpec.DoubleValue PARTICLE_OFFSET_Y;
    public static final ForgeConfigSpec.DoubleValue PARTICLE_SPREAD_RADIUS;
    public static final ForgeConfigSpec.DoubleValue PARTICLE_FORWARD_OFFSET;
    public static final ForgeConfigSpec.DoubleValue PARTICLE_AVOID_VIEW_STRENGTH;
    public static final ForgeConfigSpec.ConfigValue<String> PARTICLE_SHAPE_MODE;
    public static final ForgeConfigSpec.DoubleValue PARTICLE_UPWARD_SPEED;
    public static final ForgeConfigSpec.DoubleValue PARTICLE_RADIAL_SPEED;
    public static final ForgeConfigSpec.DoubleValue PARTICLE_SWIRL_SPEED;
    public static final ForgeConfigSpec.IntValue PARTICLE_PHASE_BURSTS;
    public static final ForgeConfigSpec.IntValue PARTICLE_PHASE_INTERVAL_TICKS;
    public static final ForgeConfigSpec.ConfigValue<String> PARTICLE_PHASE_RAMP;
    public static final ForgeConfigSpec.IntValue PARTICLE_CLUSTER_COPIES;
    public static final ForgeConfigSpec.DoubleValue PARTICLE_CLUSTER_JITTER;
    public static final ForgeConfigSpec.BooleanValue PARTICLE_ACCENT_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<String> PARTICLE_ACCENT_TYPE;
    public static final ForgeConfigSpec.DoubleValue PARTICLE_ACCENT_CHANCE;
    public static final ForgeConfigSpec.BooleanValue PARTICLE_FAVORABILITY_COLOR_ACCENT;
    public static final ForgeConfigSpec.ConfigValue<String> PARTICLE_ACCENT_COLOR_MODE;
    public static final ForgeConfigSpec.IntValue PARTICLE_ACCENT_SOLID_R;
    public static final ForgeConfigSpec.IntValue PARTICLE_ACCENT_SOLID_G;
    public static final ForgeConfigSpec.IntValue PARTICLE_ACCENT_SOLID_B;
    public static final ForgeConfigSpec.IntValue PARTICLE_ACCENT_SOLID_A;
    public static final ForgeConfigSpec.IntValue PARTICLE_ACCENT_GRADIENT_START_R;
    public static final ForgeConfigSpec.IntValue PARTICLE_ACCENT_GRADIENT_START_G;
    public static final ForgeConfigSpec.IntValue PARTICLE_ACCENT_GRADIENT_START_B;
    public static final ForgeConfigSpec.IntValue PARTICLE_ACCENT_GRADIENT_START_A;
    public static final ForgeConfigSpec.IntValue PARTICLE_ACCENT_GRADIENT_END_R;
    public static final ForgeConfigSpec.IntValue PARTICLE_ACCENT_GRADIENT_END_G;
    public static final ForgeConfigSpec.IntValue PARTICLE_ACCENT_GRADIENT_END_B;
    public static final ForgeConfigSpec.IntValue PARTICLE_ACCENT_GRADIENT_END_A;

    // FOV zoom
    public static final ForgeConfigSpec.BooleanValue FOV_ZOOM_ENABLED;
    public static final ForgeConfigSpec.IntValue FOV_ZOOM_IN_TICKS;
    public static final ForgeConfigSpec.IntValue FOV_HOLD_TICKS;
    public static final ForgeConfigSpec.IntValue FOV_ZOOM_OUT_TICKS;
    public static final ForgeConfigSpec.DoubleValue FOV_ZOOM_STRENGTH;
    public static final ForgeConfigSpec.DoubleValue CARRIED_SIDE_OFFSET;
    public static final ForgeConfigSpec.DoubleValue CARRIED_FORWARD_OFFSET;
    public static final ForgeConfigSpec.DoubleValue CARRIED_VERTICAL_OFFSET;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Kiss cooldown settings (in ticks, 20 ticks = 1 second)",
                        "Cooldown decreases as the maid's favorability level increases")
               .push("cooldown");

        COOLDOWN_LEVEL_0 = builder
                .comment("Cooldown at favorability level 0 (default: 100 = 5 seconds)")
                .defineInRange("level0", 100, 0, 6000);

        COOLDOWN_LEVEL_1 = builder
                .comment("Cooldown at favorability level 1 (default: 60 = 3 seconds)")
                .defineInRange("level1", 60, 0, 6000);

        COOLDOWN_LEVEL_2 = builder
                .comment("Cooldown at favorability level 2 (default: 20 = 1 second)")
                .defineInRange("level2", 20, 0, 6000);

        COOLDOWN_LEVEL_3 = builder
                .comment("Cooldown at favorability level 3 / max (default: 0 = no cooldown)")
                .defineInRange("level3", 0, 0, 6000);

        builder.pop();

        builder.comment("Favorability gain settings")
               .push("favorability");

        FAVORABILITY_POINTS = builder
                .comment("Favorability points gained per kiss (default: 3)")
                .defineInRange("points", 3, 1, 100);

        FAVORABILITY_COOLDOWN = builder
                .comment("Favorability gain cooldown in ticks (default: 600 = 30 seconds)",
                         "This is separate from the interaction cooldown - prevents favorability farming")
                .defineInRange("cooldownTicks", 600, 0, 72000);

        builder.pop();

        builder.comment("Maid's Prayer buff settings",
                        "Triggered by kissing multiple times in a short window")
               .push("buff");

        BUFF_ENABLED = builder
                .comment("Enable the Maid's Prayer buff (default: true)")
                .define("enabled", true);

        BUFF_KISS_THRESHOLD = builder
                .comment("Number of kisses needed to trigger the buff (default: 3)")
                .defineInRange("kissThreshold", 3, 1, 20);

        BUFF_KISS_WINDOW = builder
                .comment("Time window in ticks to reach the kiss threshold (default: 200 = 10 seconds)")
                .defineInRange("kissWindowTicks", 200, 20, 6000);

        BUFF_DURATION = builder
                .comment("Buff duration in ticks (default: 600 = 30 seconds)")
                .defineInRange("durationTicks", 600, 20, 72000);

        builder.comment("Regeneration amplifier per favorability level (0 = I, 1 = II, etc.)",
                        "Higher levels can exceed vanilla limits — the power of love knows no bounds")
               .push("amplifier");

        BUFF_AMPLIFIER_LEVEL_0 = builder
                .comment("Amplifier at favorability level 0 (default: 0 = Regen I)")
                .defineInRange("level0", 0, 0, 255);

        BUFF_AMPLIFIER_LEVEL_1 = builder
                .comment("Amplifier at favorability level 1 (default: 1 = Regen II)")
                .defineInRange("level1", 1, 0, 255);

        BUFF_AMPLIFIER_LEVEL_2 = builder
                .comment("Amplifier at favorability level 2 (default: 2 = Regen III, beyond vanilla)")
                .defineInRange("level2", 2, 0, 255);

        BUFF_AMPLIFIER_LEVEL_3 = builder
                .comment("Amplifier at favorability level 3 (default: 4 = Regen V, far beyond vanilla)")
                .defineInRange("level3", 4, 0, 255);

        builder.pop();
        builder.pop();

        builder.comment("Particle settings")
               .push("particles");

        PARTICLE_COUNT_MIN = builder
                .comment("Minimum number of heart particles per kiss (default: 3)")
                .defineInRange("minCount", 3, 0, 50);

        PARTICLE_COUNT_EXTRA = builder
                .comment("Extra random particles (total = min + random(0..extra)) (default: 4)")
                .defineInRange("extraRandom", 4, 0, 50);

        PARTICLE_OFFSET_Y = builder
                .comment("Vertical offset applied to particle anchor to avoid covering faces (default: 0.12)")
                .defineInRange("offsetY", 0.12, -1.0, 2.5);

        PARTICLE_SPREAD_RADIUS = builder
                .comment("Horizontal spread radius of the particle field (default: 0.18)")
                .defineInRange("spreadRadius", 0.18, 0.0, 2.0);

        PARTICLE_FORWARD_OFFSET = builder
                .comment("Forward offset of the particle anchor along player->maid direction (default: 0.45)")
                .defineInRange("forwardOffset", 0.45, -1.0, 1.0);

        PARTICLE_AVOID_VIEW_STRENGTH = builder
                .comment("How strongly particles avoid the center line of sight (default: 0.0)")
                .defineInRange("avoidViewStrength", 0.0, 0.0, 2.0);

        PARTICLE_SHAPE_MODE = builder
                .comment("Particle spatial mode: RING | HALO | SPIRAL (default: SPIRAL)")
                .define("shapeMode", ParticleShapeMode.SPIRAL.name());

        PARTICLE_UPWARD_SPEED = builder
                .comment("Base upward speed of particles (default: 0.08)")
                .defineInRange("upwardSpeed", 0.08, -0.2, 1.0);

        PARTICLE_RADIAL_SPEED = builder
                .comment("Base radial expansion speed from anchor (default: 0.0)")
                .defineInRange("radialSpeed", 0.0, -0.3, 0.3);

        PARTICLE_SWIRL_SPEED = builder
                .comment("Tangential swirl speed component, useful for spiral feeling (default: 0.0)")
                .defineInRange("swirlSpeed", 0.0, -2.0, 2.0);

        PARTICLE_PHASE_BURSTS = builder
                .comment("How many timed burst phases a kiss emits (default: 5)")
                .defineInRange("phaseBursts", 5, 1, 12);

        PARTICLE_PHASE_INTERVAL_TICKS = builder
                .comment("Interval between burst phases in ticks (default: 2)")
                .defineInRange("phaseIntervalTicks", 2, 1, 20);

        PARTICLE_PHASE_RAMP = builder
                .comment("Burst count ramp mode: EASE_IN | UNIFORM (default: EASE_IN)")
                .define("phaseRamp", ParticlePhaseRamp.EASE_IN.name());

        PARTICLE_CLUSTER_COPIES = builder
                .comment("Extra micro-copies per logical heart to simulate larger size (default: 1)")
                .defineInRange("clusterCopies", 1, 0, 8);

        PARTICLE_CLUSTER_JITTER = builder
                .comment("Jitter radius used for heart cluster size simulation (default: 0.03)")
                .defineInRange("clusterJitter", 0.03, 0.0, 0.3);

        PARTICLE_ACCENT_ENABLED = builder
                .comment("Enable subtle accent particles (DUST/GLOW) to enrich atmosphere (default: true)")
                .define("accentEnabled", true);

        PARTICLE_ACCENT_TYPE = builder
                .comment("Accent particle type: NONE | DUST | GLOW (default: GLOW)")
                .define("accentType", ParticleAccentType.GLOW.name());

        PARTICLE_ACCENT_CHANCE = builder
                .comment("Chance per logical particle to spawn an accent particle (default: 0.7)")
                .defineInRange("accentChance", 0.7, 0.0, 1.0);

        PARTICLE_FAVORABILITY_COLOR_ACCENT = builder
                .comment("When true, accent color can shift with maid favorability (default: true)")
                .define("favorabilityColorAccent", true);

        PARTICLE_ACCENT_COLOR_MODE = builder
                .comment("Accent color mode for DUST accent particles: FAVORABILITY | SOLID | GRADIENT (default: FAVORABILITY)")
                .define("accentColorMode", ParticleAccentColorMode.FAVORABILITY.name());

        builder.comment("Solid accent color RGBA (0..255)").push("accentSolidRgba");
        PARTICLE_ACCENT_SOLID_R = builder.defineInRange("r", 255, 0, 255);
        PARTICLE_ACCENT_SOLID_G = builder.defineInRange("g", 166, 0, 255);
        PARTICLE_ACCENT_SOLID_B = builder.defineInRange("b", 204, 0, 255);
        PARTICLE_ACCENT_SOLID_A = builder.defineInRange("a", 220, 0, 255);
        builder.pop();

        builder.comment("Gradient accent color start RGBA (0..255)").push("accentGradientStartRgba");
        PARTICLE_ACCENT_GRADIENT_START_R = builder.defineInRange("r", 255, 0, 255);
        PARTICLE_ACCENT_GRADIENT_START_G = builder.defineInRange("g", 204, 0, 255);
        PARTICLE_ACCENT_GRADIENT_START_B = builder.defineInRange("b", 224, 0, 255);
        PARTICLE_ACCENT_GRADIENT_START_A = builder.defineInRange("a", 200, 0, 255);
        builder.pop();

        builder.comment("Gradient accent color end RGBA (0..255)").push("accentGradientEndRgba");
        PARTICLE_ACCENT_GRADIENT_END_R = builder.defineInRange("r", 255, 0, 255);
        PARTICLE_ACCENT_GRADIENT_END_G = builder.defineInRange("g", 118, 0, 255);
        PARTICLE_ACCENT_GRADIENT_END_B = builder.defineInRange("b", 170, 0, 255);
        PARTICLE_ACCENT_GRADIENT_END_A = builder.defineInRange("a", 230, 0, 255);
        builder.pop();

        builder.pop();

        builder.comment("FOV zoom effect on kiss",
                        "Creates a smooth 'lean-in' feeling by narrowing the FOV")
               .push("fov");

        FOV_ZOOM_ENABLED = builder
                .comment("Enable FOV zoom on kiss (default: true)")
                .define("enabled", true);

        FOV_ZOOM_IN_TICKS = builder
                .comment("Zoom-in duration in ticks (default: 4 = 0.2s)")
                .defineInRange("zoomInTicks", 4, 1, 40);

        FOV_HOLD_TICKS = builder
                .comment("Hold at max zoom duration in ticks (default: 3 = 0.15s)")
                .defineInRange("holdTicks", 3, 0, 40);

        FOV_ZOOM_OUT_TICKS = builder
                .comment("Zoom-out duration in ticks (default: 6 = 0.3s)")
                .defineInRange("zoomOutTicks", 6, 1, 60);

        FOV_ZOOM_STRENGTH = builder
                .comment("Zoom strength (0.0 = no zoom, 1.0 = full zoom to 0 FOV) (default: 0.85)")
                .defineInRange("strength", 0.85, 0.0, 0.95);

        CARRIED_SIDE_OFFSET = builder
                .comment("Carried kiss camera side offset (left/right) for maid head targeting (default: 0.48)")
                .defineInRange("carriedSideOffset", 0.48, -2.0, 2.0);

        CARRIED_FORWARD_OFFSET = builder
                .comment("Carried kiss camera forward/back offset for maid head targeting (default: 0.16)")
                .defineInRange("carriedForwardOffset", 0.16, -2.0, 2.0);

        CARRIED_VERTICAL_OFFSET = builder
                .comment("Carried kiss camera vertical offset for maid head targeting (default: -0.10)")
                .defineInRange("carriedVerticalOffset", -0.10, -2.0, 2.0);

        builder.pop();

        SPEC = builder.build();
    }

    public static ParticleShapeMode getParticleShapeMode() {
        return parseEnum(PARTICLE_SHAPE_MODE.get(), ParticleShapeMode.SPIRAL);
    }

    public static ParticlePhaseRamp getParticlePhaseRamp() {
        return parseEnum(PARTICLE_PHASE_RAMP.get(), ParticlePhaseRamp.EASE_IN);
    }

    public static ParticleAccentType getParticleAccentType() {
        return parseEnum(PARTICLE_ACCENT_TYPE.get(), ParticleAccentType.GLOW);
    }

    public static ParticleAccentColorMode getParticleAccentColorMode() {
        return parseEnum(PARTICLE_ACCENT_COLOR_MODE.get(), ParticleAccentColorMode.FAVORABILITY);
    }

    private static <E extends Enum<E>> E parseEnum(String raw, E fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            @SuppressWarnings("unchecked")
            Class<E> enumClass = (Class<E>) fallback.getDeclaringClass();
            return Enum.valueOf(enumClass, raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    public enum ParticleShapeMode {
        RING,
        HALO,
        SPIRAL
    }

    public enum ParticlePhaseRamp {
        EASE_IN,
        UNIFORM
    }

    public enum ParticleAccentType {
        NONE,
        DUST,
        GLOW
    }

    public enum ParticleAccentColorMode {
        FAVORABILITY,
        SOLID,
        GRADIENT
    }
}
