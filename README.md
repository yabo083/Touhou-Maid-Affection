<p align="center">
  <img src="image/README/1773209564540.png" alt="Kiss your maid!" width="600"/>
</p>

<h1 align="center">💋 Touhou Maid: Affection</h1>

<p align="center">
  <b>Kiss your Touhou Little Maid. Because she deserves it.</b>
</p>

<p align="center">
  <a href="README_zh.md">🌏 中文</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.1-green?style=flat-square" alt="MC 1.21.1"/>
  <img src="https://img.shields.io/badge/NeoForge-21.1.x-orange?style=flat-square" alt="NeoForge"/>
  <img src="https://img.shields.io/badge/Requires-Touhou_Little_Maid_1.5.0+-blue?style=flat-square" alt="TLM"/>
  <img src="https://img.shields.io/badge/License-MIT-yellow?style=flat-square" alt="MIT"/>
</p>

---

## 📒 Changelog

- Full release history now lives in [CHANGELOG.md](CHANGELOG.md).
- This README keeps usage docs and only brief highlights.

## ✨ Features

### 💋 Kiss System

| Feature | Description |
|---|---|
| 💋 **Kiss Interaction** | Sneak + empty hand + right-click your maid to kiss her |
| 💕 **Heart Particles** | Romantic heart particles spawn between you and your maid |
| 🔊 **Kiss Sound Effects** | Plays crisp kissing sounds (7 random variants) |
| 📈 **Favorability Boost** | Each kiss grants **+3 favorability** (30s cooldown) |
| 👀 **Maid Gaze** | Your maid turns to look at you during the kiss |
| ⏱️ **Tiered Cooldown** | Cooldown decreases as maid favorability rises: 5s → 3s → 1s → 0s |
| 🎥 **Zero-Distance Camera** | Camera smoothly zooms into the maid's face on kiss — true face-to-face close-up |
| 💞 **Romantic Particle System v2** | Hearts now emit in timed bursts with configurable shapes (RING/HALO/SPIRAL), reducing face occlusion while keeping a dreamy look |

### 🤲 Princess-Carry Kiss Key

When you princess-carry a maid with a saddle (maid riding player), right-click can be awkward.  
This mod adds a dedicated keybind in Controls:

```
Touhou Maid: Affection -> Kiss Carried Maid (default: V)
```

- Works only while carrying a maid as passenger
- Keeps original right-click kiss behavior unchanged for normal state
- Uses a dedicated carried-kiss camera target for better head-to-head framing

### 🙏 Maid's Prayer (少女祈祷)

Kiss **3 times within 10 seconds** to trigger the **Maid's Prayer** buff on both you and your maid.

- Custom MobEffect with built-in regeneration (not vanilla Regeneration)
- Regen strength **scales with favorability level**:
  - Level 0: Regen I
  - Level 1: Regen II
  - Level 2: Regen III *(beyond vanilla!)*
  - Level 3: Regen V *(the power of love!)*
- Duration: 30 seconds (configurable)

### 🧲 Offhand Maid Attraction

In vanilla TouhouLittleMaid, only **main hand** cake attracts maids. This mod extends that behavior via Mixin:

```
Offhand holding temptation item (default: cake) → Maid is also attracted
```

### 📦 CarryOn Compatibility

Auto-detects CarryOn mod. When installed, kiss trigger changes to avoid conflict:

```
Sneak + Main Hand Empty + Offhand Holding Any Item + Right-click maid
```

> 💡 **Tip**: Hold cake in offhand to attract your maid, then sneak + right-click to kiss — seamless!

### ⚙️ Fully Configurable

All values are tunable in `config/touhou_maid_affection-common.toml`:
- Kiss cooldown per favorability level
- Kiss camera timing: adjust only `fov.zoomInTicks`, `fov.holdTicks`, `fov.zoomOutTicks`
- Favorability points and cooldown
- Maid's Prayer thresholds, duration, regen amplifiers
- FOV zoom strength and timing
- Princess-carry camera offsets:
  - `carriedSideOffset` (default `0.48`)
  - `carriedForwardOffset` (default `0.16`)
  - `carriedVerticalOffset` (default `-0.10`)
- Advanced particle controls:
  - anti-occlusion anchor tuning: `offsetY`, `forwardOffset`, `avoidViewStrength`
  - geometry + motion: `shapeMode`, `spreadRadius`, `upwardSpeed`, `radialSpeed`, `swirlSpeed`
  - timing: `phaseBursts`, `phaseIntervalTicks`, `phaseRamp`
  - size simulation: `clusterCopies`, `clusterJitter`
  - romantic accents: `accentEnabled`, `accentType`, `accentChance`, `favorabilityColorAccent`
- Particle counts

## 📥 Installation

1. Install **Minecraft 1.21.1** + **NeoForge 21.1.x**
2. Install **[Touhou Little Maid](https://modrinth.com/mod/touhou-little-maid)** 1.5.0+
3. Drop `touhou-maid-affection-x.x.x.jar` into your `.minecraft/mods/` folder
4. Launch the game!

## 🛠️ Build from Source

```bash
git clone https://github.com/yabo083/maid-affection.git
cd maid-affection
./gradlew build
```

Output jar at `build/libs/touhou-maid-affection-x.x.x.jar`.

## 📚 Maintenance Docs

- [PROJECT_ARCHITECTURE.md](PROJECT_ARCHITECTURE.md): Core architecture boundaries and module responsibilities
- [CHANGELOG.md](CHANGELOG.md): User-facing release history (Added/Changed/Fixed/Removed)
- [TESTING.md](TESTING.md): Testing scope, conventions, and regression commands
- [DEPLOYMENT.md](DEPLOYMENT.md): Build/release constraints and pre-release checklist

## 📋 Technical Details

- **Mod ID**: `touhou_maid_affection`
- **API**: Uses TouhouLittleMaid's `InteractMaidEvent` event API
- **Networking**: `KissMaidPayload` (Server → Client) for effects + `KissCarryRequestPayload` (Client → Server) for princess-carry key trigger
- **Compatibility**: Soft-detects CarryOn via `ModList.isLoaded()`, zero hard dependencies
- **Favorability**: Uses TLM's built-in `FavorabilityManager` + custom `Type("Kiss", 3, 600)`
- **Client Effects**: FOV zoom via `ComputeFovModifierEvent` + camera angles via `ViewportEvent.ComputeCameraAngles`

## 📄 License

[MIT License](LICENSE) — Free to use, modify, and distribute.

---

<p align="center">
  <i>Made with ❤️ for the Touhou Little Maid community</i>
</p>
