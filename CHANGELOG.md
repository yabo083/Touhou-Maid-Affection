# Changelog

本文件记录用户可感知的功能变更与修复历史。

格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，分类使用：
- `Added`：新功能
- `Changed`：行为/架构变更
- `Fixed`：缺陷修复
- `Removed`：移除内容

## [1.7.5.0] - 2026-09-24

### Added
- 新增游戏内「全局设置」面板「语音」tab 的「台词提示词」多行编辑框：直接编辑早安吻 AI 台词模板（占位符 `{maid}` 女仆名 / `{player}` 玩家名 / `{pool}` 时段 / `{time}` 允许时段），带占位符图例与「恢复默认」按钮。这是服务端权威的新设置类型 `TEXT`（逻辑键 `morning_kiss.text_prompt`，上限 1024 字符，保存空值写回内置默认模板），沿用现有 OP2 + 审计日志的写入路径。字数计数画在编辑框**内部右下角**（次要文字色），占位符图例与「恢复默认」排成同一行（左说明 / 右按钮，长图例按可用宽度截断），「恢复默认」改用面板通用的文字按钮渲染（与「打开」「清空」同一个 `drawTextButton` 模板，无权限时置灰）。
- 新增设置面板「状态」tab：移植 `/tma morning_kiss status` 与 `/tma morning_kiss cache` 的只读信息——开关（早安吻 / AI 台词 / AI 语音 / 立即兜底）、语种（两行说人话：`文本语种` → 值，`auto` 时显示 `auto（跟随游戏语言）`；`配音语种` → 值，`auto` 时显示 `auto（跟随女仆 AI 设置）`）、缓存策略（每池目标 / 扫描间隔 / 消费即用）、缓存统计（条目语音/纯文本、女仆数 / 在途 / 版本号）与按女仆列表（名字 + 各时段池条目数 + 总条目/目标 + 行内「清空」）。配套新增只读状态通道 `TmaAiStatusRequestPayload` / `TmaAiStatusPayload` 与清缓存操作包 `TmaAiCacheClearPayload`：状态只回该玩家名下的女仆，清缓存需 OP2 + 审计日志并复用 `/tma morning_kiss clear_ai_cache` 完全相同的服务方法。
- 新增设置面板「语音」tab 的「打开车万女仆的 AI 设置」按钮：跳转 TLM 原生 `AIChatSettingsHubScreen`（parent 传本面板，关闭后回到这里），是全局唯一的 AI 入口；TMA 不自建站点表单。
- 新增游戏内「全局设置」面板：在羁绊页顶部右侧（原 TMA AI Hub 按钮的槽位）新增「设置」入口，面板与当前女仆无关，分三个区——功能开关（早安吻 / 女仆主动早安吻 / AI 台词 / AI 语音 / 残血救护 / 随机礼物 / 少女祈祷 Buff）、语种（文本语种 / 配音语种下拉框）、音量（亲吻音效 / 早安吻语音 / 残血救护 / 语音试听，0.0–1.0 步进 0.05）。无需再手改 toml。
- 新增服务端权威设置同步通道：`TmaSettingsRequestPayload`（C2S，空列表表示只读状态）与 `TmaSettingsStatePayload`（S2C，回推全部白名单键的当前值与 `canEdit`）。面板里的开关与语种由服务端校验、应用并回推，单人存档与多人服务器行为一致。
- 新增自绘滑块组件 `BondSlider`，供设置面板的音量项使用。

