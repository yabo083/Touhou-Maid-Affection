# TMA Custom Voice Pack

这是 `Touhou Maid: Affection` 的示例数据包，可直接压缩本目录后发布或放入存档的 `datapacks` 文件夹测试。

包含内容：

- 早安吻静态台词示例（含未标记的旧写法与带语种标签的新写法）。
- 早安吻数据包语音池示例（含带语种与配对字幕的新写法）。
- 残血救护数据包语音池示例。

替换语音时，只需要把 `voices/*.ogg` 换成自己的 OGG 文件，并同步修改对应 `profile.json` 中的 `voice_files`。

## 语言标签两种写法

`dialogue.<pool>` 元素与 `voice_files` 元素都同时支持旧写法与带语种标签的写法：

```json
{
  "dialogue": {
    "morning": [
      "旧写法：未标记，任何显示语种都能选中。",
      { "text": "中文台词。", "language": "zh_cn" },
      { "text": "日本語のセリフ。", "language": "ja_jp" }
    ]
  },
  "voice_files": [
    "legacy.ogg",
    { "file": "onjdsk.ogg", "language": "ja_jp" },
    {
      "file": "morning_test.ogg",
      "language": "zh_cn",
      "text": "配对的中文字幕。",
      "text_language": "zh_cn"
    }
  ]
}
```

- 语种标签大小写与 `-`/`_` 不敏感：`zh-CN` 等价 `zh_cn`；`tlm`/`auto`/`default` 视为未标记。
- 未标记条目是「通配」：显式语种没有命中时才会被选中，保证旧包行为不变。
- `voice_files[].text` 是可选字幕，仅当该语音被选中播放时才显示，且 `text_language` 与目标显示语种不一致时会退回随机台词。
- 是否启用筛选由 `config/touhou_maid_affection-common.toml` 中 `morningKissBehavior.displayLanguage` / `voiceLanguage` 决定；默认 `auto` 与旧版完全一致。

AI 台词与 TTS 预生成不写在数据包里，请在 `config/touhou_maid_affection-common.toml` 的 `morningKissBehavior` 段配置。数据包只负责静态台词、亲吻音效和预录 OGG 语音。