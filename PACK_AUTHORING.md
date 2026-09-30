# 整合包作者指南：礼物池、语音与配置

本文对应 **Minecraft 1.20.1 / Forge**，按当前实际加载器与配置源码编写。只需 JSON 与 TOML 即可完成常见整合包调整，不需要修改 Java。

## 1. 能改什么，应该用什么包

| 需求 | 接口 / 安装位置 | 能力与边界 |
| --- | --- | --- |
| 限定礼物白名单、排除破坏平衡的物品 | 服务端数据包：两个物品标签 | 支持原版与模组物品、可选依赖、标签引用；黑名单优先 |
| 广泛随机 / 仅标签池、准备速度、积攒上限、送达行为 | `config/touhou_maid_affection-common.toml`；部分项目可在游戏内齿轮设置修改 | 服务端规则；数据包不能设置这些 TOML 项 |
| 早安吻静态台词、语种、配对字幕 | 数据包 `morning_kiss/profile.json` | `morning` / `evening` / `general` 三池；不是任意事件脚本 |
| 早安吻与残血救护预录 OGG | 数据包各功能的 `voices/` 与 `profile.json` | 模组读取并通过网络发送选中的音频；不是普通 `assets/` 声音包 |
| AI 提示词、语言、缓存与 TTS 策略 | COMMON 配置 / 游戏内设置；AI 站点使用 TLM 已有设置 | 不在 profile JSON 里配置 API 密钥或生成规则 |
| 界面翻译、内置台词翻译、标准声音资源 | 客户端资源包 `assets/` | 不改变服务端礼物池；需要客户端安装/启用 |
| 每个物品的权重、数量、附魔、NBT / 数据组件、自定义战利品表 | **没有对应礼物接口** | 礼物按候选物品抽取，每份是默认物品堆栈、数量 1；不能靠添加 JSON 字段扩展 |
| 按女仆 / 生物群系 / 任务进度配置不同礼物池 | **没有对应数据包接口** | 当前礼物规则是全局的；需要额外代码集成 |

**是否够用？** 对普通整合包的“删除危险物品、加入本包食物、控制产出频率、换台词和配音”已经够用，标签方案也方便与其他数据包组合。最可控的配方是“仅标签池 + `replace: true` 白名单 + 黑名单 + TOML 节奏配置”。如果需要经济系统的精确权重、任务条件或定制物品堆栈，当前接口不够，不能把它当作战利品表系统。

## 2. 版本差异与安装

| Minecraft | 数据包 `pack_format` | 客户端资源包 `pack_format` | 礼物物品标签目录 |
| --- | --- | --- | --- |
| 1.20.1 / Forge | 15 | 15 | `data/touhou_maid_affection/tags/items/`（复数） |
| 1.21.1 / NeoForge | 48 | 34 | `data/touhou_maid_affection/tags/item/`（单数） |

本文后续可复制目录和 `pack.mcmeta` 均使用本分支 **1.20.1** 的格式。`morning_kiss/`、`emergency_rescue/`、`rescue_sound/` 是模组自己的目录，两分支不改名。不要直接把资源包格式号用于 1.21.1 数据包。

以文件夹包 `TMA-Pack-Overrides` 为例：

```text
TMA-Pack-Overrides/
├─ pack.mcmeta
└─ data/
   └─ touhou_maid_affection/
      └─ tags/
         └─ items/
            ├─ bond_random_gift_pool.json
            └─ bond_random_gift_blacklist.json
```

`TMA-Pack-Overrides/pack.mcmeta`：

```json
{
  "pack": {
    "pack_format": 15,
    "description": "TMA 礼物池与语音定制"
  }
}
```

1. 单人：放进 `.minecraft/saves/你的存档/datapacks/`；专服：放进实际世界目录的 `datapacks/`（通常是 `world/datapacks/`）。这两个存档位置说明不是要创建名为“你的存档”的目录。
2. ZIP 包内必须直接看到 `pack.mcmeta` 和 `data/`，不要多套一层 `TMA-Pack-Overrides/`。
3. 在世界中以管理员执行下列命令；若已启用，不需要重复 enable：

```mcfunction
/datapack list available
/datapack list enabled
/datapack enable "file/TMA-Pack-Overrides" last
/reload
```

上面的 enable 命令对应本教程的**文件夹**包；ZIP 包名以 `/datapack list available` 的实际结果为准。`last` 将包放在高优先级位置。后面新增更高优先级的包仍可能覆盖或扩充它。

