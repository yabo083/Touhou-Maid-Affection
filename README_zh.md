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

`1.7.3.0` 汇总了评论区高价值玩家反馈，并强化了默认交互安全性：

- **可控礼物池**：随机礼物默认使用策划物品标签池；旧版广泛抽样模式只排除破坏沉浸感的技术/管理物品，显式标签配置可覆盖该默认规则。
- **交互开关**：`rightClickKissEnabled` 可关闭潜行右击亲吻，但不影响准星亲吻与公主抱亲吻按键。
- **整合包生命适配**：残血救护可选按最大生命百分比触发。
- **解锁说明**：羁绊页会明确要求背包中的 P 点物品，并在点击不可用按钮时反馈原因。

完整版本历史见 [CHANGELOG.md](CHANGELOG.md)。

## 功能特性

### 亲吻互动

潜行、空手右击自己的女仆即可亲吻。亲吻会提升好感、播放随机亲吻音效、生成爱心粒子，并触发短暂的贴近镜头。短时间连续亲吻可触发自定义增益「少女祈祷」。

如果该操作与女仆坐下/站起冲突，可设置 `cooldown.rightClickKissEnabled=false`，改用可配置的准星亲吻或公主抱亲吻按键。

声音音量可在 `config/touhou_maid_affection-common.toml` 中调整：`cooldown.kissSoundVolume` 控制亲吻音效，`morningKissBehavior.voiceVolume` 控制早安吻语音，`emergencyRescueBehavior.volume` 控制残血救护语音与兜底音效，`voicePreview.volume` 控制羁绊页语音试听。

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

提高最大生命值的整合包可启用 `emergencyRescueBehavior.usePercentageThreshold=true`；默认 20% 在原版生命上限下等于旧版 4 点阈值。为兼容已有服务器配置，百分比模式默认关闭。

随机礼物默认只使用 `touhou_maid_affection:bond_random_gift_pool` 物品标签中的策划池。数据包可扩展该标签，或通过 `touhou_maid_affection:bond_random_gift_blacklist` 添加排除项；只有需要旧版广泛注册表抽样时才建议设置 `bondCosts.randomGiftBehavior.curatedPoolOnly=false`。

解锁、消耗、距离、冷却和能力执行均由服务端判定；客户端羁绊页只负责展示与配置。

### 自定义文本与语音

`1.7.2+` 新增数据包语音池结构：

```text
data/touhou_maid_affection/morning_kiss/profile.json
data/touhou_maid_affection/morning_kiss/voices/*.ogg
data/touhou_maid_affection/emergency_rescue/profile.json
data/touhou_maid_affection/emergency_rescue/voices/*.ogg
```

早安吻数据包可配置静态台词池、亲吻音效行为和 OGG 语音；残血救护数据包可配置救援 OGG 语音与兜底音效。完整教程见 [早安吻相关配置说明.md](早安吻相关配置说明.md)，可直接压缩发布的示例包位于 [examples/TMA-Custom-Voice-Pack](examples/TMA-Custom-Voice-Pack)。

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
3. 将 `touhou-maid-affection-1.7.3.0.jar` 放入 `mods` 文件夹。
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
- [早安吻相关配置说明.md](早安吻相关配置说明.md)：数据包文本、语音与 AI 配置教程。
- [TESTING.md](TESTING.md)：测试范围、约定与回归命令。
- [DEPLOYMENT.md](DEPLOYMENT.md)：构建发布约束与发版前检查清单。

## 许可证

[MIT License](LICENSE)
