package com.github.touhoumaidaffection.ai.mimo;

import com.github.tartaricacid.touhoulittlemaid.ai.service.SerializableSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.SupportModelSelect;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMClient;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.openai.LLMOpenAISite;
import com.github.touhoumaidaffection.ModConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class MimoLLMSite extends LLMOpenAISite implements SupportModelSelect {
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

    public MimoLLMSite(String id, ResourceLocation icon, String url, boolean enabled, String secretKey,
                       Map<String, String> headers, Map<String, String> models) {
        super(id, icon, url, enabled, secretKey, false, new LinkedHashMap<>(headers), toModelEntries(models));
    }

    static Map<String, String> defaultModels() {
        LinkedHashMap<String, String> models = new LinkedHashMap<>();
        models.put(MimoProtocol.DEFAULT_CHAT_MODEL, "MiMo V2.5");
        models.put("mimo-v2.5-pro", "MiMo V2.5 Pro");
        models.put("mimo-v2-omni", "MiMo V2 Omni");
        return models;
    }

    @Override
    public String url() {
        return MimoProtocol.normalizeUrl(url);
    }

    @Override
    public String getApiType() {
        return API_TYPE;
    }

    @Override
    public LLMClient client() {
        return new MimoLLMClient(LLM_HTTP_CLIENT, this);
    }

    public static MimoLLMSite fromOpenAISite(LLMOpenAISite site) {
        return new MimoLLMSite(
                site.id(),
                site.icon(),
                site.url(),
                site.enabled(),
                site.secretKey(),
                site.headers(),
                site.models()
        );
    }

    private static Map<String, LLMOpenAISite.ModelEntry> toModelEntries(Map<String, String> models) {
        return models.keySet().stream()
                .collect(Collectors.toMap(
                        name -> name,
                        LLMOpenAISite.ModelEntry::new,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
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