分发整合包时要同时安排世界数据包与 COMMON 配置的安装；仅把包放在 `resourcepacks/` 不会启用这些服务端规则。客户端资源包则放 `resourcepacks/` 并在资源包菜单启用，资源重载用 `F3+T`，不等于 `/reload`。

## 3. 可直接使用的礼物白名单与黑名单

### 3.1 只送少量日用品（推荐）

文件：`data/touhou_maid_affection/tags/items/bond_random_gift_pool.json`

```json
{
  "replace": true,
  "values": [
    "minecraft:apple",
    "minecraft:bread",
    "minecraft:cookie",
    "minecraft:paper",
    { "id": "touhou_little_maid:power_point", "required": false },
    { "id": "touhou_little_maid:film", "required": false }
  ]
}
```

这些是实际物品 ID，没有示意占位 ID。可选项沿用模组内置礼物标签的 TLM 物品：注册表中不存在时会跳过，而不是让标签因为缺少必需条目而失败。加入其他模组时也可使用同样的 `{ "id": "命名空间:物品路径", "required": false }` 结构，但必须先换成该版本实际存在的物品 ID；`required: false` 不会自动安装模组或注册物品。

文件：`data/touhou_maid_affection/tags/items/bond_random_gift_blacklist.json`

```json
{
  "replace": false,
  "values": [
    "minecraft:tnt",
    "minecraft:diamond",
    { "id": "touhou_little_maid:power_point", "required": false }
  ]
}
```

这里故意把 `power_point` 同时放进两个标签以展示优先级：**黑名单胜出，P 点不会作为新准备的礼物**。TNT 和钻石在广泛池模式也会被排除。若你的整合包希望送 P 点，删除黑名单的那一项。

然后关闭游戏/服务器，在生成的 COMMON 配置文件中找到并修改已有表（不要重复声明同名表）：

```toml
[bondCosts.randomGiftBehavior]
enabled = true
curatedPoolOnly = true
intervalRealMinutes = 20
maxQueuedGifts = 7
```

重新启动；也可以由有权限的玩家在「羁绊标题右侧齿轮 → 功能 → 礼物准备」切换“仅标签物品池”、间隔和上限。升级时会保留旧设置，不要把新安装的默认值当作已有存档的实际值。

### 3.2 `replace`、标签引用与合包

- 礼物标签使用 **Minecraft 原生物品标签**，顶层是 `replace` 和 `values`，不是 TMA 专属配置结构。
- `replace: false` 合并低优先级包的同名标签；`replace: true` 丢弃此前低优先级定义。它仅作用于这个标签，不会清空另一个标签，也不会改变 TOML 池模式。
- 高优先级包仍可追加或再次替换，所以安装后应确认数据包顺序。
- `values` 也接受原生标签引用。例如 `"#minecraft:flowers"` 引用该版本的花物品标签；可选引用写成 `{ "id": "#minecraft:flowers", "required": false }`。
- 在同一标签中重复一个物品不会增加权重。不存在 `weight`、`count`、`nbt`、`components` 或 `loot_table` 礼物字段。
- [内置白名单](src/main/resources/data/touhou_maid_affection/tags/items/bond_random_gift_pool.json) 是日用品、食物、花与可选 TLM 物品；[内置黑名单](src/main/resources/data/touhou_maid_affection/tags/items/bond_random_gift_blacklist.json) 为空。若想完全掌控种类，必须替换白名单，而不是只追加几个条目。

### 3.3 广泛池与仅标签池的实际优先级

1. **硬性有效性**：空气、空默认堆栈、不能放入容器物品的物品不会入池；白名单不能绕过这层。
2. **黑名单**：两种模式都排除，优先于显式白名单。
3. **显式白名单**：有效且未被禁用的条目总能入池，包括模组物品；不受 `includeModItems` 与 `autoModSampleSize` 限制。
4. `curatedPoolOnly = true`：只使用上述标签条目；两个自动模组采样配置不生效。
5. `curatedPoolOnly = false`（新配置默认）：在显式白名单之外加入有效的原版物品，以及按配置抽样的非原版物品。自动原版池默认排除屏障、命令方块类、结构方块/结构空位、拼图方块、光源方块、调试棒、知识之书等管理物品；**显式白名单可重新引入其中满足有效性要求的物品**，不要误把这层默认排除当作安全黑名单。

