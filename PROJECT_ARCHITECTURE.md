# PROJECT_ARCHITECTURE

## 1. 项目定位

`Touhou Maid: Affection` 是 Touhou Little Maid 的 NeoForge 扩展模组。它不替代 TLM 的女仆、音包、模型或 AI 系统，而是在这些能力之上增加一层“以特定女仆与特定玩家关系为中心”的亲密互动与羁绊成长系统。

当前主闭环是：

- 以亲吻作为基础互动入口，提供好感提升、冷却、镜头、粒子、音效与按键入口。
- 以羁绊系统承载长期关系状态，并逐步解锁膝枕、早安吻、残血救护、随机礼物等能力。
- 以服务端为权威状态来源，客户端只负责缓存、界面、音频和视觉表现。
- 以数据包、TLM 音包、YSM 动作、TLM AI 站点与 MiMo 适配器形成可选增强，缺失时应降级而不是中断主流程。

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
   └─ tags/items
```

`examples/TMA-Custom-Voice-Pack` 是发布给用户的示例数据包，不参与模组运行时资源加载，但必须与数据包解析器保持格式一致。

## 4. 模块职责

### 4.1 启动与注册层

`TouhouMaidAffection.java` 负责配置、注册表、payload、事件和 tick 入口的装配。它是启动门面，不应承载具体业务判定。

`ModConfig.java` 只描述全局规则和默认供应商参数，不保存玩家或女仆运行结果。早安吻 AI/TTS 的运行时开关、提示词、扫描频率、缓存策略与 MiMo 默认值都在这里定义。

### 4.2 亲吻主链

`KissMaidHandler` 是亲吻服务端主入口，负责冷却、好感、粒子/音效 payload、少女祈祷触发与早安吻复用逻辑。普通右键、公主抱亲吻按键、准星目标亲吻按键最终都应收敛到这里，避免规则分叉。

客户端的 `KissKeyAction` 在公主抱亲吻和准星亲吻共用按键时做入口选择；服务端的 `KissTargetedMaidRequestHandler` 必须重新校验实体、距离、视线与归属，不能信任客户端命中结果。

### 4.3 羁绊域模型

`BondData` 保存玩家维度、女仆粒度的长期档案：羁绊等级、解锁能力、语音选择、早安吻计划、礼物队列、膝枕姿态等。

`BondManager` 是语义化门面，屏蔽底层 persistentData key。后续新增持久字段应集中在 `BondData` 或相关子域数据结构中，避免 handler 或 screen 直接拼 key。

`bond/ability` 的能力对象只描述成本、名称、解锁条件与二级行为入口。复杂流程应放到 `bond/service`、`bond/rescue` 或 handler 中。

### 4.4 长流程服务

`bond/service` 承载 tick 驱动或跨时间窗的服务：

- `MorningKissService`：早安吻调度、寻路、亲吻执行、台词展示与语音触发。
- `MorningKissGeneratedDialogueService`：基于 TLM LLM/TTS 站点异步预生成台词和 TTS 音频。
- `MorningKissGeneratedDialogueCache`：保存服务端运行时生成结果，不写回数据包，不触发 `/reload`。
- `MorningKissProfileParser` / `MorningKissProfileData`：读取早安吻静态数据包 profile。
- `InteractionVoiceProfileParser` / `InteractionVoiceProfileData`：早安吻与残血救护共享的数据包 OGG 语音解析。
- `RandomGiftService`：随机礼物积累与投递。

早安吻的架构边界非常明确：数据包负责静态台词、亲吻 sound event、预录 OGG 语音；AI/TTS 运行时行为负责配置、生成、缓存和失败回退。

### 4.5 残血救护

`bond/rescue` 管理紧急救援触发、每日救援次数、救援者身份 canonical id 与 provider/legacy 兼容。救援语音不再走旧的服务器文件同步服务，而是在触发 payload 中携带命中的数据包 OGG 字节，或回退到 TLM 音包与兜底 sound event。

`EmergencyRescueSoundPlayer` 只处理客户端播放策略，不决定救援是否成立。

### 4.6 膝枕

`bond/lap` 管理膝枕会话状态、锚点实体与姿态快照。膝枕是“服务端维持会话 + 客户端渲染桥接”的状态机：

- 长期配置落在 `BondData`。
- 当前会话落在 `LapPillowState` 与锚点实体。
- 客户端 mixin 只做渲染期睡姿/乘骑桥接，不改变服务端业务真相。

膝枕角度冻结通过独立 payload 保存会话姿态，默认键位不再与亲吻入口抢占。

### 4.7 客户端 UI

`BondMaidContainerScreen` 是羁绊页总屏幕；`screen/page` 承载一级/二级页控制；`screen/component` 提供按钮、滚动列表、弹窗、下拉框、语音池列表等复用组件。

语音配置页现在是动态语音池页面：服务端同步数据包候选，客户端补充 TLM 音包候选。玩家保存的是每名女仆的池选择与播放模式，而不是全局固定文件名。

`BondMaidGuiTabHandler` 不固定占用 TLM 顶部 tab 位置，而是运行时扫描可用位置，降低与 TLM 或其他扩展页签冲突。

### 4.8 AI / MiMo 适配层

`ai/mimo` 是第三方模型协议适配层。它通过 TLM 的扩展入口注册 `tma_mimo_chat` 与 `tma_mimo_tts` 站点类型：

- LLM 侧保持 OpenAI 风格站点兼容，尽量复用 TLM 原生聊天客户端和工具调用语义。
- TTS 侧解析 MiMo chat-completions 风格响应中的 base64 音频，交给 TLM 播放链路。
- API key、启用状态与站点保存仍由 TLM 管理；TMA 只提供默认 URL、模型、格式与站点类型。
- TMA 不接管 TLM STT，也不把远程服务失败变成阻断错误。

### 4.9 兼容层

`ysm`、`mixin`、`ai/<provider>` 与小型 helper 是外部生态适配的边界。与 YSM、CarryOn、TLM GUI、TLM 音包、TLM AI 的适配逻辑应保持隔离，不能扩散成到处可见的条件分支。

## 5. 数据与配置边界

项目存在四类状态容器：

| 容器 | 用途 |
|---|---|
| `ModConfig` | 全局规则、AI 默认值、运行时开关。 |
| `BondData` | 玩家-女仆长期关系档案。 |
| NeoForge Attachment | 玩家当前能力槽、每日次数等独立运行态。 |
| 内存任务表/缓存 | 当前服务器会话内的冷却、任务、AI 预生成结果。 |

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

## 8. 演进规范

- 新增能力必须经过 `bond/ability` 与 `BondAbilityManager`，复杂流程放到 service 或 handler。
- 新增跨 tick 功能必须拆分长期状态与运行态，不能只依赖静态 `Map`。
- 新增外部供应商适配应放到 `ai/<provider>`，不要改写早安吻服务本体来绑定供应商。
- 新增 GUI 功能优先抽 page/component，避免继续膨胀 `BondMaidContainerScreen`。
- 新增数据包字段必须同步教程、示例包与解析测试。
- 发布前必须同步 `CHANGELOG.md`、README 双语入口、`PROJECT_ARCHITECTURE.md` 与 `mod_version`。

## 9. 当前复杂度中心

最需要持续治理的文件/模块是：

- `MorningKissService`：已同时承载调度、任务推进、对话、语音策略与多个回退路径。
- `BondMaidContainerScreen` 与二级页：界面状态、tooltip、弹窗、动态语音池、页面切换都在此附近集中。
- `BondData`：长期状态字段持续增多，后续应优先收敛 key 常量与子结构。

后续重构的优先方向是把早安吻拆成调度器、任务执行器、对话策略、语音策略四块；把羁绊页继续拆成更独立的 page controller 与状态对象。
