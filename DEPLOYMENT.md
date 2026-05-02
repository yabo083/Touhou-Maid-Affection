# DEPLOYMENT

本文档说明本项目的构建产物、发布约束和发版前检查项，避免“本地可用但发布失败”的问题。

## Target Runtime

- Minecraft: `1.20.1`
- Loader: Forge `47.4.x`
- Java: `17`
- Required dependency: Touhou Little Maid for Forge 1.20.1
- Current compile target: `1.5.2-forge+mc1.20.1`

## Build Artifact

执行构建：

```bash
./gradlew build
```

Windows PowerShell:

```powershell
.\gradlew.bat build
```

产物位置：

- `build/libs/touhou-maid-affection-<version>.jar`
- `build/libs/touhou-maid-affection-<version>-sources.jar`

发布时使用非 `sources` 的主 jar。

## Release Channel

- GitHub Actions 在 `main`、`forge-1.20.1` 分支 push 和 `v*` tag push 时触发。
- Forge 1.20.1 发布 tag 约定：`v<mod_version>-forge1.20.1`。
- GitHub Release 上传 `build/libs/touhou-maid-affection-*.jar`。
- Modrinth 发布使用 `com.modrinth.minotaur`。
- CurseForge 发布只在 tag 名包含 `forge1.20.1` 且 token/项目变量存在时执行。

发布任务依赖环境变量或仓库配置：

- `MODRINTH_TOKEN`
- `CURSEFORGE_TOKEN`
- `CURSEFORGE_PROJECT_ID`
- `CHANGELOG`（可选，不提供则使用默认说明）

## Pre-release Checklist

- `gradle.properties` 中 `mod_version` 与目标 tag 一致。
- `CHANGELOG.md` 已有目标版本条目。
- `README.md` 与 `README_zh.md` 的版本、运行时和功能说明一致。
- `PROJECT_ARCHITECTURE.md` 已同步核心架构变更。
- `早安吻文本修改教程.md` 与 `examples/TMA-Custom-Voice-Pack` 的数据包格式一致。
- 示例数据包的 zip 根目录设计为 `pack.mcmeta` + `data/`。
- `.\gradlew.bat test` 通过。
- `.\gradlew.bat compileJava` 通过。
- `src/main/resources/META-INF/mods.toml` 的依赖范围仍符合当前发布目标。

## Compatibility Policy

- 对 YSM、CarryOn、TLM GUI、TLM 音包、TLM AI 站点、MiMo 采用软兼容策略。
- 缺少可选依赖或远程服务失败时，应回退到基础交互，不应导致模组不可运行。
- 发布前至少验证一次“仅安装必需依赖”的基础运行场景，确保亲吻、羁绊页和基础能力链路正常。
