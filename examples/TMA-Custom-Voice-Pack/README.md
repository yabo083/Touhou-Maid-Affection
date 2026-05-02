# TMA Custom Voice Pack

这是 `Touhou Maid: Affection` 的示例数据包，可直接压缩本目录后发布，或放入存档的 `datapacks` 文件夹测试。

目录根必须直接包含 `pack.mcmeta` 和 `data/`。如果你把整个外层文件夹一起压进 zip，Minecraft 可能识别不到它。

包含内容：

- 早安吻静态台词示例。
- 早安吻数据包语音池示例。
- 残血救护数据包语音池示例。
- 可直接试听的示例 OGG 文件。

替换语音时，只需要把 `voices/*.ogg` 换成自己的 OGG 文件，并同步修改对应 `profile.json` 中的 `voice_files`。

AI 台词与 TTS 预生成不写在数据包里，请在 `config/touhou_maid_affection-common.toml` 的 `morningKissBehavior` 段配置。数据包只负责静态台词、亲吻音效和预录 OGG 语音。
