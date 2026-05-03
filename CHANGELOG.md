# Changelog

本文件记录用户可感知的功能变更与修复历史。

格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，分类使用：
- `Added`：新功能
- `Changed`：行为/架构变更
- `Fixed`：缺陷修复
- `Removed`：移除内容

## [Unreleased]

## [1.7.2.2] - 2026-05-04

### Added
- 新增早安吻 AI 缓存磁盘持久化：服务启动时从 `world/generated_morning_kiss/` 自动加载缓存，服务关闭时自动保存，重启服务器后已生成的 AI 台词与 TTS 语音不会丢失。
- 新增 `/tma morning_kiss status` 命令，显示早安吻运行时状态概览（AI/TTS 开关、缓存条目总数、女仆数、带语音条目数）。
- 新增 `/tma morning_kiss cache` 命令，按女仆和语言池分组显示缓存统计详情（条目数、语音条目数、语言配置、修订号、飞行请求数）。
- 新增 `/tma morning_kiss ai on|off` 和 `/tma morning_kiss tts on|off` 命令，实时开关 AI 台词预生成与 TTS 语音生成。
- 新增 `/tma morning_kiss clear_ai_cache` 系列命令，支持女仆级、池级、条目级缓存清理与语音剥离。
- 新增 `aiDialogueCacheConsumeOnUse` 配置项（默认 `false`），管理员可选择消耗或复用缓存条目以平衡 LLM/TTS Token 成本与体验。
- 新增缓存统计报告 API，按女仆和语言分组输出条目数、语音条目数、修订号与飞行请求数。

### Changed
- `aiDialogueLanguage` 默认值从 `zh_cn` 改为 `tlm`：未显式配置时将跟随各女仆的 TLM 聊天语言和 TTS 语言偏好，而非全局固定中文。
- 语言解析分化为文本生成语言和 TTS 语音语言两套规则：`resolveGeneratedTextLanguage()` 优先使用聊天语言，`resolveGeneratedVoiceTextLanguage()` 优先使用 TTS 语言。
- 缓存淘汰策略新增 `aiDialogueCacheTargetPerPool` 容量目标约束，超出目标池容量的候选行会被自动裁剪。
- 缓存命令显示改用 Unicode 感知宽度计算（ASCII 半角=1，CJK 全角=2），对齐表格列对齐。

### Fixed
- 修复早安吻 AI/TTS 预生成未应用 `aiDialogueLanguage` 的问题：预生成 prompt 现在会追加语言覆盖指令，TTS 请求也会使用该配置归一化后的语言代码。

## [1.7.2.1] - 2026-05-02

### Added
- 早安吻与残血救护的语音列表新增右键试听：内置音效、本地 TLM 音包语音与数据包语音均可在配置页直接预览，无需调整触发时间或进入实战流程。
- 新增语音试听网络请求链路，数据包语音由服务端校验女仆归属、能力解锁和文件存在性后，再将目标音频字节回传客户端播放。

### Fixed
- 修复 TLM 原生音包语音在配置页右键试听时可能“已读取字节但无声”的问题：试听改为专用流式播放实例，使用稳定声音事件锚点、`PLAYERS` 音量分类与无位置衰减。
- 修复语音列表点击行为边界：左键只负责选择语音项，右键才触发试听，避免选择时误播放。

### Changed
- TMA AI 入口在玩家可见文本中统一为 TMA AI Hub；内部兼容旧 `tma_mimo_*` 站点 ID，避免破坏已有配置。

## [1.7.2] - 2026-05-02

