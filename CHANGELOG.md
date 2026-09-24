# Changelog

本文件记录用户可感知的功能变更与修复历史。

格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，分类使用：
- `Added`：新功能
- `Changed`：行为/架构变更
- `Fixed`：缺陷修复
- `Removed`：移除内容

## [1.7.5.0] - 2026-09-24

### Added
- 新增游戏内「全局设置」面板：在羁绊页顶部左侧新增「设置」入口，面板与当前女仆无关，分三个区——功能开关（早安吻 / 女仆主动早安吻 / AI 台词 / AI 语音 / 残血救护 / 随机礼物 / 少女祈祷 Buff）、语种（文本语种 / 配音语种下拉框）、音量（亲吻音效 / 早安吻语音 / 残血救护 / 语音试听，0.0–1.0 步进 0.05）。无需再手改 toml。
- 新增服务端权威设置同步通道：`TmaSettingsRequestPayload`（C2S，空列表表示只读状态）与 `TmaSettingsStatePayload`（S2C，回推全部白名单键的当前值与 `canEdit`）。面板里的开关与语种由服务端校验、应用并回推，单人存档与多人服务器行为一致。
- 新增自绘滑块组件 `BondSlider`，供设置面板的音量项使用。

### Fixed
- 修复设置面板语种下拉框最后一项被裁剪的问题：展开的弹层不再受面板内容区 scissor 约束，改为夹在屏幕范围内——向下会溢出屏幕底部时翻到表头之上渲染，命中测试/高亮/点击与实际渲染位置保持一致，所有候选条目可达。
- `.maid` 迁移不再携带运行态/调度键：导出（`BondData.exportMaidData`）剔除 `BondKeys.RUNTIME_KEYS`——礼物计时（`RandomGiftLastWallClock` / `RandomGiftLastDelivery` / `RandomGiftLastIntervalMinutes`）、早安吻窗口标记（`MorningKissScheduledWindow` / `MorningKissScheduledAttemptTick` / `MorningKissLastAutoAttemptGameTime` / `MorningKissLastSuccessWindow` / `MorningKissLastFailedWindow`）与本地记账 `LastSeen`——导入时再防御性剔一遍。此前这些会话/世界相关的绝对时间会随 `.maid` 迁到新女仆/新存档，导致刚导入就被判定「今天已亲过」或礼物计时错乱。待发礼物队列 `RandomGiftQueue` 是耐久状态，仍然随迁；`extras` 对外形状与整体替换语义不变。
- 空字符串不再落盘/导出：所有「getter 缺省值本就是空串」的字符串 setter（声音包、YSM 档案、救护动作、膝枕动作、早安吻计划/窗口标记、玩家粒度早安吻选择、早安吻与救护语音选择里的空字段）改为空串时移除键而非写入空值，避免无意义键堆积进存档与 `.maid`。

