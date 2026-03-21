package com.github.touhoumaidaffection.bond.service;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.touhoumaidaffection.ModConfig;
import com.github.touhoumaidaffection.TouhouMaidAffection;
import com.github.touhoumaidaffection.bond.BondManager;
import com.github.touhoumaidaffection.network.BondStateSyncPayload;
import com.github.touhoumaidaffection.ysm.YSMActionBridge;
import com.github.touhoumaidaffection.ysm.YSMMaidAnimation;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

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
            ResourceLocation.fromNamespaceAndPath(TouhouMaidAffection.MOD_ID, "bond_random_gift_pool")
    );
    private static final TagKey<Item> GIFT_BLACKLIST_TAG = TagKey.create(
            net.minecraft.core.registries.Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(TouhouMaidAffection.MOD_ID, "bond_random_gift_blacklist")
    );
    private static final Set<ResourceLocation> VANILLA_EXCLUDED_ITEMS = Set.of(
            ResourceLocation.withDefaultNamespace("air"),
            ResourceLocation.withDefaultNamespace("barrier"),
            ResourceLocation.withDefaultNamespace("command_block"),
            ResourceLocation.withDefaultNamespace("chain_command_block"),
            ResourceLocation.withDefaultNamespace("repeating_command_block"),
            ResourceLocation.withDefaultNamespace("command_block_minecart"),
            ResourceLocation.withDefaultNamespace("structure_block"),
            ResourceLocation.withDefaultNamespace("structure_void"),
            ResourceLocation.withDefaultNamespace("jigsaw"),
            ResourceLocation.withDefaultNamespace("light"),
            ResourceLocation.withDefaultNamespace("debug_stick"),
            ResourceLocation.withDefaultNamespace("knowledge_book")
    );
    private static final Map<UUID, PendingDeliveryTask> DELIVERY_TASKS = new HashMap<>();
    private static final int SCAN_INTERVAL_TICKS = 20;

    private RandomGiftService() {
    }

    public static int reconcileQueuedGifts(ServerPlayer player, EntityMaid maid) {
        return BondManager.reconcileRandomGiftQueue(player, maid.getUUID(), System.currentTimeMillis());
    }

    public static long getNextGiftReadyAtMs(ServerPlayer player, EntityMaid maid) {
        return BondManager.getNextRandomGiftReadyAtMs(player, maid.getUUID(), System.currentTimeMillis());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
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
            if (!BondManager.isAbilityUnlocked(player, maid.getUUID(), "random_gift")) {
                iterator.remove();
                continue;
            }
            int queuedGiftCount = reconcileQueuedGifts(player, maid);
            if (queuedGiftCount <= 0) {
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
        ItemStack gift = rollGiftStack(player.serverLevel(), maid);
        if (gift.isEmpty()) {
            return;
        }

        throwGiftTowardPlayer(player, maid, gift);
        maid.spawnItemParticles(gift, 5);
        YSMActionBridge.playIfAvailable(maid, YSMMaidAnimation.RANDOM_GIFT);

        int queuedAfter = Math.max(0, BondManager.getQueuedGiftCount(player, maid.getUUID()) - 1);
        BondManager.setQueuedGiftCount(player, maid.getUUID(), queuedAfter);
        BondManager.setLastGiftDeliveryGameTime(player, maid.getUUID(), player.serverLevel().getGameTime());

        if (ModConfig.BOND_RANDOM_GIFT_SHOW_ACTION_BAR.get()) {
            player.displayClientMessage(Component.translatable(
                    "bond.random_gift.received",
                    maid.getName(),
                    gift.getHoverName(),
                    gift.getCount()
            ), true);
        }
        sendStateSync(player, maid);
    }

    private static ItemStack rollGiftStack(ServerLevel level, EntityMaid maid) {
        List<Item> candidates = collectGiftCandidates(level);
        if (candidates.isEmpty()) {
            TouhouMaidAffection.LOGGER.warn("Random gift pool tag is empty at runtime, falling back to apple gift.");
            return new ItemStack(Items.APPLE);
        }
        RandomSource random = maid.getRandom();
        Item item = candidates.get(random.nextInt(candidates.size()));
        return new ItemStack(item, 1);
    }

    private static List<Item> collectGiftCandidates(ServerLevel level) {
        Set<Item> candidates = new LinkedHashSet<>();

        for (Item item : BuiltInRegistries.ITEM) {
            if (isDefaultVanillaGiftCandidate(item)) {
                candidates.add(item);
            }
        }

        if (ModConfig.BOND_RANDOM_GIFT_INCLUDE_MOD_ITEMS.get()) {
            addSampledModItems(level, candidates);
        }

        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(GIFT_POOL_TAG)) {
            Item item = holder.value();
            if (isValidGiftCandidate(item)) {
                candidates.add(item);
            }
        }

        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(GIFT_BLACKLIST_TAG)) {
            candidates.remove(holder.value());
        }

        return new ArrayList<>(candidates);
    }

    private static boolean isDefaultVanillaGiftCandidate(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (!ResourceLocation.DEFAULT_NAMESPACE.equals(id.getNamespace())) {
            return false;
        }
        if (VANILLA_EXCLUDED_ITEMS.contains(id)) {
            return false;
        }
        return isValidGiftCandidate(item);
    }

    private static boolean isValidGiftCandidate(Item item) {
        if (item == Items.AIR) {
            return false;
        }
        if (!item.canFitInsideContainerItems()) {
            return false;
        }
        ItemStack stack = item.getDefaultInstance();
        return !stack.isEmpty();
    }

    private static void addSampledModItems(ServerLevel level, Set<Item> candidates) {
        int sampleSize = Math.max(0, ModConfig.BOND_RANDOM_GIFT_AUTO_MOD_SAMPLE_SIZE.get());
        if (sampleSize <= 0) {
            return;
        }

        Map<String, List<Item>> byNamespace = new HashMap<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (ResourceLocation.DEFAULT_NAMESPACE.equals(id.getNamespace())) {
                continue;
            }
            if (item.builtInRegistryHolder().is(GIFT_BLACKLIST_TAG) || !isValidGiftCandidate(item)) {
                continue;
            }
            byNamespace.computeIfAbsent(id.getNamespace(), ignored -> new ArrayList<>()).add(item);
        }

        if (byNamespace.isEmpty()) {
            return;
        }

        long baseSeed = level.getSeed() ^ 0x5EEDC0DEL;
        List<String> namespaces = new ArrayList<>(byNamespace.keySet());
        namespaces.sort(Comparator.naturalOrder());
        shuffleStrings(namespaces, RandomSource.create(baseSeed ^ 0x1234ABCDL));

        for (String namespace : namespaces) {
            List<Item> items = byNamespace.get(namespace);
            items.sort(Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).toString()));
            shuffleItems(items, RandomSource.create(baseSeed ^ namespace.hashCode()));
        }

        int added = 0;
        int round = 0;
        while (added < sampleSize) {
            boolean progressed = false;
            for (String namespace : namespaces) {
                List<Item> items = byNamespace.get(namespace);
                if (round >= items.size()) {
                    continue;
                }
                if (candidates.add(items.get(round))) {
                    added++;
                }
                progressed = true;
                if (added >= sampleSize) {
                    break;
                }
            }
            if (!progressed) {
                break;
            }
            round++;
        }
    }

    private static void shuffleItems(List<Item> items, RandomSource random) {
        for (int i = items.size() - 1; i > 0; i--) {
            int swapIndex = random.nextInt(i + 1);
            Item tmp = items.get(i);
            items.set(i, items.get(swapIndex));
            items.set(swapIndex, tmp);
        }
    }

    private static void shuffleStrings(List<String> items, RandomSource random) {
        for (int i = items.size() - 1; i > 0; i--) {
            int swapIndex = random.nextInt(i + 1);
            String tmp = items.get(i);
            items.set(i, items.get(swapIndex));
            items.set(swapIndex, tmp);
        }
    }

    private static void throwGiftTowardPlayer(ServerPlayer player, EntityMaid maid, ItemStack stack) {
        Vec3 targetPos = new Vec3(player.getX(), player.getEyeY() - 0.2D, player.getZ());
        BehaviorUtils.throwItem(maid, stack.copy(), targetPos);
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
        long nowMs = System.currentTimeMillis();
        int queuedGiftCount = BondManager.isAbilityUnlocked(player, maid.getUUID(), "random_gift")
                ? BondManager.reconcileRandomGiftQueue(player, maid.getUUID(), nowMs)
                : 0;
        long nextGiftReadyAtMs = BondManager.isAbilityUnlocked(player, maid.getUUID(), "random_gift")
                ? BondManager.getNextRandomGiftReadyAtMs(player, maid.getUUID(), nowMs)
                : 0L;
        int nextGiftReadySeconds = nextGiftReadyAtMs > nowMs
                ? (int) Math.min(Integer.MAX_VALUE, (nextGiftReadyAtMs - nowMs + 999L) / 1000L)
                : 0;
        PacketDistributor.sendToPlayer(player, new BondStateSyncPayload(
                maid.getUUID(),
                BondManager.getUnlockedAbilityIds(player, maid.getUUID()),
                queuedGiftCount,
                Math.max(1, ModConfig.BOND_RANDOM_GIFT_MAX_QUEUED.get()),
                nextGiftReadySeconds
        ));
    }

    private record PendingDeliveryTask(ResourceKey<Level> dimension, UUID playerUuid, UUID maidUuid, long startTick, long timeoutTick) {
    }
}
