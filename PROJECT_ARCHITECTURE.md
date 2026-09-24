# PROJECT_ARCHITECTURE

## 1. 项目定位

`Touhou Maid: Affection` 是 Touhou Little Maid 的 NeoForge 扩展模组。它不替代 TLM 的女仆、音包、模型或 AI 系统，而是在这些能力之上增加一层“以特定女仆与特定玩家关系为中心”的亲密互动与羁绊成长系统。

当前主闭环是：

- 以亲吻作为基础互动入口，提供好感提升、冷却、镜头、粒子、音效与按键入口。
- 以羁绊系统承载长期关系状态，并逐步解锁膝枕、早安吻、残血救护、随机礼物等能力。
- 以服务端为权威状态来源，客户端只负责缓存、界面、音频和视觉表现。
- 以数据包、TLM 音包、YSM 动作与 TLM AI 站点形成可选增强，缺失时应降级而不是中断主流程。

## 2. 技术栈与发布约束

- Java `21`
- Gradle 单模块工程
- Minecraft `1.21.1`
- NeoForge `21.1.x`
- Touhou Little Maid `1.5.1+`，当前编译目标为 `1.5.3-neoforge+mc1.21.1`（1.5.3 相对 1.5.2 在被引用的 49 个类中 44 个成员集合完全一致、4 个仅新增成员、5 个 mixin 目标类签名逐字节一致）
- Parchment mappings `2024.11.17`
- Mixin 用于少量 TLM GUI、诱饵与膝枕渲染桥接
- Modrinth Minotaur 用于发布任务

发布版本源是 `gradle.properties` 中的 `mod_version`；GitHub Actions 由 `v*` tag 触发 release。发版时 tag、changelog 与 `mod_version` 必须一致。

## 3. 目录拓扑

```text
src/main/java/com/github/touhoumaidaffection
├─ TouhouMaidAffection.java
├─ ModConfig.java
├─ ModAttachments.java / ModEffects.java / ModSounds.java
├─ bond
│  ├─ BondData.java / BondManager.java
│  ├─ BondKeys.java / BondDataMigration.java / BondRetention.java
│  ├─ VoicePoolIds.java / VoicePoolSelection.java
│  ├─ ability
│  ├─ lap
│  ├─ rescue
│  └─ service
├─ client
│  ├─ Kiss* / MorningKiss* / EmergencyRescue*
│  └─ screen
│     ├─ component
│     └─ page
├─ command
├─ compat/maidfm
├─ effect
├─ handler
├─ inventory
├─ mixin
├─ network
├─ util
└─ ysm

src/main/resources
├─ META-INF/neoforge.mods.toml
├─ touhou_maid_affection.mixins.json
├─ assets/touhou_maid_affection
│  ├─ lang
│  ├─ sounds
│  └─ textures
└─ data/touhou_maid_affection
   ├─ morning_kiss/profile.json
   ├─ emergency_rescue/profile.json
   ├─ rescue_sound/profile.json
   └─ tags/item
```

`examples/TMA-Custom-Voice-Pack` 是发布给用户的示例数据包，不参与模组运行时资源加载，但必须与数据包解析器保持格式一致。

## 4. 模块职责

### 4.1 启动与注册层

`TouhouMaidAffection.java` 负责配置、注册表、payload、事件和 tick 入口的装配。它是启动门面，不应承载具体业务判定。

`ModConfig.java` 只描述全局规则和默认供应商参数，不保存玩家或女仆运行结果。亲吻冷却、好感收益、亲吻音效音量、随机礼物池策略、残血救护绝对/百分比阈值、早安吻语音音量、残血救护音量、语音试听音量、早安吻 AI/TTS 的运行时开关、提示词、显示/配音语言、扫描频率与缓存策略都在这里定义。

### 4.2 亲吻主链

`KissMaidHandler` 是亲吻服务端主入口，负责冷却、好感、亲吻音效播放、粒子 payload、少女祈祷触发与早安吻复用逻辑。公主抱亲吻按键和准星目标亲吻按键最终都收敛到这里，避免规则分叉；潜行空手右击入口不再拦截 TLM 女仆交互。亲吻音效响度由全局配置控制，早安吻数据包仍只负责选择 sound event。

客户端的 `KissKeyAction` 在公主抱亲吻和准星亲吻共用按键时做入口选择；服务端的 `KissTargetedMaidRequestHandler` 必须重新校验实体、距离、视线与归属，不能信任客户端命中结果。

### 4.3 羁绊域模型

`BondData` 保存玩家维度、女仆粒度的长期档案：羁绊等级、解锁能力、语音选择、早安吻计划、礼物队列、膝枕姿态等。

