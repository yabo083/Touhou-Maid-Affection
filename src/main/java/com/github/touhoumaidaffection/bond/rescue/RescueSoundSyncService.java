package com.github.touhoumaidaffection.bond.rescue;

import com.github.touhoumaidaffection.ModConfig;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.github.touhoumaidaffection.network.rescue.RescueSoundReloadPayload;
import com.github.touhoumaidaffection.network.rescue.RescueSoundSyncChunkPayload;
import com.github.touhoumaidaffection.network.rescue.RescueSoundSyncClearPayload;
import com.github.touhoumaidaffection.network.rescue.RescueSoundSyncCompletePayload;
import com.github.touhoumaidaffection.network.rescue.RescueSoundSyncManifestPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.network.PacketDistributor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@EventBusSubscriber(modid = TouhouMaidAffection.MOD_ID)
public final class RescueSoundSyncService {
    private static final Path SERVER_PREDEFINED_ROOT = FMLPaths.CONFIGDIR.get()
            .resolve(TouhouMaidAffection.MOD_ID)
            .resolve("rescue")
            .resolve("server_predefined");
    private static final int DEFAULT_SCAN_INTERVAL_SECONDS = 30;
    private static final int CHUNK_SIZE = 24 * 1024;

    private static volatile Snapshot lastSnapshot = Snapshot.empty();
    private static volatile long generation = 0L;
    private static volatile long nextScanTick = 0L;
    private static volatile String serverId = "default_server";