广泛池默认 `includeModItems = true`、`autoModSampleSize = 96`。采样按模组命名空间轮流取样，**每一批准备时重新随机抽样**；96 是自动模组样本的总上限，不是每个模组 96 个，也不是礼物数量。设为 0 或关闭 includeModItems 仅关闭自动引入，显式加入标签的模组物品仍可出现。最终从该批候选中等概率抽取，每份数量为 1；这不意味着整个注册表的所有物品具有完全相同的最终概率。

## 4. 时间、队列与重载边界

以下字段均在 **`[bondCosts.randomGiftBehavior]`** 中：

| 字段 | 默认值 | 合法范围 / 含义 |
| --- | --- | --- |
| `enabled` | `true` | 自动礼物总开关；不是清队列按钮 |
| `curatedPoolOnly` | `false` | 是否仅标签池 |
| `intervalRealMinutes` | `20` | 1–1440，真实分钟，不是游戏日或游戏 tick |
| `maxQueuedGifts` | `7` | 1–64，每个女仆的已赚取礼物上限 |
| `deliverySearchRange` | `24` | 4–128，寻找附近女仆的距离 |
| `deliveryReachDistance` | `2.25` | 0.5–16.0，基础送达距离；实际投递还含接近/超时宽限，不是严格距离围栏 |
| `deliveryCooldownTicks` | `40` | 0–24000，同一女仆两次自动投递间隔，正常 20 tick/秒 |
| `pathfindTimeoutTicks` | `200` | 20–24000，单次寻路任务超时 |
| `showActionBar` | `true` | 送出礼物时是否显示提示 |
| `includeModItems` | `true` | 广泛池自动采样模组物品 |
| `autoModSampleSize` | `96` | 0–2048，自动模组样本总数 |

`[bondCosts]` 下的 `randomGift = 6`（0–9999）是解锁 P 点成本，不是抽取数量或权重。

- **计时与选物是两步**：真实时间先累积已赚取的礼物份额，实际处理附近、已解锁且属于玩家的女仆时再补齐具体物品 ID。未加载女仆不会因打开状态页而强制加载。
- 已准备物品以 FIFO 顺序持久保存。离线/卸载期间没有后台投递；下次符合处理条件时按保存的真实时间补算，最多到配置上限。
- 改 `intervalRealMinutes` 会在后续结算时从当前时间重新建立计时基准，不会按新间隔把全部旧时间重算成大量礼物。
- 降低 `maxQueuedGifts` **不删除已赚取或已准备礼物**；旧队列可暂时高于新上限，消耗后才恢复积攒。
- `/reload` 重新加载标签与语音 profile，**不是礼物重抽命令**。仍符合新规则的已备物品保留；失效/被禁的已备 ID 在后续队列处理时剔除，但保留已赚取份额，有合法候选时再补齐。空池不会凭空产生替代礼物。
- “已积攒”可多于“已备物品”；状态页是只读快照，点“刷新”不会抽物品或推进队列。新增白名单条目不保证立刻出现在已有合法队列中。
- 送出还需女仆已加载、同维度、在搜索范围内、可接近玩家且冷却结束；膝枕期间会暂停自动投递。到期不等于已送达。
- `/reload` 不是 COMMON 配置文件专用重载命令。直接改 TOML 最稳妥的做法是停服后修改再启动；运行中需要调整的项目优先使用游戏内面板，避免保存覆盖。

## 5. 静态台词与 OGG 配音

### 5.1 不带音频也能运行的静态台词示例

文件：`data/touhou_maid_affection/morning_kiss/profile.json`

```json
{
  "dialogue_mode": "replace",
  "dialogue": {
    "morning": [
      { "text": "早安，{player}！今天也让 {maid} 陪你冒险吧。", "language": "zh_cn" }
    ],
    "evening": ["辛苦了，{player}，今天也平安回来了呢。"],
    "general": ["{maid} 为你准备了一个亲亲。"]
  }
}
```

文本支持 `{player}`、`{maid}`，也支持 `{pool}`（池名）和 `{time}`（配置的允许时间段）。`dialogue_mode: "replace"` 替换内置台词候选；`append` 与内置台词一起参与选择。当前时段池为空时尝试 `general`，仍为空则回退内置台词，不能用空数组实现静音。

**测试静态台词前暂时关闭 AI**：可用 `/tma morning_kiss ai off`。生成台词/即时 AI 成功时会优先于静态池；`dialogue_mode` 不会禁用 AI。测试完成后按整合包需求恢复。

### 5.2 使用现成音频的完整示例

