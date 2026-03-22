package com.github.touhoumaidaffection.client;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class RescueYsmActionConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get()
            .resolve(TouhouMaidAffection.MOD_ID)
            .resolve("rescue_ysm_actions.json");

    private static Map<String, String> actionByModelId = new HashMap<>();
    private static boolean loaded;

    private RescueYsmActionConfig() {
    }

    public static String getSelectedAction(String modelId, String textureId) {
        ensureLoaded();
        String key = buildModelKey(modelId, textureId);
        if (!key.isBlank()) {
            String selected = actionByModelId.get(key);
            if (selected != null) {
                return selected;
            }
        }
        return actionByModelId.getOrDefault(normalizeModelId(modelId), "");
    }

    public static void setSelectedAction(String modelId, String textureId, String actionId) {
        ensureLoaded();
        String normalizedModelId = buildModelKey(modelId, textureId);
        if (normalizedModelId.isBlank()) {
            normalizedModelId = normalizeModelId(modelId);
        }
        String normalizedActionId = actionId == null ? "" : actionId.trim();
        if (normalizedModelId.isEmpty()) {
            return;
        }
        if (normalizedActionId.isEmpty()) {
            actionByModelId.remove(normalizedModelId);
        } else {
            actionByModelId.put(normalizedModelId, normalizedActionId);
        }
        save();
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        actionByModelId = new HashMap<>();
        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject mappings = root.has("modelActions") && root.get("modelActions").isJsonObject()
                    ? root.getAsJsonObject("modelActions")
                    : new JsonObject();
            for (Map.Entry<String, com.google.gson.JsonElement> entry : mappings.entrySet()) {
                if (entry.getValue().isJsonPrimitive()) {
                    String actionId = entry.getValue().getAsString();
                    if (!actionId.isBlank()) {
                        actionByModelId.put(entry.getKey(), actionId);
                    }
                }
            }
        } catch (Exception ex) {
            TouhouMaidAffection.LOGGER.warn("Failed to load rescue YSM action config, recreating it.", ex);
            actionByModelId = new HashMap<>();
            save();
        }
    }

    private static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            JsonObject root = new JsonObject();
            JsonObject mappings = new JsonObject();
            actionByModelId.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> mappings.addProperty(entry.getKey(), entry.getValue()));
            root.add("modelActions", mappings);
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException ex) {
            TouhouMaidAffection.LOGGER.warn("Failed to save rescue YSM action config.", ex);
        }
    }

    private static String normalizeModelId(String modelId) {
        return modelId == null ? "" : modelId.trim();
    }

    public static String buildModelKey(String modelId, String textureId) {
        String normalizedModelId = normalizeModelId(modelId);
        String normalizedTextureId = normalizeModelId(textureId);
        if (normalizedModelId.isBlank()) {
            return "";
        }
        if (normalizedTextureId.isBlank()) {
            return normalizedModelId;
        }
        return normalizedModelId + "|" + normalizedTextureId;
    }
}