存储布局：数据挂在主人玩家 persistentData 的 `touhou_maid_affection.bond` 根 compound 下，**女仆粒度数据按女仆嵌套**在 `maids.<女仆UUID>.<base>` 子树里，**玩家粒度数据**（`MorningKissSelectedWindowId` / `MorningKissSelectedMaidId`）留在根上。所有 base 名常量集中在纯逻辑类 `BondKeys`，不再散落字面量。

**画像 vs 运行态**：`BondKeys.RUNTIME_KEYS` 集中列出会话/世界相关的运行态与调度键——礼物计时（`RandomGiftLastWallClock` / `RandomGiftLastDelivery` / `RandomGiftLastIntervalMinutes`）、早安吻窗口标记（`MorningKissScheduledWindow` / `MorningKissScheduledAttemptTick` / `MorningKissLastAutoAttemptGameTime` / `MorningKissLastSuccessWindow` / `MorningKissLastFailedWindow`）与本地 prune 记账 `LastSeen`。它们都是「上次何时发生」的绝对挂钟毫秒或游戏刻，只在产生它的会话/存档里有意义，因此**不参与 `.maid` 迁移**（详见 4.9）。待发礼物队列 `RandomGiftQueue` 不在其中：那是耐久状态，属于画像，随女仆迁移。空字符串值也不落盘/不导出（这些键的 getter 缺省值本就是空串，不写入与写空串读取等价）。

根上的 `SchemaVersion` 记录存储结构版本（当前 `2`）。`BondData.of(player)` 每次读取都会检查一次；版本缺失或 `< 2` 时由纯逻辑类 `BondDataMigration` 执行一次性迁移：遍历根上的键，凡能解析为 `<base>_<女仆UUID>` 的旧扁平键，**先**把值写入 `maids.<uuid>.<base>`、**成功后再**移除旧键（先写后删，中途失败不丢数据）；玩家粒度键与无法解析的键原样保留；迁移完成后写入 `SchemaVersion=2`，因此**幂等**且可安全重入。旧存档无损升级，不需要任何手动步骤。

生命周期：`maids.<uuid>.LastSeen`（epoch millis）在 `BondManager.syncMaidProfile` 时刷新。**不会**在女仆死亡 / 卸载 / 换主人时自动删除数据（TLM 的灵魂玩偶、椅子等场景会出现临时移除，自动删除会丢数据）；残留数据由显式的 `/tma bond prune [days]`（默认 90 天，权限等级 2）清理：删除 `LastSeen` 早于阈值的女仆子树，缺失 `LastSeen` 的历史数据视为过旧一并删除；`days <= 0` 表示只统计不删除。阈值判定抽在纯逻辑类 `BondRetention` 中，便于单元测试。

`BondManager` 是语义化门面，屏蔽底层 persistentData key。后续新增持久字段应集中在 `BondData` 或相关子域数据结构中，避免 handler 或 screen 直接拼 key；新增 base 名一律加到 `BondKeys`。

`bond/ability` 的能力对象只描述成本、名称、解锁条件与二级行为入口。复杂流程应放到 `bond/service`、`bond/rescue` 或 handler 中。

### 4.4 长流程服务

`bond/service` 承载 tick 驱动或跨时间窗的服务：

