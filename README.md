<p align="center">
  <img src="image/README/1773209564540.png" alt="Kiss your maid!" width="600"/>
</p>

<h1 align="center">Touhou Maid: Affection</h1>

<p align="center">
  <b>Bring affectionate interactions, bond progression, and lasting companionship to Touhou Little Maid.</b>
</p>

<p align="center">
  <a href="README_zh.md">中文</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.20.1-green?style=flat-square" alt="MC 1.20.1"/>
  <img src="https://img.shields.io/badge/Forge-47.4.x-orange?style=flat-square" alt="Forge"/>
  <img src="https://img.shields.io/badge/Java-17-blue?style=flat-square" alt="Java 17"/>
  <img src="https://img.shields.io/badge/Touhou_Little_Maid-1.5.x-informational?style=flat-square" alt="Touhou Little Maid"/>
  <img src="https://img.shields.io/badge/License-MIT-yellow?style=flat-square" alt="MIT"/>
</p>

---

## Overview

**Touhou Maid: Affection** is an affection and bond companion expansion for **Touhou Little Maid (TLM)**.

Express your love through affectionate interactions, raise favorability by kissing, and unlock an exclusive Bond System as your relationship deepens. Bond abilities include relaxing Lap Pillows, personalized Morning Kisses, Emergency Rescues in fatal situations, and thoughtful Random Gifts. The mod also integrates with TLM's native AI chat and TTS speech system, alongside full datapack support for custom dialogue and voice lines.

## Core Mechanics

### Kiss Interaction
- **Keybind Actions**: Aim your crosshair at your maid and press the kiss key. When carrying your maid using a saddle, use the dedicated carried kiss key to interact directly.
- **Visual & Audio Feedback**: Each kiss brings heart particles, playful sound effects, and a brief close-up camera zoom.
- **Favorability Progression**: Kissing steadily increases maid favorability. Higher favorability levels grant shorter kiss cooldowns.
- **Maid's Prayer**: Rapid consecutive kisses reward both player and maid with the "Maid's Prayer" regeneration buff.

### Bond System
When a maid reaches Favorability Level 3 (Max), the **Bond Tab** unlocks at the top of her inventory GUI.
Players can spend P-Points (PowerPoint items in inventory) to unlock unique bond abilities:

| Bond Ability | Description |
| :--- | :--- |
| **Lap Pillow** | Rest together sitting or lying down. Supports adjustable offsets, camera lock, and YSM custom animations. |
| **Morning Kiss** | Scheduled morning greeting upon waking. The maid approaches with a morning kiss, custom lines, and voice playback. |
| **Emergency Rescue** | When you suffer fatal damage, bonded maids rush in to shield you and restore emergency health with unique rescue lines. |
| **Random Gift** | Bonded maids gather and present thoughtful gifts over time, with progress tracked directly in the GUI. |

### AI Morning Kiss & Bilingual Voice System
Morning Kiss can leverage Touhou Little Maid's configured AI chat and TTS providers to pre-generate morning greetings in the background.
- **Bilingual Combinations**: Display text and spoken voice languages are completely decoupled, supporting combinations like English text with Japanese voice.
- **Auto Matching**: Defaults to `auto`, where text matches your Minecraft language and voice inherits the maid's TLM AI voice settings.
- **Unified Management**: Directly uses your existing TLM AI providers and API keys without redundant setup.

## How to Use

### Keybindings
Configure the following keys under **Options → Controls → Key Binds**:
- **Targeted Kiss**: Kiss the maid in your crosshair.
- **Carried Maid Kiss**: Kiss while carrying a maid with a saddle.
- **Lap Pillow**: Trigger the lap pillow rest pose.
- **Lock View Angle**: Lock the camera angle during a lap pillow session.

### In-Game Settings Panel
Click the **Settings** gear icon in the top-right corner of the Bond GUI to open the in-game control panel:
- **Status**: Monitor AI dialogue cache progress, active toggles, and per-maid statistics.
- **Features**: Toggle individual bond mechanics and adjust AI cache generation limits and scan intervals.
- **Voice**: Switch display/voice languages, edit the Morning Kiss prompt template live, or jump directly to TLM's AI provider settings.
- **Volume**: Adjust volume sliders in real-time for kiss sounds, morning kiss voices, rescue lines, and voice previews.

### Admin Commands
All data and settings follow server-authoritative validation. Operators (Permission Level 2) can manage features via `/tma`:
```
/tma morning_kiss status                       # View morning kiss status and AI cache stats
/tma morning_kiss clear_ai_cache [all|maid...]  # Clear generated dialogue and voice cache
/tma rescue on|off|toggle                      # Enable or disable emergency rescue server-wide
/tma bond prune [days]                         # Clean up bond data for maids inactive over N days (default: 90)
```

### Custom Datapack Voices
Expand dialogue pools and `.ogg` voice lines via Datapacks:
- Datapack paths: `data/touhou_maid_affection/morning_kiss/` and `emergency_rescue/`
- Ready-to-use sample pack: [examples/TMA-Custom-Voice-Pack](examples/TMA-Custom-Voice-Pack)
- Detailed tutorial: [早安吻相关配置说明.md](早安吻相关配置说明.md)

## Important Notes

- **Server Authority**: Favorability growth, cooldowns, line-of-sight checks, and ability activation are fully validated by the server. Singleplayer and multiplayer servers share identical behavior.
- **Volume Attenuation**: In-game volume sliders control client-side attenuation (`0.0` for mute, `1.0` for full volume). Increase master/system volume for higher output.
- **Permissions**: On dedicated servers, global toggles, language rules, and prompt templates are operator-only. Non-op players view the settings panel in read-only mode.
- **Data Persistence**: Maid bond data is safely preserved across maid deaths, chunk unloads, or dimensional transfers. Use `/tma bond prune` to purge abandoned data.

## Miscellaneous & Compatibility

### Mod Integrations
- **Touhou Little Maid (TLM)**: Required core dependency (built against `1.5.3-forge+mc1.20.1`).
- **MaidFileManager**: Seamlessly carries bond levels, unlocked abilities, and voice settings across `.maid` file migrations.
- **Epic Fight**: Compatible with `[史诗战斗：车万女仆]` bridge & `Epic Fight - Avalon`; bond tab automatically avoids GUI tab collisions.
- **Yes Steve Model (YSM)**: Supports custom animation sync during lap pillows.
- **Tweakerge / Tweakeroo**: Fully compatible with Free Camera mode without causing player pose conflicts.

### Installation
1. Install Minecraft `1.20.1` and Forge `47.4.x`.
2. Install **Touhou Little Maid** for Forge 1.20.1 (`1.5.0+`).
3. Place `touhou-maid-affection-1.7.5.1.jar` into your `.minecraft/mods` directory.
4. Launch the game.

### Developer & Architecture Docs
- [PROJECT_ARCHITECTURE.md](PROJECT_ARCHITECTURE.md): Architecture overview and module boundaries.
- [CHANGELOG.md](CHANGELOG.md): Complete version history.
- [早安吻相关配置说明.md](早安吻相关配置说明.md): Datapack dialogue, voice, and AI guide.
- [TESTING.md](TESTING.md): Testing suite and regression workflows.

### License
This project is open-source under the [MIT License](LICENSE).
