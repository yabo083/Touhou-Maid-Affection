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
- GitHub Actions 在 `main` 分支或 `v*` tag push 时构建；NeoForge 正式发布 tag 为 `v<mod_version>`，Forge 使用 `v<mod_version>-forge1.20.1` 并指向 Forge 分支提交。
- tag 工作流上传 GitHub Release；配置相应凭据后再发布 Modrinth 和 CurseForge。GitHub Release 已生成不代表两个平台上传均已成功，需分别检查工作流步骤。
- CurseForge 去重同时精确匹配文件名、Minecraft 版本与加载器，避免双版本同名 jar 互相误判为已上传。
- `build` job 只执行一次完整 `./gradlew build`（包含测试），Build 步骤限时 30 分钟；`release` job 下载同次运行的 `touhou-maid-affection` artifact 到 `build/libs`，不再次编译。
- Modrinth 使用 `./gradlew modrinth -x assemble -x jar`，跳过 Minotaur 自动关联的打包任务，上传已通过构建的 jar；保留版本号、必需依赖、更新说明与 README 正文同步。GitHub 与 CurseForge 使用同一份下载产物。
- 普通分支 push / PR 只构建，不发布；只有 `v*` tag 触发发布。已发布 tag 不移动、不删除，排障时先确认各平台已有产物，不为修 CI 重发旧版本。
- 发布任务依赖环境变量：
  - `MODRINTH_TOKEN`
  - `CHANGELOG`（可选，不提供则使用默认说明）
- CurseForge 使用仓库 secret `CURSEFORGE_TOKEN` 与仓库 variable `CURSEFORGE_PROJECT_ID`。

## Pre-release Checklist

- `./gradlew test` 通过。
- `./gradlew compileJava` 通过。
- `README.md` 与 `README_zh.md` 的功能说明与当前版本一致。
- `PROJECT_ARCHITECTURE.md` 已同步核心架构变更（若本次涉及模块职责或拓扑调整）。
- `META-INF/neoforge.mods.toml` 中版本号与依赖范围正确。
- `PACK_AUTHORING.md` 的数据包/资源包示例与实际加载器一致；两种包不能混淆安装位置与 `pack_format`。

## Compatibility Policy

- 对 `YSM`、`CarryOn` 等生态采用软兼容策略：缺失时应静默降级，不应导致模组不可运行。
- 发布前至少验证一次“仅安装必需依赖”的基础运行场景，确保主功能链路正常。