- `MorningKissService`：早安吻时间窗调度、寻路和亲吻任务推进。
- `MorningKissDialogueService`：生成缓存、即时 AI、数据包台词与内置台词之间的回退链，以及聊天气泡/聊天栏/动作栏显示策略。
- `MorningKissVoiceService`：每名女仆的语音池解析、顺序/随机选择、数据包/TLM 回退和客户端播放 payload 分发。
- `MorningKissGeneratedDialogueService`：基于 TLM LLM/TTS 站点异步预生成台词和 TTS 音频；维护 MAID_REVISIONS 实现女仆级缓存失效，支持 LLM、跨语言翻译与 TTS 三阶段 IN_FLIGHT 跟踪，在服务器启动时从磁盘恢复缓存、关闭时持久化。显示语言与配音语言不同时，先生成显示文本，再用单次批量翻译保持逐行映射，最后以 `tts_text` 请求 TTS；翻译失败则安全降级为纯显示文本。
- `MorningKissGeneratedDialogueLanguage`：早安吻 AI 显示/配音语言的纯逻辑归一化、继承规则、翻译 prompt 与严格 JSON 数组解析。支持 `inherit`、`tlm/auto/default` 和显式语言代码。
- `MorningKissGeneratedDialogueCache`：保存女仆粒度的运行时生成台词、独立 `tts_text` 和 TTS 语音缓存。支持消耗/非消耗两种取出模式（CACHE_CONSUME_ON_USE），提供女仆级、池级、条目级的清理与语音剥离操作。缓存容量受 maxLinesPerPool 和 aiDialogueCacheTargetPerPool 双重约束。实现 snapshot() / replaceAll() 接口以支持磁盘持久化。统计报告通过 stats() 按女仆和语言分组输出。读取（`pollRandom` / `peekRandom` / `hasCachedLine`）与满度判定（`countMatching`、`addIfBelowTarget`）均按当前解析出的显示/配音语种过滤，旧语种条目不再阻塞重新预热（条目保留，可手动清理）。
- `MorningKissGeneratedDialogueStorage`：将运行时 AI 生成缓存持久化到 `world/generated_morning_kiss/{maid_uuid}/{pool}/` 目录下，每条条目写为 `001.json`（元数据）+ 同编号 `.ogg`/`.mp3`（语音），格式兼容手动编辑。路径遍历防护通过 `normalize()` + `startsWith()` 检查实现。服务器启动时自动加载、服务器关闭时自动保存。
- `MorningKissProfileParser` / `MorningKissProfileData`：读取早安吻静态数据包 profile。
- `InteractionVoiceProfileParser` / `InteractionVoiceProfileData`：早安吻与残血救护共享的数据包 OGG 语音解析。
- `RandomGiftService`：随机礼物积累与投递。默认礼物来源是显式物品标签池；广泛注册表抽样是可选兼容模式，仅默认过滤破坏沉浸感的技术/管理物品。显式礼物池可覆盖默认过滤，黑名单仍具有最终否决权。

早安吻的架构边界非常明确：数据包负责静态台词、亲吻 sound event、预录 OGG 语音；全局配置负责亲吻 sound event 响度与早安吻语音响度；AI/TTS 运行时行为负责配置、生成、持久化缓存、清理和失败回退。`/tma morning_kiss clear_ai_cache` 会同时清理内存与世界目录中的生成缓存，不改变数据包或 BondData。语言变更后无需手动清理：缓存读取与满度判定均按当前解析出的显示/配音语种过滤，会自动按新语种重新预热（旧语种条目保留，可手动清理）；提示词变更仍需手动清理。新增的 `aiDialogueCacheConsumeOnUse` 配置允许管理员选择消耗或复用缓存条目以平衡 LLM/TTS Token 成本与体验。

### 4.5 残血救护

`bond/rescue` 管理紧急救援触发、每日救援次数、救援者身份 canonical id 与 provider/legacy 兼容。救援语音不再走旧的服务器文件同步服务，而是在触发 payload 中携带命中的数据包 OGG 字节，或回退到 TLM 音包与兜底 sound event。

`EmergencyRescueSoundPlayer` 只处理客户端播放策略，不决定救援是否成立。数据包语音、TLM 音包语音和兜底 sound event 的响度统一服从残血救护全局音量配置。

### 4.6 膝枕

`bond/lap` 管理膝枕会话状态、锚点实体与姿态快照。膝枕是“服务端维持会话 + 客户端渲染桥接”的状态机：

- 长期配置落在 `BondData`。
- 当前会话落在 `LapPillowState` 与锚点实体。
- 客户端 mixin 只做渲染期睡姿/乘骑桥接，不改变服务端业务真相。

膝枕角度冻结通过独立 payload 保存会话姿态，默认键位不再与亲吻入口抢占。

### 4.7 客户端 UI

`BondMaidContainerScreen` 是羁绊页总屏幕；`screen/page` 承载一级/二级页控制；`screen/component` 提供按钮、滚动列表、弹窗、下拉框、语音池列表等复用组件。

`BondGuiTokens` 是所有羁绊 UI 的唯一配色与尺寸来源（业务代码不得硬编码 ARGB）：暖木色为面板/控件底，玫瑰色（`COLOR_ACCENT`、`*_ON_*`、`*_SELECTED_*`、`PRIMARY_BUTTON_*`）只作交互面与强调，金色（`HIGHLIGHT_TEXT`、`COLOR_TEXT_SELECTED`、`TAG_SERVER`）只作高亮文字与作用域标签。`textures/gui/rose_vine.png`（132×165 RGBA，高清原图裁剪，按 44×55 缩放绘制）是纯装饰资源，只在设置面板侧栏底部绘制（茎根落在面板底边上、整株在面板内），不接收鼠标事件。