    private RescueSoundSyncService() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        serverId = computeServerId(server);
        ensureServerDirs();
        lastSnapshot = scanSnapshot();
        generation = 1L;
        nextScanTick = server.getTickCount() + getScanIntervalTicks();
        TouhouMaidAffection.LOGGER.info(
                "Rescue sound sync initialized: serverId={}, files={}",
                serverId,
                lastSnapshot.files().size()
        );
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        lastSnapshot = Snapshot.empty();
        generation = 0L;
        nextScanTick = 0L;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = event.getServer();
        if (server == null || server.getPlayerList() == null || server.getPlayerList().getPlayers().isEmpty()) {
            return;
        }
        if (server.getTickCount() < nextScanTick) {
            return;
        }
        nextScanTick = server.getTickCount() + getScanIntervalTicks();
        syncIfChanged(server, false);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (generation <= 0L) {
            return;
        }
        Snapshot current = scanSnapshot();
        Diff diff = Diff.between(lastSnapshot, current);
        if (!diff.isEmpty()) {
            generation++;
            lastSnapshot = current;
        }
        sendFullSnapshot(player);
    }

    public static void forceResync(MinecraftServer server) {
        if (server == null) {
            return;
        }
        syncIfChanged(server, true);
    }

    public static void requestClientReload(ServerPlayer player, String reason) {
        if (player == null) {
            return;
        }
        TouhouMaidAffection.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new RescueSoundReloadPayload(reason == null ? "manual" : reason));
    }

    private static void syncIfChanged(MinecraftServer server, boolean force) {
        Snapshot current = scanSnapshot();
        Snapshot previous = lastSnapshot;
        Diff diff = Diff.between(previous, current);
        if (!force && diff.isEmpty()) {
            return;
        }

        generation++;
        lastSnapshot = current;
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) {
            return;
        }

        List<String> changedPaths = new ArrayList<>(diff.changedOrAdded().keySet());
        List<String> removedPaths = new ArrayList<>(diff.removedPaths());
        for (ServerPlayer player : players) {
            TouhouMaidAffection.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new RescueSoundSyncManifestPayload(serverId, generation, false, changedPaths));
            if (!removedPaths.isEmpty()) {
                TouhouMaidAffection.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new RescueSoundSyncClearPayload(serverId, generation, false, removedPaths));
            }
            sendFiles(player, generation, diff.changedOrAdded());
            TouhouMaidAffection.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new RescueSoundSyncCompletePayload(serverId, generation, changedPaths));
        }

        TouhouMaidAffection.LOGGER.info(
                "Rescue sound sync updated: generation={}, changed={}, removed={}, players={}",
                generation,
                changedPaths.size(),
                removedPaths.size(),
                players.size()
        );
    }

    private static void sendFullSnapshot(ServerPlayer player) {
        Map<String, SnapshotFile> files = lastSnapshot.files();
        List<String> relativePaths = new ArrayList<>(files.keySet());
        TouhouMaidAffection.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new RescueSoundSyncManifestPayload(serverId, generation, true, relativePaths));
        TouhouMaidAffection.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new RescueSoundSyncClearPayload(serverId, generation, true, List.of()));
        sendFiles(player, generation, files);
        TouhouMaidAffection.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new RescueSoundSyncCompletePayload(serverId, generation, relativePaths));
        TouhouMaidAffection.LOGGER.info(
                "Rescue sound full snapshot sent to {} (generation={}, files={})",
                player.getGameProfile().getName(),
                generation,
                relativePaths.size()
        );
    }

    private static void sendFiles(ServerPlayer player, long generation, Map<String, SnapshotFile> files) {
        for (Map.Entry<String, SnapshotFile> entry : files.entrySet()) {
            String relativePath = entry.getKey();
            byte[] data = entry.getValue().bytes();
            int totalChunks = Math.max(1, (data.length + CHUNK_SIZE - 1) / CHUNK_SIZE);
            for (int chunkIndex = 0; chunkIndex < totalChunks; chunkIndex++) {
                int start = chunkIndex * CHUNK_SIZE;
                int end = Math.min(data.length, start + CHUNK_SIZE);
                byte[] chunk = Arrays.copyOfRange(data, start, end);
                TouhouMaidAffection.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new RescueSoundSyncChunkPayload(
                        serverId,
                        generation,
                        relativePath,
                        chunkIndex,
                        totalChunks,
                        data.length,
                        chunk
                ));
            }
        }
    }

    private static Snapshot scanSnapshot() {
        ensureServerDirs();
        LinkedHashMap<String, SnapshotFile> files = new LinkedHashMap<>();
        scanScopeDir(SERVER_PREDEFINED_ROOT.resolve("maids"), "maids", files);
        scanScopeDir(SERVER_PREDEFINED_ROOT.resolve("common"), "common", files);
        return new Snapshot(Map.copyOf(files));
    }

    private static void scanScopeDir(Path scopeRoot, String scopePrefix, Map<String, SnapshotFile> output) {
        if (!Files.isDirectory(scopeRoot)) {
            return;
        }
        try (var paths = Files.walk(scopeRoot)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".ogg"))
                    .forEach(path -> {
                        Path relative = scopeRoot.relativize(path);
                        String normalized = relative.toString().replace('\\', '/');
                        if (normalized.isBlank()) {
                            return;
                        }
                        String key = scopePrefix + "/" + normalized;
                        try {
                            byte[] bytes = Files.readAllBytes(path);
                            long size = bytes.length;
                            long modified = Files.getLastModifiedTime(path).toMillis();
                            String sha1 = sha1Hex(bytes);
                            output.put(key, new SnapshotFile(size, modified, sha1, bytes));
                        } catch (Exception ex) {
                            TouhouMaidAffection.LOGGER.warn("Failed to scan rescue sound file '{}'", path, ex);
                        }
                    });
        } catch (IOException ex) {
            TouhouMaidAffection.LOGGER.warn("Failed to scan rescue sound scope '{}'", scopeRoot, ex);
        }
    }

    private static String computeServerId(MinecraftServer server) {
        String base;
        if (server.isDedicatedServer()) {
            base = "dedicated_" + server.getPort();
        } else {
            base = "singleplayer_" + server.getWorldData().getLevelName();
        }
        String sanitized = base.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (sanitized.isBlank()) {
            return "default_server";
        }
        return sanitized.toLowerCase(Locale.ROOT);
    }

    private static int getScanIntervalTicks() {
        int configured = ModConfig.BOND_EMERGENCY_RESCUE_SYNC_SCAN_INTERVAL_SECONDS == null
                ? DEFAULT_SCAN_INTERVAL_SECONDS
                : ModConfig.BOND_EMERGENCY_RESCUE_SYNC_SCAN_INTERVAL_SECONDS.get();
        return Math.max(5, configured) * 20;
    }

    private static String sha1Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(bytes);
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format(Locale.ROOT, "%02x", b));
            }
            return builder.toString();
        } catch (Exception ex) {
            return "";
        }
    }

    private static void ensureServerDirs() {
        try {
            Files.createDirectories(SERVER_PREDEFINED_ROOT.resolve("maids"));
            Files.createDirectories(SERVER_PREDEFINED_ROOT.resolve("common"));
        } catch (IOException ex) {
            TouhouMaidAffection.LOGGER.warn("Failed to create rescue server predefined directories", ex);
        }
    }

    private record Snapshot(Map<String, SnapshotFile> files) {
        private static Snapshot empty() {
            return new Snapshot(Map.of());
        }
    }

    private record SnapshotFile(long size, long modifiedMillis, String sha1, byte[] bytes) {
    }

    private record Diff(Map<String, SnapshotFile> changedOrAdded, Set<String> removedPaths) {
        private static Diff between(Snapshot previous, Snapshot current) {
            LinkedHashMap<String, SnapshotFile> changedOrAdded = new LinkedHashMap<>();
            LinkedHashSet<String> removed = new LinkedHashSet<>(previous.files().keySet());
            removed.removeAll(current.files().keySet());

            for (Map.Entry<String, SnapshotFile> entry : current.files().entrySet()) {
                String path = entry.getKey();
                SnapshotFile next = entry.getValue();
                SnapshotFile old = previous.files().get(path);
                if (old == null || old.size() != next.size() || old.modifiedMillis() != next.modifiedMillis() || !old.sha1().equals(next.sha1())) {
                    changedOrAdded.put(path, next);
                }
            }
            return new Diff(Map.copyOf(changedOrAdded), Set.copyOf(removed));
        }

        private boolean isEmpty() {
            return changedOrAdded.isEmpty() && removedPaths.isEmpty();
        }
    }
}
