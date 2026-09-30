package com.github.touhoumaidaffection.bond;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class RandomGiftPersistenceTest {
    private static final UUID MAID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID IMPORTED = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void preparedIdentityAndUnresolvedLegacySlotsSurviveReloadAndMaidTransfer() {
        CompoundTag root = new CompoundTag();
        BondData data = BondData.forTest(root);
        data.setQueuedGiftCount(MAID, 3);
        assertEquals(new RandomGiftQueue(3, List.of()), data.getRandomGiftQueue(MAID));
        data.setRandomGiftQueue(MAID, new RandomGiftQueue(3, List.of("minecraft:apple", "example:tea")));
        BondData restored = BondData.forTest(root.copy());
        assertEquals(data.getRandomGiftQueue(MAID), restored.getRandomGiftQueue(MAID));
        CompoundTag exported = restored.exportMaidData(MAID);
        BondData imported = BondData.forTest(new CompoundTag());
        imported.importMaidData(IMPORTED, exported);
        assertEquals(new RandomGiftQueue(3, List.of("minecraft:apple", "example:tea")), imported.getRandomGiftQueue(IMPORTED));
        imported.recordGiftDelivery(IMPORTED, imported.getRandomGiftQueue(IMPORTED).consumeFirst(), 90L, 100_000L, "minecraft:apple");
        assertEquals(new RandomGiftQueue(2, List.of("example:tea")), imported.getRandomGiftQueue(IMPORTED));
        assertEquals(100_000L, imported.getLastGiftDeliveryWallClockMs(IMPORTED));
        assertEquals("minecraft:apple", imported.getLastDeliveredGiftId(IMPORTED));
        assertEquals(3, restored.getQueuedGiftCount(MAID));
        CompoundTag afterDelivery = imported.exportMaidData(IMPORTED);
        assertFalse(afterDelivery.contains(BondKeys.RANDOM_GIFT_LAST_DELIVERY_WALL_CLOCK));
        assertFalse(afterDelivery.contains(BondKeys.RANDOM_GIFT_LAST_ITEM));
        assertEquals(2, afterDelivery.getInt(BondKeys.RANDOM_GIFT_QUEUE));
    }

    @Test
    void importingLegacyCounterReplacesOldPreparedContentsAndHistory() {
        BondData data = BondData.forTest(new CompoundTag());
        data.recordGiftDelivery(MAID, new RandomGiftQueue(2, List.of("minecraft:apple", "minecraft:bread")), 20L, 100_000L, "minecraft:carrot");
        CompoundTag legacy = new CompoundTag();
        legacy.putInt(BondKeys.RANDOM_GIFT_QUEUE, 4);
        data.importMaidData(MAID, legacy);
        assertEquals(new RandomGiftQueue(4, List.of()), data.getRandomGiftQueue(MAID));
        assertEquals(0L, data.getLastGiftDeliveryWallClockMs(MAID));
        assertEquals("", data.getLastDeliveredGiftId(MAID));
    }
}