语音配置页现在是动态语音池页面：服务端同步数据包候选，客户端补充 TLM 音包候选。玩家保存的是每名女仆的池选择与播放模式，而不是全局固定文件名。早安吻与残血救护语音列表都支持试听：本地内置亲吻音效试听跟随亲吻音效音量，数据包/TLM 语音试听跟随语音试听音量。数据包语音通过服务端校验后把目标字节发送回客户端播放。TLM 音包试听同样读取原始音频字节，但使用专用 preview stream：以 `minecraft:music.menu` 作为稳定声音事件锚点，走 `PLAYERS` 音量分类，并关闭位置衰减，避免右键试听依赖 TLM 音包自身的 sound event 注册状态或玩家的环境音量设置。TLM 音包实际播放仍由功能流程创建跟随女仆或触发点的流式 SoundInstance，避免把 Opus/Vorbis 兼容性压到 `SoundBuffer` 旧链路上。所有内存 OGG/MP3 字节统一由 `InMemoryVoiceStream` 异步解码，跟随实体的语音统一复用 `TrackedEntityVoiceSoundInstance`，但各场景仍保留自己的声源分类、位置衰减和锚点策略。

`BondMaidGuiTabHandler` 不固定占用 TLM 顶部 tab 位置，而是运行时扫描可用位置，降低与 TLM 或其他扩展页签冲突。

`TmaSettingsScreen` 是与女仆无关的全局设置面板，作为**独立 Screen** 从羁绊页顶部右侧（原 TMA AI Hub 按钮槽位，50×12）的「设置」按钮进入（`BondPrimaryPageHost#openSettingsPage` 内部改为 `Minecraft#setScreen(new TmaSettingsScreen(this))`，不走 `BondSecondaryPageRegistry` 的能力页流程）：面板不再嵌在女仆 GUI 内，而是自带全屏压暗背景的独立窗口，关闭（footer「完成」/ESC/点击压暗区）时 `setScreen` 回来源界面。模态框比其它二级页宽且高（340×230，`BondGuiTokens.SETTINGS_MODAL_WIDTH` / `SETTINGS_MODAL_HEIGHT`）：左侧 46px 导航轨按「状态 / 功能 / 语音 / 音量」四个 tab 切换单区内容（「状态」在第一位且打开面板默认选中），导航轨内一条装饰藤蔓（`textures/gui/rose_vine.png`，素材 132×165、按 44×55 绘制、水平居中于导航轨、茎根落在面板底边上且整株在面板内）。它复用 `BondModalPage` / `BondDropdown` / `BondGuiTokens`，并新增自绘 `BondSlider`（88×13，数值金色居中）与胶囊开关。开关、语种与缓存策略是**服务端权威**项，走 `TmaSettingsRequestPayload` / `TmaSettingsStatePayload`，点击即时发包；音量是纯客户端项，直接写 `ModConfig` 并 `SPEC.save()`。行内状态点不依赖任何协议扩展：客户端记录 `pending`（key→请求值），收到状态回推后逐个比对——相等即「已保存」（不画点），不等即「被拒绝」（红点约 3 秒后自动清除），超过 5 秒仍无回推按超时视为被拒绝；存在请求中/被拒项时 footer 的「完成」左侧出现纯文字「重载」（清本地标记并 `requestSync()`）。白名单为 **9 个开关 + 2 个语种 + 1 个自由文本 + 2 个整数 = 14 项**：面板语种只暴露「文本语种」（`morning_kiss.display_language`）与「配音语种」（`morning_kiss.voice_language`）；`morning_kiss.text_prompt` 是早安吻台词模板（`Type.TEXT`，上限 1024 字符，空值写回内置默认）；新增 `Type.INT`（只接受纯数字，逐键按 `ModConfig` `defineInRange` 的上下界校验，越界/非数字整包拒绝）：`morning_kiss.cache_target_per_pool`（1..8）与 `morning_kiss.cache_scan_interval_ticks`（20..72000）；AI 专用语种 `morningKissBehavior.aiDialogueLanguage` / `aiDialogueVoiceLanguage` 仍是有效的 toml 配置（AI 语言解析逻辑不变），但已不再出现在面板白名单里，需要时请手改 toml。布局参数集中在页面顶部常量，内容区可滚动（下拉框与滑块通过 `setPosition` 跟随滚动偏移）；展开的下拉弹层不做面板内容区裁剪，而是夹在屏幕范围内——向下会溢出屏幕底部时翻到表头之上渲染，命中测试/高亮/点击与实际渲染位置一致，所有条目可达。

