package com.github.touhoumaidaffection.ai.mimo;

import com.github.tartaricacid.touhoulittlemaid.ai.service.SerializableSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.SupportModelSelect;
import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.TTSClient;
import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.TTSSite;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.layout.TTSSiteFormLayout;
import com.github.touhoumaidaffection.ModConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

public class MimoTTSSite implements TTSSite, SupportModelSelect {
    public static final String API_TYPE = "tma_mimo_tts";
    public static final String SITE_ID = "tma_mimo_tts";

    public static final Codec<MimoTTSSite> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("id", SITE_ID).forGetter(MimoTTSSite::id),
            ResourceLocation.CODEC.optionalFieldOf("icon", MimoCodecHelper.icon("textures/gui/ai_chat/mimo.png")).forGetter(MimoTTSSite::icon),
            Codec.STRING.optionalFieldOf("url", MimoProtocol.DEFAULT_URL).forGetter(MimoTTSSite::url),
            Codec.BOOL.optionalFieldOf("enabled", false).forGetter(MimoTTSSite::enabled),
            Codec.STRING.optionalFieldOf("secret_key", "").forGetter(MimoTTSSite::secretKey),
            Codec.STRING.optionalFieldOf("voice_prompt", MimoProtocol.DEFAULT_TTS_VOICE_PROMPT).forGetter(MimoTTSSite::voicePrompt),
            Codec.STRING.optionalFieldOf("audio_format", MimoProtocol.DEFAULT_TTS_AUDIO_FORMAT).forGetter(MimoTTSSite::audioFormat),
            MimoCodecHelper.STRING_MAP_CODEC.optionalFieldOf("headers", Map.of()).forGetter(MimoTTSSite::headers),
            MimoCodecHelper.STRING_MAP_CODEC.optionalFieldOf("models", defaultModels()).forGetter(MimoTTSSite::models)
    ).apply(instance, MimoTTSSite::new));

    private final String id;
    private final ResourceLocation icon;
    private final Map<String, String> headers;
    private final Map<String, String> models;
    private String url;
    private boolean enabled;
    private String secretKey;
    private String voicePrompt;
    private String audioFormat;

    public MimoTTSSite(String id, ResourceLocation icon, String url, boolean enabled, String secretKey,
                       String voicePrompt, String audioFormat, Map<String, String> headers, Map<String, String> models) {
        this.id = id;
        this.icon = icon;
        this.url = url;
        this.enabled = enabled;
        this.secretKey = secretKey;
        this.voicePrompt = voicePrompt;
        this.audioFormat = audioFormat;
        this.headers = new LinkedHashMap<>(headers);
        this.models = new LinkedHashMap<>(models);
    }

    static Map<String, String> defaultModels() {
        LinkedHashMap<String, String> models = new LinkedHashMap<>();
        models.put(MimoProtocol.DEFAULT_TTS_MODEL, "MiMo V2.5 TTS VoiceDesign");
        models.put("mimo-v2.5-tts", "MiMo V2.5 TTS");
        return models;
    }

    @Override public String id() { return id; }
    @Override public boolean enabled() { return enabled; }
    @Override public void setEnabled(boolean enabled) { this.enabled = enabled; }
    @Override public ResourceLocation icon() { return icon; }
    @Override public String url() { return MimoProtocol.normalizeUrl(url); }
    public void setUrl(String url) { this.url = url; }
    public String secretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
    public String voicePrompt() { return voicePrompt; }
    public String audioFormat() { return audioFormat; }
    @Override public Map<String, String> headers() { return headers; }
    @Override public String getApiType() { return API_TYPE; }
    @Override public TTSClient client() { return new MimoTTSClient(TTSSite.TTS_HTTP_CLIENT, this); }
    @Override public TTSSiteFormLayout formLayout() { return new MimoTTSFormLayout(this); }
    @Override public Map<String, String> models() { return models; }

    MimoTTSSite copyWith(String url, String secretKey, String voicePrompt, String audioFormat, Map<String, String> models) {
        return new MimoTTSSite(id, icon, url, enabled, secretKey, voicePrompt, audioFormat, headers, models);
    }

    public static class Serializer implements SerializableSite<MimoTTSSite> {
        @Override public Codec<MimoTTSSite> codec() { return CODEC; }
        @Override public MimoTTSSite defaultSite() {
            return new MimoTTSSite(SITE_ID, MimoCodecHelper.icon("textures/gui/ai_chat/mimo.png"), ModConfig.TMA_MIMO_TTS_URL.get(),
                    false, ModConfig.TMA_MIMO_API_KEY.get(), ModConfig.TMA_MIMO_TTS_VOICE_PROMPT.get(),
                    ModConfig.TMA_MIMO_TTS_AUDIO_FORMAT.get(), Map.of(), defaultModels());
        }
    }
}
