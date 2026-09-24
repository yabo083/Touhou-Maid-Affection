# PROJECT_ARCHITECTURE

本文档是 `Touhou Maid: Affection` 的长期架构地图。它记录模块职责、数据边界与发布约束，不追踪琐碎实现细节。

## 1. 项目定位

`Touhou Maid: Affection` 是 Touhou Little Maid 的 Forge 1.20.1 扩展模组。它不替代 TLM 的女仆、模型、音包、AI 站点或 GUI 体系，而是在这些能力之上叠加“玩家与特定女仆之间的亲密互动和长期羁绊”。

当前核心闭环：

- 亲吻是基础互动入口，负责好感、冷却、镜头、粒子、音效和按键触发。
- 羁绊系统承载长期关系状态，并解锁休闲膝枕、早安吻、残血救护、随机礼物等能力。
- 服务端是权限、归属、距离、成本、冷却、日次数和任务推进的权威来源。
- 客户端负责缓存、GUI、按键、音频播放、渲染桥接和视觉反馈。
- 数据包、TLM 音包、TLM AI 站点、MiMo、YSM、CarryOn 都是可选增强；缺失或失败时应降级，不应阻断主流程。

## 2. 技术栈与发布约束

- Java `17`
- Minecraft `1.20.1`
- Forge `47.4.16`
- Gradle 单模块工程
- Official mappings `1.20.1`
- Touhou Little Maid 编译目标：`1.5.2-forge+mc1.20.1`
- Mixin 用于 TLM GUI、TLM AI 编辑器、诱饵行为与膝枕渲染桥接
- Modrinth Minotaur 与 GitHub Actions 负责发布

版本源是 `gradle.properties` 的 `mod_version`。Forge 1.20.1 分支发布 tag 使用 `v<mod_version>-forge1.20.1`，例如 `v1.7.2.1-forge1.20.1`。发版前必须同步 `mod_version`、`CHANGELOG.md`、README 双语门面、教程、示例数据包和本文档。

### 2.1 第三方兼容层

- **MaidFileManager（车万女仆档案管理器）迁移 SPI**：`com/github/touhoumaidaffection/compat/maidfm/BondMaidMigrationProvider` 实现 `io.github.zgxhzhr.maidfm.spi.MaidMigrationProvider`，把 `BondData`（挂在主人玩家 persistentData 上、不在女仆实体 NBT 内）按女仆 UUID 导出/导入。纯逻辑键名转换抽到 `MaidDataKeyCodec`（`_<uuid>` 后缀的剥离与重建），便于单测。
- SPI 两个接口源文件 vendored 到 `src/main/java/io/github/zgxhzhr/maidfm/spi/`（包名不变），**仅供编译期**：`build.gradle` 的 `jar` 任务用 `exclude 'io/github/zgxhzhr/**'` 把它们排除出产物，运行期只由管理器的 jar 提供这两个类。原因是重复同名类在不同加载器/类加载器下不保证被去重，若 TMA 自带一份，可能出现「TMA 注册进自己的 registry、管理器读自己的 registry」的静默失联。
- 注册入口在 mod 构造器内、紧邻 `BondAbilityManager.registerDefaults()`，并用 `ModList.get().isLoaded("maid_file_manager")` 做软依赖守卫，避免管理器缺失时类加载期解析 SPI 类型抛 `NoClassDefFoundError`。
- 不做迁移的部分：`world/generated_morning_kiss/<uuid>/` 的 AI 台词/TTS 缓存（可再生、有 `MAID_REVISIONS` 失效机制）、玩家粒度的 `MorningKissSelectedWindowId/MaidId`、玩家 Capability/Attachment 的每日救护次数。

## 3. 目录拓扑