最方便的起点是复制整个 [TMA-Custom-Voice-Pack 示例目录](examples/TMA-Custom-Voice-Pack)，其中已经包含实际 OGG 文件及正确的数据包格式。也可把其中的 `data/` 合并到上面的礼物包。不要只复制声明音频文件名的 JSON 而漏掉音频。

使用示例包内已有的 `data/touhou_maid_affection/morning_kiss/voices/morning_test.ogg`，将早安吻 profile 设置为：

```json
{
  "kiss_sound_event": "touhou_maid_affection:touhou_maid_affection.kiss",
  "play_kiss_sound_with_voice": false,
  "voice_mode": "replace",
  "dialogue_mode": "replace",
  "dialogue": {
    "general": ["早安，{player}。"]
  },
  "voice_files": [
    {
      "file": "morning_test.ogg",
      "language": "zh_cn",
      "text": "早安，主人。测试语音播放成功。",
      "text_language": "zh_cn"
    }
  ]
}
```

残血救护使用示例包实际存在的 `data/touhou_maid_affection/emergency_rescue/voices/rescue_test.ogg`；文件 `data/touhou_maid_affection/emergency_rescue/profile.json`：

```json
{
  "voice_mode": "replace",
  "voice_files": ["rescue_test.ogg"],
  "sound_event": "minecraft:entity.player.levelup"
}
```

- `voice_mode` 的 `append` / `replace` 决定数据包语音是否与基础语音池组合；`replace` 在有有效数据包语音时只保留数据包池，不是清空所有其他数据包文件的指令。女仆已有的语音选择也会影响实际播放，请在对应能力的语音设置里确认选中项。
- `voice_files` 是相对于对应 `voices/` 的文件名列表，不是 `assets/` 资源 ID；支持小写字母、数字、下划线、短横线和子目录，扩展名必须是 `.ogg`。禁止绝对路径、反斜杠和 `..`。建议采用可正常解码的 Ogg Vorbis 音频。
- 每个功能最多解析 64 个不同文件名，每个音频最多 **2 MiB**；超限文件不会作为有效音频加载。不能把 MP3 改扩展名冒充 OGG。
- 早安吻支持纯字符串文件名，以及上述带 `language`、`text`、`text_language` 的对象。配对字幕仅在该语音实际被选中且字幕语种匹配时使用；否则回退台词流程。
- 早安吻显式语种筛选顺序是“匹配语种 → 未标记条目 → 全部条目”；不是不匹配就完全禁用。`auto` 不按数据包语言标签筛选，沿用原有自动行为。TLM 音包没有这里的语种元数据。
- 救护 profile 当前只消费语音文件名及救护声音设置，**没有早安吻那套静态 `dialogue` 池或配对字幕接口**；不要把早安吻语种对象理解为救护的双语能力。

### 5.3 多包覆盖不是无限追加

加载器只读取固定命名空间 `touhou_maid_affection` 下的上述 `profile.json`，不扫描任意命名空间/任意文件名。多个包按加载顺序逐层合并：未声明字段保留前值；后声明的 `voice_files` **替换整份列表**；早安吻每个非空台词池覆盖之前的同名池，空池不会清掉旧池。

`dialogue_mode` / `voice_mode` 控制运行时与内置/基础池组合，**不是跨数据包的数组合并开关**；profile 也没有礼物标签那种顶层 `replace` 布尔语义。多个语音包要共同生效，应由高优先级 profile 列出最终所需的文件。相同音频路径则由后加载资源覆盖。

当前实际加载入口没有加载按 `maids` / `match` 匹配的 profile；不要根据解析器里存在相关类型就把它当作可用数据包 API。每个女仆的语音选择应使用游戏内已有的能力设置。

详细字段与 AI 使用方法见 [早安吻相关配置说明](早安吻相关配置说明.md) 及 [示例包说明](examples/TMA-Custom-Voice-Pack/README.md)。**实际 TOML 完整表路径带 `bondCosts.` 前缀**；不要把说明中的功能简称当作顶层表名，以游戏生成的文件为准。例如：

```toml
[bondCosts.morningKissBehavior]
displayLanguage = "zh_cn"
voiceLanguage = "zh_cn"
aiDialogueEnabled = false
```

这是修改已有表的示例，不要在同一个 TOML 里复制出第二个同名表。AI 服务商、模型和凭据仍在 TLM 中配置，不要随整合包发布个人 API 密钥。

## 6. 数据包与资源包的声音区别

