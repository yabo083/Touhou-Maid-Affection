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
- 已覆盖数据包语音池选择、早安吻 profile 解析、交互语音 profile 解析、MiMo 协议组包与客户端按键默认值等纯逻辑。
- 每次维护任务至少执行一次 `test + compileJava`，作为提交前最小质量门禁。