### Changed
- 音量上限从 `4.0` 收至 `1.0`：四项音量配置（`cooldown.kissSoundVolume`、`morningKissBehavior.voiceVolume`、`emergencyRescueBehavior.volume`、`voicePreview.volume`）与设置面板滑块现在**只做衰减**——`0.0` 静音、`1.0` 保持原有响度，不再放大。需要更大音量请使用 Minecraft 或系统音量。旧配置里大于 `1.0` 的值会被配置系统在加载时纠正为 `1.0`（Forge `defineInRange` 行为）。
- 音量四项（`cooldown.kissSoundVolume`、`morningKissBehavior.voiceVolume`、`emergencyRescueBehavior.volume`、`voicePreview.volume`）是纯客户端配置，面板里拖动即写入本机配置并立即生效，不走网络。
- 语音试听冷却从 5 秒降到 0.5 秒（100 → 10 tick）；被限流时给客户端一行 action bar 提示「试听过快，请稍候」，不再静默丢弃；非数据包（本地）试听请求不再占用限流额度。
- 早安吻 AI 台词缓存改为按语种读取：`pollRandom` / `peekRandom` 与预热满度判定（`countMatching`、`addIfBelowTarget`）都按当前解析出的显示/配音语种过滤，改了显示或配音语种后会自动按新语种重新预热，旧语种条目保留但不再被选中，也不再阻塞重新预热（仍可手动 `clear_ai_cache`）。目标语种为空（`auto`）时不过滤，保持旧行为。
- `/tma morning_kiss status` 的语种行改为显示最终生效信息：全局语种（归一化后，未配置显示 `auto`）、AI 覆盖原始值，以及生效优先级说明。
- 全局羁绊 UI 配色统一为「暖木底 + 玫瑰（交互面 / 装饰）+ 金（仅高亮文字）」：`BondGuiTokens` 换用新调色板（保留原常量名，新增开关 / 字段 / 滑块 / 导航等语义色常量），所有调用点自动跟随。设置面板同步按设计稿收口：300×188 模态框、46px 侧栏三 tab、胶囊开关、78×20 字段式下拉、88×13 滑块（数值金色居中）、分区作用域标签、侧栏藤蔓装饰（`rose_vine.png`，素材 132×165、按 44×55 绘制，水平居中于导航轨、茎根落在面板底边上，整株在面板内）、行内请求状态点与「完成 / 重载」footer。不影响功能与其它二级页布局尺寸（仍为 172×150）。
- 设置面板从「女仆 GUI 内嵌二级页」改为**独立 Screen**（`TmaSettingsScreen`，删除 `SettingsSecondaryPage`）：在羁绊页「设置」按钮处用 `Minecraft#setScreen` 打开，自带全屏压暗背景，关闭（footer「完成」/ESC/点击压暗区）返回来源的女仆 GUI。面板尺寸/配色/控件与交互 token 不变，服务端权威语义、payload、状态机与音量直写零变更；除设置页外的其它二级页与 `BondSecondaryPage` 接口不受影响。
- 设置面板语种从 4 项砍到 2 项：白名单变为 **7 开关 + 2 语种 = 9 项**，面板只保留「文本语种」（`display_language`）与「配音语种」（`voice_language`）。AI 专用语种 `morningKissBehavior.aiDialogueLanguage` / `aiDialogueVoiceLanguage` 仍是有效 toml 配置、AI 语言解析逻辑不变，只是不再暴露在面板里（需要时手改 toml）。
- 羁绊数据改为**按女仆嵌套存储**：女仆粒度数据从「根上扁平键 `<base>_<女仆UUID>`」改为 `touhou_maid_affection.bond.maids.<女仆UUID>.<base>` 子树，玩家粒度键（早安吻选择）仍在根上；键名常量集中到 `BondKeys`。首次读取旧存档时自动执行**一次性迁移**（根上新增 `SchemaVersion`，先写后删、幂等、无法解析的键原样保留），旧存档无损升级，无需手动操作。反查（按能力找女仆、按模型 ID 找女仆、按救护 provider 找女仆、批量重置能力）改为遍历女仆子树，不再全键扫描。
- 新增 `/tma bond prune [days]`（默认 90 天，权限等级 2）：清理执行者羁绊数据中 `LastSeen` 早于阈值的女仆子树（缺失 `LastSeen` 的历史数据视为过旧；`days <= 0` 只统计不删除），并回显删除/保留数量。`LastSeen` 在同步女仆档案时刷新；**不**在女仆死亡 / 卸载 / 换主人时自动删除数据（TLM 灵魂玩偶、椅子等会临时移除实体，自动删会丢数据），只能靠该命令显式清理。
- `.maid` 迁移（MaidFileManager SPI）的附加数据对外格式**不变**：仍是「base 名 → 值」的 compound（即女仆子树本身），旧导出文件仍可导入，导入后刷新 `LastSeen`。
- 删除若干零调用者死代码：`BondData/BondManager.getUnlockedMaidModelIdsForAbility`、`BondData/BondManager.findMaidProfileByRescueProviderId`、`MorningKissGeneratedDialogueService.hasCachedLine`（两个重载）、`MorningKissGeneratedDialogueCache.isEmpty`，以及已被 `BondKeys` 取代的 `compat/maidfm/MaidDataKeyCodec`。

### Notes
- 权限：开关与语种属于服务端权威设置，**只有 OP（权限等级 2）可以修改**；普通玩家能看、不能改，界面底部会显示「只读」提示，按钮 tooltip 提示需要管理员权限。每次成功修改都会在服务端日志打印 `[TMA Settings] player=... key=... old=... new=...`；越权或非法请求打印 WARN。
- 非法请求（未知 key、非法布尔、非法语种、超长值）整包拒绝，不会部分生效。
- 语种下拉框只提供 `auto` 与常见 locale，当前值若不在列表里会动态补上；`tlm` / `inherit` / `default` 仍是合法值（会作为当前值显示），但不在下拉候选中，需要时请改 toml。

## [1.7.4.0] - 2026-09-24

### Added
- 新增 `morningKissBehavior.displayLanguage`（默认 `auto`）与 `morningKissBehavior.voiceLanguage`（默认 `auto`），把原先只存在于 AI 链路的「显示语种 / 配音语种」解耦提升为全局可配，并让数据包链路同样支持按语种选择台词与语音。
- 数据包 `data/touhou_maid_affection/morning_kiss/profile.json` 纯增量扩展：`dialogue.<pool>` 元素接受 `{"text": "...", "language": "zh_cn"}`，`voice_files` 元素接受 `{"file": "x.ogg", "language": "ja_jp", "text": "可选字幕", "text_language": "zh_cn"}`；旧的纯字符串写法保留并视为「未标记」通配，向后兼容。
- `voice_files[].text` / `text_language` 支持语音与字幕配对：选中该语音时直接显示配对字幕，不再随机抽台词；`text_language` 与目标显示语种不一致时退回随机台词。

