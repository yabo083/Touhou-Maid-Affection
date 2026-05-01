package com.github.touhoumaidaffection.ai.mimo;

import com.github.tartaricacid.touhoulittlemaid.ai.service.SerializableSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.SupportModelSelect;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.openai.LLMOpenAISite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.openai.LLMOpenAIClient;
import com.github.touhoumaidaffection.ModConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

public class MimoLLMSite extends LLMOpenAISite implements LLMSite, SupportModelSelect {
    public static final String API_TYPE = "tma_mimo_chat";
    public static final String SITE_ID = "tma_mimo_chat";

    public static final Codec<MimoLLMSite> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("id", SITE_ID).forGetter(MimoLLMSite::id),
            ResourceLocation.CODEC.optionalFieldOf("icon", MimoCodecHelper.icon("textures/gui/ai_chat/mimo.png")).forGetter(MimoLLMSite::icon),
            Codec.STRING.optionalFieldOf("url", MimoProtocol.DEFAULT_URL).forGetter(MimoLLMSite::url),
            Codec.BOOL.optionalFieldOf("enabled", false).forGetter(MimoLLMSite::enabled),
            Codec.STRING.optionalFieldOf("secret_key", "").forGetter(MimoLLMSite::secretKey),
            MimoCodecHelper.STRING_MAP_CODEC.optionalFieldOf("headers", Map.of()).forGetter(MimoLLMSite::headers),
            MimoCodecHelper.STRING_MAP_CODEC.optionalFieldOf("models", defaultModels()).forGetter(MimoLLMSite::models)
    ).apply(instance, MimoLLMSite::new));

    private final String id;
    private final ResourceLocation icon;
    private final Map<String, String> headers;
    private final Map<String, String> models;
    private String url;
    private boolean enabled;
    private String secretKey;

    public MimoLLMSite(String id, ResourceLocation icon, String url, boolean enabled, String secretKey,
                       Map<String, String> headers, Map<String, String> models) {
        super(id, icon, url, enabled, secretKey, false, headers, toModelEntries(models));
        this.id = id;
        this.icon = icon;
        this.url = url;
        this.enabled = enabled;
        this.secretKey = secretKey;
        this.headers = new LinkedHashMap<>(headers);
        this.models = new LinkedHashMap<>(models);
    }

    private static List<LLMOpenAISite.ModelEntry> toModelEntries(Map<String, String> models) {
        return models.keySet().stream().map(LLMOpenAISite.ModelEntry::new).toList();
    }

    static Map<String, String> defaultModels() {
        LinkedHashMap<String, String> models = new LinkedHashMap<>();
        models.put(MimoProtocol.DEFAULT_CHAT_MODEL, "MiMo V2.5");
        models.put("mimo-v2.5-pro", "MiMo V2.5 Pro");
        models.put("mimo-v2-omni", "MiMo V2 Omni");
        return models;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public ResourceLocation icon() {
        return icon;
    }

    @Override
    public String url() {
        return MimoProtocol.normalizeUrl(url);
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String secretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    @Override
    public Map<String, String> headers() {
        return headers;
    }

    @Override
    public String getApiType() {
        return API_TYPE;
    }

    @Override
    public com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMClient client() {
        return new LLMOpenAIClient(LLMSite.LLM_HTTP_CLIENT, this);
    }

    @Override
    public Map<String, String> models() {
        return models;
    }

    @Override
    public void addModel(String name) {
        super.addModel(name);
        models.put(name, name);
    }

    @Override
    public void addModel(String name, String displayName) {
        super.addModel(name, displayName);
        models.put(name, displayName);
    }

    @Override
    public void removeModel(String name) {
        super.removeModel(name);
        models.remove(name);
    }

    public static class Serializer implements SerializableSite<MimoLLMSite> {
        @Override
        public Codec<MimoLLMSite> codec() {
            return CODEC;
        }

        @Override
        public MimoLLMSite defaultSite() {
            return new MimoLLMSite(
                    SITE_ID,
                    MimoCodecHelper.icon("textures/gui/ai_chat/mimo.png"),
                    ModConfig.TMA_MIMO_CHAT_URL.get(),
                    false,
                    ModConfig.TMA_MIMO_API_KEY.get(),
                    Map.of(),
                    defaultModels()
            );
        }
    }
}