```text
src/main/java/com/github/touhoumaidaffection
├─ TouhouMaidAffection.java
├─ ModConfig.java
├─ ModCapabilities.java / ModEffects.java / ModEntityTypes.java / ModSounds.java
├─ ai/mimo
├─ bond
│  ├─ BondData.java / BondManager.java
│  ├─ VoicePoolIds.java / VoicePoolSelection.java
│  ├─ ability
│  ├─ lap
│  ├─ rescue
│  └─ service
├─ client
│  ├─ Kiss* / MorningKiss* / EmergencyRescue* / VoicePreview*
│  └─ screen
│     ├─ component
│     └─ page
├─ command
├─ effect
├─ handler
├─ inventory
├─ mixin
│  └─ client
├─ network
├─ util
└─ ysm

src/main/resources
├─ META-INF/mods.toml
├─ touhou_maid_affection.mixins.json
├─ assets/touhou_maid_affection
│  ├─ lang
│  ├─ sounds
│  └─ textures
└─ data/touhou_maid_affection
   ├─ morning_kiss/profile.json
   ├─ emergency_rescue/profile.json
   ├─ rescue_sound/profile.json
   └─ tags/items

examples/TMA-Custom-Voice-Pack
├─ pack.mcmeta
├─ README.md
└─ data/touhou_maid_affection
   ├─ morning_kiss/profile.json
   └─ emergency_rescue/profile.json
```

`examples/TMA-Custom-Voice-Pack` 是给玩家和发布页使用的示例数据包，不参与模组自身资源加载，但格式必须与解析器保持一致。

## 4. 启动与注册层

`TouhouMaidAffection.java` 是启动门面，负责配置注册、注册表、payload、事件监听、TLM AI 扩展和 tick 入口装配。它不应承载业务规则。

`ModConfig.java` 保存全局规则、默认阈值、亲吻冷却与右键入口开关、好感收益、亲吻音效/早安吻语音/残血救护/语音试听音量、随机礼物池策略、残血救护绝对/百分比阈值、早安吻 AI/TTS 运行时开关、提示词、显示语言与配音语言、扫描频率、缓存策略、MiMo 默认值与兼容项。它不保存玩家或女仆的运行结果。

注册层的原则是“装配而非决策”：具体触发条件、资源解析、能力逻辑和错误回退应下放到 handler、service 或领域对象。

## 5. 亲吻主链

`KissMaidHandler` 是亲吻服务端主入口，负责冷却、好感、亲吻音效播放、粒子 payload、少女祈祷触发与早安吻复用逻辑。公主抱亲吻按键和准星目标亲吻按键最终都收敛到这里，避免规则分叉；潜行空手右击入口不再拦截 TLM 女仆交互。亲吻音效响度由全局配置控制，早安吻数据包仍只负责选择 sound event。

客户端的 `KissKeyAction` 在共享默认键位时选择公主抱亲吻或准星亲吻入口。服务端的 `KissTargetedMaidRequestHandler` 必须重新校验实体存在、归属、距离、视线和正常亲吻规则，不能信任客户端命中结果。

## 6. 羁绊域模型

`BondData` 保存玩家维度、女仆粒度的长期档案：羁绊等级、解锁能力、语音选择、早安吻计划、礼物队列、膝枕姿态等。

`BondManager` 是语义化门面，屏蔽底层 persistentData key。新增持久字段应集中在 `BondData` 或相邻子结构中，避免 handler、service 或 screen 直接拼 key。

`bond/ability` 描述能力名称、成本、解锁条件和二级行为入口。复杂流程应放入 `bond/service`、`bond/rescue`、`bond/lap` 或 `handler`。

## 7. 长流程服务

`bond/service` 承载跨 tick、跨时间窗或可重载资源相关的流程：

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

早安吻边界：