### Added
- 新增早安吻与残血救护的功能级语音池：数据包可分别提供 `morning_kiss/voices/*.ogg` 与 `emergency_rescue/voices/*.ogg`，玩家可在羁绊页中为每名女仆选择随机、顺序或指定语音项。
- 新增早安吻静态台词数据包配置，支持 `morning`、`evening`、`general` 台词池，以及 `{maid}`、`{player}` 占位符。
- 新增早安吻 AI 台词与 TTS 预生成链路：服务端可在非触发时段提前为已解锁早安吻的女仆生成台词与语音缓存，并在触发时优先播放缓存结果。
- 新增 TMA MiMo AI 适配器，为 Touhou Little Maid 新版 AI 聊天架构提供 `tma_mimo_chat` 与 `tma_mimo_tts` 站点类型，并在羁绊页提供 MiMo 设置入口。
- 新增“准星目标女仆亲吻”按键入口：玩家无需公主抱，也可通过按键请求服务端校验目标女仆后执行亲吻。
- 新增 `examples/TMA-Custom-Voice-Pack` 示例数据包，可直接压缩后作为早安吻/残血救护自定义语音与文本的发布样例。

### Changed
- 早安吻 AI 运行时配置从数据包迁移到 `config/touhou_maid_affection-common.toml` 的 `morningKissBehavior` 段，数据包只负责静态台词、亲吻音效与预录 OGG 语音池。
- 残血救护预录语音链路移除旧的服务端/客户端文件同步服务，改为触发时通过 payload 下发命中的数据包 OGG 字节或回退到 TLM 音包/兜底音效。
- 早安吻与残血救护语音二级页改为统一的动态语音池页面，服务端同步数据包候选项，客户端补充 TLM 音包候选项。
- 羁绊页顶部入口改为运行时推导可用 tab 位置，减少与 Touhou Little Maid 或其他扩展页签的冲突。
- 默认按键策略调整：公主抱亲吻与准星亲吻可共享默认键位，膝枕角度冻结默认不再占用该键位。

### Fixed
- 修复膝枕角度冻结按键与亲吻入口同键时可能出现的误提示问题。
- 修复早安吻运行时 AI/TTS 失败时的回退策略，避免远程服务异常阻断静态台词或已有语音播放。
- 修复 MiMo TTS 音频响应解析与缓存格式边界，避免不可播放格式进入客户端播放队列。

### Removed
- 移除旧版救援语音资源同步 payload 与本地文件同步配置类，救援语音统一走当前的功能级数据包语音池。
- 移除数据包中的早安吻 AI 开关职责，避免静态资源包与运行时 AI 供应商状态互相耦合。

## [1.7.1.4] - 2026-04-06

### Changed
- 紧急救援链路从玩家 tick 轮询改为事件驱动：由伤害预处理与死亡回调触发，日刷新改为按需懒刷新，并抽离 `EmergencyRescueService` 统一编排。
- 建立统一变更记录规范：`CHANGELOG.md` 作为用户向更新历史主入口，README 不再内嵌历史版本日志。
- 客户端 YSM 路径归一化逻辑收敛为 `NamespacedPathNormalizer`，减少分散字符串解析。
- Modrinth 页面正文同步源切换为英文 README，保持对外发布页信息一致。

### Fixed
- 修复女仆配置页读取 YSM 动作时可能触发的 `ArrayIndexOutOfBoundsException` 崩溃问题：当资源索引异常或命名空间字符串不规范时，改为 best-effort 降级，而不是直接中断界面。

## [1.7.1.3] - 2026-03-29

### Changed
- 强化膝枕姿态桥接，降低飞行状态覆盖导致的姿态错乱与会话不稳定。

## [1.7.1.2] - 2026-03-27

### Fixed
- 修复残血救护贡献统计重复计数问题：改为按女仆持久身份去重，互转/复活后不会在同一周期重复贡献。

## [1.7.1.1] - 2026-03-26

### Fixed
- 修复膝枕能力未解锁前仍可通过按键触发的状态判定缺陷，并补充客户端与服务端双侧拦截反馈。

## [1.7.1]

### Added
- 新增羁绊系统四项能力：休闲膝枕、早安吻、残血救护、随机礼物。

## [1.6.1] - 2026-03-13

### Fixed
- 修复跨存档会话导致的亲吻冷却误共享问题。
- 冷却粒度细化为按女仆独立计算，避免一名女仆冷却影响其他女仆。
