# PROJECT_ARCHITECTURE

## 1. 项目全局意图

`Touhou Maid: Affection` 是一个构建在 `Touhou Little Maid` 之上的 NeoForge 扩展模组，目标不是重写女仆系统，而是在原有“女仆归属、好感度、模型与音包生态”之上，增加一条更强调陪伴感与长期成长的互动层。

它当前的核心业务闭环可以概括为四件事：

- 把“亲吻”从一次即时交互，扩展成带冷却、好感提升、粒子/镜头/音效反馈的主互动入口。
- 把高好感女仆抽象成“羁绊对象”，围绕其逐步解锁可持续能力，而不是一次性奖励。
- 把能力设计成“服务器判定 + 客户端展示 + 配置驱动”的模块化系统，便于后续继续追加新互动。
- 在不硬绑定外部模组的前提下，向 `YSM / CarryOn / TLM GUI / TLM SoundPack` 等生态做软兼容桥接。

从系统定位上看，这个项目是一个“以女仆为中心的关系增强层”，而不是一个通用框架库；所有架构选择都服务于“特定女仆、特定玩家、长期关系状态”的持续演进。

## 2. 核心技术栈与环境

### 2.1 语言与运行时

- `Java 21`
- `Gradle`
- `NeoForge ModDev 2.0.95`
- Minecraft `1.21.1`
- NeoForge `21.1.18`
- Parchment mappings `2024.11.17`

### 2.2 核心依赖与生态关系

- `Touhou Little Maid 1.5.1+`：唯一强依赖，当前编译目标为 `1.5.2-neoforge+mc1.21.1`，提供女仆实体、好感度、GUI、音包、交互事件与新版 AI 聊天架构等基础能力。
- `NeoForge Attachment / EventBus / Payload API`：用于挂接状态、监听事件、注册网络消息。
- `Mixin`：用于改写 TLM 的 GUI 名称渲染与“副手诱饵物品”行为。
- `Yes Steve Model (YSM)`：软兼容，仅在存在时触发动画或读取动作资源。
- `CarryOn`：软兼容，仅用于规避按键/右键交互冲突。
- `Modrinth Minotaur`：发布流程依赖，不影响运行时架构。

### 2.3 工程环境判断

- 工程是标准单模块 NeoForge 模组工程，没有额外子模块。
- 运行端是“单包内同时包含服务端逻辑与客户端逻辑”的典型 Mod 结构。
- 服务端是真正的状态来源；客户端主要承担缓存、界面、视觉和音频表现。

### 2.4 工程约束文档

- `TESTING.md`：维护测试范围、命名约定和最小回归命令（`test + compileJava`）。
- `DEPLOYMENT.md`：维护构建产物、发布约束和发版前检查清单。
- `CHANGELOG.md`：维护用户可感知的版本变更历史（`Added/Changed/Fixed/Removed`）。

### 2.5 CI/CD 与发布链路（当前仓库事实）

- 工作流文件为 `.github/workflows/build.yml`，触发条件是：`push main`、`pull_request main`、以及 tag `v*`。
- `build` 作业在 `ubuntu-latest + Java 21` 上执行 `./gradlew build`，并上传 `build/libs/touhou-maid-affection-*.jar`。
- `release` 作业仅在 `refs/tags/v*` 下运行：先创建 GitHub Release，再按 token 条件发布到 Modrinth 与 CurseForge。
- Gradle 发布侧使用 `com.modrinth.minotaur`；`modrinth` 任务依赖 `MODRINTH_TOKEN`，项目正文当前由 `README.md` 同步（`syncBodyFrom`）。
- `gradle.properties` 中的 `mod_version` 是产物版本源；tag 是自动发布闸门，两者需要保持一致。

### 2.6 关键外部参考入口（长期有效）

- Touhou Little Maid 源码（1.21 分支）：`https://github.com/TartaricAcid/TouhouLittleMaid/tree/1.21`
- TLM 附属开发示例：`https://github.com/TartaricAcid/TLMAdditionExample`
- NeoForge 文档：`https://docs.neoforged.net/`
- NeoForge Mixin 指南：`https://docs.neoforged.net/docs/advanced/mixin/`

## 3. 架构与目录拓扑

### 3.1 核心目录树

