# Touhou Maid: Affection — 开发工作流规范

> 本文档是 AI 编码 Agent 在本项目中的行为规范。
> 所有对本仓库的开发操作（写代码、构建、发布）都应遵循以下流程。

---

## 📐 项目基本信息

| 项 | 值 |
|---|---|
| **Mod ID** | `touhou_maid_affection` |
| **加载器** | NeoForge 21.1.x（`net.neoforged.moddev` 2.0.95） |
| **Minecraft** | 1.21.1 |
| **Java** | 21 |
| **核心依赖** | TouhouLittleMaid ≥ 1.5.0（`compileOnly`，不打包） |
| **仓库** | https://github.com/yabo083/maid-affection |
| **许可证** | MIT |

---

## 🔄 开发全流程

### Phase 0：需求与设计

1. **明确需求**：用户描述想法或问题
2. **调研可行性**：
   - 查阅 TouhouLittleMaid 源码确认可用 API/事件/类
   - 查阅目标交互模组的源码（如 CarryOn）确认冲突点
   - 如需 Mixin，确认目标方法签名、访问修饰符、调用链
3. **输出计划**：将方案写入 session memory 或 todo list，包含：
   - 改动的文件列表
   - 具体逻辑描述
   - 兼容性影响评估

### Phase 1：编码实现

4. **先读后改**：修改任何文件前，必须先 `read_file` 获取当前内容
5. **最小改动**：只改需要改的，不做"顺便优化"
6. **Mixin 规范**：
   - Mixin 类放 `com.github.touhoumaidaffection.mixin` 包
   - Mixin 配置文件：`src/main/resources/touhou_maid_affection.mixins.json`
   - 在 `neoforge.mods.toml` 中用 `[[mixins]]` 段注册
   - 目标是 mod 类（非 Minecraft 原版）时，设 `remap = false`
7. **音效规范**：
   - 源文件（wav 等）放 `src/main/resources/kissSound/`，已被 `.gitignore` 排除
   - 转换后的 OGG 放 `assets/touhou_maid_affection/sounds/` 下对应子目录
   - 在 `assets/touhou_maid_affection/sounds.json` 中声明
   - 用 `DeferredRegister<SoundEvent>` 在 `ModSounds.java` 中注册
8. **网络包规范**：
   - 实现 `CustomPacketPayload`
   - 使用 `PayloadRegistrar.playToClient()` + `.optional()` 注册
   - Server→Client 广播用 `PacketDistributor.sendToPlayersTrackingEntityAndSelf()`
9. **兼容性规范**：
   - 软检测其他模组：`ModList.get().isLoaded("mod_id")`
   - 不引入硬依赖，不 import 其他模组的类（除非是 `compileOnly` 依赖）
   - 缓存检测结果到 `static Boolean` 字段

### Phase 2：构建验证

