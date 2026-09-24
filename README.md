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

`1.7.3.0` incorporates high-value player feedback and hardens interaction defaults:

- **Controllable gifts**: Random Gift now uses the curated item-tag pool by default. Legacy broad-pool mode excludes only immersion-breaking technical/admin items, and explicit tag entries can override that default policy.
- **Dedicated kiss keys**: The conflicting sneak-right-click entry is removed; targeted and carried-maid kiss keys remain available.
- **Bilingual AI Morning Kiss**: Display dialogue and generated TTS speech can use different languages, such as Chinese text with Japanese audio.
- **Modded health support**: Emergency Rescue can optionally trigger from a percentage of maximum health.
- **Clearer unlocking**: The bond UI explains that P Point items must be present in the player inventory and shows blocked-click feedback.

Full release history lives in [CHANGELOG.md](CHANGELOG.md).

## Features

### Kiss Interaction

Bind the targeted kiss action in Controls and aim at an owned maid to kiss her. Princess-carried maids also have a dedicated kiss action. Kisses grant favorability, play random kiss sounds, spawn heart particles, and use a short close-up camera effect. Repeated kisses can trigger the custom Maid's Prayer effect. Sneak-right-click kissing has been removed so TLM's sit/stand interaction remains untouched.

Sound volumes can be tuned in `config/touhou_maid_affection-common.toml`: `cooldown.kissSoundVolume` controls kiss sound events, `morningKissBehavior.voiceVolume` controls Morning Kiss voices, `emergencyRescueBehavior.volume` controls Emergency Rescue voices and fallback sounds, and `voicePreview.volume` controls bond-page voice previews. All four range from `0.0` to `1.0` and only attenuate — `0.0` mutes and `1.0` keeps the original loudness; use Minecraft or system volume for louder playback.

When CarryOn is installed, a dedicated keybind supports kissing a princess-carried maid.

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

Since `1.7.5.0` the bond page has a **Settings** button in the top-left corner: common settings can now be changed in game — feature switches (Morning Kiss, proactive Morning Kiss, AI dialogue, AI voice, Emergency Rescue, Random Gift, Maid's Prayer buff), the Morning Kiss display and voice languages, and four volume sliders. Switches and languages are server-authoritative, so on a dedicated server only operators may change them (everyone else is read-only); the volumes are client-side and apply as soon as you drag them.

### Custom Dialogue And Voices

Version `1.7.2+` adds datapack-driven voice pools:

```text
data/touhou_maid_affection/morning_kiss/profile.json
data/touhou_maid_affection/morning_kiss/voices/*.ogg
data/touhou_maid_affection/emergency_rescue/profile.json
data/touhou_maid_affection/emergency_rescue/voices/*.ogg
```

Morning Kiss datapacks can define static dialogue pools, kiss sound behavior, and OGG voice files. Emergency Rescue datapacks define rescue OGG voice files and a fallback sound event. See [早安吻相关配置说明.md](早安吻相关配置说明.md) and the ready-to-zip sample pack in [examples/TMA-Custom-Voice-Pack](examples/TMA-Custom-Voice-Pack).

### AI Hub

Morning Kiss can optionally use TLM AI sites to pre-generate dialogue and TTS audio. `aiDialogueLanguage` controls display text while `aiDialogueVoiceLanguage` controls spoken TTS text. They are fully independent and accept arbitrary locale codes rather than a hard-coded Chinese/Japanese pair. When they differ, TMA translates the generated lines in one ordered batch before requesting TTS, supporting combinations such as Chinese/Japanese, English/Korean, French/German, or others. These settings live in `config/touhou_maid_affection-common.toml`. Changing the language no longer requires a manual cache clear: cache reads and target checks filter by the current languages, so the cache re-warms for the new language automatically (old-language entries are kept and can be cleared with `/tma morning_kiss clear_ai_cache`); only prompt changes need that command.

TMA does not register its own chat/TTS providers: every LLM and TTS request (including Morning Kiss) goes through Touhou Little Maid's own AI sites, so user API keys, models, voices, and enabled state stay managed by Touhou Little Maid. AI-related configuration will move into TMA's own settings panel in a later release.

### Compatibility

- Touhou Little Maid: required dependency.
- Yes Steve Model: optional action playback and action discovery.
- CarryOn: optional princess-carry kiss integration.
- TLM GUI and sound packs: soft integration where available, silent fallback where absent.
- MaidFileManager (Maid File Manager): optional soft dependency; migrates per-maid bond data through its `.maid` migration SPI, silent no-op when absent.

## Installation

1. Install Minecraft `1.21.1` with NeoForge `21.1.x`.
2. Install Touhou Little Maid `1.5.1+`.
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
