package com.github.touhoumaidaffection.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PowerPointHintLocalizationTest {
    @Test
    void explainsThatUnlockingConsumesInventoryPowerPointItems() throws Exception {
        JsonObject zhCn = readLang("zh_cn");
        JsonObject enUs = readLang("en_us");

        assertTrue(zhCn.get("bond.power_point_item_hint").getAsString().contains("背包"));
        assertTrue(enUs.get("bond.power_point_item_hint").getAsString().toLowerCase().contains("inventory"));
        assertTrue(zhCn.has("bond.unlock_click_blocked"));
        assertTrue(enUs.has("bond.unlock_click_blocked"));
    }

    private static JsonObject readLang(String language) throws Exception {
        Path path = Path.of("src", "main", "resources", "assets", "touhou_maid_affection", "lang", language + ".json");
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }
}
