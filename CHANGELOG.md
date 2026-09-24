# Changelog

本文件记录用户可感知的功能变更与修复历史。

格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，分类使用：
- `Added`：新功能
- `Changed`：行为/架构变更
- `Fixed`：缺陷修复
- `Removed`：移除内容

## [1.7.5.0] - 2026-09-24

### Added
- 新增游戏内「全局设置」面板：在羁绊页顶部左侧新增「设置」入口，面板与当前女仆无关，分三个区——功能开关（早安吻 / 女仆主动早安吻 / AI 台词 / AI 语音 / 残血救护 / 随机礼物 / 少女祈祷 Buff）、AI 早安吻语种（显示语种 / 配音语种下拉框）、音量（亲吻音效 / 早安吻语音 / 残血救护 / 语音试听，0.0–4.0 步进 0.05）。无需再手改 toml。
- 新增服务端权威设置同步通道：`TmaSettingsRequestPayload`（C2S，空列表表示只读状态）与 `TmaSettingsStatePayload`（S2C，回推全部白名单键的当前值与 `canEdit`）。面板里的开关与语种由服务端校验、应用并回推，单人存档与多人服务器行为一致。
- 新增自绘滑块组件 `BondSlider`，供设置面板的音量项使用。

### Changed
- 音量四项（`cooldown.kissSoundVolume`、`morningKissBehavior.voiceVolume`、`emergencyRescueBehavior.volume`、`voicePreview.volume`）是纯客户端配置，面板里拖动即写入本机配置并立即生效，不走网络。

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
- 接入 MaidFileManager（车万女仆档案管理器）迁移 SPI v1.4.0：把 TMA 挂在女仆身上但不在女仆实体 NBT 内的数据（`BondData`，实际存于主人玩家 persistentData）接入 `.maid` 文件的导出/导入流程；SPI 两个接口以编译期 shim 形式内置并从产物 jar 排除，运行期由管理器 jar 提供实现，注册带软依赖守卫。

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

## [1.7.2.2] - 2026-05-03

### Added
- 新增 `/tma morning_kiss clear_ai_cache` 管理命令，可清空当前服务器会话内已预生成的早安吻 AI 台词与 TTS 语音缓存，便于切换语言或提示词后重新生成。
- `/tma morning_kiss` 现在提供状态与缓存查看入口，便于确认早安吻 AI/TTS 配置和当前会话缓存情况。
- `/tma morning_kiss ai on/off` 与 `/tma morning_kiss tts on/off` 可在游戏内开关早安吻 AI 文本生成和生成式 TTS。

### Fixed
- 修复早安吻 AI/TTS 预生成未应用 `aiDialogueLanguage` 的问题：默认 `tlm` 会跟随 TLM 本体语言；当后台预生成会生成远程 TTS 时，待合成文本会优先跟随 TLM 原生“语音合成”语言按钮，避免中文文本被送进英文 TTS；显式配置 `zh_cn`、`en_us`、`ja_jp` 等语言时，预生成 prompt 和 TTS 请求会使用该配置统一覆盖。
- 新增 `aiDialogueCacheConsumeOnUse` 配置，默认关闭缓存消费；早安吻触发会复用已生成台词和语音，不再因为播放而消耗缓存、反复补池，手动执行 `clear_ai_cache` 后才会重新预热生成。
- 修复 TMA MiMo TTS 适配器未把 `TTSConfig.language` 写入请求的问题，生成式早安吻 TTS 现在会在请求体和 voice prompt 中显式声明目标语言。
- 修复早安吻 AI 缓存统计提前结束进行中状态的问题：LLM 返回后若仍在等待 TTS 回调，`/tma morning_kiss cache` 会继续显示对应女仆的 in-flight 阶段和待完成语音数。
- 修复早安吻 AI 缓存目标只作为触发阈值、不作为写入上限的问题：当某个时间池接近目标值时，新增生成结果会按剩余容量裁剪，并且 TTS 回调最终入池时也会再次检查上限；默认每名女仆三个时间池总计最多 12 条，不会因不同语种缓存而扩容。
- 修复英文早安吻 AI 预生成结果被误判为过长而全部丢弃的问题：缓存文本过滤改为按显示宽度限制，英文短句可正常入池，过长中文仍会被过滤。