「语音」tab 在语种下拉框下方增加「台词提示词」区：一个原版 `MultiLineEditBox` 编辑 `morning_kiss.text_prompt`（模板占位符 `{maid}` 女仆名 / `{player}` 玩家名 / `{pool}` 时段 / `{time}` 允许时段），下方整行是占位符图例（占满内容区整宽、不再与按钮同行，因此中英双语都完整显示不被截断；「恢复默认」文字按钮移到上方标签行右对齐，复用 `drawTextButton` 通用模板，无权限置灰）（把模板写回 `ModConfig.BOND_MORNING_KISS_AI_DIALOGUE_PROMPT` 的内置默认；客户端用 `getDefault()` 本地解析默认串，使 pending 比对能匹配服务端回推）。编辑框内右下角显示字数计数（次要文字色）：`MultiLineEditBox` 自带的计数画在框下方会与图例行重叠，因此这里关闭原版字符上限（`Integer.MAX_VALUE`），由面板自绘计数并在每次按键/提交前把长度夹到 `TmaSettingsKeys.MAX_TEXT_LENGTH`。提交时机为编辑框失焦（点击别处 / 切换 tab / 关闭面板），聚焦期间服务端回推不覆盖输入。该 tab 底部「AI 站点」区只有**唯一**一个 AI 入口按钮「打开车万女仆的 AI 设置」，跳转到 TLM 原生 `AIChatSettingsHubScreen.openDefault(this, AvailableSites.LLM_SITES, AvailableSites.TTS_SITES, false)`，parent 传本面板，关闭后回到这里：站点 / 密钥 / 模型 / TTS 音色全部由 TLM 原生页面配置，TMA 只提供跳转，不自建站点表单。该 tab 的全部元素（语种两行 + 台词提示词区 + AI 站点区）按**放宽后的间距常量**排版（`SECTION_HEADER_HEIGHT` 15、`ROW_GAP` 6、`LANGUAGE_ROW_HEIGHT` 20、`PROMPT_BOX_HEIGHT` 49、`PROMPT_ROW_HEIGHT` 14），总高 208px > 内容可视高 168px（230 模态高 − 24 标题 − 8 内容上留白 − 30 footer），因此打开后按需**内部滚动**（间距放宽后不再追求「免滚动」，功能/状态/语音三个 tab 超出可视高一律走面板滚动）。编辑框高度取 `9 × 行数 + 4`（49 = 5 行 + 内边距），子类 `BondPromptBox` 把滚动步长与滚动上限都吸附到 9px 行网格（`scrollRate()` = 9、`getMaxScrollAmount()` = `9 × (总行数 − 可见行数)`、`setScrollAmount()` 吸附到最近整行），因此文字永远按整行显示、不会从行中间被切断；光标可见性沿用原版 `MultilineTextField` 的 cursor listener 自动滚动，因为每次偏移都经过吸附，自动滚动同样落在行网格上。**滚轮事件的区分规则**：`TmaSettingsScreen#mouseScrolled` 先判断指针是否落在编辑框矩形内（`promptBox.isMouseOver(...)`），是则把事件转发给 `promptBox.mouseScrolled(...)` 并 `return true`（只滚框内文本、不再冒泡到面板），否则才走原有逻辑滚动面板；编辑框只占「语音」tab 的一小块矩形，框内滚动与面板滚动互不干扰。

「状态」tab 是**纯只读**视图（导航第一位、打开面板默认选中），把 `/tma morning_kiss status` 与 `/tma morning_kiss cache` 的信息原样搬进面板：开关（早安吻 / AI 台词 / AI 语音 / 立即兜底）、语种（`文本语种` / `配音语种`，未配置显示 `auto（跟随游戏语言）` / `auto（跟随女仆 AI 设置）`）、缓存策略（每池目标条数 / 扫描间隔 / 消费即用）、缓存统计（条目总数与语音/纯文本拆分、女仆数 / 在途请求数 / 版本号 revision）与按女仆列表（名字 + 各时段池条目数 + 该女仆总条目/目标 + 行内「清空」按钮）。这些行一律用 `addStatusKv` 渲染成纯文本（开关与「消费即用」显示「开 / 关」；没有胶囊开关、没有下拉框、没有数值框、也没有 pending 状态点），整页唯一可点的只有按女仆行的「清空」按钮。只读行的值与女仆行文本一律按「控件左边界 − 间距」用 `clip(...)` 截断并补省略号（名字列与计数列分列右对齐），文本不会压到「清空」按钮下面。对应的**可编辑**控件全部集中在「功能」tab：8 个布尔开关按白名单顺序排列，末尾是「缓存策略」区（`bond.settings.section.cache_policy`），放 `每池目标条数` / `扫描间隔` 两个整数（紧凑数值控件 `BondNumberField`：单行 `EditBox`、只允许数字、失焦提交、提交前按 `TmaSettingsKeys.intBounds` 夹取、右侧显示单位如 `1200t`；范围 20..72000 用滑块精度不可用，故不做滑块）与 `消费即用` 胶囊开关；`morning_kiss.cache_consume_on_use` 被显式排除在通用布尔列表之外（`TOGGLE_KEYS` 过滤），保证每个键在整个面板里只有一个控件、不会重复出现。数据来源：只读行来自 AI 状态通道（`TmaAiStatusPayload`，开关 / 语种 / 缓存策略是服务端 `ModConfig` 的生效值），缓存统计来自 `MorningKissGeneratedDialogueService.cacheStats()`，按女仆列表用 TLM 的 `MaidBackupsManager.getMaidIndexMap(player)` 限定为**该玩家名下**的女仆（名字取自备份索引，缺失时回退缓存标签）。按女仆行里每个时段池的计数标签取自 `bond.settings.status.pool.<池名>`（`DialoguePool` 的 `MORNING`/`EVENING`/`GENERAL` 三个值都必须有中英键，缺失会渲染成原始 key；`TmaSettingsLangKeysTest` 逐值断言中英 lang 都存在这些键）。footer 为「刷新 / 清空全部 / 完成」：刷新重发 `TmaAiStatusRequestPayload`；清空全部与行内清空走 `TmaAiCacheClearPayload`（scope = ALL / MAID），服务端要求权限等级 2、打印审计日志，并**复用与 `/tma morning_kiss clear_ai_cache` 完全相同的** `MorningKissGeneratedDialogueService.clearCache(...)` 方法，随后回推一份新状态，所有打开的状态 tab 自动收敛。

