# DEPLOYMENT

本文档说明本项目的构建产物、发布约束和发版前检查项，避免“本地可用但发布失败”的问题。

## Target Runtime

- Minecraft: `1.21.1`
- Loader: NeoForge `21.1.x`
- Required dependency: Touhou Little Maid `1.5.0+`

## Build Artifact

执行构建：

```bash
./gradlew build
```

产物位置：

- `build/libs/touhou-maid-affection-<version>.jar`

## Release Channel

- 项目使用 `com.modrinth.minotaur` 插件发布到 Modrinth。
- 发布任务依赖环境变量：
  - `MODRINTH_TOKEN`
  - `CHANGELOG`（可选，不提供则使用默认说明）

## Pre-release Checklist

- `./gradlew test` 通过。
- `./gradlew compileJava` 通过。
- `README.md` 与 `README_zh.md` 的功能说明与当前版本一致。
- `PROJECT_ARCHITECTURE.md` 已同步核心架构变更（若本次涉及模块职责或拓扑调整）。
- `META-INF/neoforge.mods.toml` 中版本号与依赖范围正确。

## Compatibility Policy

- 对 `YSM`、`CarryOn` 等生态采用软兼容策略：缺失时应静默降级，不应导致模组不可运行。
- 发布前至少验证一次“仅安装必需依赖”的基础运行场景，确保主功能链路正常。
