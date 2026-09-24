# PROJECT_ARCHITECTURE

## 1. 项目定位

`Touhou Maid: Affection` 是 Touhou Little Maid 的 NeoForge 扩展模组。它不替代 TLM 的女仆、音包、模型或 AI 系统，而是在这些能力之上增加一层“以特定女仆与特定玩家关系为中心”的亲密互动与羁绊成长系统。

当前主闭环是：

- 以亲吻作为基础互动入口，提供好感提升、冷却、镜头、粒子、音效与按键入口。
- 以羁绊系统承载长期关系状态，并逐步解锁膝枕、早安吻、残血救护、随机礼物等能力。
- 以服务端为权威状态来源，客户端只负责缓存、界面、音频和视觉表现。
- 以数据包、TLM 音包、YSM 动作、TLM AI 站点与 TMA AI Hub 形成可选增强，缺失时应降级而不是中断主流程。

## 2. 技术栈与发布约束

- Java `21`
- Gradle 单模块工程
- Minecraft `1.21.1`
- NeoForge `21.1.x`
- Touhou Little Maid `1.5.1+`，当前编译目标为 `1.5.2-neoforge+mc1.21.1`
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
├─ ai/mimo
├─ bond
│  ├─ BondData.java / BondManager.java
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

`ModConfig.java` 只描述全局规则和默认供应商参数，不保存玩家或女仆运行结果。亲吻冷却、好感收益、亲吻音效音量、随机礼物池策略、残血救护绝对/百分比阈值、早安吻语音音量、残血救护音量、语音试听音量、早安吻 AI/TTS 的运行时开关、提示词、显示/配音语言、扫描频率、缓存策略与 TMA AI Hub 默认值都在这里定义。

### 4.2 亲吻主链

`KissMaidHandler` 是亲吻服务端主入口，负责冷却、好感、亲吻音效播放、粒子 payload、少女祈祷触发与早安吻复用逻辑。公主抱亲吻按键和准星目标亲吻按键最终都收敛到这里，避免规则分叉；潜行空手右击入口不再拦截 TLM 女仆交互。亲吻音效响度由全局配置控制，早安吻数据包仍只负责选择 sound event。

客户端的 `KissKeyAction` 在公主抱亲吻和准星亲吻共用按键时做入口选择；服务端的 `KissTargetedMaidRequestHandler` 必须重新校验实体、距离、视线与归属，不能信任客户端命中结果。

### 4.3 羁绊域模型

`BondData` 保存玩家维度、女仆粒度的长期档案：羁绊等级、解锁能力、语音选择、早安吻计划、礼物队列、膝枕姿态等。

`BondManager` 是语义化门面，屏蔽底层 persistentData key。后续新增持久字段应集中在 `BondData` 或相关子域数据结构中，避免 handler 或 screen 直接拼 key。

`bond/ability` 的能力对象只描述成本、名称、解锁条件与二级行为入口。复杂流程应放到 `bond/service`、`bond/rescue` 或 handler 中。

### 4.4 长流程服务

`bond/service` 承载 tick 驱动或跨时间窗的服务：

- `MorningKissService`：早安吻时间窗调度、寻路和亲吻任务推进。
- `MorningKissDialogueService`：生成缓存、即时 AI、数据包台词与内置台词之间的回退链，以及聊天气泡/聊天栏/动作栏显示策略。
- `MorningKissVoiceService`：每名女仆的语音池解析、顺序/随机选择、数据包/TLM 回退和客户端播放 payload 分发。
- `MorningKissGeneratedDialogueService`：基于 TLM LLM/TTS 站点异步预生成台词和 TTS 音频；维护 MAID_REVISIONS 实现女仆级缓存失效，支持 LLM、跨语言翻译与 TTS 三阶段 IN_FLIGHT 跟踪，在服务器启动时从磁盘恢复缓存、关闭时持久化。显示语言与配音语言不同时，先生成显示文本，再用单次批量翻译保持逐行映射，最后以 `tts_text` 请求 TTS；翻译失败则安全降级为纯显示文本。
- `MorningKissGeneratedDialogueLanguage`：早安吻 AI 显示/配音语言的纯逻辑归一化、继承规则、翻译 prompt 与严格 JSON 数组解析。支持 `inherit`、`tlm/auto/default` 和显式语言代码。
- `MorningKissGeneratedDialogueCache`：保存女仆粒度的运行时生成台词、独立 `tts_text` 和 TTS 语音缓存。支持消耗/非消耗两种取出模式（CACHE_CONSUME_ON_USE），提供女仆级、池级、条目级的清理与语音剥离操作。缓存容量受 maxLinesPerPool 和 aiDialogueCacheTargetPerPool 双重约束。实现 snapshot() / replaceAll() 接口以支持磁盘持久化。统计报告通过 stats() 按女仆和语言分组输出。
- `MorningKissGeneratedDialogueStorage`：将运行时 AI 生成缓存持久化到 `world/generated_morning_kiss/{maid_uuid}/{pool}/` 目录下，每条条目写为 `001.json`（元数据）+ 同编号 `.ogg`/`.mp3`（语音），格式兼容手动编辑。路径遍历防护通过 `normalize()` + `startsWith()` 检查实现。服务器启动时自动加载、服务器关闭时自动保存。
- `MorningKissProfileParser` / `MorningKissProfileData`：读取早安吻静态数据包 profile。
- `InteractionVoiceProfileParser` / `InteractionVoiceProfileData`：早安吻与残血救护共享的数据包 OGG 语音解析。
- `RandomGiftService`：随机礼物积累与投递。默认礼物来源是显式物品标签池；广泛注册表抽样是可选兼容模式，仅默认过滤破坏沉浸感的技术/管理物品。显式礼物池可覆盖默认过滤，黑名单仍具有最终否决权。

