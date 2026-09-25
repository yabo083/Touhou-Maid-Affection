<p align="center">
  <img src="image/README/1773209564540.png" alt="亲亲你的女仆！" width="600"/>
</p>

<h1 align="center">Touhou Maid: Affection - 女仆亲亲</h1>

<p align="center">
  <b>为车万女仆（Touhou Little Maid）带来亲密互动、羁绊成长与深情陪伴。</b>
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

## 模组介绍

**Touhou Maid: Affection** 是车万女仆（Touhou Little Maid）的亲密互动与羁绊扩展模组。

你可以对女仆表达爱意、亲吻提升好感。好感达到一定程度后可开启专属羁绊系统，解锁休闲膝枕、早安吻问候、残血紧急救护以及贴心随机礼物等能力。模组还深度接入了车万女仆的 AI 对话与语音合成体系，支持数据包自定义台词及语音。

## 核心玩法与机理

### 亲吻互动
- **按键互动**：准星对准女仆按下亲吻键即可触发亲吻；使用鞍公主抱起女仆时，也有专属按键可直接亲吻。
- **互动反馈**：每次亲吻均有心形粒子、专属音效及特写镜头推进。
- **好感成长**：亲吻可稳步提升女仆的好感度。好感等级越高，亲吻冷却时间越短。
- **少女祈祷**：短时间内连续亲吻可以为双方带来「少女祈祷」生命恢复增益。

### 羁绊能力
当女仆好感度达到 3 级（最高级）时，女仆背包界面顶部将解锁**羁绊页签**。
玩家消耗背包内的 P 点（PowerPoint）即可逐一激活羁绊能力：

| 羁绊能力 | 功能说明 |
| :--- | :--- |
| **休闲膝枕** | 与女仆共同就坐或平躺休憩。支持自由调节相对位置与视角锁定，兼容 YSM 自定义动作。 |
| **早安吻** | 清晨唤醒或主动问候。女仆会主动上前送上晨安吻，并播放定制台词与配音。 |
| **残血救护** | 遭遇致命危险时，已羁绊女仆会挺身而出抵御伤害并紧急回血，附带专属救护语音。 |
| **随机礼物** | 女仆日常会为你收集并赠送贴心小礼物，可在界面中随时查收礼物堆积进度。 |

### 早安吻 AI 与双语体系
早安吻支持复用车万女仆原生配置的 AI 聊天与 TTS 站点，在后台异步预生成个性化问候台词与语音缓存。
- **跨语种配对**：文本显示语种与语音合成语种彼此独立，支持“中文台词 + 日文配音”等双语搭配。
- **自动适配**：默认设置为 `auto`，文本自动跟随客户端语言，配音自动跟随女仆在车万女仆中的 AI 音色语种。
- **统一管理**：直接复用车万女仆已有的 AI 模型与 API 密钥，无需重复配置服务商。

## 使用方法

### 常用按键
在游戏「选项 → 控制 → 按键绑定」中可自定义以下快捷键：
- **准星亲吻**：对准归属于自己的女仆进行互动。
- **抱起亲吻**：使用鞍抱起女仆时的专属亲吻按键。
- **休闲膝枕**：快捷发起膝枕休息。
- **锁定视角**：膝枕状态下固定观赏视角。

### 游戏内设置面板
在女仆界面的羁绊页右上角，点击「设置」齿轮按钮即可打开独立配置面板：
- **状态 (Status)**：实时查看当前所有女仆的早安吻 AI 缓存进度、生效开关与统计详情。
- **功能 (Features)**：开关各项羁绊功能，并调节 AI 台词生成频率与缓存上限。
- **语音 (Voice)**：自由切换文本与配音语种，在线编辑早安吻 Prompt 提示词模板，并可一键直达车万女仆 AI 站点设置。
- **音量 (Volume)**：拖动滑块实时调节亲吻音效、早安吻语音、残血救护及试听音量。

### 管理员命令
游戏内所有配置均走服务端权威同步，管理员（OP 权限等级 2）可使用 `/tma` 指令进行运维：
```
/tma morning_kiss status                       # 查看早安吻全局状态与缓存统计
/tma morning_kiss clear_ai_cache [all|maid...]  # 清理 AI 问候台词与语音缓存
/tma rescue on|off|toggle                      # 开启或关闭全服残血救护功能
/tma bond prune [days]                         # 清理离线超过指定天数的女仆羁绊数据（默认 90 天）
```

### 数据包自定义语音
支持通过数据包（Datapack）拓展早安吻与残血救护的台词池及 `.ogg` 语音包：
- 数据包根路径：`data/touhou_maid_affection/morning_kiss/` 与 `emergency_rescue/`
- 示例语音包参考：[examples/TMA-Custom-Voice-Pack](examples/TMA-Custom-Voice-Pack)
- 详细编写教程：[早安吻相关配置说明.md](早安吻相关配置说明.md)

## 注意事项

- **服务端权威**：好感度计算、冷却时间、视线判定及羁绊能力均由服务端校验。单人游戏与多人联机体验完全一致。
- **音量控制**：面板内的音量滑块属于本地客户端衰减设置（`0.0` 为静音，`1.0` 为标准音量）。若需要更大音量，请提高系统或游戏主音量。
- **权限控制**：在多人服务器中，全局功能开关、语种策略及提示词模板仅限管理员修改，普通玩家打开设置面板为只读状态。
- **数据保留**：女仆死亡重生、卸载或使用道具收纳时羁绊数据完整保留，如需清理长期废弃数据请使用 `/tma bond prune`。

## 杂项与兼容性

### 模组联动
- **Touhou Little Maid (车万女仆)**：核心前置模组。
- **MaidFileManager (女仆档案管理器)**：支持将女仆的羁绊等级、解锁能力与语音偏好随 `.maid` 档案一同无损迁移导出。
- **Yes Steve Model (YSM)**：支持膝枕状态下的自定义动作播放。
- **Tweakerge / Tweakeroo**：完美兼容自由摄像机视角，膝枕平躺不会引发任何冲突。

### 运行环境与安装
1. 安装 Minecraft `1.21.1` 与 NeoForge `21.1.x`。
2. 安装前置模组 **Touhou Little Maid**（`1.5.1+`）。
3. 将 `touhou-maid-affection-1.7.5.1.jar` 放入 `.minecraft/mods` 文件夹。
4. 启动游戏。

### 开发者文档
- [PROJECT_ARCHITECTURE.md](PROJECT_ARCHITECTURE.md)：架构设计与模块分层说明。
- [CHANGELOG.md](CHANGELOG.md)：详细更新日志。
- [早安吻相关配置说明.md](早安吻相关配置说明.md)：数据包台词、语音与 AI 进阶配置指南。
- [TESTING.md](TESTING.md)：自动化测试与回归验证规范。

### 开源许可证
本项目遵循 [MIT 许可证](LICENSE) 开源。
