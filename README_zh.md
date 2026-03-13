<p align="center">
  <img src="image/README/1773209564540.png" alt="亲亲你的女仆！" width="600"/>
</p>

<h1 align="center">💋 Touhou Maid: Affection — 女仆亲亲</h1>

<p align="center">
  <b>亲亲你的女仆吧，她值得。</b>
</p>

<p align="center">
  <a href="README.md">🌏 English</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.1-green?style=flat-square" alt="MC 1.21.1"/>
  <img src="https://img.shields.io/badge/NeoForge-21.1.x-orange?style=flat-square" alt="NeoForge"/>
  <img src="https://img.shields.io/badge/Requires-Touhou_Little_Maid_1.5.0+-blue?style=flat-square" alt="TLM"/>
  <img src="https://img.shields.io/badge/License-MIT-yellow?style=flat-square" alt="MIT"/>
</p>

---

## ✨ 功能特性

### 💋 亲吻系统

| 特性 | 描述 |
|---|---|
| 💋 **亲吻互动** | 潜行 + 空手右击你的女仆，触发亲吻 |
| 💕 **爱心粒子** | 亲吻时在两人之间生成浪漫的爱心粒子效果 |
| 🔊 **亲吻音效** | 播放清脆的亲吻音效（7 个随机变体） |
| 📈 **好感度提升** | 每次亲吻为女仆增加 **+3 好感度**（30 秒冷却） |
| 👀 **女仆凝视** | 亲吻时女仆会转头看向你 |
| ⏱️ **分级冷却** | 冷却时间随好感度等级降低：5秒 → 3秒 → 1秒 → 0秒 |
| 🎥 **零距离镜头** | 亲吻时镜头平滑推近到女仆面前——真正的面对面特写 |
| 💞 **浪漫粒子系统 v2** | 爱心改为分阶段生成并支持 RING/HALO/SPIRAL 形态，可减少遮脸、提升氛围感 |

### 🤲 公主抱亲吻按键

当你用鞍把女仆公主抱起来（女仆作为玩家乘客）时，右键命中会变得困难。  
本模组新增了原生可改键位：

```
女仆亲亲 -> 公主抱亲吻（默认：V）
```

- 仅在“正在公主抱女仆”时生效
- 不影响普通状态下的原有右键亲吻
- 公主抱状态会使用专门的相机对焦逻辑，尽量保持头对头构图

### 🙏 少女祈祷

在 **10 秒内亲吻 3 次** 即可触发「少女祈祷」增益，同时施加给你和女仆。

- 自定义药水效果，内置生命恢复（非原版再生）
- 恢复强度**随好感度等级提升**：
  - 等级 0：恢复 I
  - 等级 1：恢复 II
  - 等级 2：恢复 III *（超越原版！）*
  - 等级 3：恢复 V *（爱的力量！）*
- 持续时间：30 秒（可配置）

### 🧲 副手吸引女仆

原版 TouhouLittleMaid 中，只有**主手**持蛋糕才能吸引女仆。本模组通过 Mixin 扩展了这一行为：

```
副手持有诱惑物品（默认蛋糕）→ 女仆也会被吸引过来
```

### 📦 CarryOn 兼容

自动检测 CarryOn 模组。安装后亲吻触发方式会自动调整以避免冲突：

```
潜行 + 主手为空 + 副手持有任意物品 + 右击女仆
```

> 💡 **提示**：副手放蛋糕吸引女仆过来，然后潜行右击亲吻——一气呵成！

### ⚙️ 完全可配置

所有数值均可在 `config/touhou_maid_affection-common.toml` 中调整：
- 各好感度等级的亲吻冷却时间
- 好感度增加量和冷却
- 少女祈祷的触发阈值、持续时间、恢复倍率
- FOV 缩放强度和动画时长
- 公主抱相机偏移参数：
  - `carriedSideOffset`（默认 `0.48`）
  - `carriedForwardOffset`（默认 `0.16`）
  - `carriedVerticalOffset`（默认 `-0.10`）
- 进阶粒子控制参数：
  - 防遮挡锚点：`offsetY`、`forwardOffset`、`avoidViewStrength`
  - 空间与运动：`shapeMode`、`spreadRadius`、`upwardSpeed`、`radialSpeed`、`swirlSpeed`
  - 时间节奏：`phaseBursts`、`phaseIntervalTicks`、`phaseRamp`
  - 大小模拟：`clusterCopies`、`clusterJitter`
  - 浪漫陪衬：`accentEnabled`、`accentType`、`accentChance`、`favorabilityColorAccent`
- 粒子数量

## 📥 安装

1. 安装 **Minecraft 1.21.1** + **NeoForge 21.1.x**
2. 安装 **[Touhou Little Maid](https://modrinth.com/mod/touhou-little-maid)** 1.5.0+
3. 将 `touhou-maid-affection-x.x.x.jar` 放入 `.minecraft/mods/` 文件夹
4. 启动游戏！

## 🛠️ 从源码构建

```bash
git clone https://github.com/yabo083/maid-affection.git
cd maid-affection
./gradlew build
```

构建产物在 `build/libs/touhou-maid-affection-x.x.x.jar`。

## 📋 技术细节

- **Mod ID**: `touhou_maid_affection`
- **API**: 使用 TouhouLittleMaid 提供的 `InteractMaidEvent` 事件 API
- **网络**: `KissMaidPayload`（Server → Client）用于特效同步，`KissCarryRequestPayload`（Client → Server）用于公主抱按键触发
- **兼容性**: 通过 `ModList.isLoaded()` 软检测 CarryOn，零硬依赖
- **好感度**: 使用 TLM 内置的 `FavorabilityManager` + 自定义 `Type("Kiss", 3, 600)`
- **客户端效果**: FOV 缩放通过 `ComputeFovModifierEvent` + 镜头角度通过 `ViewportEvent.ComputeCameraAngles` 实现

## 📄 许可证

[MIT License](LICENSE) — 自由使用、修改和分发。

---

<p align="center">
  <i>Made with ❤️ for the Touhou Little Maid community</i>
</p>