## [1.7.2.1] - 2026-05-02

Forge 1.20.1 分支的短期增强版。该版本汇总当前本地 6 个提交：`542d229`、`ac183a6`、`1427d3c`、`1036896`、`77d2352`、`03b8060`。

### Added
- 新增早安吻与残血救护的功能级数据包语音池：数据包可分别提供 `morning_kiss/voices/*.ogg` 与 `emergency_rescue/voices/*.ogg`，玩家可在羁绊页为每名女仆选择随机、顺序或指定语音项。
- 新增早安吻静态台词数据包配置，支持 `morning`、`evening`、`general` 台词池，以及 `{maid}`、`{player}` 占位符。
- 新增早安吻 AI 台词与 TTS 预生成链路：服务端可在非触发时段为已解锁早安吻的女仆生成台词与语音缓存，触发时优先消费缓存结果。
- 新增 TMA MiMo AI 适配器，为 Touhou Little Maid AI 设置页提供 `tma_mimo_chat` 与 `tma_mimo_tts` 站点类型，并在羁绊页提供 MiMo 设置入口。
- 新增准星目标女仆亲吻按键：玩家无需公主抱，也可通过按键请求服务端校验目标女仆后执行亲吻。
- 新增语音试听功能：早安吻与残血救护的语音池二级页支持可改键试听；数据包语音通过服务端预览 payload 下发字节，TLM 音包与内置音效走客户端本地索引播放。
- 新增 `examples/TMA-Custom-Voice-Pack` 示例数据包，可直接作为早安吻/残血救护自定义语音与文本的发布样例。

### Changed
- 早安吻 AI 运行时配置从数据包迁移到 `config/touhou_maid_affection-common.toml` 的 `morningKissBehavior` 段；数据包只负责静态台词、亲吻音效与预录 OGG 语音池。
- 残血救护预录语音链路移除旧的服务端/客户端文件同步服务，改为触发时通过 payload 下发命中的数据包 OGG 字节，或回退到 TLM 音包/兜底音效。
- 早安吻与残血救护语音二级页改为统一的动态语音池页面，服务端同步数据包候选项，客户端补充 TLM 音包候选项。
- 羁绊页顶部入口改为运行时推导可用 tab 位置，减少与 Touhou Little Maid 或其他扩展页签的冲突。
- 默认按键策略调整：公主抱亲吻与准星亲吻可共享默认键位，膝枕角度冻结默认不再占用该键位。
- MiMo LLM 适配改为独立 client 链路，并通过窄 mixin 保持 TLM 编辑器保存后的 `tma_mimo_chat` 站点类型不丢失。

### Fixed
- 修复膝枕角度冻结按键与亲吻入口同键时可能出现的误提示问题。
- 修复早安吻运行时 AI/TTS 失败时的回退策略，避免远程服务异常阻断静态台词或已有语音播放。
- 修复 MiMo TTS 音频响应解析与缓存格式边界，避免不可播放格式进入客户端播放队列。
- 修复 TLM 音包与内置音效试听时的流式播放兼容性，减少伪造 `SoundBuffer` 带来的格式边界问题。

### Removed
- 移除旧版救援语音资源同步 payload 与本地文件同步配置类，救援语音统一走当前的功能级数据包语音池。
- 移除数据包中的早安吻 AI 开关职责，避免静态资源包与运行时 AI 供应商状态互相耦合。

## [1.7.1.4] - 2026-04-06

### Changed
- 紧急救援链路从玩家 tick 轮询改为事件驱动：由伤害预处理与死亡回调触发，日刷新改为按需懒刷新，并抽离 `EmergencyRescueService` 统一编排。
- 建立统一变更记录规范：`CHANGELOG.md` 作为用户向更新历史主入口，README 不再内嵌历史版本日志。
- 客户端 YSM 路径归一化逻辑收敛为 `NamespacedPathNormalizer`，减少分散字符串解析。
- Modrinth 页面正文同步源切换为英文 README，保持对外发布页信息一致。
- 残血救护覆盖层视角默认偏航调整为 `180`（`overlayViewYawOffset`），默认朝向更贴合正面展示。

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