### 4.8 AI 集成

早安吻的 LLM/TTS 一律使用车万女仆自己的 AI 站点（TLM 原生支持类 OpenAI 站点）；TMA 不再注册自己的 provider，也不提供站点表单。语音音色由 TLM 站点配置决定（voice/model；GPT-SoVITS 站点另有其原生的 prompt 字段）：

- 早安吻的台词生成与 TTS 请求全部走 `maid.getAiChatManager()` 内 TLM 自己的站点选择，TMA 不介入协议层。
- 羁绊页不再提供 AI 入口按钮：原右上角按钮已整条删除；全局唯一的 AI 入口是设置面板「语音」tab 的「打开车万女仆的 AI 设置」按钮，它只是 `setScreen` 到 TLM 原生 `AIChatSettingsHubScreen`，站点表单仍由 TLM 提供。
- API key、启用状态与站点保存全部由 TLM 管理（`config/touhou_little_maid/sites/*.json`）。
- 旧版 TMA 注册过的 `tma_mimo_chat` / `tma_mimo_tts` 站点条目在 TLM 读取时因缺少对应 serializer 被跳过（仅记 error 日志），随后 TLM 保存站点时即被清除；无需玩家手动删除。

### 4.9 兼容层

`ysm`、`mixin`、`compat/<mod>` 与小型 helper 是外部生态适配的边界。与 YSM、CarryOn、TLM GUI、TLM 音包、TLM AI 的适配逻辑应保持隔离，不能扩散成到处可见的条件分支。

`compat/maidfm` 是对 MaidFileManager（女仆档案管理器，modid `maid_file_manager`）迁移 SPI 的适配边界，为**软依赖**：未安装管理器时行为与之前完全一致。

