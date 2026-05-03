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

`ModConfig.java` 保存全局规则、默认阈值、早安吻 AI/TTS 运行时开关、提示词、语言、扫描频率、缓存策略、MiMo 默认值与兼容项。它不保存玩家或女仆的运行结果。

注册层的原则是“装配而非决策”：具体触发条件、资源解析、能力逻辑和错误回退应下放到 handler、service 或领域对象。

## 5. 亲吻主链

`KissMaidHandler` 是服务端亲吻主入口，负责冷却、好感增长、亲吻音效/粒子 payload、少女祈祷触发和早安吻复用逻辑。普通右键、公主抱亲吻按键、准星目标亲吻按键都应收敛到这里，避免规则分叉。

客户端的 `KissKeyAction` 在共享默认键位时选择公主抱亲吻或准星亲吻入口。服务端的 `KissTargetedMaidRequestHandler` 必须重新校验实体存在、归属、距离、视线和正常亲吻规则，不能信任客户端命中结果。

## 6. 羁绊域模型

`BondData` 保存玩家维度、女仆粒度的长期档案：羁绊等级、解锁能力、语音选择、早安吻计划、礼物队列、膝枕姿态等。

`BondManager` 是语义化门面，屏蔽底层 persistentData key。新增持久字段应集中在 `BondData` 或相邻子结构中，避免 handler、service 或 screen 直接拼 key。

`bond/ability` 描述能力名称、成本、解锁条件和二级行为入口。复杂流程应放入 `bond/service`、`bond/rescue`、`bond/lap` 或 `handler`。

## 7. 长流程服务

`bond/service` 承载跨 tick、跨时间窗或可重载资源相关的流程：

- `MorningKissService`：早安吻调度、寻路、亲吻执行、台词选择与语音触发。
- `MorningKissGeneratedDialogueService`：基于 TLM LLM/TTS 站点异步预生成台词和 TTS 音频；默认仅文本生成跟随 TLM 聊天语言；当后台预生成会生成远程 TTS 时，待合成文本跟随 TLM TTS 语言按钮，确保传给 TTS 的文本语种与请求语种一致；只有 `aiDialogueLanguage` 显式配置为具体语言时才统一覆盖。
- `MorningKissGeneratedDialogueLanguage`：早安吻 AI 语言配置的纯逻辑归一化与 prompt 语言覆盖规则。
- `MorningKissGeneratedDialogueCache`：保存服务端运行时生成结果，缓存键必须至少包含女仆 UUID 与时间池，避免多名女仆共享同一生成池。
- `MorningKissGeneratedDialogueStorage`：把早安吻 AI 生成文本与 TTS 音频持久化到世界目录下的 `generated_morning_kiss/<maidUuid>/<pool>/`，以 `001.json` + 可选 `001.ogg/mp3` 的形式提供外部可编辑入口；它是生成缓存的磁盘镜像，不属于数据包，也不触发 `/reload`。
- `MorningKissProfileParser` / `MorningKissProfileData`：读取早安吻静态数据包 profile。
- `InteractionVoiceProfileParser` / `InteractionVoiceProfileData`：解析早安吻和残血救护共享的数据包 OGG 语音池。
- `RandomGiftService`：随机礼物积累、选择和投递。

早安吻边界：

- 数据包负责静态台词、亲吻 sound event 和预录 OGG。
- `morningKissBehavior` TOML 配置负责运行时 AI/TTS、提示词、语言、扫描频率、缓存目标数、消费策略和失败回退；`aiDialogueLanguage=tlm/auto/default` 表示跟随 TLM 本体语言设置，其中生成式语音缓存的文本和语音均以 TLM TTS 语言按钮为准，具体 locale 表示 TMA 统一覆盖；`aiDialogueCacheTargetPerPool` 同时是预热目标和最终入池硬上限，默认每名女仆三个时间池合计最多 12 条生成缓存，不因文本/语音语种分组而扩容；`aiDialogueCacheConsumeOnUse=false` 时早安吻触发复用缓存且不消耗，只有清理缓存后才重新预热。
- `/tma morning_kiss` 命令组提供 AI/TTS 状态、生成缓存明细、运行中请求、AI/TTS 开关和缓存清理入口；`clear_ai_cache` 保留全清入口，同时支持按女仆、按时间池、按条目删除，以及只清除某条生成语音但保留文本。清理生成缓存不改变数据包或 BondData，持久化镜像会随内存缓存同步更新。
- AI/TTS 失败只影响增强体验，不能阻断静态台词或已有语音。