10. **每次改动后必须构建**：`.\gradlew.bat build`
11. **确认产出**：`Get-ChildItem build\libs\` 检查 jar 文件名和大小
12. **构建失败时**：读取完整错误信息，修复后重新构建，不要猜测性地反复重试

### Phase 3：版本管理与发布

> ⚠️ **这是最容易遗漏的环节——每次功能变更都必须走完以下所有步骤**

13. **更新版本号**：修改 `gradle.properties` 中的 `mod_version`
    - 版本号遵循语义化版本：`MAJOR.MINOR.PATCH`
    - 新功能（新交互、新 Mixin、新音效等用户可感知的变化）→ MINOR +1
    - 小修改（重命名、文档、CI、构建配置、元数据等非功能性变更）→ PATCH +1
    - 破坏性变更（mod ID 变更、删除功能、不向后兼容）→ MAJOR +1
    - **禁止频繁升 MINOR**：仅文档/配置/重命名等修改绝不升 MINOR，一律升 PATCH
14. **重新构建**：版本号改后必须重新 `.\gradlew.bat build`
15. **提交代码**：
    ```
    git add .
    git commit -m "<type>: <简短描述>"
    ```
    type 可选：`feat`（新功能）、`fix`（修复）、`chore`（杂务）、`docs`（文档）
16. **打标签**：
    ```
    git tag -a "v<版本号>" -m "v<版本号> - <版本代号>"
    ```
17. **推送**：
    ```
    git push
    git push origin "v<版本号>"
    ```
    推送 tag 会自动触发 `.github/workflows/build.yml` 中的 release job，
    CI 会构建并创建 GitHub Release（附带 jar），并自动发布到 Modrinth。

18. **验证 CI**：推送后提醒用户检查：
    - GitHub Actions 是否成功
    - Modrinth 版本是否已发布（需确保 `MODRINTH_TOKEN` secret 已配置）

### Modrinth 发布配置

- 使用 [Minotaur](https://github.com/modrinth/minotaur) Gradle 插件（`com.modrinth.minotaur` v2.+）
- `modrinth_project_id` 在 `gradle.properties` 中配置
- Token 通过 GitHub Secrets 的 `MODRINTH_TOKEN` 环境变量传入
- CI release job 中在 GitHub Release 之后自动执行 `./gradlew modrinth`
- `syncBodyFrom` 自动将 README_zh.md 同步为 Modrinth 项目描述
- 依赖声明：`required.project "touhou-little-maid"`
- Modrinth **不支持 OIDC**，只能用 PAT

---

## 📁 项目结构

```
touhou-maid-affection/
├── .github/workflows/build.yml    # CI/CD：push 构建 + tag 发布
├── .gitignore
├── LICENSE
├── README.md                       # 英文 README（GitHub 默认展示）
├── README_zh.md                    # 中文 README（同步到 Modrinth）
├── build.gradle                    # NeoForge moddev + Minotaur 构建脚本
├── gradle.properties               # 版本号、mod 信息、依赖版本
├── settings.gradle
├── gradle/wrapper/                 # Gradle Wrapper（必须提交 jar）
├── src/main/java/com/github/touhoumaidaffection/
│   ├── TouhouMaidAffection.java    # @Mod 主类，注册事件和网络包
│   ├── ModSounds.java              # DeferredRegister<SoundEvent>
│   ├── client/
│   │   └── KissClientHandler.java  # 客户端粒子渲染
│   ├── handler/
│   │   └── KissMaidHandler.java    # 服务端事件处理（核心逻辑）
│   ├── mixin/
│   │   ├── MaidBegTaskMixin.java   # 副手诱惑物品检测
│   │   └── MaidRideBegTaskMixin.java
│   └── network/
│       └── KissMaidPayload.java    # Server→Client 网络包
├── src/main/resources/
│   ├── META-INF/neoforge.mods.toml # 模组元数据 + Mixin 注册
│   ├── touhou_maid_affection.mixins.json  # Mixin 配置
│   ├── pack.mcmeta
│   └── assets/touhou_maid_affection/
│       ├── lang/en_us.json
│       ├── lang/zh_cn.json
│       ├── sounds.json             # 音效声明
│       └── sounds/kiss/*.ogg       # 音效文件
└── src/main/resources/kissSound/   # 源 wav 文件（gitignore 排除）
```

---

## 📚 参考资料

### TouhouLittleMaid（上游模组）

| 资源 | 链接 |
|---|---|
| 源码（1.21 分支） | https://github.com/TartaricAcid/TouhouLittleMaid/tree/1.21 |
| 附属开发示例 | https://github.com/TartaricAcid/TLMAdditionExample |
| API 事件：InteractMaidEvent | `com.github.tartaricacid.touhoulittlemaid.api.event.InteractMaidEvent` |
| 好感度系统 | `entity/favorability/FavorabilityManager.java` + `Type.java` |
| 女仆 AI（Brain 系统） | `entity/ai/brain/task/MaidBegTask.java`（诱惑/祈求行为） |
| 网络包注册参考 | `network/NetworkHandler.java` → `PayloadRegistrar` |

### 兼容模组

| 模组 | 源码 | 兼容策略 |
|---|---|---|
| CarryOn | https://github.com/Tschipp/CarryOn (1.21 branch) | `ModList.isLoaded("carryon")` + 副手非空条件分离 |

### NeoForge 开发

| 资源 | 链接 |
|---|---|
| NeoForge 文档 | https://docs.neoforged.net/ |
| Mixin 指南 | https://docs.neoforged.net/docs/advanced/mixin/ |
| moddev Gradle 插件 | https://github.com/neoforged/ModDevGradle |

### 灵感来源

| 模组 | 链接 |
|---|---|
| Kiss a Friend（Fabric） | https://codeberg.org/MeAlam/Kiss-a-Friend |

---

## ⚠️ 常见坑与教训

1. **版本号必须和 tag 一起更新**：改了功能但没改 `gradle.properties` 里的 `mod_version` → CI 不触发新 Release，用户看不到新 jar
2. **`gradle-wrapper.jar` 必须提交**：Windows 本地有缓存所以能构建，但 CI runner 是干净环境，缺 jar 就报错 `Unable to access jarfile`
3. **`gradlew` 需要可执行权限**：Windows git 默认不跟踪 Unix 权限，用 `git update-index --chmod=+x gradlew` 修复
4. **TLM 依赖用 `compileOnly`**：用 `implementation` 会把整个 TLM 打包进 jar，体积暴涨且可能冲突
5. **Mixin 目标是 mod 类时设 `remap = false`**：mod 类名不会被混淆，remap 会导致找不到目标
6. **音效文件必须是 OGG Vorbis**：Minecraft 只支持 `.ogg` 格式，用 `ffmpeg -acodec libvorbis` 转换
7. **网络包注册用 `.optional()`**：确保客户端没装本 mod 时不会报错断开连接
8. **CarryOn 搬起实体要求双手都空**：这是区分亲吻和搬起的关键——副手有物品时 CarryOn 不触发

---

## 🏷️ 版本历史命名惯例

| 版本 | 代号 | 内容 |
|---|---|---|
| v1.0.0 | The Maiden's Awakening | 初始发布：亲吻互动 + 粒子 + 好感度 |
| v1.1.0 | The Maiden's Embrace | CarryOn 兼容 + 副手食物吸引（Mixin） |
| v1.2.0 | The Maiden's Voice | 自定义亲吻音效（7 个随机变体） |
| v1.3.0 | The Maiden's Identity | Modrinth 自动发布 + README 中英分离 + 显示名重命名 |
| v1.3.1 | — | Mod ID 统一重命名为 touhou_maid_affection |