- **契约**：`BondMaidMigrationProvider` 实现管理器的 `MaidMigrationProvider`，把 TMA 唯一「挂在女仆身上但不在女仆实体 NBT 内」的数据——主人玩家 persistentData 中 `touhou_maid_affection.bond.maids.<女仆UUID>` 子树（即 `BondData` 的女仆粒度数据）——导出为 `.maid` 的 extras 段，导入时按新女仆 UUID 整体写回并刷新 `LastSeen`。extras 的对外格式始终是「base 名 → 值」的 compound，与旧版扁平键时代一致，因此**旧导出文件仍可导入**。女仆实体 NBT（含 ForgeData）由管理器自身负责，TMA 不重复导出。
- **迁移边界：画像随迁、运行态不随迁**：`exportMaidData` 取子树副本后剔除 `BondKeys.RUNTIME_KEYS`（见 4.3）与空字符串值；`importMaidData` 防御性再剔一遍（旧 `.maid` 文件里可能带着运行态键），然后**整体替换**目标子树（源里没有的键在目标上即为缺失，因此导入仍能清掉多余值）并刷新 `LastSeen`。理由：运行态键是会话/世界相关的绝对时间，随 `.maid` 迁到另一只女仆或另一个存档会带上别处的会话时间戳，导致新女仆的礼物计时或「今天是否已亲过」判定被污染。导入仍是整体替换语义，不是合并。
- **为什么 vendored**：SPI v1.4.0 未发布到 CurseForge/Modrinth，也没有 Maven 仓库，因此按上游文档认可的方式把 `io.github.zgxhzhr.maidfm.spi` 两个源文件复制进源码树，仅作编译期 shim。
- **为什么必须从 jar 排除**：NeoForge 1.21.1 用 securejarhandler 的 module classloader（每个 mod 一个 module），跨 mod 的同名类**不保证**被去重；若 TMA 的 jar 也带一份同名 SPI，可能出现「TMA 注册进自己的 registry、管理器读自己的 registry」的静默失联。因此 `build.gradle` 的 `jar` 任务 exclude 掉整个 `io/github/zgxhzhr/**` 命名空间，运行期只有管理器提供这两个类。
- **为什么注册要守卫**：`BondMaidMigrationProvider` 在类加载期会解析 SPI 类型，管理器缺失时会 `NoClassDefFoundError`；主类构造器用 `ModList.get().isLoaded("maid_file_manager")` 包裹 `register()`，未安装时该分支不执行，provider 类不会被解析。
- **不做 AI 缓存迁移**：`world/generated_morning_kiss/<uuid>/` 下的 AI 台词/TTS 语音是可再生缓存，已有 MAID_REVISIONS 失效机制，迁移到新存档反而可能携带过期内容；玩家粒度的 `MorningKissSelectedWindowId/MaidId` 与每日救护次数（`EmergencyRescueAttachment`，玩家 Capability）也不属于女仆粒度，因此一并排除。

## 5. 数据与配置边界

项目存在四类状态容器：

| 容器 | 用途 |
|---|---|
| `ModConfig` | 全局规则、AI 默认值、运行时开关。 |
| `BondData` | 玩家-女仆长期关系档案。 |
| NeoForge Attachment | 玩家当前能力槽、每日次数等独立运行态。 |
| 内存任务表/生成缓存 | 当前服务器会话内的冷却与任务；早安吻 AI 结果另有世界目录持久化副本，并允许通过管理命令同步清理。 |

数据包不是运行时状态存储。`data/touhou_maid_affection/morning_kiss/profile.json` 和 `data/touhou_maid_affection/emergency_rescue/profile.json` 只描述可重载的静态资源入口。

## 6. 数据包格式边界

早安吻：

```text
data/touhou_maid_affection/morning_kiss/profile.json
data/touhou_maid_affection/morning_kiss/voices/*.ogg
```

残血救护：

```text
data/touhou_maid_affection/emergency_rescue/profile.json
data/touhou_maid_affection/emergency_rescue/voices/*.ogg
```

`voice_mode=append` 表示追加到功能语音池；`voice_mode=replace` 表示存在数据包语音时替换基础/TLM 候选。文件必须是 `.ogg`，路径不能包含 `..`、反斜杠或绝对路径，单个文件大小受解析器限制。

早安吻数据包自 1.7.4.0 起支持语言标签（纯增量，旧写法仍兼容）：

- `dialogue.<pool>` 元素：`"文本"`（未标记/通配）或 `{"text": "...", "language": "zh_cn"}`。
- `voice_files` 元素：`"x.ogg"`（未标记/通配）或 `{"file": "x.ogg", "language": "ja_jp", "text": "可选字幕", "text_language": "zh_cn"}`。
- 语种归一化复用 `MorningKissGeneratedDialogueLanguage.normalizeLocaleCode`（小写、`-` → `_`，`tlm`/`auto`/`default` 视为未指定）；`voice_files[].text` 长度按 `BondDataLimits` 有界（≤256），语音条目上限 64。
- 选择规则（由 `MorningKissDataPackEntries.selectByLanguage` 承载，纯逻辑可单测）：目标语种显式时按「语言匹配 → 未标记 → 全部」；`auto`（空目标）原样返回，保持 1.7.3.0 行为。`dialogue_mode=append` 时内置 i18n 台词按「语言 = 客户端语言」并入同一候选池。
- 语音配对字幕：`voice_files[].text` 仅在该语音被选中播放时作为字幕，`text_language` 与目标显示语种不一致时退回随机台词。
- 语种只有两项全局开关：`morningKissBehavior.displayLanguage`（文本语种：内置数据包台词 + AI 台词）与 `voiceLanguage`（配音语种：数据包语音 + AI 合成语音）。`auto` 时文本语种跟随游戏语言、配音语种跟随女仆的 TLM AI 语言设置（AI 台词回退到女仆 TLM 聊天语言、AI 合成回退到 TLM TTS 语言）；显式 locale 固定该语言。旧关键字 `tlm` / `inherit` / `default` 仍等价于 `auto`。TLM 音包无语言元数据，不参与筛选。