- 数据包负责静态台词、亲吻 sound event 和预录 OGG。
- 全局配置负责亲吻 sound event 与早安吻语音的响度：亲吻音效跟随 `cooldown.kissSoundVolume`，早安吻 TLM/数据包/AI-TTS 语音跟随 `morningKissBehavior.voiceVolume`。
- `morningKissBehavior` TOML 配置负责运行时 AI/TTS、提示词、语言、扫描频率、缓存目标数、消费策略和失败回退；`aiDialogueLanguage=tlm/auto/default` 表示跟随 TLM 本体语言设置，其中生成式语音缓存的文本和语音均以 TLM TTS 语言按钮为准，具体 locale 表示 TMA 统一覆盖；`aiDialogueCacheTargetPerPool` 同时是预热目标和最终入池硬上限，默认每名女仆三个时间池合计最多 12 条生成缓存，不因文本/语音语种分组而扩容；`aiDialogueCacheConsumeOnUse=false` 时早安吻触发复用缓存且不消耗。缓存读取与满度判定均按当前解析出的显示/配音语种过滤，语言变更后会自动按新语种重新预热，无需手动清理（旧语种条目保留，可手动清理）；提示词变更仍需手动清理。
- `/tma morning_kiss` 命令组提供 AI/TTS 状态、生成缓存明细、运行中请求、AI/TTS 开关和缓存清理入口；`clear_ai_cache` 保留全清入口，同时支持按女仆、按时间池、按条目删除，以及只清除某条生成语音但保留文本。清理生成缓存不改变数据包或 BondData，持久化镜像会随内存缓存同步更新。
- AI/TTS 失败只影响增强体验，不能阻断静态台词或已有语音。

## 8. 残血救护

`bond/rescue` 管理紧急救援触发、每日救援次数、救援者身份 canonical id、provider/legacy 数据兼容与视觉弹出。

救援语音当前使用功能级数据包语音池：触发 payload 可携带命中的数据包 OGG 字节；若没有命中，则回退到 TLM 音包或兜底 sound event。旧的服务器文件同步服务已移除，新开发不要恢复该路径。

`EmergencyRescueSoundPlayer` 只处理客户端播放策略，不决定救援是否成立。数据包语音、TLM 音包语音和兜底 sound event 的响度统一服从残血救护全局音量配置。

## 9. 膝枕

`bond/lap` 管理膝枕会话状态、锚点实体与姿态快照。膝枕是“服务端维持会话 + 客户端渲染桥接”的状态机：

- 长期配置落在 `BondData`。
- 当前会话落在 `LapPillowState` 与锚点实体。
- 客户端 mixin 只做渲染期睡姿、乘骑和模型桥接，不改变服务端业务真相。

膝枕角度冻结通过独立 payload 保存会话姿态。默认按键不再与亲吻入口抢占。

## 10. 客户端 UI 与音频

`BondMaidContainerScreen` 是羁绊页总屏幕；`screen/page` 承载一级/二级页控制；`screen/component` 提供按钮行、滚动列表、弹窗、下拉框、语音池列表等复用组件。

`BondGuiTokens` 是所有羁绊 UI 的唯一配色与尺寸来源（业务代码不得硬编码 ARGB）：暖木色为面板/控件底，玫瑰色（`COLOR_ACCENT`、`*_ON_*`、`*_SELECTED_*`、`PRIMARY_BUTTON_*`）只作交互面与强调，金色（`HIGHLIGHT_TEXT`、`COLOR_TEXT_SELECTED`、`TAG_SERVER`）只作高亮文字与作用域标签。`textures/gui/rose_vine.png`（34×24 RGBA）是纯装饰资源，只在设置面板侧栏底部绘制，不接收鼠标事件。

语音配置页是动态语音池页面：

- 服务端同步数据包候选项。
- 客户端补充 TLM 音包候选项。
- 玩家保存的是每名女仆的池选择和播放模式，而不是全局固定文件名。
- 试听动作由可改键 `key.touhou_maid_affection.voice_preview` 和右键列表项触发。
- 内置亲吻音效试听跟随 `cooldown.kissSoundVolume`；数据包与 TLM 语音试听跟随 `voicePreview.volume`。

音频播放分三类：

- 数据包语音：触发或试听时由服务端下发 OGG 字节，客户端使用流式实例播放。
- TLM 音包：客户端本地索引后直接流式播放，不依赖伪造 `SoundBuffer`。
- 内置或兜底 sound event：走 Minecraft 原生 sound event 播放。

