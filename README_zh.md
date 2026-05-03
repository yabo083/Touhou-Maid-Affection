<p align="center">
  <img src="image/README/1773209564540.png" alt="亲亲你的女仆！" width="600"/>
</p>

<h1 align="center">Touhou Maid: Affection - 女仆亲亲</h1>

<p align="center">
  <b>为 Touhou Little Maid 增加亲密互动、羁绊能力与长期陪伴感。</b>
</p>

<p align="center">
  <a href="README.md">English</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.1-green?style=flat-square" alt="MC 1.21.1"/>
  <img src="https://img.shields.io/badge/NeoForge-21.1.x-orange?style=flat-square" alt="NeoForge"/>
  <img src="https://img.shields.io/badge/Requires-Touhou_Little_Maid_1.5.1+-blue?style=flat-square" alt="TLM"/>
  <img src="https://img.shields.io/badge/License-MIT-yellow?style=flat-square" alt="MIT"/>
</p>

---

## 最新版本

`1.7.2.2` 带来 AI 缓存磁盘持久化、扩展的管理命令和更智能的语言默认值：

- **缓存持久化**：AI 生成的台词和 TTS 音频现在保存到 `world/generated_morning_kiss/` 目录，服务器重启后不会丢失。
- **新命令**：`/tma morning_kiss status`（运行时概览）、`/tma morning_kiss cache`（按女仆统计）、`/tma morning_kiss ai on|off` 和 `/tma morning_kiss tts on|off`（运行时开关），以及扩展的 `/tma morning_kiss clear_ai_cache`（支持女仆/池/条目/语音不同粒度）。
- **配置**：新增 `aiDialogueCacheConsumeOnUse`（默认 `false`）控制缓存消耗或复用模式。`aiDialogueLanguage` 默认值改为 `tlm`，跟随各女仆的 TLM 语言偏好。
- **语言解析**：文本生成语言和 TTS 语音语言现在会根据聊天与 TTS 来源语言独立解析。

完整版本历史见 [CHANGELOG.md](CHANGELOG.md)。

## 功能特性

### 亲吻互动

潜行、空手右击自己的女仆即可亲吻。亲吻会提升好感、播放随机亲吻音效、生成爱心粒子，并触发短暂的贴近镜头。短时间连续亲吻可触发自定义增益「少女祈祷」。

安装 CarryOn 时，右键触发条件会自动调整以避免冲突。公主抱女仆时，也可以使用专门的公主抱亲吻按键。

### 准星亲吻按键

按键设置中新增准星目标女仆亲吻入口。客户端只发送目标实体 id；服务端会重新校验归属、距离、视线、冷却和正常亲吻规则，再决定是否执行亲吻。

### 羁绊系统

高好感女仆可进入羁绊系统。当前能力包括：

| 能力 | 作用 |
|---|---|
| 休闲膝枕 | 与女仆一起坐下或躺下休息，可配置双方姿态与 YSM 动作。 |
| 早安吻 | 定时或手动呼叫女仆问候，支持亲吻、台词与语音播放。 |
| 残血救护 | 让已羁绊女仆贡献每日救援次数，并播放救援语音。 |
| 随机礼物 | 女仆随时间积累并送出小礼物。 |

解锁、消耗、距离、冷却和能力执行均由服务端判定；客户端羁绊页只负责展示与配置。

### 自定义文本与语音

`1.7.2+` 新增数据包语音池结构：

```text
data/touhou_maid_affection/morning_kiss/profile.json
data/touhou_maid_affection/morning_kiss/voices/*.ogg
data/touhou_maid_affection/emergency_rescue/profile.json
data/touhou_maid_affection/emergency_rescue/voices/*.ogg
```

早安吻数据包可配置静态台词池、亲吻音效行为和 OGG 语音；残血救护数据包可配置救援 OGG 语音与兜底音效。完整教程见 [早安吻文本修改教程.md](早安吻文本修改教程.md)，可直接压缩发布的示例包位于 [examples/TMA-Custom-Voice-Pack](examples/TMA-Custom-Voice-Pack)。

### AI Hub

早安吻可以选择复用 TLM AI 站点，在非触发时段提前生成台词与 TTS 语音缓存。运行时开关、提示词与 `aiDialogueLanguage` 语言配置位于 `config/touhou_maid_affection-common.toml`，数据包仍只负责静态文本和预录 OGG 文件。切换语言或提示词后，管理员可执行 `/tma morning_kiss clear_ai_cache` 清空当前服务器会话内的已生成缓存，让后续扫描重新生成。

TMA 还会向 TLM AI 设置页注册 AI Hub 聊天与 TTS 站点预设。当前供应商实现仍兼容 MiMo，但游戏内入口改为围绕 TMA 自身 AI 行为命名，方便后续聊天、TTS 与 STT 相关能力共用同一个入口。用户 API key 和启用状态仍由 Touhou Little Maid 自己保存。

### 兼容性

- Touhou Little Maid：必需依赖。
- Yes Steve Model：可选动作播放与动作列表扫描。
- CarryOn：可选右键冲突规避。
- TLM GUI 与音包：存在时增强，不存在时静默回退。

## 安装

1. 安装 Minecraft `1.21.1` 与 NeoForge `21.1.x`。
2. 安装 Touhou Little Maid `1.5.1+`。
3. 将 `touhou-maid-affection-1.7.2.2.jar` 放入 `mods` 文件夹。
4. 启动游戏。

## 从源码构建

```bash
git clone https://github.com/yabo083/Touhou-Maid-Affection.git
cd Touhou-Maid-Affection
./gradlew build
```

构建产物：

```text
build/libs/touhou-maid-affection-<version>.jar
```

## 维护文档

- [PROJECT_ARCHITECTURE.md](PROJECT_ARCHITECTURE.md)：核心架构边界与模块职责。
- [CHANGELOG.md](CHANGELOG.md)：面向用户的版本更新历史。
- [早安吻文本修改教程.md](早安吻文本修改教程.md)：数据包文本、语音与 AI 配置教程。
- [TESTING.md](TESTING.md)：测试范围、约定与回归命令。
- [DEPLOYMENT.md](DEPLOYMENT.md)：构建发布约束与发版前检查清单。

## 许可证

[MIT License](LICENSE)