### Changed
- 语言解析优先级统一为「AI 专用显式 locale > 全局显式 locale > 旧语义（`tlm` / `inherit` / `auto`）」；`tlm`/`inherit`/`auto` 语义与翻译、缓存机制保持不变。旧配置项名字与默认值不变。
- 数据包台词与语音选择新增语种优先级：匹配目标语种的条目 → 未标记条目 → 全部条目。`dialogue_mode=append` 时内置台词按「语言 = 客户端语言」参与同一筛选。
- `auto`（默认值）下行为与 1.7.3.0 完全一致：不启用任何语种筛选。

### Notes
- 内置台词仍是 i18n key，由客户端按自身语言渲染，服务端无法指定渲染语种；TLM 音包没有语言元数据，天然单语种，不参与筛选。

## [1.7.3.0] - 2026-07-24

### Added
- 新增 `cooldown.kissSoundVolume` 配置项，可在 `0.0` 到 `4.0` 之间调整普通亲吻与早安吻复用亲吻音效的音量；`0.0` 可静音，`1.0` 保持旧版默认响度。
- 新增 `morningKissBehavior.voiceVolume`、`emergencyRescueBehavior.volume` 与 `voicePreview.volume` 配置项，分别控制早安吻语音、残血救护语音/兜底音效和羁绊页语音试听音量。
- 新增 `morningKissBehavior.aiDialogueVoiceLanguage`，可将 AI 早安吻的显示文本与配音文本设为不同语言；例如 `aiDialogueLanguage=zh_cn`、`aiDialogueVoiceLanguage=ja_jp` 会显示中文台词，并先翻译为日文后请求 TTS。
- 新增 `randomGiftBehavior.curatedPoolOnly`，默认只从 `touhou_maid_affection:bond_random_gift_pool` 物品标签抽取礼物；可通过 `bond_random_gift_blacklist` 继续追加黑名单。
- 新增可选的残血救护最大生命百分比阈值配置；启用后按最大生命的 20% 触发，在原版 20 点生命下仍等于旧版 4 点阈值。为兼容旧服自定义配置，该模式默认关闭。
- 接入 MaidFileManager（女仆档案管理器）迁移 SPI：导出 `.maid` 时携带该女仆的羁绊数据（羁绊等级、能力解锁、语音池与播放模式、膝枕姿态、随机礼物队列与计时、早安吻计划/窗口、救援动作与提供者 id 等），导入到新存档/新版本时按新 UUID 自动还原；未安装管理器时静默不生效。

### Changed
- 休闲膝枕按键默认改为未绑定，仍可在 Minecraft 控制设置中自行绑定，避免默认占用常见的 `B` 键。
- 羁绊页中的休闲膝枕触发提示与能力描述改为显示当前实际绑定按键，不再固定显示 `B`。
- 随机礼物默认不再遍历几乎全部物品注册表；显式标签池成为默认来源，旧的广泛抽样模式仍可通过配置恢复。
- 羁绊解锁提示现在显示背包中的 P 点物品数量，并明确其不是神社庭灯存储的 P 点数值；点击不可用按钮时会显示失败原因。
- 早安吻调度、对话来源与语音池策略拆分为独立模块；客户端内存语音统一复用格式检测、异步解码和实体跟随实现，减少不同播放入口之间的行为漂移。

### Fixed
- 拒绝膝枕姿态中的 `NaN`/无限坐标，避免无效网络数据污染玩家存档和实体姿态。
- 对持久化 AI 早安吻语音执行 2 MiB 单文件上限并隔离可变字节数组，避免异常 TTS 或手工缓存文件造成内存放大与缓存篡改。
- 拒绝膝枕角度锁中的非有限旋转值，并在启动与持续执行期间复核女仆当前所有权。
- 数据包语音改为有界流式读取，并在构造资源位置前过滤非法大小写、空格和路径字符，避免资源重载期间的过量分配或中断。
- 限制客户端可控语音配置和动作标识的持久化长度，并为数据包语音试听增加每玩家 100 tick 冷却和请求字段上限，避免玩家 NBT 超限和小请求放大为连续大响应。
- 旧版广泛随机礼物模式仅默认排除屏障、命令方块、结构方块、调试棒等破坏沉浸感的技术/管理物品；基岩与刷怪蛋仍可正常出现。显式礼物池标签可覆盖默认排除规则。
- MiMo Chat 与 TTS 响应改为有界流式接收：聊天响应上限 1 MiB、TTS JSON 响应上限 4 MiB、解码后音频上限 2 MiB；远端错误正文只保留 512 字符摘要，避免异常响应造成内存和日志放大。

### Removed
- 移除潜行空手右击女仆的亲吻入口及其 `cooldown.rightClickKissEnabled` 配置，避免永久占用 TLM 坐下/站起交互；亲吻改用准星目标或公主抱专用按键。

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