`BondMaidGuiTabHandler` 运行时扫描可用 tab 位置，降低与 TLM 或其他扩展页签冲突。

`SettingsSecondaryPage` 是与女仆无关的全局设置面板，从羁绊页顶部左侧的「设置」按钮进入（`BondPrimaryPageHost#openSettingsPage`，不走 `BondSecondaryPageRegistry` 的能力页流程）。模态框比其它二级页宽（216×150，`BondGuiTokens.SETTINGS_MODAL_WIDTH`）：左侧 46px 导航轨按「功能 / 语音 / 音量」三个 tab 切换单区内容，导航底部留白区绘制装饰藤蔓（`textures/gui/rose_vine.png`，不接受鼠标事件）。它复用 `BondModalPage` / `BondDropdown` / `BondGuiTokens`，并新增自绘 `BondSlider`（88×13，数值金色居中）与胶囊开关。开关与语种是**服务端权威**项，走 `TmaSettingsRequestPayload` / `TmaSettingsStatePayload`，点击即时发包；音量是纯客户端项，直接写 `ModConfig` 并 `SPEC.save()`。行内状态点不依赖任何协议扩展：客户端记录 `pending`（key→请求值），收到状态回推后逐个比对——相等即「已保存」（不画点），不等即「被拒绝」（红点约 3 秒后自动清除），超过 5 秒仍无回推按超时视为被拒绝；存在请求中/被拒项时 footer 的「完成」左侧出现纯文字「重载」（清本地标记并 `requestSync()`）。布局参数集中在页面顶部常量，内容区可滚动（下拉框与滑块通过 `setPosition` 跟随滚动偏移）。

## 11. AI / MiMo 适配层

`ai/mimo` 是 MiMo 协议适配层，通过 TLM 扩展入口注册 `tma_mimo_chat` 与 `tma_mimo_tts`：

- LLM 侧复用 TLM OpenAI 站点编辑器的表单体验，但实际请求由 `MimoLLMClient` 发起。
- `LLMSiteEditorScreenMixin` 只解决 TLM 编辑器保存后站点类型被普通 OpenAI 类型覆盖的问题，作用域必须保持窄。
- TTS 侧实现 TLM 1.5.2 的旧接口，解析 MiMo chat-completions 风格响应中的 base64 音频后交给 TLM/TMA 播放链路；从 `TTSConfig.language` 传入的语言必须写入请求体与 voice prompt，避免回落到 TLM 站点默认语种。
- `BoundedHttpClient` / `BoundedHttpResponse` 在字节进入字符串缓冲前执行响应上限；TTS 还会在 Base64 解码前后复核音频大小，错误正文只传递有界摘要。
- MiMo TTS 默认请求 MP3；远程响应会被格式校验，不能把不可播放格式塞进客户端队列。
- API key、站点启用状态、站点保存仍由 TLM 管理；TMA 只提供站点类型、默认 URL、默认模型、格式和羁绊页跳转入口。

TMA 不接管 TLM STT，也不把远程服务失败变成阻断错误。

## 12. 数据与配置边界

| 容器 | 用途 |
|---|---|
| `ModConfig` | 全局规则、AI 默认值、运行时开关和兼容配置。 |
| `BondData` | 玩家-女仆长期关系档案。 |
| Forge Capability | 玩家当前能力槽、救援日次数等独立运行态。 |
| 内存任务表/缓存 | 当前服务器会话内的冷却、任务、语音预览、AI 预生成内存镜像；早安吻 AI 缓存按女仆与时间池分桶，并允许通过管理命令细粒度清理。 |
| 世界目录生成库 | 早安吻 AI 生成文本与 TTS 音频的持久化镜像，位于 `generated_morning_kiss/<maidUuid>/<pool>/`；玩家或整合包作者可在停服后编辑 JSON 与音频文件，下一次进入世界时自动加载。 |
| 数据包 | 可 `/reload` 的静态文本、预录 OGG 和 sound event 声明。 |

数据包不是运行时状态存储。AI 开关、API key、扫描频率、缓存策略和供应商默认值不应写进数据包。

