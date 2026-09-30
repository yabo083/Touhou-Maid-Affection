package com.github.touhoumaidaffection.bond.service;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.touhoumaidaffection.ModConfig;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.github.touhoumaidaffection.bond.BondManager;
import com.github.tartaricacid.touhoulittlemaid.world.backups.MaidBackupsManager;
import com.github.touhoumaidaffection.bond.BondData;
import com.github.touhoumaidaffection.bond.RandomGiftClock;
import com.github.touhoumaidaffection.bond.RandomGiftQueue;
import com.github.touhoumaidaffection.bond.settings.TmaGiftStatusWire;
import com.github.touhoumaidaffection.bond.lap.LapPillowState;
import com.github.touhoumaidaffection.handler.BondSyncHelper;
import com.github.touhoumaidaffection.util.MaidDisplayNameResolver;
import com.github.touhoumaidaffection.ysm.YSMActionBridge;
import com.github.touhoumaidaffection.ysm.YSMMaidAnimation;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.TickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = TouhouMaidAffection.MOD_ID)
public final class RandomGiftService {
    private static final TagKey<Item> GIFT_POOL_TAG = TagKey.create(
            net.minecraft.core.registries.Registries.ITEM,
            new ResourceLocation(TouhouMaidAffection.MOD_ID, "bond_random_gift_pool")
    );
    private static final TagKey<Item> GIFT_BLACKLIST_TAG = TagKey.create(
            net.minecraft.core.registries.Registries.ITEM,
            new ResourceLocation(TouhouMaidAffection.MOD_ID, "bond_random_gift_blacklist")
    );

    private static final Map<UUID, PendingDeliveryTask> DELIVERY_TASKS = new HashMap<>();
    private static final int SCAN_INTERVAL_TICKS = 20;

    private RandomGiftService() {
    }

    public static int reconcileQueuedGifts(ServerPlayer player, EntityMaid maid) {
        if (!ModConfig.BOND_RANDOM_GIFT_ENABLED.get() || !maid.isOwnedBy(player)
                || !BondManager.isAbilityUnlocked(player, maid.getUUID(), "random_gift")) {
            return BondManager.getQueuedGiftCount(player, maid.getUUID());
        }
        BondManager.reconcileRandomGiftQueue(player, maid.getUUID(), System.currentTimeMillis());
        return prepareGifts(player, maid).queued();
    }

    public static long getNextGiftReadyAtMs(ServerPlayer player, EntityMaid maid) {
        return BondManager.getNextRandomGiftReadyAtMs(player, maid.getUUID(), System.currentTimeMillis());
    }