### Fixed
- 修复设置面板语种下拉框最后一项被裁剪的问题：展开的弹层不再受面板内容区 scissor 约束，改为夹在屏幕范围内——向下会溢出屏幕底部时翻到表头之上渲染，命中测试/高亮/点击与实际渲染位置保持一致，所有候选条目可达。
- `.maid` 迁移不再携带运行态/调度键：导出（`BondData.exportMaidData`）剔除 `BondKeys.RUNTIME_KEYS`——礼物计时（`RandomGiftLastWallClock` / `RandomGiftLastDelivery` / `RandomGiftLastIntervalMinutes`）、早安吻窗口标记（`MorningKissScheduledWindow` / `MorningKissScheduledAttemptTick` / `MorningKissLastAutoAttemptGameTime` / `MorningKissLastSuccessWindow` / `MorningKissLastFailedWindow`）与本地记账 `LastSeen`——导入时再防御性剔一遍。此前这些会话/世界相关的绝对时间会随 `.maid` 迁到新女仆/新存档，导致刚导入就被判定「今天已亲过」或礼物计时错乱。待发礼物队列 `RandomGiftQueue` 是耐久状态，仍然随迁；`extras` 对外形状与整体替换语义不变。
- 空字符串不再落盘/导出：所有「getter 缺省值本就是空串」的字符串 setter（声音包、YSM 档案、救护动作、膝枕动作、早安吻计划/窗口标记、玩家粒度早安吻选择、早安吻与救护语音选择里的空字段）改为空串时移除键而非写入空值，避免无意义键堆积进存档与 `.maid`。

