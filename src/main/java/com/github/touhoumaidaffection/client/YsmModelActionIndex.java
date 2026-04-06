package com.github.touhoumaidaffection.client;

import com.github.touhoumaidaffection.util.NamespacedPathNormalizer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class YsmModelActionIndex {
    private static final Set<String> IGNORED_ANIMATION_IDS = Set.of(
            "gui",
            "idle",
            "walk",
            "run",
            "jump",
            "sneak",
            "sneaking",
            "swim",
            "swim_stand",
            "fly",
            "elytra_fly",
            "sit",
            "ride",
            "ride_pig",
            "climb",
            "climbing",
            "death",
            "sleep",
            "riptide",
            "use_mainhand",
            "use_offhand",
            "swing_hand",
            "swing_offhand",
            "ladder_up",
            "ladder_stillness",
            "ladder_down",
            "empty"
    );

    private YsmModelActionIndex() {
    }

    public static List<DetectedYsmAction> getActions(String modelId) {
        return getActions(modelId, "");
    }

    public static List<DetectedYsmAction> getActions(String modelId, String textureId) {
        String normalizedModelId = normalizeModelId(modelId);
        if (normalizedModelId.isEmpty()) {
            return List.of();
        }
        List<String> lookupRoots = buildLookupRoots(normalizedModelId, textureId);

        Minecraft minecraft = Minecraft.getInstance();
        ResourceManager resourceManager = minecraft.getResourceManager();
        String languageCode = minecraft.getLanguageManager().getSelected();

        ResourceLocation descriptorLocation = findDescriptor(resourceManager, lookupRoots);
        if (descriptorLocation == null) {
            return loadActionsFromArchives(minecraft, lookupRoots, languageCode);
        }

        JsonObject descriptor = readJson(resourceManager, descriptorLocation);
        if (descriptor == null) {
            return List.of();
        }

        String modelRoot = descriptorLocation.getPath().substring(0, descriptorLocation.getPath().length() - "/ysm.json".length());
        JsonObject localized = readJson(resourceManager, new ResourceLocation(descriptorLocation.getNamespace(), modelRoot + "/lang/" + languageCode + ".json"));
        JsonObject fallbackLocalized = "en_us".equals(languageCode)
                ? localized
                : readJson(resourceManager, new ResourceLocation(descriptorLocation.getNamespace(), modelRoot + "/lang/en_us.json"));

        Map<String, DetectedYsmAction> actions = new LinkedHashMap<>();
        JsonObject properties = descriptor.has("properties") && descriptor.get("properties").isJsonObject()
                ? descriptor.getAsJsonObject("properties")
                : null;
        if (properties == null) {
            return List.of();
        }

        JsonObject extraAnimations = properties.has("extra_animation") && properties.get("extra_animation").isJsonObject()
                ? properties.getAsJsonObject("extra_animation")
                : null;
        if (extraAnimations != null) {
            for (Map.Entry<String, JsonElement> entry : extraAnimations.entrySet()) {
                String actionId = entry.getKey();
                if (actionId.startsWith("#")) {
                    continue;
                }
                String rawLabel = entry.getValue().isJsonPrimitive() ? entry.getValue().getAsString() : actionId;
                if (rawLabel.startsWith("#")) {
                    rawLabel = rawLabel.substring(1);
                }
                if (rawLabel.isBlank()) {
                    rawLabel = actionId;
                }
                String localizedLabel = localizedString(localized, fallbackLocalized, "properties.extra_animation." + actionId, rawLabel);
                actions.put(actionId, new DetectedYsmAction(actionId, localizedLabel));
            }
        }

        if (properties.has("extra_animation_classify") && properties.get("extra_animation_classify").isJsonArray()) {
            int index = 0;
            for (JsonElement classifyElement : properties.getAsJsonArray("extra_animation_classify")) {
                if (!classifyElement.isJsonObject()) {
                    index++;
                    continue;
                }
                JsonObject classify = classifyElement.getAsJsonObject();
                String classifyId = classify.has("id") ? classify.get("id").getAsString() : "group" + index;
                JsonObject classifyAnimations = classify.has("extra_animation") && classify.get("extra_animation").isJsonObject()
                        ? classify.getAsJsonObject("extra_animation")
                        : null;
                if (classifyAnimations == null) {
                    index++;
                    continue;
                }
                for (Map.Entry<String, JsonElement> entry : classifyAnimations.entrySet()) {
                    String actionId = entry.getKey();
                    if (actionId.startsWith("#")) {
                        continue;
                    }
                    String rawLabel = entry.getValue().isJsonPrimitive() ? entry.getValue().getAsString() : actionId;
                    if (rawLabel.isBlank()) {
                        rawLabel = actions.containsKey(actionId) ? actions.get(actionId).displayName() : actionId;
                    }
                    String localizedLabel = localizedString(
                            localized,
                            fallbackLocalized,
                            "properties.extra_animation_classify." + classifyId + ".extra_animation." + actionId,
                            rawLabel
                    );
                    actions.put(actionId, new DetectedYsmAction(actionId, localizedLabel));
                }
                index++;
            }
        }

        mergeAnimationResources(resourceManager, descriptorLocation, localized, fallbackLocalized, actions);

        if (actions.isEmpty()) {
            return loadActionsFromArchives(minecraft, lookupRoots, languageCode);
        }

        List<DetectedYsmAction> result = new ArrayList<>(actions.values());
        result.sort(Comparator.comparing(DetectedYsmAction::displayName, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    private static void mergeAnimationResources(ResourceManager resourceManager,
                                               ResourceLocation descriptorLocation,
                                               JsonObject localized,
                                               JsonObject fallbackLocalized,
                                               Map<String, DetectedYsmAction> actions) {
        String modelRoot = descriptorLocation.getPath().substring(0, descriptorLocation.getPath().length() - "/ysm.json".length());
        String animationPrefix = modelRoot + "/animations/";
        Set<String> seen = new HashSet<>(actions.keySet());
        for (ResourceLocation location : listResourcesSafely(resourceManager, "", candidate ->
                candidate.getNamespace().equals(descriptorLocation.getNamespace())
                        && candidate.getPath().startsWith(animationPrefix)
                        && candidate.getPath().endsWith(".animation.json")).keySet()) {
            JsonObject animationJson = readJson(resourceManager, location);
            if (animationJson == null || !animationJson.has("animations") || !animationJson.get("animations").isJsonObject()) {
                continue;
            }
            String relativePath = location.getPath().substring(animationPrefix.length());
            String family = relativePath.substring(0, relativePath.length() - ".animation.json".length());
            JsonObject animations = animationJson.getAsJsonObject("animations");
            for (Map.Entry<String, JsonElement> entry : animations.entrySet()) {
                String actionId = entry.getKey();
                if (shouldIgnoreAnimationId(actionId)) {
                    continue;
                }
                if (!seen.add(actionId)) {
                    continue;
                }
                String label = resolveAnimationLabel(localized, fallbackLocalized, family, actionId);
                actions.put(actionId, new DetectedYsmAction(actionId, label));
            }
        }
    }

    private static boolean shouldIgnoreAnimationId(String actionId) {
        if (actionId == null || actionId.isBlank()) {
            return true;
        }
        if (actionId.startsWith("pre_parallel") || actionId.startsWith("parallel")) {
            return true;
        }
        if (actionId.startsWith("——") || actionId.endsWith("——")) {
            return true;
        }
        return IGNORED_ANIMATION_IDS.contains(actionId);
    }

    private static String resolveAnimationLabel(JsonObject localized, JsonObject fallbackLocalized, String family, String actionId) {
        String fromLang = localizedString(localized, fallbackLocalized, "properties.extra_animation." + actionId, "");
        if (!fromLang.isBlank()) {
            return fromLang;
        }
        fromLang = localizedString(localized, fallbackLocalized, "properties.extra_animation_classify." + family + ".extra_animation." + actionId, "");
        if (!fromLang.isBlank()) {
            return fromLang;
        }

        String prettyAction = prettifyIdentifier(actionId);
        String familyLeaf = family.substring(family.lastIndexOf('/') + 1);
        if ("main".equals(familyLeaf) || "extra".equals(familyLeaf) || "tlm".equals(familyLeaf)) {
            return prettyAction;
        }
        return prettifyIdentifier(familyLeaf) + " | " + prettyAction;
    }

    private static String prettifyIdentifier(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.replace('$', '|').replace('_', ' ').replace('/', ' ').trim();
        if (normalized.isBlank()) {
            return value;
        }
        return normalized;
    }

    private static List<DetectedYsmAction> loadActionsFromArchives(Minecraft minecraft, List<String> lookupRoots, String languageCode) {
        java.io.File modsDir = new java.io.File(minecraft.gameDirectory, "mods");
        if (!modsDir.isDirectory()) {
            return List.of();
        }
        java.io.File[] archives = modsDir.listFiles(file -> file.isFile() && file.getName().endsWith(".jar"));
        if (archives == null || archives.length == 0) {
            return List.of();
        }
        for (java.io.File archive : archives) {
            List<DetectedYsmAction> actions = loadActionsFromArchive(archive, lookupRoots, languageCode);
            if (!actions.isEmpty()) {
                return actions;
            }
        }
        return List.of();
    }

    private static List<DetectedYsmAction> loadActionsFromArchive(java.io.File archive, List<String> lookupRoots, String languageCode) {
        try (ZipFile zip = new ZipFile(archive, StandardCharsets.UTF_8)) {
            String modelRoot = findArchiveModelRoot(zip, lookupRoots);
            if (modelRoot == null) {
                return List.of();
            }

            JsonObject descriptor = readJson(zip, modelRoot + "/ysm.json");
            if (descriptor == null) {
                return List.of();
            }
            JsonObject localized = readJson(zip, modelRoot + "/lang/" + languageCode + ".json");
            JsonObject fallbackLocalized = "en_us".equals(languageCode)
                    ? localized
                    : readJson(zip, modelRoot + "/lang/en_us.json");

            Map<String, DetectedYsmAction> actions = new LinkedHashMap<>();
            JsonObject properties = descriptor.has("properties") && descriptor.get("properties").isJsonObject()
                    ? descriptor.getAsJsonObject("properties")
                    : null;
            if (properties != null) {
                JsonObject extraAnimations = properties.has("extra_animation") && properties.get("extra_animation").isJsonObject()
                        ? properties.getAsJsonObject("extra_animation")
                        : null;
                if (extraAnimations != null) {
                    for (Map.Entry<String, JsonElement> entry : extraAnimations.entrySet()) {
                        String actionId = entry.getKey();
                        if (actionId.startsWith("#")) {
                            continue;
                        }
                        String rawLabel = entry.getValue().isJsonPrimitive() ? entry.getValue().getAsString() : actionId;
                        if (rawLabel.startsWith("#")) {
                            rawLabel = rawLabel.substring(1);
                        }
                        if (rawLabel.isBlank()) {
                            rawLabel = actionId;
                        }
                        String localizedLabel = localizedString(localized, fallbackLocalized, "properties.extra_animation." + actionId, rawLabel);
                        actions.put(actionId, new DetectedYsmAction(actionId, localizedLabel));
                    }
                }
            }

            String animationPrefix = modelRoot + "/animations/";
            Set<String> seen = new HashSet<>(actions.keySet());
            zip.stream()
                    .filter(entry -> !entry.isDirectory())
                    .map(ZipEntry::getName)
                    .filter(name -> name.startsWith(animationPrefix) && name.endsWith(".animation.json"))
                    .forEach(name -> {
                        JsonObject animationJson = readJson(zip, name);
                        if (animationJson == null || !animationJson.has("animations") || !animationJson.get("animations").isJsonObject()) {
                            return;
                        }
                        String relativePath = name.substring(animationPrefix.length());
                        String family = relativePath.substring(0, relativePath.length() - ".animation.json".length());
                        JsonObject animations = animationJson.getAsJsonObject("animations");
                        for (Map.Entry<String, JsonElement> entry : animations.entrySet()) {
                            String actionId = entry.getKey();
                            if (shouldIgnoreAnimationId(actionId) || !seen.add(actionId)) {
                                continue;
                            }
                            String label = resolveAnimationLabel(localized, fallbackLocalized, family, actionId);
                            actions.put(actionId, new DetectedYsmAction(actionId, label));
                        }
                    });

            List<DetectedYsmAction> result = new ArrayList<>(actions.values());
            result.sort(Comparator.comparing(DetectedYsmAction::displayName, String.CASE_INSENSITIVE_ORDER));
            return result;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private static JsonObject readJson(ZipFile zip, String entryName) {
        try {
            ZipEntry entry = zip.getEntry(entryName);
            if (entry == null) {
                return null;
            }
            try (Reader reader = new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String findArchiveModelRoot(ZipFile zip, List<String> lookupRoots) {
        for (ZipEntry entry : java.util.Collections.list(zip.entries())) {
            String name = entry.getName();
            if (!name.endsWith("/ysm.json")) {
                continue;
            }
            int assetsIndex = name.indexOf("assets/");
            if (assetsIndex < 0) {
                continue;
            }
            int namespaceSlash = name.indexOf('/', "assets/".length());
            if (namespaceSlash < 0 || namespaceSlash + 1 >= name.length()) {
                continue;
            }
            String pathWithinNamespace = name.substring(namespaceSlash + 1, name.length() - "/ysm.json".length());
            int score = matchScore(lookupRoots, pathWithinNamespace);
            if (score >= 0) {
                return name.substring(0, name.length() - "/ysm.json".length());
            }
        }
        return null;
    }

    private static JsonObject readJson(ResourceManager resourceManager, ResourceLocation location) {
        try {
            Resource resource = resourceManager.getResourceOrThrow(location);
            try (Reader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static ResourceLocation findDescriptor(ResourceManager resourceManager, List<String> lookupRoots) {
        ResourceLocation best = null;
        int bestScore = Integer.MAX_VALUE;
        for (ResourceLocation candidate : listResourcesSafely(resourceManager, "", location -> location.getPath().endsWith("/ysm.json")).keySet()) {
            String rootPath = candidate.getPath().substring(0, candidate.getPath().length() - "/ysm.json".length());
            int score = matchScore(lookupRoots, rootPath);
            if (score >= 0 && score < bestScore) {
                best = candidate;
                bestScore = score;
            }
        }
        return best;
    }

    private static Map<ResourceLocation, Resource> listResourcesSafely(ResourceManager resourceManager,
                                                                       String pathPrefix,
                                                                       Predicate<ResourceLocation> filter) {
        try {
            return resourceManager.listResources(pathPrefix, filter);
        } catch (RuntimeException ignored) {
            return Map.of();
        }
    }

    private static int matchScore(List<String> lookupRoots, String rootPath) {
        int best = Integer.MAX_VALUE;
        for (String lookupRoot : lookupRoots) {
            int score = matchSingleRoot(lookupRoot, rootPath);
            if (score >= 0 && score < best) {
                best = score;
            }
        }
        return best == Integer.MAX_VALUE ? -1 : best;
    }

    private static int matchSingleRoot(String normalizedModelId, String rootPath) {
        String normalizedRoot = normalizeModelId(rootPath);
        if (normalizedModelId.equals(normalizedRoot)) {
            return 0;
        }
        if (normalizedRoot.endsWith("/" + normalizedModelId) || normalizedModelId.endsWith("/" + normalizedRoot)) {
            return Math.abs(normalizedRoot.length() - normalizedModelId.length()) + 10;
        }
        String inputLeaf = normalizedModelId.substring(normalizedModelId.lastIndexOf('/') + 1);
        String rootLeaf = normalizedRoot.substring(normalizedRoot.lastIndexOf('/') + 1);
        if (!inputLeaf.isBlank() && inputLeaf.equals(rootLeaf)) {
            return Math.abs(normalizedRoot.length() - normalizedModelId.length()) + 50;
        }
        return -1;
    }

    private static List<String> buildLookupRoots(String normalizedModelId, String textureId) {
        LinkedHashMap<String, Boolean> roots = new LinkedHashMap<>();
        addLookupRoot(roots, normalizedModelId);

        String normalizedTextureId = normalizeTextureId(textureId);
        if (!normalizedTextureId.isBlank()) {
            addLookupRoot(roots, normalizedTextureId);
            if (!normalizedTextureId.startsWith(normalizedModelId + "/")) {
                addLookupRoot(roots, normalizedModelId + "/" + normalizedTextureId);
            }
        }
        return new ArrayList<>(roots.keySet());
    }

    private static void addLookupRoot(Map<String, Boolean> roots, String candidate) {
        String normalized = normalizeModelId(candidate);
        if (!normalized.isBlank()) {
            roots.put(normalized, Boolean.TRUE);
        }
    }

    private static String localizedString(JsonObject primary, JsonObject fallback, String key, String defaultValue) {
        if (primary != null && primary.has(key) && primary.get(key).isJsonPrimitive()) {
            return primary.get(key).getAsString();
        }
        if (fallback != null && fallback.has(key) && fallback.get(key).isJsonPrimitive()) {
            return fallback.get(key).getAsString();
        }
        return defaultValue;
    }

    private static String normalizeModelId(String modelId) {
        return NamespacedPathNormalizer.normalizeModelId(modelId);
    }

    private static String normalizeTextureId(String textureId) {
        return NamespacedPathNormalizer.normalizeTextureId(textureId);
    }

    public record DetectedYsmAction(String actionId, String displayName) {
    }
}
