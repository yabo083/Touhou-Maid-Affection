<p align="center">
  <img src="image/README/1773209564540.png" alt="Kiss your maid!" width="600"/>
</p>

<h1 align="center">Touhou Maid: Affection</h1>

<p align="center">
  <b>An affection and bond expansion for Touhou Little Maid on Forge 1.20.1.</b>
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

## Latest Release

`1.7.2.2-forge1.20.1` is a short-lived Forge branch release based on the 1.7.2 feature line. It focuses on custom voice packs, AI-assisted Morning Kiss, safer voice preview UX, and a Morning Kiss AI/TTS language cache fix:

- Per-feature datapack voice pools for Morning Kiss and Emergency Rescue.
- Static Morning Kiss dialogue packs with `{maid}` and `{player}` placeholders.
- Optional AI-generated Morning Kiss dialogue and TTS pre-generation through TLM AI sites.
- TMA MiMo adapter sites for TLM AI chat and TTS.
- Targeted-maid kiss key action for crosshair-based kissing.
- Voice preview in the Morning Kiss and Emergency Rescue voice-pool pages.
- `aiDialogueLanguage` now affects Morning Kiss AI dialogue pregeneration and generated TTS requests.
- After changing language or prompts, admins can run `/tma morning_kiss clear_ai_cache` to clear generated Morning Kiss AI voice cache for the current server session.
- Ready-to-zip sample datapack in `examples/TMA-Custom-Voice-Pack`.

Full release history lives in [CHANGELOG.md](CHANGELOG.md).

## Features

### Kiss Interaction

Sneak with an empty hand and right-click your maid to kiss her. Kisses grant favorability, play random kiss sounds, spawn heart particles, and use a short close-up camera effect. Repeated kisses can trigger the custom Maid's Prayer effect.

When CarryOn is installed, the right-click condition changes to avoid interaction conflicts. A dedicated keybind also supports kissing a princess-carried maid.

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

Datapack-driven voice pools use this layout:

```text
data/touhou_maid_affection/morning_kiss/profile.json
data/touhou_maid_affection/morning_kiss/voices/*.ogg
data/touhou_maid_affection/emergency_rescue/profile.json
data/touhou_maid_affection/emergency_rescue/voices/*.ogg
```

Morning Kiss datapacks can define static dialogue pools, kiss sound behavior, and OGG voice files. Emergency Rescue datapacks define rescue OGG voice files and a fallback sound event. The voice-pool pages can preview selected voices before saving.

See [早安吻文本修改教程.md](早安吻文本修改教程.md) and the sample pack in [examples/TMA-Custom-Voice-Pack](examples/TMA-Custom-Voice-Pack).

### AI And MiMo

Morning Kiss can optionally use TLM AI sites to pre-generate dialogue and TTS audio. Runtime behavior, prompts, and the `aiDialogueLanguage` language setting are configured in `config/touhou_maid_affection-common.toml`, while datapacks stay responsible for static text and pre-recorded OGG files. After changing language or prompt settings, admins can run `/tma morning_kiss clear_ai_cache` to clear generated cache for the current server session so later scans regenerate it.

TMA also registers MiMo-compatible chat and TTS site types for TLM's AI settings UI. The adapter supplies provider defaults only; user API keys and enabled site state remain managed by Touhou Little Maid.

### Compatibility

- Touhou Little Maid: required dependency, built against `1.5.2-forge+mc1.20.1`.
- Yes Steve Model: optional action playback and action discovery.
- CarryOn: optional right-click conflict avoidance.
- TLM GUI, AI sites, and sound packs: soft integration where available, silent fallback where absent.

## Installation

1. Install Minecraft `1.20.1` with Forge `47.4.x`.
2. Install Touhou Little Maid for Forge 1.20.1.
3. Put `touhou-maid-affection-1.7.2.2.jar` into your `mods` folder.
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