### Changed
- 设置面板尺寸 300×188 → 340×230，侧栏 tab 从 3 个（功能 / 语音 / 音量）扩到 4 个（新增「状态」）；内容超出可视区走内部滚动。配色 token、玫瑰藤蔓锚点（`modal().bottom() - NAV_VINE_HEIGHT`）、服务端权威语义与既有状态机不变。
- 羁绊页「设置」按钮从左上空位移到面板右上角（原 TMA AI Hub 按钮的槽位）：尺寸 50×12，位置 `x = panelX + panelWidth - 50 - 2`、`y = panelY - 12 - 3`（即原 AI 按钮的 Y），文案仍是「设置」、居中绘制；命中测试与 tooltip 同步更新，左上空位不再有按钮。
- 音量上限从 `4.0` 收至 `1.0`：四项音量配置（`cooldown.kissSoundVolume`、`morningKissBehavior.voiceVolume`、`emergencyRescueBehavior.volume`、`voicePreview.volume`）与设置面板滑块现在**只做衰减**——`0.0` 静音、`1.0` 保持原有响度，不再放大。需要更大音量请使用 Minecraft 或系统音量。旧配置里大于 `1.0` 的值会被配置系统在加载时纠正为 `1.0`（Forge `defineInRange` 行为）。
- 音量四项（`cooldown.kissSoundVolume`、`morningKissBehavior.voiceVolume`、`emergencyRescueBehavior.volume`、`voicePreview.volume`）是纯客户端配置，面板里拖动即写入本机配置并立即生效，不走网络。
- 语音试听冷却从 5 秒降到 0.5 秒（100 → 10 tick）；被限流时给客户端一行 action bar 提示「试听过快，请稍候」，不再静默丢弃；非数据包（本地）试听请求不再占用限流额度。
- 早安吻 AI 台词缓存改为按语种读取：`pollRandom` / `peekRandom` 与预热满度判定（`countMatching`、`addIfBelowTarget`）都按当前解析出的显示/配音语种过滤，改了显示或配音语种后会自动按新语种重新预热，旧语种条目保留但不再被选中，也不再阻塞重新预热（仍可手动 `clear_ai_cache`）。目标语种为空（`auto`）时不过滤，保持旧行为。
- `/tma morning_kiss status` 的语种行改为两行说人话：`文本语种：<值>`（未配置显示 `auto`）、`配音语种：<值>`（未配置显示 `auto`），删掉 AI 覆盖与生效优先级两行。
- 早安吻语种语义统一为两个：**文本语种**（`displayLanguage`，内置数据包台词 + AI 生成台词用哪种语言显示；`auto` = 跟随游戏语言，内置台词按客户端语言渲染、未标记数据包台词通配、AI 台词沿用女仆的 TLM 聊天语言）与**配音语种**（`voiceLanguage`，数据包语音 + AI 合成语音用哪种语言；`auto` = 跟随女仆的 TLM AI 语言设置，数据包语音不按语言过滤、AI 合成沿用女仆的 TLM TTS 语言）。具体 locale（`zh_cn` / `ja_jp`…）固定该语言。旧关键字 `tlm` / `inherit` / `default` 继续接受，等价于 `auto`，但面板与状态页一律显示归一化后的 `auto`。
- 全局羁绊 UI 配色统一为「暖木底 + 玫瑰（交互面 / 装饰）+ 金（仅高亮文字）」：`BondGuiTokens` 换用新调色板（保留原常量名，新增开关 / 字段 / 滑块 / 导航等语义色常量），所有调用点自动跟随。设置面板同步按设计稿收口：300×188 模态框、46px 侧栏三 tab、胶囊开关、78×20 字段式下拉、88×13 滑块（数值金色居中）、分区作用域标签、侧栏藤蔓装饰（`rose_vine.png`，素材 132×165、按 44×55 绘制，水平居中于导航轨、茎根落在面板底边上，整株在面板内）、行内请求状态点与「完成 / 重载」footer。不影响功能与其它二级页布局尺寸（仍为 172×150）。
- 设置面板从「女仆 GUI 内嵌二级页」改为**独立 Screen**（`TmaSettingsScreen`，删除 `SettingsSecondaryPage`）：在羁绊页「设置」按钮处用 `Minecraft#setScreen` 打开，自带全屏压暗背景，关闭（footer「完成」/ESC/点击压暗区）返回来源的女仆 GUI。面板尺寸/配色/控件与交互 token 不变，服务端权威语义、payload、状态机与音量直写零变更；除设置页外的其它二级页与 `BondSecondaryPage` 接口不受影响。
- 设置面板语种从 4 项砍到 2 项：白名单变为 **7 开关 + 2 语种 = 9 项**，面板只保留「文本语种」（`display_language`）与「配音语种」（`voice_language`），两者统管内置数据包与 AI 两条链路。
- 羁绊数据改为**按女仆嵌套存储**：女仆粒度数据从「根上扁平键 `<base>_<女仆UUID>`」改为 `touhou_maid_affection.bond.maids.<女仆UUID>.<base>` 子树，玩家粒度键（早安吻选择）仍在根上；键名常量集中到 `BondKeys`。首次读取旧存档时自动执行**一次性迁移**（根上新增 `SchemaVersion`，先写后删、幂等、无法解析的键原样保留），旧存档无损升级，无需手动操作。反查（按能力找女仆、按模型 ID 找女仆、按救护 provider 找女仆、批量重置能力）改为遍历女仆子树，不再全键扫描。
- 新增 `/tma bond prune [days]`（默认 90 天，权限等级 2）：清理执行者羁绊数据中 `LastSeen` 早于阈值的女仆子树（缺失 `LastSeen` 的历史数据视为过旧；`days <= 0` 只统计不删除），并回显删除/保留数量。`LastSeen` 在同步女仆档案时刷新；**不**在女仆死亡 / 卸载 / 换主人时自动删除数据（TLM 灵魂玩偶、椅子等会临时移除实体，自动删会丢数据），只能靠该命令显式清理。
- `.maid` 迁移（MaidFileManager SPI）的附加数据对外格式**不变**：仍是「base 名 → 值」的 compound（即女仆子树本身），旧导出文件仍可导入，导入后刷新 `LastSeen`。
- 删除若干零调用者死代码：`BondData/BondManager.getUnlockedMaidModelIdsForAbility`、`BondData/BondManager.findMaidProfileByRescueProviderId`、`MorningKissGeneratedDialogueService.hasCachedLine`（两个重载）、`MorningKissGeneratedDialogueCache.isEmpty`，以及已被 `BondKeys` 取代的 `compat/maidfm/MaidDataKeyCodec`。