早安吻的架构边界非常明确：数据包负责静态台词、亲吻 sound event、预录 OGG 语音；全局配置负责亲吻 sound event 响度与早安吻语音响度；AI/TTS 运行时行为负责配置、生成、持久化缓存、清理和失败回退。`/tma morning_kiss clear_ai_cache` 会同时清理内存与世界目录中的生成缓存，供语言或提示词变更后重新预热，不改变数据包或 BondData。新增的 `aiDialogueCacheConsumeOnUse` 配置允许管理员选择消耗或复用缓存条目以平衡 LLM/TTS Token 成本与体验。

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

语音配置页现在是动态语音池页面：服务端同步数据包候选，客户端补充 TLM 音包候选。玩家保存的是每名女仆的池选择与播放模式，而不是全局固定文件名。早安吻与残血救护语音列表都支持试听：本地内置亲吻音效试听跟随亲吻音效音量，数据包/TLM 语音试听跟随语音试听音量。数据包语音通过服务端校验后把目标字节发送回客户端播放。TLM 音包试听同样读取原始音频字节，但使用专用 preview stream：以 `minecraft:music.menu` 作为稳定声音事件锚点，走 `PLAYERS` 音量分类，并关闭位置衰减，避免右键试听依赖 TLM 音包自身的 sound event 注册状态或玩家的环境音量设置。TLM 音包实际播放仍由功能流程创建跟随女仆或触发点的流式 SoundInstance，避免把 Opus/Vorbis 兼容性压到 `SoundBuffer` 旧链路上。所有内存 OGG/MP3 字节统一由 `InMemoryVoiceStream` 异步解码，跟随实体的语音统一复用 `TrackedEntityVoiceSoundInstance`，但各场景仍保留自己的声源分类、位置衰减和锚点策略。

`BondMaidGuiTabHandler` 不固定占用 TLM 顶部 tab 位置，而是运行时扫描可用位置，降低与 TLM 或其他扩展页签冲突。

`SettingsSecondaryPage` 是与女仆无关的全局设置面板，从羁绊页顶部左侧的「设置」按钮进入（`BondPrimaryPageHost#openSettingsPage`，不走 `BondSecondaryPageRegistry` 的能力页流程）。它复用 `BondModalPage` / `BondButtonRow` / `BondDropdown` / `BondGuiTokens`，并新增自绘 `BondSlider`。面板分三区：功能开关、AI 早安吻语种、音量。开关与语种是**服务端权威**项，走 `TmaSettingsRequestPayload` / `TmaSettingsStatePayload`；音量是纯客户端项，直接写 `ModConfig` 并 `SPEC.save()`。布局参数集中在页面顶部常量，内容区可滚动（下拉框与滑块通过 `setPosition` 跟随滚动偏移）。

### 4.8 TMA AI Hub / MiMo 适配层

`ai/mimo` 是当前第三方模型协议适配层。用户界面统一称为 `TMA AI Hub`，内部包名与 `tma_mimo_chat` / `tma_mimo_tts` 站点类型保持稳定以兼容已有配置：

- LLM 侧保持 OpenAI 风格站点兼容，尽量复用 TLM 原生聊天客户端和工具调用语义。
- TTS 侧解析 MiMo chat-completions 风格响应中的 base64 音频，交给 TLM 播放链路。
- `BoundedHttpClient` / `BoundedHttpResponse` 在字节进入字符串缓冲前执行响应上限；TTS 还会在 Base64 解码前后复核音频大小，错误正文只传递有界摘要。
- API key、启用状态与站点保存仍由 TLM 管理；TMA 只提供默认 URL、模型、格式与站点类型。
- TMA 不接管 TLM STT，也不把远程服务失败变成阻断错误。

### 4.9 兼容层

`ysm`、`mixin`、`ai/<provider>` 与小型 helper 是外部生态适配的边界。与 YSM、CarryOn、TLM GUI、TLM 音包、TLM AI 的适配逻辑应保持隔离，不能扩散成到处可见的条件分支。

`compat/maidfm` 是对 MaidFileManager（女仆档案管理器，modid `maid_file_manager`）迁移 SPI 的适配边界，为**软依赖**：未安装管理器时行为与之前完全一致。

- **契约**：`BondMaidMigrationProvider` 实现管理器的 `MaidMigrationProvider`，把 TMA 唯一「挂在女仆身上但不在女仆实体 NBT 内」的数据——主人玩家 persistentData 中 `touhou_maid_affection.bond` 子树里以 `_<女仆UUID>` 结尾的 `BondData` 键——导出为 `.maid` 的 extras 段，导入时按新女仆 UUID 重建。女仆实体 NBT（含 ForgeData）由管理器自身负责，TMA 不重复导出。键的匹配/剥离/重建逻辑抽成纯逻辑类 `MaidDataKeyCodec`，便于单元测试。
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
- 全局开关为 `morningKissBehavior.displayLanguage` / `voiceLanguage`；AI 专用配置（`aiDialogueLanguage` / `aiDialogueVoiceLanguage`）显式 locale 时优先于全局。TLM 音包无语言元数据，不参与筛选。

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
- `BondData`：长期状态字段持续增多，后续应优先收敛 key 常量与子结构。

后续重构的优先方向是按增长情况继续把早安吻调度器与任务执行器分离；把羁绊页继续拆成更独立的 page controller 与状态对象。