旧的 `rescue_sound/profile.json` 只保留兼容入口，新开发应优先使用 `emergency_rescue/profile.json`。

## 7. 网络边界

`network/*Payload.java` 只定义协议字段与编解码，不写业务。所有权限、归属、距离、成本、冷却与触发条件必须在 `handler`、`service` 或领域层判断。

当前关键 payload：

- `KissMaidPayload`：服务端向客户端同步亲吻表现。
- `KissCarryRequestPayload`：公主抱亲吻请求。
- `KissTargetedMaidRequestPayload`：准星目标亲吻请求。
- `BondStateSyncPayload` / `BondStateRequestPayload`：羁绊页状态同步。
- `MorningKissVoicePlayPayload`：TLM 音包语音播放。
- `MorningKissDataVoicePlayPayload`：数据包或运行时 TTS 字节语音播放。
- `MaidRescuePopPayload`：救援弹出、救援者档案与可选救援音频字节。
- `VoicePreviewRequestPayload` / `VoicePreviewDataPackPlayPayload`：语音配置页的数据包语音试听请求与回放，服务端负责女仆归属、能力解锁和文件存在性校验。
- `TmaSettingsRequestPayload`：客户端设置请求（C2S）。空 `entries` 表示只读状态；非空表示请求修改。服务端要求权限等级 2，逐条按白名单与取值语法校验，**任一条不合法整包拒绝**，合法则写入 `ModConfig` 并 `SPEC.save()`，逐条打印审计日志 `[TMA Settings] player=<name> key=<k> old=<a> new=<b>`。
- `TmaSettingsStatePayload`：服务端回推完整生效状态（S2C），包含全部白名单键的当前值与 `canEdit`（是否 OP）。只读请求也会收到该包。
- 两个设置包的条目列表编解码复用纯逻辑类 `bond/settings/TmaSettingsWire`（≤ 32 条），netty `ByteBuf` 适配在 `network/TmaSettingsByteBuf`；白名单与取值校验在纯逻辑类 `bond/settings/TmaSettingsKeys`，逻辑键到 `ModConfig` 的映射在 `bond/settings/TmaSettingsResolver`。
- `TmaAiStatusRequestPayload`：只读 AI 状态请求（C2S，空包）。
- `TmaAiStatusPayload`：服务端回推的只读 AI 状态（S2C）。`maids` **只包含该玩家名下**的女仆（uuid + 名字 + 各时段池条目数 + 总条目/目标），其余为服务器全局计数；`canClear` 表示接收者是否可清缓存（权限等级 2）。
- `TmaAiCacheClearPayload`：清缓存请求（C2S，`scope` = ALL / MAID / POOL）。服务端要求权限等级 2、打印审计日志，并复用与 `/tma morning_kiss clear_ai_cache` 相同的 `MorningKissGeneratedDialogueService.clearCache(...)`，随后回推一份 `TmaAiStatusPayload`。
- 三个 AI 状态包的编解码复用纯逻辑类 `bond/settings/TmaAiStatusWire`（maids ≤ 64、pools ≤ 8、字符串 ≤ 128），netty `ByteBuf` 适配在 `network/TmaAiStatusByteBuf`。

## 8. 演进规范

- 新增能力必须经过 `bond/ability` 与 `BondAbilityManager`，复杂流程放到 service 或 handler。
- 新增跨 tick 功能必须拆分长期状态与运行态，不能只依赖静态 `Map`。
- 新增外部供应商适配应放到 `ai/<provider>`，不要改写早安吻服务本体来绑定供应商。
- 新增 GUI 功能优先抽 page/component，避免继续膨胀 `BondMaidContainerScreen`。
- 新增数据包字段必须同步教程、示例包与解析测试。
- 发布前必须同步 `CHANGELOG.md`、README 双语入口、`PROJECT_ARCHITECTURE.md` 与 `mod_version`。

## 9. 当前复杂度中心

最需要持续治理的文件/模块是：

- `MorningKissService`：对话与语音策略已拆出，剩余复杂度集中在自动时间窗调度和进行中任务推进；后续如继续增长，应优先分离 scheduler 与 task runner。
- `BondMaidContainerScreen` 与二级页：界面状态、tooltip、弹窗、动态语音池、页面切换都在此附近集中。
- `BondData`：长期状态字段持续增多；key 常量已收敛到 `BondKeys`、存储已改为按女仆嵌套，后续新增字段继续走 `BondKeys` + 女仆子树，避免回到扁平键。

后续重构的优先方向是按增长情况继续把早安吻调度器与任务执行器分离；把羁绊页继续拆成更独立的 page controller 与状态对象。