## 13. 数据包格式边界

Minecraft 1.20.1 数据包示例使用 `pack_format: 15`。用户压缩发布时，zip 根目录必须直接包含 `pack.mcmeta` 与 `data/`。

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

约束：

- `dialogue_mode` 和 `voice_mode` 支持 `append` / `replace`。
- `voice_files` 只接受相对对应 `voices/` 目录的 `.ogg`。
- 路径不能包含 `..`、反斜杠或绝对路径。
- 单个数据包语音有大小限制，过大文件会被跳过并记录警告。
- 旧 `rescue_sound/profile.json` 仅保留兼容入口，新开发优先使用 `emergency_rescue/profile.json`。

早安吻数据包自 1.7.4.0 起支持语言标签（纯增量，旧写法仍兼容）：

- `dialogue.<pool>` 元素：`"文本"`（未标记/通配）或 `{"text": "...", "language": "zh_cn"}`。
- `voice_files` 元素：`"x.ogg"`（未标记/通配）或 `{"file": "x.ogg", "language": "ja_jp", "text": "可选字幕", "text_language": "zh_cn"}`。
- 语种归一化复用 `MorningKissGeneratedDialogueLanguage.normalizeLocaleCode`（小写、`-` → `_`，`tlm`/`auto`/`default` 视为未指定）；`voice_files[].text` 长度按 `BondDataLimits` 有界（≤256），语音条目上限 64。
- 选择规则（由 `MorningKissDataPackEntries.selectByLanguage` 承载，纯逻辑可单测）：目标语种显式时按「语言匹配 → 未标记 → 全部」；`auto`（空目标）原样返回，保持 1.7.3.0 行为。`dialogue_mode=append` 时内置 i18n 台词按「语言 = 客户端语言」并入同一候选池。
- 语音配对字幕：`voice_files[].text` 仅在该语音被选中播放时作为字幕，`text_language` 与目标显示语种不一致时退回随机台词。
- 全局开关为 `morningKissBehavior.displayLanguage` / `voiceLanguage`；AI 专用配置（`aiDialogueLanguage` / `aiDialogueVoiceLanguage`）显式 locale 时优先于全局。TLM 音包无语言元数据，不参与筛选。

## 14. 网络边界

`network/*Payload.java` 只定义协议字段与编解码，不写业务。所有权限、归属、距离、成本、冷却、日次数与触发条件必须在 `handler`、`service` 或领域层判断。

关键 payload：

- `KissMaidPayload`：亲吻表现同步。
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

## 15. 演进规范

- 新增能力必须经过 `bond/ability` 与 `BondAbilityManager`。
- 新增跨 tick 功能必须拆分长期状态与运行态，不能只依赖静态 `Map`。
- 新增外部供应商适配应放到 `ai/<provider>`，不要让早安吻服务直接绑定供应商。
- 新增 GUI 功能优先抽 page/component，避免继续膨胀 `BondMaidContainerScreen`。
- 新增数据包字段必须同步教程、示例包、解析测试与本文档。
- 新增 payload 必须保持“协议定义”和“业务校验”分离。
- 发版前必须执行 `test + compileJava`，并确认 tag、`mod_version`、README、CHANGELOG 和教程一致。

## 16. 当前复杂度中心

最需要持续治理的模块：

- `MorningKissService`：对话与语音策略已拆出，剩余复杂度集中在自动时间窗调度和进行中任务推进；后续如继续增长，应优先分离 scheduler 与 task runner。
- `BondMaidContainerScreen` 与二级页：页面切换、tooltip、弹窗、动态语音池、试听动作都在此附近集中。
- `BondData`：长期状态字段持续增多，应继续收敛 key 常量与子结构。
- `ai/mimo`：依赖 TLM AI 旧接口与编辑器行为，后续 TLM 升级时需要优先回归。

后续重构优先级：按增长情况继续把早安吻调度器与任务执行器分离；再把羁绊页拆成更独立的 page controller 与状态对象。