```text
.
├─ build.gradle
├─ TESTING.md
├─ DEPLOYMENT.md
├─ gradle.properties
├─ settings.gradle
├─ src/main/java/com/github/touhoumaidaffection
│  ├─ TouhouMaidAffection.java
│  ├─ ModConfig.java
│  ├─ ModAttachments.java
│  ├─ ModEffects.java
│  ├─ ModSounds.java
│  ├─ bond
│  │  ├─ BondConfig.java
│  │  ├─ BondData.java
│  │  ├─ BondManager.java
│  │  ├─ MorningKissVoiceSettings.java
│  │  ├─ ability
│  │  ├─ lap
│  │  ├─ rescue
│  │  └─ service
│  │     ├─ InteractionVoiceProfileData.java
│  │     ├─ InteractionVoiceProfileParser.java
│  │     ├─ MorningKissScheduleRules.java
│  │     ├─ MorningKissProfileData.java
│  │     └─ MorningKissProfileParser.java
│  ├─ client
│  │  ├─ BondClientPayloadHandler.java
│  │  ├─ BondClientStateCache.java
│  │  ├─ *Key*Handler.java
│  │  ├─ Kiss* / EmergencyRescue* / MorningKissVoice* / YsmModelActionIndex.java
│  │  └─ screen
│  │     └─ component
│  ├─ command
│  ├─ effect
│  ├─ handler
│  │  └─ MaidPayloadResolver.java
│  ├─ inventory
│  ├─ mixin
│  ├─ network
│  ├─ util
│  │  └─ PowerPointInventoryHelper.java
│  └─ ysm
└─ src/main/resources
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

### 3.2 目录职责边界

#### 根级启动与注册文件

- `TouhouMaidAffection.java`
  - 负责：模组启动、配置注册、音效/效果/Attachment 注册、Payload 注册、全局事件挂接。
  - 不负责：具体业务判定、UI 绘制、具体能力实现。

- `ModConfig.java`
  - 负责：全部公共配置项定义与枚举解析。
  - 不负责：运行时状态保存；它只描述规则，不保存结果。

- `ModAttachments.java` / `ModEffects.java` / `ModSounds.java`
  - 负责：NeoForge 注册表对象声明。
  - 不负责：业务调度。

#### `bond/`：羁绊域模型层

- `BondData.java`
  - 负责：玩家持久化羁绊数据的底层读写，存于 `ServerPlayer.persistentData`。
  - 不负责：复杂业务流程编排；它是数据仓库，不是服务层。

- `BondManager.java`
  - 负责：羁绊域的统一门面，屏蔽 `BondData` 的 key 细节，对外提供语义化 API。
  - 不负责：事件监听、网络处理、客户端显示。

- `bond/ability/`
  - 负责：定义“能力”接口与默认能力注册表。
  - 不负责：网络传输与界面渲染。

- `bond/service/`
  - 负责：长生命周期、持续 tick 驱动的业务编排。
  - 当前主要包含 `MorningKissService`、`RandomGiftService`，以及用于晨安吻时间窗解析、数据包 profile 解析与数值修正的 `MorningKissScheduleRules` / `MorningKissProfileData` / `MorningKissProfileParser`。
  - `InteractionVoiceProfileParser` / `InteractionVoiceProfileData` 是早安吻与残血救护共享的数据包语音入口，但配置文件按功能隔离：早安吻读取 `data/touhou_maid_affection/morning_kiss/profile.json` 与 `morning_kiss/voices/`，残血救护读取 `data/touhou_maid_affection/emergency_rescue/profile.json` 与 `emergency_rescue/voices/`。每个功能目录只保留一份 `profile.json`，不再拆出额外的语音池 JSON，也不再把两个功能混在同一个根文件里。

- `bond/rescue/`
  - 负责：紧急救援能力的独立状态与触发逻辑。
  - 特点是同时使用 `Attachment` 存放“每日救援电量”，与 `BondData` 中的女仆档案信息协作。
  - 该目录维护“救援者池 canonical id（`maid:<uuid>`）”与 legacy/provider 兼容解析，并基于 provider 贡献者身份做跨 UUID 去重，避免同一女仆复活后重复计入。
  - 该目录还维护服务端数据包音效配置（兼容 `rescue_sound/profile.json`）与玩家个人开关状态；救援预录语音统一改由 `InteractionVoiceProfileData` 读取并随触发 payload 下发，不再维护旧的服务器/客户端文件同步服务。

- `bond/lap/`
  - 负责：膝枕姿态配置、会话状态与中间锚点实体抽象。
  - 不负责：能力解锁。

#### `handler/`：网络入站与服务端交互入口

- 负责：处理 `client -> server` 的 payload 请求，做身份校验、实体解析、权限判定和状态更新。
- 不负责：复杂持久化细节；应委托给 `BondManager` 或具体 service。

这是当前服务端“命令式入口层”，包括：

- `KissMaidHandler`：直接基于 TLM 事件处理亲吻。
- `BondAbilityActivateHandler` / `BondStateRequestHandler`：处理羁绊页按钮与状态同步请求。
- `LapPillowHandler` / `MorningKissVoiceConfigHandler` / `RescueActionConfigHandler` / `RescueVoiceConfigHandler`：处理特定子功能配置或动作触发。
- `MaidPayloadResolver`：统一处理 payload 中的女仆解析与 owner 校验，减少重复判断和漏检风险。

#### `network/`：协议层

- 负责：定义所有自定义 payload 的编码/解码结构。
- 不负责：业务逻辑。

设计上很干净：每个消息一个 `record`，字段即协议本体。
晨安吻数据包语音链路新增 `MorningKissDataVoicePlayPayload`，用于把服务端数据包内的 OGG 语音按触发时机发送给对应客户端播放；它与 TLM 音包语音 payload 分离，避免把“数据包预录语音”和“TLM 声包选择”混成同一个协议。
紧急救援音频链路收敛到 `MaidRescuePopPayload`：触发 payload 同时携带女仆档案、TLM 音包选择、兜底 sound event，以及可选的数据包 OGG 字节。旧的救援音频资源同步 payload 已移除。

#### `client/`：客户端展示与缓存层

- `BondClientStateCache`
  - 负责：缓存服务端同步来的羁绊页状态。
  - 不负责：权威判定；任何“是否真的解锁”都应以服务端为准。

- `BondClientPayloadHandler`
  - 负责：`server -> client` payload 分发。
  - 不负责：复杂渲染逻辑本身。

- `Kiss*` / `EmergencyRescue*` / `MorningKissVoice*`
  - 负责：镜头、粒子、按键、救援弹出、语音检索与播放。
  - `EmergencyRescueSoundPlayer` 现在只处理两类主要语音源：随 payload 下发的数据包 OGG，以及 TLM 模型/音源包中的语音；若两者都不可用，才播放服务端指定的兜底 sound event。
  - 不负责：服务端状态存储。

- `screen/` 与 `screen/component/`
  - 负责：羁绊分页 UI 与可复用的列表、弹窗、按钮排版组件。
  - 不负责：服务端解锁逻辑。

#### `ysm/`：外部动画桥接层

- 负责：YSM 是否存在的检测、动画名桥接、资源动作索引辅助。
- 不负责：主业务流程。

这是一个典型的“可有可无增强层”，不存在时主功能应继续可用。

#### `mixin/`

- 负责：对第三方既有行为做最小侵入修补。
- 不负责：新增主业务。

当前只用于：

- 扩展 TLM 的副手诱饵逻辑。
- 调整 TLM GUI 中的女仆显示名来源。
- 为膝枕提供“乘骑时改用睡姿渲染”的客户端桥接。

#### `resources/`

- `META-INF/neoforge.mods.toml`：模组元信息与依赖声明。
- `touhou_maid_affection.mixins.json`：Mixin 装配清单。
- `assets/...`：语言、贴图、声音。
- `data/.../tags/items`：随机礼物池/黑名单数据定义。

## 4. 核心特性与模块编排

### 4.1 模块总览

当前代码库的主模块不是按“系统层”切开的，而是按“特性域”自然形成：

1. `亲吻互动主链`
2. `羁绊解锁与能力系统`
3. `晨安吻服务`
4. `随机礼物服务`
5. `紧急救援系统`
6. `膝枕姿态系统`
7. `客户端羁绊页与配置子页`
8. `YSM / CarryOn / TLM GUI / TLM SoundPack 兼容桥接`

下面按真实生命周期展开。

### 4.2 亲吻互动主链

#### 生命周期

- 玩家在服务端触发 `InteractMaidEvent`。
- `KissMaidHandler` 校验姿势、空手条件、CarryOn 兼容条件。
- 基于 `MinecraftServer` 级 `SessionState` 计算“玩家-女仆”二元冷却。
- 应用 TLM 好感度、播放音效、发送 `KissMaidPayload` 给追踪客户端。
- 客户端 `KissClientHandler` 决定是否触发 FOV 拉近，并将粒子效果排队给 `KissParticleEffectManager`。
- 若短时间内亲吻次数达阈值，再由服务端施加 `MaidsPrayerEffect`。

#### 数据流向

- 短时状态：保存在 `KissMaidHandler.SessionState`，按服务器实例隔离。
- 长期状态：女仆羁绊等级会回写进 `BondData`。
- 表现层状态：粒子与镜头完全在客户端本地推进。

#### 耦合关系

- 与 `BondManager` 弱耦合：只同步羁绊档案。
- 与 `ModConfig` 强耦合：几乎全部阈值可配置。
- 与客户端表现层通过单向 payload 解耦。

### 4.3 羁绊解锁与能力系统

#### 核心结构

- `BondData` 保存“某玩家对某女仆”的羁绊等级、是否解锁、已解锁能力、档案信息及各类附属状态。
- `BondManager` 提供统一读写 API。
- `IBondAbility` 是能力扩展点。
- `BondAbilityManager` 是能力注册中心。

#### 生命周期

- 女仆好感达到 `BondConfig.DEFAULT_UNLOCK_LEVEL` 后，视为进入羁绊系统。
- 玩家在羁绊页点击按钮，客户端发 `BondActivateAbilityPayload`。
- `BondAbilityActivateHandler` 完成：
  - 能力查找
  - 女仆实体解析
  - 羁绊解锁校验
  - P 点消耗
  - 解锁或执行二级动作
  - 状态回包 `BondStateSyncPayload`

#### 当前能力版图

- `lap_pillow`：解锁后通过热键进入膝枕状态。
- `emergency_heal`：解锁后为每日紧急救援池贡献次数。
- `morning_kiss`：解锁后可手动呼叫，且可参与自动晨安吻调度。
- `random_gift`：解锁后开始进入礼物积累与投递循环。
- `ysm_action`：配置和类已存在，但当前未注册进默认能力表，属于预留/未启用能力。

#### 架构特点

- 能力对象本身非常薄，只描述“费用、名称、可解锁条件、次级行为入口”。
- 真正复杂的行为被放进 service 或 handler，而不是塞进 ability 实现里。
- 这是一种偏“命令描述符”而不是“富领域对象”的能力设计。

### 4.4 晨安吻服务

`MorningKissService` 是当前最完整、最接近“子系统”的模块。

#### 生命周期

- 每个服务端 tick 执行两件事：
  - 自动扫描并调度符合条件的女仆。
  - 推进已创建的晨安吻任务。
- 启动任务后，女仆会寻路接近玩家。
- 进入可亲吻距离后，调用 `KissMaidHandler.performMorningKiss` 执行连续亲吻。
- 任务结束后记录“本时间窗成功/失败”状态，避免同一时间窗重复触发。

#### 数据流向

- 长期调度状态写入 `BondData`：
  - 上次成功/失败时间窗
  - 本次计划时间窗与计划 tick
  - 选中的女仆
  - 语音设置
- 短期运行状态保存在内存 `TASKS` 表中。
- 客户端只接收语音播放 payload，不参与任务判定。

#### 模块交互

- 调用 `BondManager` 读写调度元数据。
- 调用 `KissMaidHandler` 复用亲吻主逻辑。
- 调用 `YSMActionBridge` 播放晨安吻动作。
- 调用 `MorningKissVoicePlayback` 所对应的 payload 在客户端播音；语音来源现在先由服务端按玩家保存的动态语音池命中一个条目，再下发“命中项 id”或数据包 OGG 字节给客户端播放。
- 通过服务端数据包 `data/touhou_maid_affection/morning_kiss/profile.json` 读取早安吻台词池、亲吻 sound event、AI 提示词与预录 OGG 语音池。`dialogue_mode` 控制数据包台词与内置 lang 台词是 `replace` 覆盖还是 `append` 追加；`voice_mode` 控制数据包 OGG 进入玩家可选池的方式：`append` 会保留 mod 原声/TLM 基础池并追加数据包项，`replace` 在存在数据包语音时会把基础池整体排除，只保留数据包项；`dialogue` 静态台词池支持 `{maid}` / `{player}` / `{pool}` / `{time}` 占位符；`play_kiss_sound_with_voice` 用于控制早安吻预录/TLM 语音与原生亲吻音效是否同时播放。
- 在膝枕会话中，晨安吻只允许“当前膝枕女仆”继续调度与执行；同玩家的其它女仆晨安吻任务会被拦截或撤销，避免多源并发抢占。

#### 设计评价

- 这是“服务器权威任务编排 + 客户端纯表现”的正确分层。
- 自动调度、任务推进、对话/语音仍在一个服务内闭环；时间窗解析和亲吻次数边界修正已抽到 `MorningKissScheduleRules`，可独立测试与复用。
- 台词/AI 提示词已从 lang 硬编码推进到 server data profile；跨功能预录语音已进一步抽到 `InteractionVoiceProfileParser` / `InteractionVoiceProfileData`，以统一 JSON 表达“场景语音池 + 女仆匹配覆盖”。`MorningKissProfileParser` 与 `InteractionVoiceProfileParser` 都保持为纯 Java 解析器以便普通 JUnit 覆盖。
- 该服务仍是复杂度热点，但职责边界已从“全量混合”向“调度主干 + 规则模块”过渡。
- `LapPillowState` 现在同时承担“会话互斥信号”的职责：晨安吻流程会把它作为前置门禁，允许膝枕女仆内联亲吻，但阻止其它女仆插入任务。

### 4.5 随机礼物服务

#### 生命周期

- `RandomGiftService` 每秒扫描玩家附近已解锁随机礼物能力的女仆。
- 调用 `BondManager.reconcileRandomGiftQueue` 按真实墙钟时间补齐待投递礼物数。
- 若队列非空，则创建投递任务并驱动女仆寻路到玩家附近。
- 达到条件后投掷物品、播放粒子与动作、更新队列与冷却，并回包同步羁绊页状态。

#### 数据流向

- 长期状态存于 `BondData`：
  - 礼物队列数
  - 上次生成墙钟时间
  - 上次投递游戏时间
  - 上次使用的礼物生产间隔
- 短期投递任务存于 `DELIVERY_TASKS` 内存表。

#### 模块交互

- 通过 `data/.../tags/items` 注入礼物池和黑名单。
- 通过 `BuiltInRegistries.ITEM` 动态采样物品。
- 通过 `YSMActionBridge` 增强投递动画。
- 在膝枕会话中，随机礼物会停止新建投递任务，并清理同女仆的待投递运行任务，避免寻路与动作链路干扰膝枕。

#### 设计特点

- 这是一个“持久化生产队列 + 短期行为任务”的二段式系统。
- 它把“礼物产生”和“礼物送达”明确分离，避免了能力解锁后必须在线才能累计的问题。

### 4.6 紧急救援系统

这是当前唯一同时使用 `Attachment` 与 `BondData` 协作的模块。

#### 生命周期

- 救援链路改为事件驱动：`EmergencyHealListener` 监听 `LivingDamageEvent.Pre` 与 `LivingDeathEvent`，不再使用玩家 tick 轮询。
- 事件入口最前置执行轻量门禁：仅检查服务端玩家类型、`ModConfig` 全局开关与 `Attachment` 玩家个人开关，未通过直接返回。
- 仅当伤害进入“致命或阈值窗口”时，才懒刷新每日次数并进入救援计算；常规伤害不触发重逻辑。
- `LivingDeathEvent` 作为兜底链路，覆盖极端伤害流程，避免漏救援。
- 女仆解锁 `emergency_heal` 时，先按“贡献者身份（provider 优先，`maid:<uuid>` 兜底）”判重；同一女仆跨复活/换壳仅首次生效，随后才按 canonical id 进行当日即时补充。
- 玩家受到致命或濒死伤害时，监听器尝试消耗一个救援名额。
- 若成功，则取消伤害或取消死亡、回复生命并施加再生/伤害吸收/抗火。
- 随后发送 `MaidRescuePopPayload`（含音效策略）到客户端播放弹出表现。
- payload 中的 `maidUuid` 语义固定为“女仆实体 UUID（档案键）”；服务端会先按 canonical id 解析女仆，再以 provider/model 作为 fallback，避免历史脏数据导致语音源错配。
- 玩家执行 `/tma rescue` 查询时也会触发一次懒刷新，保证展示的是当天最新救援次数。

#### 数据流向

- `EmergencyRescueAttachment`
  - 保存每日剩余可用救援者列表（列表元素为 canonical id：`maid:<uuid>`）
  - 保存已注册过的救援者列表（同样按 canonical id；登录/触发时会做 legacy/provider 规范化与贡献者去重）
  - 保存最后补充日
  - 保存玩家个人残血救护开关
- `BondData`
  - 保存女仆模型、显示名、语音包 ID、YSM 配置、救援动作、救护语音来源设置等档案信息
  - 额外保存 `maidUuid -> rescue provider id` 映射，用于 legacy 恢复、女仆互转后的历史兼容与冲突判定
  - 提供 `provider id / model id -> maidUuid` 反查，且 provider 命中时会优先选择已激活 `emergency_heal` 的候选，避免错绑到旧档案
  - 客户端展示时依赖这些信息还原救援者形象
- 数据包 `data/touhou_maid_affection/rescue_sound/profile.json`
  - 保存服务端救护音效策略：`sound_event`、客户端覆盖开关、客户端自定义音效格式与最大时长限制

#### 模块交互

- 服务端：`EmergencyHealListener` + `EmergencyRescueData` + `MaidRescueContributorSyncHandler` + `EmergencyRescueSoundProfileData` + `InteractionVoiceProfileData`
- 客户端：`EmergencyRescueVisualHandler` + `EmergencyRescueOverlayRenderer` + `EmergencyRescueSoundPlayer`
- YSM：若女仆模型支持，则在 overlay 中播放预设动作
- overlay 渲染已改为“仅依赖 payload 构造临时女仆实体”，不再克隆世界内已加载女仆，避免睡姿/坐姿串扰。
- 音效来源收敛为 `数据包 OGG` 与 `TLM语音包` 两种模式：数据包模式由 `emergency_rescue/profile.json` 提供，并随 `MaidRescuePopPayload` 下发；TLM 模式使用 `RescueTlmVoiceIndex` 扫描 `sounds/maid/**` 全量语音（不再限定晨安/晚安）。触发时服务端按玩家保存的动态池选出一个命中项，数据包项随 payload 下发 OGG 字节，TLM 项随 payload 下发命中 id 由客户端播放。
- `voice_mode=replace` 时，匹配到的数据包救援 OGG 作为硬边界覆盖 TLM 包语音，即使女仆之前保存过 TLM 项也会被当前可用池过滤；`voice_mode=append` 时，数据包 OGG 默认追加到 TLM 全量池。数据包只决定哪些 OGG 进入可选池以及进入方式，不再决定具体哪位女仆播放哪条语音。
- 旧的“服务端预定义语音目录同步到客户端落盘”链路已移除，避免维护服务器文件同步、客户端目录缓存与数据包语音三套并行方案。
- 管理指令补充：`/tma rescue clear`（`/tma rescue reset` 同义）为 OP 命令，会清空 `EmergencyRescueAttachment` 的可用池/已注册列表，并把该玩家档案中所有女仆的 `emergency_heal` 解锁位重置为未激活，便于单女仆链路测试与脏状态清理。

#### 架构意义

- `Attachment` 在这里承载的是“运行中的玩家能力槽位”。
- `BondData` 承载的是“可展示、可回放的女仆身份档案”。
- `maid:<uuid>` 作为救援池与 payload 的主键，仍用于稳定客户端展示；同时引入 provider 贡献者身份作为“去重维度”，用于消除同女仆多 UUID 档案导致的重复救援次数。
- 同一能力同时提供“服务端总开关 + 玩家个人开关”，把玩法自由度控制与反作弊统计解耦。
- 这两层分工是合理的，也说明项目已经开始出现多种状态容器并存的趋势。

### 4.7 膝枕姿态系统

#### 生命周期

- 客户端按键触发 `LapPillowStartPayload` / `LapPillowExitPayload`。
- 服务端 `LapPillowHandler` 校验能力、距离与实体状态后，读取 `BondData` 中该女仆的膝枕姿态配置。
- 若允许开始，则生成本模组自注册的 `LapPillowAnchorEntity` 作为中间锚点，形成“女仆 -> 锚点 -> 玩家”的三元结构。
- `LapPillowState` 保存本次会话的女仆 UUID、锚点 UUID、姿态快照、女仆原始坐/睡状态，以及玩家进入会话前的 `noGravity` 快照，保证退出时可无损恢复。
- 会话激活前会主动清理该玩家的随机礼物投递任务，并只保留“当前膝枕女仆”的晨安吻任务，先收敛外部行为队列再进入膝枕状态。
- 姿态快照不再只保存“玩家相对女仆的单组偏移”，而是显式拆成“女仆相对锚点偏移 + 玩家相对锚点偏移”，锚点成为真正的局部坐标系原点。
- 会话开始时，锚点不再通过“反推女仆偏移”来决定根坐标，而是直接以启动瞬间的锚点位置作为会话原点；之后女仆与玩家都只根据各自的相对坐标解算世界位置，避免调一方高度时把另一方整体抬起或压下。
- 服务端每 tick 由锚点实体统一解算玩家世界坐标、女仆世界坐标以及两者的 fake-bed block pos；`LapPillowHandler` 只负责会话维持与状态切换。
- 锚点实体会把姿态快照（模式与双方偏移）作为 `SynchedEntityData` 下发客户端，客户端渲染不再依赖本地默认值推断，避免“服务端已改高度但客户端模型不动”的错位。
- 玩家处于 lying 分支时，服务端会主动解除玩家与锚点的乘骑关系，并让锚点在该模式下拒绝新增乘客，彻底切断“躺姿回流到骑乘链路”的路径。
- 躺姿分支采用“`noGravity` + 零动量 + 阈值纠偏”策略：只在偏移超过阈值时才进行安全高度探测与传送修正，同时持续清零 `deltaMovement` 与 `fallDistance`，避免每 tick 传送导致的网络抖动。
- 客户端睡姿桥接在 `exitRequested` 置位后会立刻停止续期，防止退出膝枕后残留躺姿。
- 客户端渲染仍保留“双层桥接”作为兼容兜底：主链路在 `LivingEntityRenderer#render` 改写 `isPassenger`，并通过 `EntityRenderDispatcher` 渲染深度门禁启用 `Entity#isPassenger` / `Entity#getVehicle` / `Entity#hasPose(SLEEPING)` / `Entity#getPose` / `LivingEntity#isSleeping` 的渲染期局部覆写，兼容替换渲染器与动画管线差异。
- 睡姿桥接坚持“仅渲染期生效”边界：`Entity` 与 `LivingEntity` 相关覆写必须受渲染深度门禁约束，避免影响常规游戏逻辑、网络 `SetPassengersPacket` 与原版睡眠界面状态机。
- 锚点实体本身带有 owner / maid 语义与自清理逻辑，因此即便玩家强制传送、跨维度或会话状态异常，也能在服务端自行清理残留非法锚点。
- 角度冻结由独立的 `LapPillowAngleLockPayload` 上报到服务端，状态保存在 `LapPillowState`；冻结时只锁定会话内实体姿态（玩家模型、女仆、锚点朝向），不接管第三人称镜头旋转。
- 锚点朝向在会话启动时确定，运行期不再每 tick 跟随玩家鼠标，保证“女仆/锚点相对世界静止”。
- 女仆动作源已拆成“builtin 坐/躺”和“YSM 动作”两类平级输入：选择 YSM 时不会再叠加默认坐/躺状态，切换动作时按签名变化触发一次性重放并清理残留。
- 膝枕会话期间，玩家获得复合增益 `eternal_utopia`（恒久遥远的理想乡）：每秒回血与饱和、每 tick 清除负面效果、持续重置 `TIME_SINCE_REST` 防幻翼、并通过目标切换事件让怪物失去锁定。

#### 设计特点

- 这是“客户端热键发起，服务端会话维持”的姿态状态机；实际空间基准依赖中间锚点实体，玩家传送只作为超阈值偏移时的纠偏手段而非常态更新路径。
- `BondData` 负责保存长期配置，`LapPillowState` 负责保存一次交互会话，这让“配置”与“运行中状态”第一次在膝枕系统里被明确拆开。
- `LapPillowState` 新增“当前会话女仆判定”接口，供晨安吻/随机礼物等外部服务复用，形成统一的会话互斥边界而非分散补丁。
- 客户端仍保留乐观启动，但会通过锚点实体 tag、同步到 `BondClientStateCache` 的膝枕姿态配置、以及 mixin 睡姿桥接在渲染层自校正。
- 玩家 lying 分支当前采用“非乘骑 + forced pose + 客户端渲染桥接”组合，未正式接入 fake-bed 睡眠链路；此前对 `sleepingPos` 的试探性接入已回退，因为它会把玩家拖入原版真正睡眠流程并引入黑屏与坐立抖动。当前策略优先保证跨渲染器稳定躺姿，而不是回到原版睡眠状态机。
- 为兼容会在 tick 中改写玩家姿态的第三方模组（如 crawl / 枪械姿态链路），膝枕 lying 会话新增“forced pose 写入护栏”：客户端与服务端在会话期间仅接受 `Pose.SLEEPING`，会话退出先撤销会话态再恢复 `forcedPose`，避免外部 `null/SWIMMING` 覆写导致“悬空站立”。
- 当玩家处于 lying 分支时，锚点仍负责局部坐标求解与会话存活判定，但不再承担玩家乘客挂点输出。
- 女仆处于 lying 分支时，仍借由睡眠姿态接口触发躺姿，但已避免“每 tick 同时强制传送 + startSleeping”这类抖动放大组合。
- 历史的 `GoldenDream` 效果已从注册与膝枕链路中移除，避免与当前复合效果职责重复。

### 4.8 客户端羁绊页与配置子页

#### 组成

- `BondMaidGuiTabHandler`：把羁绊页入口插入 TLM 女仆 GUI。
- `BondContainer`：菜单容器。
- `BondMaidContainerScreen`：主羁绊页。
- `screen/component/*`：弹窗、滚动列表、按钮行、分栏页等 UI 基础件。
- `BondGuiTokens`：统一维护羁绊页与二级页的设计 token（间距、控件高度、状态色、语义色），避免各页面继续散落硬编码常量。
- `util/PowerPointInventoryHelper`：统一 P 点统计/扣除逻辑，避免服务端解锁流程与客户端显示各自维护一套实现。
- `util/NamespacedPathNormalizer`：统一 `modelId/textureId` 的 namespace 剥离与路径归一化，避免在异常命名输入下出现解析越界。

#### 生命周期

- 进入页面时立刻向服务端请求 `BondStateRequestPayload`。
- 服务端回包后更新 `BondClientStateCache`。
- 页面根据缓存决定按钮状态、已解锁能力、礼物队列、晨安吻语音配置与膝枕姿态配置等。
- 早安吻与残血救护语音二级页统一改为动态语音池配置页：服务端回包同步数据包候选项，客户端扫描 TLM 音包候选项，页面通过可复用 `BondVoicePoolList` 展示多源语音并允许玩家多选；弹窗尺寸必须限制在一级羁绊页内部，避免遮挡 TLM 原生 tab/侧边按钮；批量操作使用单个动态按钮在 `全选 / 全不选` 间切换；底部播放模式按钮仅循环切换 `随机 / 顺序`，固定播放由“只勾选一条语音”自然表达，保存后把所选池写回女仆档案。
- 膝枕二级页已从单点偏移页演进为“双坐标点编辑页”：左侧同屏展示玩家/女仆在锚点坐标系下的二维相对位置，点击坐标点即可切换当前编辑对象，滚轮单独修改该对象的 Y 高度。
- 膝枕页顶部不再保留独立的“四模式”下拉，而是改成“女仆动作 + 玩家动作”两个短下拉；默认坐/躺组合会反推出四种基础模式，自定义动作则建立在这个基础姿态之上。
- 配置操作再回发新的 payload 给服务端保存。
- 二级页交互已统一为“分级关闭”：`ESC` 优先关闭当前展开的下拉层；仅当无下拉展开时才关闭整个二级页。此规则用于减少误退页并与复杂配置页交互一致。

#### 架构特点

- 客户端页面并不直接拥有服务端状态，而是显式依赖同步缓存。
- 页面视觉规范已从“页面内常量”转向“组件 token 驱动”：Modal / Dropdown / ScrollableList / ButtonRow 共享同一状态色与尺寸语义。
- 这是正确的网络边界，但也导致 `BondMaidContainerScreen` 过于庞大，已经承担：
  - 页面绘制
  - 交互命中判定
  - tooltip 生成
  - 弹窗逻辑
  - 语音配置构建
  - 救援语音来源配置构建

它是当前代码库第二个明显的复杂度中心。

### 4.9 兼容与桥接层

#### CarryOn 兼容

- 只在 `KissMaidHandler` 中做右键触发条件调整。
- 原则是“避免冲突，不接管对方逻辑”。

#### YSM 兼容

- `YSMCompatibility` 只判断模组是否存在。
- `YSMActionBridge` / `YSMAnimationHelper` 做 best-effort 调用。
- `YsmModelActionIndex` 在客户端扫描资源与 jar，提取可选动作供 GUI 配置；资源枚举阶段采用 best-effort 兜底，遇到异常资源索引时会降级回退而不是让页面崩溃。
- 动作互斥策略位于业务层（膝枕/晨安吻/随机礼物服务）而非桥接层：桥接保持薄封装，具体是否允许触发由会话状态门禁统一决定。

#### TLM GUI / 音包兼容

- Mixin 改写 GUI 中的显示名称来源。
- `MorningKissVoiceIndex` / `MorningKissVoicePlayback` 直接复用 TLM 的音包缓存系统。

这类桥接都遵循同一原则：存在即增强，不存在即静默降级。

## 5. 代码编写与演进规范

### 5.1 当前代码库体现出的命名惯例

- 类命名以职责为中心，常见后缀为：
  - `*Handler`：事件或 payload 入口
  - `*Service`：持续调度或 tick 驱动流程
  - `*Manager`：领域门面或注册中心
  - `*Data` / `*Attachment` / `*State`：状态容器
  - `*Payload`：网络协议对象
  - `*Effect` / `*OverlayRenderer` / `*Playback`：客户端表现层

- 能力 ID、动作 ID、payload 路径统一使用 `snake_case` 字符串。
- 配置项常量统一为 `UPPER_SNAKE_CASE`。
- 大多数“仅工具/门面用途”的类都使用 `final + private constructor`。
- 网络消息统一使用 Java `record`，这是当前工程里最稳定、最值得延续的协议风格。

### 5.2 当前状态管理机制

项目现在实际存在四类状态容器，职责不能混淆：

1. `ModConfig`
   - 全局规则与参数。
   - 只读，不承载玩家/女仆运行结果。

2. `BondData`
   - 玩家维度、女仆粒度的长期持久化羁绊档案。
   - 适合保存“是否已解锁、队列数、语音配置、档案快照”。

3. `Attachment`
   - 玩家当前能力槽、每日次数等更偏运行态的数据。
   - 适合像紧急救援这样的独立子系统。

4. 内存表 `Map/Task Registry`
   - 服务器会话内的短期状态。
   - 例如亲吻冷却、晨安吻任务、礼物投递任务。

后续新功能必须先明确自己属于哪一类状态，再决定落在哪个容器。

### 5.3 当前错误处理与失败策略

代码库整体采用“静默失败 + 条件前置返回”的风格：

- 网络 handler 基本都是一层层 `if (...) return;`
- 客户端播放、YSM 触发、资源解析大量使用 best-effort 兜底
- 对外部兼容失败时优先降级，而不是抛异常中断主链路
- 少数关键 fallback 会打日志，例如礼物池为空或救援 overlay 创建失败

这说明本项目的错误处理原则不是“强一致报错”，而是“优先保留游玩流程”。

后续新增代码时应保持一致：

- 玩家触发链路优先用 guard clause 早返回。
- 外部模组、资源、音包、模型相关逻辑必须允许缺失。
- 只有影响诊断价值的异常才打日志；不要在高频 tick 中大量刷日志。

### 5.4 基于现状提炼出的强制性结构规范

以下规范建议视为后续演进的硬约束。

#### 规范 A：新增能力必须走统一能力入口

新增羁绊能力时，必须同时落点于：

- `bond/ability/`：新增 `IBondAbility` 实现
- `BondAbilityManager.registerDefaults()`：注册默认能力
- `ModConfig`：补齐成本与行为配置
- 如需服务端长期编排：新增或复用 `bond/service/`
- 如需客户端配置页：在 `BondMaidContainerScreen` 中接入显示和二级页面

不要把一个新能力直接写进 `BondAbilityActivateHandler` 的条件分支里作为“匿名逻辑块”。

#### 规范 B：网络协议只做传输，不做业务

- `network/*Payload.java` 只能定义字段与编解码。
- 一切业务判断必须放在 `handler/`、`service/` 或 `bond/` 域层；同类前置校验（如 owner 判定）优先沉淀到共享 resolver，避免多处复制后出现漏判。
- 客户端 cache 是显示缓存，不是业务真相。

#### 规范 C：服务端必须保持权威

任何影响以下内容的逻辑都必须在服务端判定：

- 是否解锁
- 是否可释放能力
- 是否扣除 P 点
- 是否满足距离/时间/所有权条件
- 是否应产生奖励、礼物、救援、Buff

客户端可以做预显示和预禁用，但不能成为判定来源。

#### 规范 D：长流程必须拆成“持久态 + 运行态”

凡是跨 tick 的功能，都应参考 `MorningKissService` 与 `RandomGiftService`：

- 持久态写入 `BondData` 或 `Attachment`
- 运行态保存在内存任务表
- 每 tick 只推进任务，不把所有历史都留在内存

不要把长期可恢复状态只放在静态 `Map` 中。

#### 规范 E：兼容层必须单独隔离

与 `YSM / CarryOn / TLM` 的适配代码应继续放在：

- `ysm/`
- `mixin/`
- 或对应 feature 的小型 bridge/helper

不要把“如果装了某模组就这样做”的分支扩散到多个 service 和 screen 中。

#### 规范 F：GUI 大文件不得继续膨胀

`BondMaidContainerScreen` 已经很大。后续新增页面或配置项时，优先：

- 抽出新的 `component`
- 抽出独立 `Page Controller` 或 `Page State` 类
- 保持 screen 主类只负责“页面切换与总调度”

不要继续在一个类里叠加更多渲染、点击、tooltip、列表组装逻辑。

#### 规范 G：持久化 key 必须统一收口

当前 `BondData` 仍大量使用字符串拼接 key，例如 `BondLevel_<uuid>`。
后续若继续扩展：

- 至少保持 key 前缀命名统一
- 同一特性的数据 key 必须集中写在对应 `Data/Attachment` 类中
- 不允许在 handler 或 screen 中直接拼接 persistentData key

#### 规范 H：新增功能先判定其归属层

添加任意新功能前，先回答这四个问题：

1. 它是“能力”还是“基础互动”？
2. 它的长期状态放 `BondData`、`Attachment` 还是根本不持久化？
3. 它是否需要单独 payload？
4. 它是客户端表现增强，还是服务端业务新增？

只有先明确这四点，代码结构才不会继续横向污染。

### 5.5 对未来演进的架构建议

从当前代码规模看，最值得优先治理的不是功能缺失，而是两个复杂度中心：

- `MorningKissService`
- `BondMaidContainerScreen`

建议未来如果继续扩展：

- 把晨安吻拆成“时间窗解析 / 调度器 / 任务执行器 / 对话语音策略”四层。
- 把羁绊页拆成“能力列表页控制器 / 救援配置页 / 语音配置页 / tooltip 组装器”。
- 为 `BondData` 引入更明确的 key 常量区或子结构，降低字符串拼接扩散。

当前整体架构并不混乱，主问题是“功能越来越多后，少数文件开始变成总控中心”。只要后续新增功能继续遵守“域层存状态、handler 做入口、service 跑长流程、client 只做表现”的边界，这个项目仍然具备继续扩展的可维护性。

### 5.6 工程守护清单（从历史 Agent 规范提炼并校正）

- 发布前必须同时校验 `mod_version` 与 Git tag 对齐；当前 release 自动化由 `v*` tag 触发。
- `gradle/wrapper/gradle-wrapper.jar` 必须保留在仓库内，否则 CI 无法使用 wrapper 构建。
- Touhou Little Maid 依赖应保持 `compileOnly`，避免把上游模组打进产物造成体积膨胀与潜在冲突。
- 音效资源最终格式必须是 OGG（Minecraft 运行时约束），源格式可在独立目录维护。
- 以第三方 mod 类为目标的 Mixin，应显式评估并在需要时设置 `remap = false`，避免映射链路误判。

### 5.7 变更记录与发布叙事规范（强制）

为确保“每次更新都有迹可循”，变更信息必须分层维护，不得混用：

- `CHANGELOG.md`（主记录）：面向用户与整合包作者，记录“这个版本新增/变更/修复了什么”。
- `README*.md`（入口页）：面向新用户，保留安装与使用说明；仅允许放“最新变更摘要 + CHANGELOG 链接”。
- `Git Commit Message`（开发轨迹）：面向协作者，记录微观实现细节、重构步骤、issue 对应关系。

执行规则如下：

1. **触发条件（强制）**：只有在以下任一条件满足时，才允许执行一次“更新总结（release summary）”：
   - 手动检测到版本号提升（如 `gradle.properties` 的 `mod_version` 变更，或出现新的版本 tag）。
   - 用户明确要求“提升版本号/进行版本总结”。
2. 若未满足触发条件：允许继续开发与提交，但**不得**生成新的版本总结条目（包括 README 版本摘要和正式 release 文案）。
3. 触发后，发布前必须更新 `CHANGELOG.md` 的 `Unreleased`，按 `Added / Changed / Fixed / Removed` 分类整理用户可见变更。
4. 打版本 tag 时，必须将 `Unreleased` 切分为对应版本号条目并写入日期（倒序，最新在上）。
5. GitHub Release 文案应优先复用该版本在 `CHANGELOG.md` 中的内容，避免手写分叉。
6. Commit message 不能替代 changelog：commit 可细、可多；changelog 必须是面向用户的一次性摘要。
7. README 不承载完整历史；若需要展示版本动态，仅展示最近 1 个版本摘要并指向 `CHANGELOG.md`。
8. 任何 Hotfix（包括崩溃修复）在进入“版本提升触发窗口”后，必须在 `CHANGELOG.md` 的 `Fixed` 下留痕，禁止“只改代码不记变更”。