    /** Read-only, owner-scoped projection. It never loads chunks, rolls gifts, or advances the clock. */
    public static TmaGiftStatusWire.Status giftStatus(ServerPlayer player, int requestedPage) {
        long nowMs = System.currentTimeMillis();
        BondData data = BondData.readOnly(player);
        Map<UUID, String> ownedNames = new HashMap<>();
        try {
            MaidBackupsManager.getMaidIndexMap(player).forEach((uuid, entry) ->
                    ownedNames.put(uuid, entry.name() == null ? "" : entry.name().getString()));
        } catch (RuntimeException ex) {
            TouhouMaidAffection.LOGGER.warn("[TMA Gift Status] Failed to read maid index for player={}", player.getScoreboardName(), ex);
        }
        Map<UUID, EntityMaid> loaded = new HashMap<>();
        List<UUID> unlocked = data.getUnlockedMaidIdsForAbility("random_gift");
        for (UUID uuid : unlocked) {
            for (ServerLevel level : player.getServer().getAllLevels()) {
                Entity entity = level.getEntity(uuid);
                if (!(entity instanceof EntityMaid maid)) continue;
                // A loaded entity is authoritative if an old backup still names a former owner.
                if (maid.isAlive() && maid.isOwnedBy(player)) {
                    loaded.put(uuid, maid);
                    ownedNames.put(uuid, MaidDisplayNameResolver.resolveDisplayName(maid).getString());
                } else {
                    ownedNames.remove(uuid);
                }
                break;
            }
        }
        unlocked.removeIf(uuid -> !ownedNames.containsKey(uuid));
        unlocked.sort(Comparator.comparing((UUID uuid) -> ownedNames.get(uuid), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(UUID::toString));
        int page = Math.max(0, Math.min(requestedPage, Math.max(0, (unlocked.size() - 1) / TmaGiftStatusWire.MAX_MAIDS)));
        int candidateCount = collectGiftCandidates(null).size();
        boolean enabled = ModConfig.BOND_RANDOM_GIFT_ENABLED.get();
        int interval = ModConfig.BOND_RANDOM_GIFT_INTERVAL_REAL_MINUTES.get();
        int maxQueued = ModConfig.BOND_RANDOM_GIFT_MAX_QUEUED.get();
        List<TmaGiftStatusWire.MaidStatus> maids = new ArrayList<>();
        int end = Math.min(unlocked.size(), (page + 1) * TmaGiftStatusWire.MAX_MAIDS);
        for (int i = page * TmaGiftStatusWire.MAX_MAIDS; i < end; i++) {
            UUID uuid = unlocked.get(i);
            RandomGiftQueue queue = data.getRandomGiftQueue(uuid).retain(RandomGiftService::isPreparedGiftAllowed);
            RandomGiftClock.State clock = RandomGiftClock.reconcile(queue.queued(), data.getLastGiftWallClockMs(uuid),
                    data.getLastGiftIntervalMinutes(uuid), interval, maxQueued, nowMs);
            int queued = enabled ? clock.queued() : queue.queued();
            List<TmaGiftStatusWire.Gift> gifts = queue.prepared().stream()
                    .map(id -> new TmaGiftStatusWire.Gift(id, 1)).toList();
            EntityMaid maid = loaded.get(uuid);
            int cooldown = deliveryCooldownSeconds(data, uuid, player.serverLevel().getGameTime());
            TmaGiftStatusWire.DeliveryState state = deliveryState(player, maid, queued, gifts.size(), candidateCount, cooldown);
            String name = ownedNames.get(uuid);
            if (name.isBlank()) name = data.getMaidDisplayName(uuid);
            maids.add(new TmaGiftStatusWire.MaidStatus(uuid.toString(), statusText(name), queued, gifts.size(), gifts,
                    enabled ? clock.nextReadyAtMs() : 0L, data.getLastGiftDeliveryWallClockMs(uuid),
                    statusText(data.getLastDeliveredGiftId(uuid)), state, cooldown));
        }
        return new TmaGiftStatusWire.Status(nowMs, enabled, ModConfig.BOND_RANDOM_GIFT_CURATED_POOL_ONLY.get(),
                ModConfig.BOND_RANDOM_GIFT_INCLUDE_MOD_ITEMS.get(), candidateCount, interval, maxQueued, page, unlocked.size(), maids);
    }

    private static String statusText(String value) {
        return value.length() <= TmaGiftStatusWire.MAX_STRING_LENGTH ? value
                : value.substring(0, TmaGiftStatusWire.MAX_STRING_LENGTH);
    }


    private static int deliveryCooldownSeconds(BondData data, UUID uuid, long gameTime) {
        long elapsed = Math.max(0L, gameTime - data.getLastGiftDeliveryGameTime(uuid));
        long remaining = Math.max(0L, ModConfig.BOND_RANDOM_GIFT_DELIVERY_COOLDOWN_TICKS.get() - elapsed);
        return (int) ((remaining + 19L) / 20L);
    }

    private static TmaGiftStatusWire.DeliveryState deliveryState(ServerPlayer player, EntityMaid maid,
            int queued, int prepared, int candidates, int cooldown) {
        if (!ModConfig.BOND_RANDOM_GIFT_ENABLED.get()) return TmaGiftStatusWire.DeliveryState.DISABLED;
        if (maid == null) return TmaGiftStatusWire.DeliveryState.UNLOADED;
        if (maid.level() != player.level()) return TmaGiftStatusWire.DeliveryState.OTHER_DIMENSION;
        double range = ModConfig.BOND_RANDOM_GIFT_DELIVERY_SEARCH_RANGE.get();
        if (player.distanceToSqr(maid) > range * range) return TmaGiftStatusWire.DeliveryState.TOO_FAR;
        if (LapPillowState.isActive(player)) return TmaGiftStatusWire.DeliveryState.LAP_PILLOW;
        if (cooldown > 0) return TmaGiftStatusWire.DeliveryState.COOLDOWN;
        if (queued == 0) return TmaGiftStatusWire.DeliveryState.PREPARING;
        if (prepared == 0) return candidates == 0 ? TmaGiftStatusWire.DeliveryState.POOL_EMPTY : TmaGiftStatusWire.DeliveryState.PREPARING;
        PendingDeliveryTask task = DELIVERY_TASKS.get(maid.getUUID());
        double reach = Math.max(ModConfig.BOND_RANDOM_GIFT_DELIVERY_REACH_DISTANCE.get(), 2.8D);
        boolean ready = task == null ? isReadyToThrowGift(maid, player, reach)
                : shouldDeliverNow(maid, player, task, range, reach, player.serverLevel().getGameTime());
        return ready ? TmaGiftStatusWire.DeliveryState.READY : TmaGiftStatusWire.DeliveryState.APPROACHING;
    }


    public static void cancelForMaid(UUID maidUuid) {
        if (maidUuid == null) {
            return;
        }
        DELIVERY_TASKS.remove(maidUuid);
    }

    public static void cancelForPlayer(ServerPlayer player) {
        if (player == null) {
            return;
        }
        DELIVERY_TASKS.values().removeIf(task -> player.getUUID().equals(task.playerUuid()));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = event.getServer();
        tickActiveDeliveries(server);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level().isClientSide) {
                continue;
            }
            long gameTime = player.serverLevel().getGameTime();
            if (gameTime % SCAN_INTERVAL_TICKS != 0L) {
                continue;
            }
            tickNearbyGiftMaids(player);
        }
    }

    private static void tickNearbyGiftMaids(ServerPlayer player) {
        if (!ModConfig.BOND_RANDOM_GIFT_ENABLED.get()) {
            return;
        }
        if (LapPillowState.isActive(player)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        double searchRange = ModConfig.BOND_RANDOM_GIFT_DELIVERY_SEARCH_RANGE.get();
        List<EntityMaid> nearbyMaids = level.getEntitiesOfClass(
                EntityMaid.class,
                player.getBoundingBox().inflate(searchRange),
                maid -> maid.isAlive() && maid.isOwnedBy(player)
        );

        for (EntityMaid maid : nearbyMaids) {
            if (!BondManager.isAbilityUnlocked(player, maid.getUUID(), "random_gift")) {
                continue;
            }
            int queuedGiftCount = reconcileQueuedGifts(player, maid);
            if (BondData.of(player).getRandomGiftQueue(maid.getUUID()).prepared().isEmpty()) continue;
            if (queuedGiftCount <= 0 || DELIVERY_TASKS.containsKey(maid.getUUID())) {
                continue;
            }
            long lastDelivery = BondManager.getLastGiftDeliveryGameTime(player, maid.getUUID());
            if (level.getGameTime() - lastDelivery < ModConfig.BOND_RANDOM_GIFT_DELIVERY_COOLDOWN_TICKS.get()) {
                continue;
            }
            DELIVERY_TASKS.put(maid.getUUID(), new PendingDeliveryTask(
                    level.dimension(),
                    player.getUUID(),
                    maid.getUUID(),
                    level.getGameTime(),
                    level.getGameTime() + ModConfig.BOND_RANDOM_GIFT_PATHFIND_TIMEOUT_TICKS.get()
            ));
        }
    }

    private static void tickActiveDeliveries(MinecraftServer server) {
        if (!ModConfig.BOND_RANDOM_GIFT_ENABLED.get()) {
            DELIVERY_TASKS.clear();
            return;
        }
        Iterator<PendingDeliveryTask> iterator = DELIVERY_TASKS.values().iterator();
        while (iterator.hasNext()) {
            PendingDeliveryTask task = iterator.next();
            ServerLevel level = server.getLevel(task.dimension());
            if (level == null) {
                iterator.remove();
                continue;
            }
            ServerPlayer player = server.getPlayerList().getPlayer(task.playerUuid());
            if (player == null || player.level() != level || !player.isAlive()) {
                iterator.remove();
                continue;
            }
            Entity entity = level.getEntity(task.maidUuid());
            if (!(entity instanceof EntityMaid maid) || !maid.isAlive() || !maid.isOwnedBy(player)) {
                iterator.remove();
                continue;
            }
            if (LapPillowState.isActive(player)) {
                iterator.remove();
                continue;
            }
            if (!BondManager.isAbilityUnlocked(player, maid.getUUID(), "random_gift")) {
                iterator.remove();
                continue;
            }
            int queuedGiftCount = BondManager.getQueuedGiftCount(player, maid.getUUID());
            if (queuedGiftCount <= 0 || deliveryCooldownSeconds(BondData.of(player), maid.getUUID(), level.getGameTime()) > 0) {
                iterator.remove();
                continue;
            }

            double searchRange = ModConfig.BOND_RANDOM_GIFT_DELIVERY_SEARCH_RANGE.get();
            if (player.distanceToSqr(maid) > searchRange * searchRange) {
                iterator.remove();
                continue;
            }

            maid.getNavigation().moveTo(player, 1.0D);
            maid.getLookControl().setLookAt(player, 30.0F, 30.0F);
            double reach = Math.max(ModConfig.BOND_RANDOM_GIFT_DELIVERY_REACH_DISTANCE.get(), 2.8D);
            long gameTime = level.getGameTime();
            if (shouldDeliverNow(maid, player, task, searchRange, reach, gameTime)) {
                maid.getNavigation().stop();
                deliverOneGift(player, maid);
                iterator.remove();
                continue;
            }
            if (gameTime > task.timeoutTick()) {
                iterator.remove();
            }
        }
    }

    private static void deliverOneGift(ServerPlayer player, EntityMaid maid) {
        if (!ModConfig.BOND_RANDOM_GIFT_ENABLED.get()) return;
        BondData data = BondData.of(player);
        RandomGiftQueue queue = data.getRandomGiftQueue(maid.getUUID());
        if (queue.prepared().isEmpty()) return;
        String itemId = queue.prepared().get(0);
        if (!isPreparedGiftAllowed(itemId)) return;
        ItemStack gift = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(itemId)), 1);
        if (!throwGiftTowardPlayer(player, maid, gift)) return;
        data.recordGiftDelivery(maid.getUUID(), queue.consumeFirst(), player.serverLevel().getGameTime(), System.currentTimeMillis(), itemId);
        maid.spawnItemParticles(gift, 5);
        YSMActionBridge.playIfAvailable(maid, YSMMaidAnimation.RANDOM_GIFT);


        if (ModConfig.BOND_RANDOM_GIFT_SHOW_ACTION_BAR.get()) {
            player.displayClientMessage(Component.translatable(
                    "bond.random_gift.received",
                    MaidDisplayNameResolver.resolveDisplayName(maid),
                    gift.getHoverName(),
                    gift.getCount()
            ), true);
        }
        sendStateSync(player, maid);
    }

    private static RandomGiftQueue prepareGifts(ServerPlayer player, EntityMaid maid) {
        BondData data = BondData.of(player);
        RandomGiftQueue original = data.getRandomGiftQueue(maid.getUUID());
        RandomGiftQueue queue = original.retain(RandomGiftService::isPreparedGiftAllowed);
        if (queue.prepared().size() < queue.queued()) {
            // One registry pass and one fresh mod sample per batch, not per earned slot.
            List<Item> candidates = collectGiftCandidates(maid.getRandom());
            if (!candidates.isEmpty()) {
                List<String> prepared = new ArrayList<>(queue.prepared());
                while (prepared.size() < queue.queued()) {
                    Item item = candidates.get(maid.getRandom().nextInt(candidates.size()));
                    prepared.add(BuiltInRegistries.ITEM.getKey(item).toString());
                }
                queue = new RandomGiftQueue(queue.queued(), prepared);
            }
        }
        if (!queue.equals(original)) data.setRandomGiftQueue(maid.getUUID(), queue);
        return queue;
    }

    private static boolean isPreparedGiftAllowed(String itemId) {
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) return false;
        Item item = BuiltInRegistries.ITEM.get(id);
        return isValidGiftCandidate(item) && RandomGiftPolicy.allows(itemId,
                item.builtInRegistryHolder().is(GIFT_POOL_TAG), item.builtInRegistryHolder().is(GIFT_BLACKLIST_TAG),
                ModConfig.BOND_RANDOM_GIFT_CURATED_POOL_ONLY.get(), ModConfig.BOND_RANDOM_GIFT_INCLUDE_MOD_ITEMS.get(),
                ModConfig.BOND_RANDOM_GIFT_AUTO_MOD_SAMPLE_SIZE.get());
    }

    /** A null RNG counts the effective pool without consuming randomness or preparing gifts. */
    private static List<Item> collectGiftCandidates(RandomSource random) {
        Set<Item> candidates = new LinkedHashSet<>();
        Map<String, List<Item>> modItems = new HashMap<>();
        boolean curatedOnly = ModConfig.BOND_RANDOM_GIFT_CURATED_POOL_ONLY.get();
        boolean includeMods = ModConfig.BOND_RANDOM_GIFT_INCLUDE_MOD_ITEMS.get();
        int sampleSize = Math.max(0, ModConfig.BOND_RANDOM_GIFT_AUTO_MOD_SAMPLE_SIZE.get());
        if (curatedOnly) {
            for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(GIFT_POOL_TAG)) {
                Item item = holder.value();
                if (isValidGiftCandidate(item) && !holder.is(GIFT_BLACKLIST_TAG)) candidates.add(item);
            }
        } else {
            for (Item item : BuiltInRegistries.ITEM) {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                boolean explicit = item.builtInRegistryHolder().is(GIFT_POOL_TAG);
                if (!RandomGiftPolicy.allows(id.toString(), explicit,
                        item.builtInRegistryHolder().is(GIFT_BLACKLIST_TAG), false, includeMods, sampleSize)
                        || !isValidGiftCandidate(item)) continue;
                if (explicit || ResourceLocation.DEFAULT_NAMESPACE.equals(id.getNamespace())) {
                    candidates.add(item);
                } else {
                    modItems.computeIfAbsent(id.getNamespace(), ignored -> new ArrayList<>()).add(item);
                }
            }
            RandomGiftPolicy.addSampled(modItems, candidates, sampleSize, random == null ? bound -> bound - 1 : random::nextInt);
        }
        return new ArrayList<>(candidates);
    }

    private static boolean isValidGiftCandidate(Item item) {
        if (item == net.minecraft.world.item.Items.AIR) return false;
        return item.canFitInsideContainerItems() && !item.getDefaultInstance().isEmpty();
    }

    private static boolean throwGiftTowardPlayer(ServerPlayer player, EntityMaid maid, ItemStack stack) {
        ItemEntity entity = new ItemEntity(maid.level(), maid.getX(), maid.getEyeY() - 0.3D, maid.getZ(), stack);
        Vec3 target = new Vec3(player.getX(), player.getEyeY() - 0.2D, player.getZ());
        entity.setDeltaMovement(target.subtract(maid.position()).normalize().scale(0.3D));
        entity.setDefaultPickUpDelay();
        return maid.level().addFreshEntity(entity);
    }

    private static boolean isReadyToThrowGift(EntityMaid maid, ServerPlayer player, double reach) {
        double dx = maid.getX() - player.getX();
        double dz = maid.getZ() - player.getZ();
        double horizontalDistSqr = dx * dx + dz * dz;
        double verticalDist = Math.abs(maid.getY() - player.getY());
        return horizontalDistSqr <= reach * reach && verticalDist <= 2.5D;
    }

    private static boolean shouldDeliverNow(EntityMaid maid, ServerPlayer player, PendingDeliveryTask task, double searchRange, double reach, long gameTime) {
        if (isReadyToThrowGift(maid, player, reach)) {
            return true;
        }

        double relaxedReach = Math.max(reach + 1.75D, 4.0D);
        if (gameTime - task.startTick() >= 10L && isReadyToThrowGift(maid, player, relaxedReach)) {
            return true;
        }

        double timeoutReach = Math.min(searchRange, 6.0D);
        if (gameTime + 10L >= task.timeoutTick() && isReadyToThrowGift(maid, player, timeoutReach)) {
            return true;
        }

        double navigationDoneReach = Math.max(reach + 1.0D, 3.5D);
        return maid.getNavigation().isDone() && isReadyToThrowGift(maid, player, navigationDoneReach);
    }

    private static void sendStateSync(ServerPlayer player, EntityMaid maid) {
        BondSyncHelper.sendBondState(player, maid);
    }

    private record PendingDeliveryTask(ResourceKey<Level> dimension, UUID playerUuid, UUID maidUuid, long startTick, long timeoutTick) {
    }
}
