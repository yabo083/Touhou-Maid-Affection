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

## 🆕 v1.7.1 - 心有灵犀 (Soul Resonance)

- Added four Bond System abilities: Lap Pillow, Morning Kiss, Emergency Rescue, and Random Gift.
- These abilities become available when the maid reaches favorability level 3; each one consumes inventory Power Points only on first unlock (not on every use).

### 🛏️ Lap Pillow

- Press `B` to start/exit after unlocking, with a visual configuration UI.
- Supports drag-and-scroll relative position tuning plus saved default sit/lie combinations for maid and player.
- Maid-side YSM actions are supported when model actions are detected; player-side remains built-in sit/lie, and players can still use native YSM keys (such as `Z`) manually.
- Press `V` to toggle angle lock for second/third-person photo composition.
- While active, both player and maid receive the Eternal Utopia buff.

### 🌅 Morning Kiss

- Requires level-3 favorability and ability unlock; supports both manual call and automatic triggering in configured time windows.
- Voice settings are linked with the maid's current sound pack, with random-all/random-group/specific-clip modes.
- Time ranges use 24-hour format, for example `06:00-08:00` and `18:00-20:00` (see config examples).

### 🚨 Emergency Rescue

- Requires level-3 favorability and ability unlock, then auto-triggers when the player's HP is in danger.
- Rescue action and voice behavior are configurable, including TLM voice and custom OGG files.
- Supports server-side predefined voice sync to clients, with optional fallback to the `common` pool.
- Player-facing commands: `/tma rescue`, `/tma rescue on`, `/tma rescue off`, `/tma rescue toggle`.
- Admin commands: `/tma rescue clear|reset`, `/tma rescue sound sync`.

### 🎁 Random Gift

- Requires level-3 favorability and ability unlock; gifts accumulate on real-time intervals and queue while away.
- Default pool covers most container-valid vanilla items, with optional sampled mod items.
- Datapack tags can extend/override and blacklist gift entries:
  - `data/touhou_maid_affection/tags/items/bond_random_gift_pool.json`
  - `data/touhou_maid_affection/tags/items/bond_random_gift_blacklist.json`
- Modpack authors can freely tune what maids are allowed to gift.

- Thank you for playing.

## 🆕 1.6.1 Fixes

- Fixed an issue where kissing could be incorrectly blocked after switching to another save without restarting the game.
- Kiss cooldown state is now isolated per world/server session, so saves no longer affect each other.
- Kiss cooldown is now tracked per maid, so one maid's cooldown no longer blocks kissing other maids.

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
