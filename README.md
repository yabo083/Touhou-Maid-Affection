<p align="center">
  <img src="image/README/1773209564540.png" alt="Kiss your maid!" width="600"/>
</p>

<h1 align="center">Touhou Maid: Affection</h1>

<p align="center">
  <b>An affection and bond expansion for Touhou Little Maid.</b>
</p>

<p align="center">
  <a href="README_zh.md">中文</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.1-green?style=flat-square" alt="MC 1.21.1"/>
  <img src="https://img.shields.io/badge/NeoForge-21.1.x-orange?style=flat-square" alt="NeoForge"/>
  <img src="https://img.shields.io/badge/Requires-Touhou_Little_Maid_1.5.1+-blue?style=flat-square" alt="TLM"/>
  <img src="https://img.shields.io/badge/License-MIT-yellow?style=flat-square" alt="MIT"/>
</p>

---

## Latest Release

`1.7.3` reworks the kiss interaction:

- **K-key kisses**: Kissing is now triggered with the **K key** — one press kisses once, and holding the key does not repeat it. The old sneak + right-click trigger has been removed.
- **Hold-to-kiss camera**: Press K to glide the camera to your maid's face; keep holding to stay there; release K to move the camera back.
- **Crosshair-accurate aim**: When the maid renders low (sitting pose or model animation), the kiss camera aims where your crosshair points, so sitting kisses land on her face.
- **Configurable kiss range**: `fov.targetMaxDistance` (default 3 blocks, previously a fixed 6).
- **Max-favorability cooldown**: the default kiss cooldown at favorability level 3 is now 0.5 seconds.
- **Bond UI hint**: the P-point-insufficient tooltip now explains that item-form P-points are required to unlock.

Full release history lives in [CHANGELOG.md](CHANGELOG.md).

## Features

### Kiss Interaction

Point your crosshair at a maid and press the **K key** to kiss her — one press kisses once, and holding the key does not repeat it. Kisses grant favorability, play random kiss sounds, spawn heart particles, and use a close-up camera effect that stays at her face while you hold K and returns when you release it. Repeated kisses can trigger the custom Maid's Prayer effect. Pressing K while a maid is princess-carried (saddle) also kisses her.

### Targeted Kiss Key

Controls include a targeted kiss action. When your crosshair points at an owned maid within range, the client sends only the entity id; the server re-checks ownership, distance, line of sight, cooldown, and normal kiss rules before applying the interaction.

### Bond System

High-affection maids can become bonded companions. Bond abilities currently include:

| Ability | Purpose |
|---|---|
| Lap Pillow | Rest with your maid using configurable sit/lie poses and optional YSM actions. |
| Morning Kiss | Schedule or manually call a morning greeting with kisses, dialogue, and voice playback. |
| Emergency Rescue | Let bonded maids contribute daily rescue chances and rescue voice lines. |
| Random Gift | Let bonded maids accumulate and deliver small gifts over time. |

The server remains authoritative for unlocks, costs, distance checks, cooldowns, and ability execution. The client UI is a display and configuration surface.

### Custom Dialogue And Voices

Version `1.7.2+` adds datapack-driven voice pools:

```text
data/touhou_maid_affection/morning_kiss/profile.json
data/touhou_maid_affection/morning_kiss/voices/*.ogg
data/touhou_maid_affection/emergency_rescue/profile.json
data/touhou_maid_affection/emergency_rescue/voices/*.ogg
```

Morning Kiss datapacks can define static dialogue pools, kiss sound behavior, and OGG voice files. Emergency Rescue datapacks define rescue OGG voice files and a fallback sound event. See [早安吻文本修改教程.md](早安吻文本修改教程.md) and the ready-to-zip sample pack in [examples/TMA-Custom-Voice-Pack](examples/TMA-Custom-Voice-Pack).

### AI Hub

Morning Kiss can optionally use TLM AI sites to pre-generate dialogue and TTS audio. Runtime behavior, prompts, and the `aiDialogueLanguage` language setting are configured in `config/touhou_maid_affection-common.toml`, while datapacks stay responsible for static text and pre-recorded OGG files. After changing language or prompt settings, admins can run `/tma morning_kiss clear_ai_cache` to clear generated cache for the current server session so later scans regenerate it.

TMA also registers AI Hub chat and TTS site presets for TLM's AI settings UI. The current provider implementation is MiMo-compatible, but the in-game entry is named around TMA AI behavior so future chat, TTS, and STT-facing features can share the same doorway. User API keys and enabled site state remain managed by Touhou Little Maid.

### Compatibility

- Touhou Little Maid: required dependency.
- Yes Steve Model: optional action playback and action discovery.
- TLM GUI and sound packs: soft integration where available, silent fallback where absent.

## Installation

1. Install Minecraft `1.21.1` with NeoForge `21.1.x`.
2. Install Touhou Little Maid `1.5.1+`.
3. Put `touhou-maid-affection-1.7.3.jar` into your `mods` folder.
4. Launch the game.

## Build From Source

```bash
git clone https://github.com/yabo083/Touhou-Maid-Affection.git
cd Touhou-Maid-Affection
./gradlew build
```

Output jar:

```text
build/libs/touhou-maid-affection-<version>.jar
```

## Maintenance Docs

- [PROJECT_ARCHITECTURE.md](PROJECT_ARCHITECTURE.md): architecture boundaries and module responsibilities.
- [CHANGELOG.md](CHANGELOG.md): user-facing release history.
- [早安吻文本修改教程.md](早安吻文本修改教程.md): datapack text, voice, and AI setup guide.
- [TESTING.md](TESTING.md): test scope and regression commands.
- [DEPLOYMENT.md](DEPLOYMENT.md): release constraints and pre-release checklist.

## License

[MIT License](LICENSE)
