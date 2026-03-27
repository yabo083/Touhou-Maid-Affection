package com.github.touhoumaidaffection.client;

import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.github.touhoumaidaffection.network.rescue.RescueSoundReloadPayload;
import com.github.touhoumaidaffection.network.rescue.RescueSoundSyncChunkPayload;
import com.github.touhoumaidaffection.network.rescue.RescueSoundSyncClearPayload;
import com.github.touhoumaidaffection.network.rescue.RescueSoundSyncCompletePayload;
import com.github.touhoumaidaffection.network.rescue.RescueSoundSyncManifestPayload;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class EmergencyRescueServerSoundSyncClient {
    private static final Map<String, PendingTransfer> PENDING_TRANSFERS = new ConcurrentHashMap<>();
    private static volatile String activeServerId = "default_server";
    private static volatile long activeGeneration = -1L;

    private EmergencyRescueServerSoundSyncClient() {
    }

    public static void handleManifest(RescueSoundSyncManifestPayload payload) {
        activeServerId = EmergencyRescueCustomVoiceConfig.sanitizeServerId(payload.serverId());
        activeGeneration = payload.generation();
        TouhouMaidAffection.LOGGER.info(
                "Rescue sound sync manifest received: serverId={}, generation={}, fullSnapshot={}, fileCount={}",
                activeServerId,
                activeGeneration,
                payload.fullSnapshot(),
                payload.relativePaths().size()
        );
    }

    public static void handleClear(RescueSoundSyncClearPayload payload) {
        String serverId = EmergencyRescueCustomVoiceConfig.sanitizeServerId(payload.serverId());
        Path root = EmergencyRescueCustomVoiceConfig.syncedRootForServer(serverId);
        if (payload.clearAll()) {
            deleteRecursively(root);
            EmergencyRescueCustomVoiceConfig.syncedRootForServer(serverId);
            TouhouMaidAffection.LOGGER.info("Rescue sound sync clear-all applied for server {}", serverId);
        } else {
            for (String relativePath : payload.relativePaths()) {
                Path target = resolveTargetPath(serverId, relativePath);
                if (target == null) {
                    continue;
                }
                deleteRecursively(target);
            }
            TouhouMaidAffection.LOGGER.info(
                    "Rescue sound sync clear applied for server {} ({} paths)",
                    serverId,
                    payload.relativePaths().size()
            );
        }
        EmergencyRescueSoundPlayer.invalidateCaches();
    }

    public static void handleChunk(RescueSoundSyncChunkPayload payload) {
        String serverId = EmergencyRescueCustomVoiceConfig.sanitizeServerId(payload.serverId());
        Path target = resolveTargetPath(serverId, payload.relativePath());
        if (target == null) {
            return;
        }
        if (payload.totalChunks() <= 0 || payload.chunkIndex() < 0 || payload.chunkIndex() >= payload.totalChunks()) {
            TouhouMaidAffection.LOGGER.warn(
                    "Invalid rescue sound chunk metadata for '{}' (index={}, total={})",
                    payload.relativePath(),
                    payload.chunkIndex(),
                    payload.totalChunks()
            );
            return;
        }

        String transferKey = serverId + "|" + payload.generation() + "|" + payload.relativePath();
        PendingTransfer transfer = PENDING_TRANSFERS.computeIfAbsent(transferKey, ignored ->
                new PendingTransfer(serverId, payload.generation(), payload.relativePath(), payload.totalChunks(), payload.totalSize()));
        if (transfer.totalChunks() != payload.totalChunks()) {
            TouhouMaidAffection.LOGGER.warn(
                    "Rescue sound chunk mismatch for '{}' (expectedChunks={}, got={})",
                    payload.relativePath(),
                    transfer.totalChunks(),
                    payload.totalChunks()
            );
            PENDING_TRANSFERS.remove(transferKey);
            return;
        }
        if (transfer.chunks()[payload.chunkIndex()] == null) {
            transfer.chunks()[payload.chunkIndex()] = payload.chunkData();
            transfer.receivedChunks(transfer.receivedChunks() + 1);
        }
        if (transfer.receivedChunks() < transfer.totalChunks()) {
            return;
        }

        byte[] bytes = mergeChunks(transfer.chunks(), transfer.totalSize());
        if (bytes == null) {
            TouhouMaidAffection.LOGGER.warn("Failed to merge rescue sound chunks for '{}'", payload.relativePath());
            PENDING_TRANSFERS.remove(transferKey);
            return;
        }

        try {
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
        } catch (IOException ex) {
            TouhouMaidAffection.LOGGER.warn("Failed to write synced rescue sound '{}'", target, ex);
            PENDING_TRANSFERS.remove(transferKey);
            return;
        }

        PENDING_TRANSFERS.remove(transferKey);
        TouhouMaidAffection.LOGGER.info(
                "Rescue sound sync file updated: serverId={}, path='{}', bytes={}",
                serverId,
                payload.relativePath(),
                bytes.length
        );
        EmergencyRescueSoundPlayer.invalidateCaches();
    }

    public static void handleComplete(RescueSoundSyncCompletePayload payload) {
        String serverId = EmergencyRescueCustomVoiceConfig.sanitizeServerId(payload.serverId());
        activeServerId = serverId;
        activeGeneration = payload.generation();
        TouhouMaidAffection.LOGGER.info(
                "Rescue sound sync complete: serverId={}, generation={}, updatedFiles={}",
                serverId,
                payload.generation(),
                payload.updatedPaths().size()
        );
    }

    public static void handleReload(RescueSoundReloadPayload payload) {
        TouhouMaidAffection.LOGGER.info("Rescue sound local reload requested: {}", payload.reason());
        EmergencyRescueSoundPlayer.invalidateCaches();
    }

    public static String getActiveServerId() {
        return activeServerId;
    }

    private static Path resolveTargetPath(String serverId, String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        String normalized = relativePath.replace('\\', '/').trim();
        if (normalized.startsWith("/") || normalized.contains("..")) {
            TouhouMaidAffection.LOGGER.warn("Rejected unsafe rescue sync path '{}'", relativePath);
            return null;
        }
        Path root = EmergencyRescueCustomVoiceConfig.syncedRootForServer(serverId);
        Path target = root.resolve(normalized).normalize();
        if (!target.startsWith(root)) {
            TouhouMaidAffection.LOGGER.warn("Rejected escape rescue sync path '{}'", relativePath);
            return null;
        }
        return target;
    }

    private static byte[] mergeChunks(byte[][] chunks, int declaredTotalSize) {
        int computedSize = Arrays.stream(chunks).filter(chunk -> chunk != null).mapToInt(chunk -> chunk.length).sum();
        if (declaredTotalSize > 0 && computedSize != declaredTotalSize) {
            TouhouMaidAffection.LOGGER.warn(
                    "Rescue sound chunk size mismatch (declared={}, computed={})",
                    declaredTotalSize,
                    computedSize
            );
        }
        byte[] merged = new byte[computedSize];
        int offset = 0;
        for (byte[] chunk : chunks) {
            if (chunk == null) {
                return null;
            }
            System.arraycopy(chunk, 0, merged, offset, chunk.length);
            offset += chunk.length;
        }
        return merged;
    }

    private static void deleteRecursively(Path path) {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try {
            if (Files.isDirectory(path)) {
                try (var stream = Files.list(path)) {
                    stream.forEach(EmergencyRescueServerSoundSyncClient::deleteRecursively);
                }
            }
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            TouhouMaidAffection.LOGGER.warn("Failed to delete synced rescue path '{}'", path, ex);
        }
    }

    private static final class PendingTransfer {
        private final String serverId;
        private final long generation;
        private final String relativePath;
        private final int totalChunks;
        private final int totalSize;
        private final byte[][] chunks;
        private int receivedChunks;

        private PendingTransfer(String serverId, long generation, String relativePath, int totalChunks, int totalSize) {
            this.serverId = serverId;
            this.generation = generation;
            this.relativePath = relativePath;
            this.totalChunks = totalChunks;
            this.totalSize = totalSize;
            this.chunks = new byte[totalChunks][];
            this.receivedChunks = 0;
        }

        private int totalChunks() {
            return totalChunks;
        }

        private int totalSize() {
            return totalSize;
        }

        private byte[][] chunks() {
            return chunks;
        }

        private int receivedChunks() {
            return receivedChunks;
        }

        private void receivedChunks(int value) {
            this.receivedChunks = value;
        }
    }
}
