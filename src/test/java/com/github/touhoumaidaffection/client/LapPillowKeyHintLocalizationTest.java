package com.github.touhoumaidaffection.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LapPillowKeyHintLocalizationTest {
    @Test
    void lapPillowActionHintUsesDynamicKeyPlaceholder() throws IOException {
        JsonObject zhCn = readLang("zh_cn");
        JsonObject enUs = readLang("en_us");

        assertTrue(zhCn.get("bond.action.press_key").getAsString().contains("%s"));
        assertTrue(enUs.get("bond.action.press_key").getAsString().contains("%s"));
        assertFalse(zhCn.has("bond.action.press_b"));
        assertFalse(enUs.has("bond.action.press_b"));
    }

    @Test
    void lapPillowDescriptionUsesDynamicKeyPlaceholder() throws IOException {
        JsonObject zhCn = readLang("zh_cn");
        JsonObject enUs = readLang("en_us");

        assertTrue(zhCn.get("bond.ability.lap.desc").getAsString().contains("%s"));
        assertTrue(enUs.get("bond.ability.lap.desc").getAsString().contains("%s"));
        assertFalse(zhCn.get("bond.ability.lap.desc").getAsString().contains("B"));
        assertFalse(enUs.get("bond.ability.lap.desc").getAsString().contains("B"));
    }

    private static JsonObject readLang(String language) throws IOException {
        Path path = Path.of("src", "main", "resources", "assets", "touhou_maid_affection", "lang", language + ".json");
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }
}
