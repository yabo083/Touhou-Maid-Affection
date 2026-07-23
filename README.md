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

`1.7.3.0` incorporates high-value player feedback and hardens interaction defaults:

- **Safer gifts**: Random Gift now uses the curated item-tag pool by default and excludes bedrock and spawn eggs in legacy broad-pool mode.
- **Interaction control**: `rightClickKissEnabled` can disable sneak-right-click kissing without disabling targeted or carried-maid kiss keys.
- **Modded health support**: Emergency Rescue can optionally trigger from a percentage of maximum health.
- **Clearer unlocking**: The bond UI explains that P Point items must be present in the player inventory and shows blocked-click feedback.

Full release history lives in [CHANGELOG.md](CHANGELOG.md).

## Features

### Kiss Interaction

Sneak with an empty hand and right-click your maid to kiss her. Kisses grant favorability, play random kiss sounds, spawn heart particles, and use a short close-up camera effect. Repeated kisses can trigger the custom Maid's Prayer effect.

If this conflicts with the maid sit/stand interaction, set `cooldown.rightClickKissEnabled=false` and use the configurable targeted/carried-maid kiss keys instead.

Sound volumes can be tuned in `config/touhou_maid_affection-common.toml`: `cooldown.kissSoundVolume` controls kiss sound events, `morningKissBehavior.voiceVolume` controls Morning Kiss voices, `emergencyRescueBehavior.volume` controls Emergency Rescue voices and fallback sounds, and `voicePreview.volume` controls bond-page voice previews.

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

For modpacks with increased maximum health, enable `emergencyRescueBehavior.usePercentageThreshold=true`; the default 20% value equals the legacy 4-point threshold at vanilla health. Percentage mode remains off by default to preserve existing server configuration behavior.

Random Gift uses the curated `touhou_maid_affection:bond_random_gift_pool` item tag by default. Datapacks can extend that tag or add exclusions through `touhou_maid_affection:bond_random_gift_blacklist`; set `bondCosts.randomGiftBehavior.curatedPoolOnly=false` only if you want the legacy broad registry sampling mode.

The server remains authoritative for unlocks, costs, distance checks, cooldowns, and ability execution. The client UI is a display and configuration surface.

### Custom Dialogue And Voices

Datapack-driven voice pools use this layout:

```text
data/touhou_maid_affection/morning_kiss/profile.json
data/touhou_maid_affection/morning_kiss/voices/*.ogg
data/touhou_maid_affection/emergency_rescue/profile.json
data/touhou_maid_affection/emergency_rescue/voices/*.ogg
```

Morning Kiss datapacks can define static dialogue pools, kiss sound behavior, and OGG voice files. Emergency Rescue datapacks define rescue OGG voice files and a fallback sound event. The voice-pool pages can preview selected voices before saving. See [早安吻相关配置说明.md](早安吻相关配置说明.md) and the ready-to-zip sample pack in [examples/TMA-Custom-Voice-Pack](examples/TMA-Custom-Voice-Pack).

### AI And MiMo

Morning Kiss can optionally use TLM AI sites to pre-generate dialogue and TTS audio. Runtime behavior, prompts, and the `aiDialogueLanguage` language setting are configured in `config/touhou_maid_affection-common.toml`; `tlm`, `auto`, or `default` follows Touhou Little Maid's own language settings. Text-only generation follows the maid chat language, while generated voice cache text follows TLM's TTS language button so the text sent to TTS matches the requested voice language. Explicit values such as `en_us` or `ja_jp` override both. Datapacks stay responsible for static text and pre-recorded OGG files. `/tma morning_kiss` reports the current AI/TTS settings, `/tma morning_kiss cache` reports generated cache details per maid, pool, text language, voice language, and in-flight request, and admins can use `ai on/off`, `tts on/off`, or `clear_ai_cache` while testing language or prompt settings. Generated cache capacity is enforced per maid and per time pool; with the default target of 4 and three pools, one maid keeps up to 12 generated entries total even if several languages appear in the cache details. By default, generated cache entries are reused without being consumed; set `aiDialogueCacheConsumeOnUse=true` if you prefer the old auto-refill behavior.

TMA also registers MiMo-compatible chat and TTS site types for TLM's AI settings UI. The adapter supplies provider defaults only; user API keys and enabled site state remain managed by Touhou Little Maid.

### Compatibility

- Touhou Little Maid: required dependency, built against `1.5.2-forge+mc1.20.1`.
- Yes Steve Model: optional action playback and action discovery.
- CarryOn: optional right-click conflict avoidance.
- TLM GUI, AI sites, and sound packs: soft integration where available, silent fallback where absent.

## Installation

1. Install Minecraft `1.20.1` with Forge `47.4.x`.
2. Install Touhou Little Maid for Forge 1.20.1.
3. Put `touhou-maid-affection-1.7.3.0.jar` into your `mods` folder.
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
- [早安吻相关配置说明.md](早安吻相关配置说明.md): datapack text, voice, and AI setup guide.
- [TESTING.md](TESTING.md): test scope and regression commands.
- [DEPLOYMENT.md](DEPLOYMENT.md): release constraints and pre-release checklist.

## License

[MIT License](LICENSE)
