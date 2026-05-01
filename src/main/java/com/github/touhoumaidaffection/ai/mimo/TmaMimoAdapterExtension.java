package com.github.touhoumaidaffection.ai.mimo;

import com.github.tartaricacid.touhoulittlemaid.ai.service.SerializerRegister;
import com.github.tartaricacid.touhoulittlemaid.ai.service.ServiceType;
import com.github.tartaricacid.touhoulittlemaid.api.ILittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.LittleMaidExtension;
import com.github.touhoumaidaffection.ModConfig;
import com.github.touhoumaidaffection.TouhouMaidAffection;

@LittleMaidExtension
public class TmaMimoAdapterExtension implements ILittleMaid {
    @Override
    public void registerAIChatSerializer(SerializerRegister register) {
        if (!ModConfig.TMA_MIMO_ADAPTER_ENABLED.get()) {
            TouhouMaidAffection.LOGGER.info("TMA MiMo adapter is disabled by config; skipping TLM AI serializer registration");
            return;
        }
        register.register(ServiceType.LLM, MimoLLMSite.API_TYPE, new MimoLLMSite.Serializer());
        register.register(ServiceType.TTS, MimoTTSSite.API_TYPE, new MimoTTSSite.Serializer());
        TouhouMaidAffection.LOGGER.info("Registered TMA MiMo adapter serializers for TLM AI chat and TTS");
    }
}
