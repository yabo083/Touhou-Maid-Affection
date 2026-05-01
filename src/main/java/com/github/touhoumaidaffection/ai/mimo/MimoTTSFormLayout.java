package com.github.touhoumaidaffection.ai.mimo;

import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.TTSSite;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.Translations;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.layout.FieldDescriptor;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.layout.TTSSiteFormLayout;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class MimoTTSFormLayout extends TTSSiteFormLayout {
    public MimoTTSFormLayout(TTSSite sourceSite) {
        super(sourceSite);
    }

    @Override
    public List<FieldDescriptor> getFieldDescriptors() {
        MimoTTSSite site = (MimoTTSSite) sourceSite;
        return List.of(
                new FieldDescriptor("url", site.url(), true, false),
                new FieldDescriptor("secret_key", site.secretKey(), true, true),
                new FieldDescriptor("voice_prompt", site.voicePrompt(), true, false),
                new FieldDescriptor("audio_format", site.audioFormat(), true, false)
        );
    }

    @Override
    public boolean supportsModelRows() {
        return true;
    }

    @Override
    public Map<String, String> getInitialModels() {
        return ((MimoTTSSite) sourceSite).models();
    }

    @Override
    public TTSSite buildSite(Function<String, String> fieldValue, Map<String, String> models, Consumer<Component> feedback) {
        MimoTTSSite site = (MimoTTSSite) sourceSite;
        String url = StringUtils.trimToEmpty(fieldValue.apply("url"));
        if (url.isBlank()) {
            feedback.accept(Translations.URL_IS_EMPTY);
            return null;
        }
        String secretKey = StringUtils.trimToEmpty(fieldValue.apply("secret_key"));
        if (secretKey.isBlank()) {
            feedback.accept(Translations.SECRET_KEY_IS_EMPTY);
            return null;
        }
        if (models.isEmpty()) {
            feedback.accept(Translations.MODEL_IS_EMPTY);
            return null;
        }
        return site.copyWith(
                url,
                secretKey,
                StringUtils.defaultIfBlank(fieldValue.apply("voice_prompt"), MimoProtocol.DEFAULT_TTS_VOICE_PROMPT),
                MimoProtocol.normalizeTtsAudioFormat(fieldValue.apply("audio_format")),
                models
        );
    }
}