- **预录互动语音**：本教程的 `data/.../voices/*.ogg` 由 TMA 服务端读取，并随播放消息发送给客户端；玩家无需为这些文件另外装同内容资源包，但客户端仍需安装模组。
- **标准声音事件**：`kiss_sound_event` / `sound_event` 引用声音事件，不会自动把 `assets/` 文件发送给客户端。自定义这类声音资源需客户端资源包；仅填写 ID 不会生成声音。
- **替换内置声音**：可从 [内置 sounds.json](src/main/resources/assets/touhou_maid_affection/sounds.json) 确认真实事件与文件路径，按 Minecraft 资源包规则覆盖对应 `assets/touhou_maid_affection/` 资源。翻译在 `assets/touhou_maid_affection/lang/zh_cn.json` 等文件中。
- **救护回退音效**：还存在 `data/touhou_maid_affection/rescue_sound/profile.json`，但实际救护发送时优先使用 `emergency_rescue/profile.json` 的 `sound_event`；内置 emergency profile 已声明此字段。要改救护事件，优先改上节的 emergency profile，避免只改回退文件却被覆盖。不要把这些声音字段当作礼物接口。

## 7. 安装后如何确认与排错

1. 执行 `/datapack list enabled`、`/reload`，确认包已启用且无 JSON / 标签解析错误；检查服务端 `logs/latest.log`，必要时看 `logs/debug.log`。JSON 不接受注释和尾随逗号。
2. 礼物：确认已为自己的女仆解锁随机礼物，在齿轮「状态 → 随机礼物」查看实际池模式、候选数、已积攒/已备物品和投递状态。点刷新取新快照；按本文的队列边界判断，不要靠连续刷新试图重抽。
3. 用测试存档可把 `intervalRealMinutes` 临时设为合法最小值 1，让已解锁女仆留在附近并等待真实一分钟；用本文仅标签示例时，应只有白名单内且未入黑名单的有效物品。已有合法队列仍可能先送旧物品，必要时用新解锁的女仆观察新池。
4. 语音日志关注 `Morning kiss profile loaded`、`Interaction voice profile loaded`、`Loaded morning kiss data-pack voice` / `Loaded interaction data-pack voice`；`voices=0`、加载异常、超出字节限制都提示需要检查声明、大小、路径和文件。客户端能否实际播放还取决于音频编码及音量。
5. 可用 `/tma morning_kiss status`、`/tma morning_kiss cache` 查看状态；测试静态内容时用 `/tma morning_kiss ai off`，测试后需要 AI 再 `/tma morning_kiss ai on`。`/tma morning_kiss clear_ai_cache all` 只清 AI 缓存，不是重载数据包或清礼物队列；修改类命令需要 OP 等级 2。
6. 没声音先检查对应能力的语音选择和本地音量，再检查服务端 profile 是否加载、客户端是否能解码；修改普通资源包后用 `F3+T`。不要用 `/tma rescue clear` 或 `/tma bond prune` 当普通调试刷新，它们会修改救护/羁绊状态。

礼物没有 `/tma gift reload`、按物品设权重或清队列的专用命令。请优先使用状态面板和现有数据包机制，不要照抄并不存在的指令。

## 8. 对照源码

维护整合包时可从这些实际入口核对接口是否变化：

- [ModConfig.java](src/main/java/com/github/touhoumaidaffection/ModConfig.java)：TOML 层级、默认值和取值范围。
- [RandomGiftService.java](src/main/java/com/github/touhoumaidaffection/bond/service/RandomGiftService.java) / [RandomGiftPolicy.java](src/main/java/com/github/touhoumaidaffection/bond/service/RandomGiftPolicy.java)：标签、有效性、采样与投递。
- [RandomGiftQueue.java](src/main/java/com/github/touhoumaidaffection/bond/RandomGiftQueue.java) / [RandomGiftClock.java](src/main/java/com/github/touhoumaidaffection/bond/RandomGiftClock.java)：已备物品保留与真实时间结算。
- [MorningKissProfileData.java](src/main/java/com/github/touhoumaidaffection/bond/service/MorningKissProfileData.java) / [MorningKissProfileParser.java](src/main/java/com/github/touhoumaidaffection/bond/service/MorningKissProfileParser.java)：早安吻实际加载与合并。
- [InteractionVoiceProfileData.java](src/main/java/com/github/touhoumaidaffection/bond/service/InteractionVoiceProfileData.java)：救护/早安吻固定路径及音频读取。