### Removed
- 删除 AI 专用语种覆盖层：`ModConfig` 的 `morningKissBehavior.aiDialogueLanguage` / `aiDialogueVoiceLanguage` 两项（含 toml 键 `aiDialogueLanguage` / `aiDialogueVoiceLanguage`）与解析链里的优先级逻辑（`MorningKissLanguageSettings#liveChatLanguage` 的「AI 显式 > 全局显式 > 旧语义」判定、`resolveGeneratedTextLanguage` / `resolveGeneratedVoiceTextLanguage` 的 AI 专用入参与配音继承显示语种的规则）。若此前用 AI 覆盖让 AI 台词/配音与内置台词/语音用了不同语言，现在统一由**文本语种**与**配音语种**控制——把原来的值填到这两项即可（例：原来 `aiDialogueLanguage=zh_cn` + `aiDialogueVoiceLanguage=ja_jp`，现在填 `displayLanguage=zh_cn` + `voiceLanguage=ja_jp`）。旧 toml 里残留的这两个键不再被定义，配置系统直接忽略，无需手动清理。
- 删除 TMA 自研的 AI 站点适配层：`com.github.touhoumaidaffection.ai.mimo` 整包（`TmaMimoAdapterExtension` provider 注册、`MimoLLMSite` / `MimoTTSSite` 站点类型与 serializer、`MimoTTSFormLayout` 站点表单、`MimoLLMClient` / `MimoTTSClient` / `MimoHttp` / `MimoProtocol` / `MimoCodecHelper` 协议实现、`BoundedHttpClient` / `BoundedHttpResponse` 有界响应工具）及其单元测试（`MimoProtocolTest` 7 例、`BoundedHttpResponseTest` 3 例）。
- 删除仅供 MiMo 站点编辑器使用的 `mixin/client/LLMSiteEditorScreenMixin`（1.20.1 分支独有的窄 mixin，用于在 TLM 站点编辑器保存后保留 `tma_mimo_chat` 类型）及其在 `touhou_maid_affection.mixins.json` 中的注册。
- 删除 `ModConfig` 的 `tmaMimoAdapter` 配置段：`enabled` / `apiKey` / `chatUrl` / `ttsUrl` / `maxCompletionTokens` / `ttsVoicePrompt` / `ttsAudioFormat` 七项。旧 toml 里残留的 `[tmaMimoAdapter]` 键不再被定义，配置系统会直接忽略，**无需手动清理，也没有迁移代码**。
- 删除中英文文案中的 MiMo 站点名与入口文案（各 6 个键）。
- 羁绊页顶部不再有任何 AI 入口：原右上角「TMA AI Hub」按钮整条删除（含字段、渲染、命中测试、tooltip、`BondPrimaryPageHost#openMimoAdapterSettings` / `isMimoAdapterAvailable` 及其实现与 TLM AI hub 跳转）。AI 相关配置今后由 TMA 自己的设置面板承担。
- 早安吻的 LLM/TTS 本就完全走 `maid.getAiChatManager()` 里 TLM 自己的站点，删除适配层不影响任何功能；TMA 也不再提供全局音色提示词（音色由 TLM 站点配置决定，GPT-SoVITS 站点另有其原生 prompt 字段）。
- 旧站点条目处理：TLM 读取 `config/touhou_little_maid/sites/{llm,tts}.json` 时，`api_type` 找不到对应 serializer 的条目只记一条 error 日志并跳过（**不抛异常、不崩溃**），随后 TLM 保存站点时把该条目从文件里清掉。**实测**：删掉适配层后启动服务器，日志出现 `Unknown LLM site type: tma_mimo_chat` 与 `Unknown TTS site type: tma_mimo_tts`，服务器正常完成启动，且 `tma_mimo_chat` / `tma_mimo_tts` 两个条目已从两个 json 中消失——玩家无需手动删旧站点条目。

### Notes
- 权限：开关与语种属于服务端权威设置，**只有 OP（权限等级 2）可以修改**；普通玩家能看、不能改，界面底部会显示「只读」提示，按钮 tooltip 提示需要管理员权限。每次成功修改都会在服务端日志打印 `[TMA Settings] player=... key=... old=... new=...`；越权或非法请求打印 WARN。
- 非法请求（未知 key、非法布尔、非法语种、超长值）整包拒绝，不会部分生效。
- 语种下拉框只提供 `auto` 与常见 locale，当前值若不在列表里会动态补上；旧关键字 `tlm` / `inherit` / `default` 仍是合法值（服务端原样存储、语义等价 `auto`），但面板与状态页显示归一化后的 `auto`，不再作为候选条目出现。

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
