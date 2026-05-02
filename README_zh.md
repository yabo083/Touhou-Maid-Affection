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

`1.7.2.1` 是一次语音试听体验修补版：

- 早安吻与残血救护语音列表支持右键试听。
- 数据包语音试听会先由服务端校验，再回传音频给客户端播放。
- TLM 原生音包试听改为专用的玩家中心流式播放路径，修复部分原生语音包右键试听无声的问题。
- 左键现在只负责选择语音项，右键才触发试听，避免选择时误播放。
- 玩家可见文本统一使用 TMA AI Hub 命名，同时保留旧内部站点 ID 兼容已有配置。

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

早安吻可以选择复用 TLM AI 站点，在非触发时段提前生成台词与 TTS 语音缓存。运行时开关与提示词配置位于 `config/touhou_maid_affection-common.toml`，数据包仍只负责静态文本和预录 OGG 文件。

TMA 还会向 TLM AI 设置页注册 AI Hub 聊天与 TTS 站点预设。当前供应商实现仍兼容 MiMo，但游戏内入口改为围绕 TMA 自身 AI 行为命名，方便后续聊天、TTS 与 STT 相关能力共用同一个入口。用户 API key 和启用状态仍由 Touhou Little Maid 自己保存。

### 兼容性

- Touhou Little Maid：必需依赖。
- Yes Steve Model：可选动作播放与动作列表扫描。
- CarryOn：可选右键冲突规避。
- TLM GUI 与音包：存在时增强，不存在时静默回退。

## 安装

1. 安装 Minecraft `1.21.1` 与 NeoForge `21.1.x`。
2. 安装 Touhou Little Maid `1.5.1+`。
3. 将 `touhou-maid-affection-1.7.2.1.jar` 放入 `mods` 文件夹。
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