## 8. 残血救护

`bond/rescue` 管理紧急救援触发、每日救援次数、救援者身份 canonical id、provider/legacy 数据兼容与视觉弹出。

救援语音当前使用功能级数据包语音池：触发 payload 可携带命中的数据包 OGG 字节；若没有命中，则回退到 TLM 音包或兜底 sound event。旧的服务器文件同步服务已移除，新开发不要恢复该路径。

`EmergencyRescueSoundPlayer` 只处理客户端播放策略，不决定救援是否成立。

## 9. 膝枕

`bond/lap` 管理膝枕会话状态、锚点实体与姿态快照。膝枕是“服务端维持会话 + 客户端渲染桥接”的状态机：

- 长期配置落在 `BondData`。
- 当前会话落在 `LapPillowState` 与锚点实体。
- 客户端 mixin 只做渲染期睡姿、乘骑和模型桥接，不改变服务端业务真相。

膝枕角度冻结通过独立 payload 保存会话姿态。默认按键不再与亲吻入口抢占。

## 10. 客户端 UI 与音频

`BondMaidContainerScreen` 是羁绊页总屏幕；`screen/page` 承载一级/二级页控制；`screen/component` 提供按钮行、滚动列表、弹窗、下拉框、语音池列表等复用组件。

语音配置页是动态语音池页面：

- 服务端同步数据包候选项。
- 客户端补充 TLM 音包候选项。
- 玩家保存的是每名女仆的池选择和播放模式，而不是全局固定文件名。
- 试听动作由可改键 `key.touhou_maid_affection.voice_preview` 和右键列表项触发。

音频播放分三类：

- 数据包语音：触发或试听时由服务端下发 OGG 字节，客户端使用流式实例播放。
- TLM 音包：客户端本地索引后直接流式播放，不依赖伪造 `SoundBuffer`。
- 内置或兜底 sound event：走 Minecraft 原生 sound event 播放。

`BondMaidGuiTabHandler` 运行时扫描可用 tab 位置，降低与 TLM 或其他扩展页签冲突。

## 11. AI / MiMo 适配层

`ai/mimo` 是 MiMo 协议适配层，通过 TLM 扩展入口注册 `tma_mimo_chat` 与 `tma_mimo_tts`：

- LLM 侧复用 TLM OpenAI 站点编辑器的表单体验，但实际请求由 `MimoLLMClient` 发起。
- `LLMSiteEditorScreenMixin` 只解决 TLM 编辑器保存后站点类型被普通 OpenAI 类型覆盖的问题，作用域必须保持窄。
- TTS 侧实现 TLM 1.5.2 的旧接口，解析 MiMo chat-completions 风格响应中的 base64 音频后交给 TLM/TMA 播放链路；从 `TTSConfig.language` 传入的语言必须写入请求体与 voice prompt，避免回落到 TLM 站点默认语种。
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
- `VoicePreviewRequestPayload` / `VoicePreviewDataPackPlayPayload`：语音列表试听请求与数据包试听字节下发。

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

- `MorningKissService`：调度、任务推进、对话、语音策略和多个回退路径仍集中在一个服务里。
- `BondMaidContainerScreen` 与二级页：页面切换、tooltip、弹窗、动态语音池、试听动作都在此附近集中。
- `BondData`：长期状态字段持续增多，应继续收敛 key 常量与子结构。
- `ai/mimo`：依赖 TLM AI 旧接口与编辑器行为，后续 TLM 升级时需要优先回归。

后续重构优先级：先把早安吻拆成调度器、任务执行器、对话策略、语音策略四块；再把羁绊页拆成更独立的 page controller 与状态对象。
