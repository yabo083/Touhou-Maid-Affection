# TESTING

本文档定义本项目的测试目标、约定和本地执行方式，确保后续维护改动具备可回归验证路径。

## Scope

- 纯逻辑模块优先补单元测试（例如时间窗解析、数值范围修正、字符串规则转换）。
- 与 Minecraft 运行时强耦合的流程以编译校验和手动联机/单机回归为主。
- 复杂交互链路（如亲吻、膝枕、救援）暂不强行编写脆弱的深度模拟测试。

## Commands

```bash
./gradlew test
./gradlew compileJava
```

Windows PowerShell:

```powershell
.\gradlew.bat test
.\gradlew.bat compileJava
```

## Conventions

- 测试代码位于 `src/test/java`，包路径与主代码保持镜像。
- 测试命名使用 `*Test` 后缀，方法名描述行为结果而非实现细节。
- 新增可提纯逻辑时，优先抽到独立类后再补单测，避免在超大业务类里堆无法测试的私有方法。

## Current Baseline

- 已覆盖 `MorningKissScheduleRules` 的时间窗解析、跨午夜判断、默认回退和亲吻次数边界修正。
- 已覆盖数据包语音池选择、早安吻 profile 解析、交互语音 profile 解析与客户端按键默认值等纯逻辑。
- 每次维护任务至少执行一次 `test + compileJava`，作为提交前最小质量门禁。

## Random Gift regression coverage

- `RandomGiftPolicyTest`：标签池/广泛池、显式标签与黑名单优先级、模组分组抽样和候选变化。
- `RandomGiftClockTest` / `RandomGiftQueueTest`：现实时间积攒、旧队列、容量降低、FIFO 消耗、非法或失效物品。
- `RandomGiftPersistenceTest`：真实 NBT 保存读取、`.maid` 导入导出、送礼记录的迁移边界。
- `TmaGiftStatusWireTest` / `TmaGiftStatusViewRulesTest`：消息字段与长度边界、倒计时、分页和可视区命中。
- 2026-09-30 在独立临时世界执行服务端集成探针：使用真实 TLM 女仆、FakePlayer、已加载标签与网络 ByteBuf，核对备礼 → 状态快照 → 实际物品实体 → 队列扣减 → 送礼记录；覆盖只读查询、所有权隔离及距离/冷却条件。NeoForge 21.1.18 开发运行环境与 Forge 47.4.16 正式映射 jar 均通过。探针与临时世界位于仓库外，交付 jar 不含探针。
- `TmaGiftStatusSelectionTest` / `TmaGiftStatusSelectionIntegrationTest`：多于 16 名女仆的分批读取、同名女仆按 UUID 选择、刷新与顺序变化、女仆移除及空列表；通过实际 `STREAM_CODEC` 与 Netty `ByteBuf` 解码后验证固定模板的物品聚合与倒计时。
- `BondHeaderLayoutTest` / `BondTextFitTest` / `BondButtonRowLayoutTest`：齿轮位于标题栏内、避开页签区域，标题/图标互不重叠，长标签缩放和按钮点击范围一致。
- `TmaStatusLayoutTest`：功能同级、子分类与数据缩进、加载态归属、滚动命中与末行可达性。
- 系统测试由用户在两个实例中验收：标题栏齿轮与其他 TLM 页签、状态功能分组、女仆下拉框、默认字体观感以及实际送礼。两个实例保持广泛池。

