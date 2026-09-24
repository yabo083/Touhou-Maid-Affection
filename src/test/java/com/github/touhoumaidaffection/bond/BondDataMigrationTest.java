package com.github.touhoumaidaffection.bond;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BondDataMigrationTest {

    private static final UUID MAID_A = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID MAID_B = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Test
    void migratesFlatKeysIntoMaidSubtrees() {
        MapSink sink = new MapSink();
        sink.root.put(BondKeys.flatKey(BondKeys.BOND_LEVEL, MAID_A), 5);
        sink.root.put(BondKeys.flatKey(BondKeys.MAID_MODEL, MAID_A), "tlm:reimu");
        sink.root.put(BondKeys.flatKey(BondKeys.LAP_PILLOW_MODE, MAID_A), "maid_sit_player_lie");
        sink.root.put(BondKeys.flatKey(BondKeys.BOND_LEVEL, MAID_B), 1);

        BondDataMigration.Result result = BondDataMigration.migrate(sink);

        assertEquals(4, result.migrated());
        assertEquals(0, result.retained());
        assertTrue(sink.root.isEmpty());
        assertEquals(5, sink.maids.get(MAID_A).get(BondKeys.BOND_LEVEL));
        assertEquals("tlm:reimu", sink.maids.get(MAID_A).get(BondKeys.MAID_MODEL));
        assertEquals("maid_sit_player_lie", sink.maids.get(MAID_A).get("BondMaidLapPillow_Mode"));
        assertEquals(1, sink.maids.get(MAID_B).get(BondKeys.BOND_LEVEL));
    }

    @Test
    void migratedShapeMatchesMaidExtrasFormat() {
        MapSink sink = new MapSink();
        sink.root.put(BondKeys.flatKey(BondKeys.BOND_LEVEL, MAID_A), 5);
        sink.root.put(BondKeys.flatKey(BondKeys.MAID_DISPLAY_NAME, MAID_A), "灵梦");

        BondDataMigration.migrate(sink);

        // .maid 附加数据对外格式 = base 名 -> 值的 compound，即 maids.<uuid> 子树本身。
        assertEquals(Set.of(BondKeys.BOND_LEVEL, BondKeys.MAID_DISPLAY_NAME), sink.maids.get(MAID_A).keySet());
    }

    @Test
    void keepsPlayerLevelKeysAndUnparseableKeys() {
        MapSink sink = new MapSink();
        sink.root.put(BondKeys.MORNING_KISS_SELECTED_WINDOW_ID, "morning");
        sink.root.put(BondKeys.MORNING_KISS_SELECTED_MAID_ID, MAID_A.toString());
        sink.root.put(BondKeys.SCHEMA_VERSION_KEY, 1);
        sink.root.put("SomeKey_notauuid", "keep");
        sink.root.put(BondKeys.flatKey(BondKeys.BOND_LEVEL, MAID_A), 5);

        BondDataMigration.Result result = BondDataMigration.migrate(sink);

        assertEquals(1, result.migrated());
        assertEquals(4, result.retained());
        assertEquals("morning", sink.root.get(BondKeys.MORNING_KISS_SELECTED_WINDOW_ID));
        assertEquals(MAID_A.toString(), sink.root.get(BondKeys.MORNING_KISS_SELECTED_MAID_ID));
        assertEquals(1, sink.root.get(BondKeys.SCHEMA_VERSION_KEY));
        assertEquals("keep", sink.root.get("SomeKey_notauuid"));
        assertFalse(sink.root.containsKey(BondKeys.flatKey(BondKeys.BOND_LEVEL, MAID_A)));
        assertEquals(5, sink.maids.get(MAID_A).get(BondKeys.BOND_LEVEL));
    }

    @Test
    void migrationIsIdempotent() {
        MapSink sink = new MapSink();
        sink.root.put(BondKeys.flatKey(BondKeys.BOND_LEVEL, MAID_A), 5);
        sink.root.put(BondKeys.MORNING_KISS_SELECTED_WINDOW_ID, "morning");

        BondDataMigration.Result first = BondDataMigration.migrate(sink);
        Map<UUID, Map<String, Object>> snapshot = sink.copyMaids();
        BondDataMigration.Result second = BondDataMigration.migrate(sink);

        assertEquals(1, first.migrated());
        assertEquals(0, second.migrated());
        assertEquals(1, second.retained());
        assertEquals(snapshot, sink.maids);
        assertEquals(5, sink.maids.get(MAID_A).get(BondKeys.BOND_LEVEL));
    }

    @Test
    void migrationOnEmptyRootIsNoOp() {
        MapSink sink = new MapSink();

        BondDataMigration.Result result = BondDataMigration.migrate(sink);

        assertEquals(0, result.migrated());
        assertEquals(0, result.retained());
        assertTrue(sink.maids.isEmpty());
    }

    @Test
    void migrationDoesNotMutateSourceValues() {
        MapSink sink = new MapSink();
        Object value = new Object();
        sink.root.put(BondKeys.flatKey(BondKeys.BOND_LEVEL, MAID_A), value);

        BondDataMigration.migrate(sink);

        assertTrue(sink.maids.get(MAID_A).containsValue(value));
    }

    @Test
    void migrationGroupsMultipleKeysPerMaid() {
        MapSink sink = new MapSink();
        sink.root.put(BondKeys.flatKey(BondKeys.BOND_LEVEL, MAID_A), 5);
        sink.root.put(BondKeys.flatKey(BondKeys.MAID_SOUND_PACK, MAID_A), "pack");
        sink.root.put(BondKeys.flatKey(BondKeys.BOND_LEVEL, MAID_B), 2);

        BondDataMigration.migrate(sink);

        assertEquals(2, sink.maids.size());
        assertEquals(Set.of(BondKeys.BOND_LEVEL, BondKeys.MAID_SOUND_PACK), sink.maids.get(MAID_A).keySet());
        assertEquals(Set.of(BondKeys.BOND_LEVEL), sink.maids.get(MAID_B).keySet());
    }

    /** 内存版 {@link BondDataMigration.Sink}，键值用 Object 代替 NBT Tag。 */
    private static final class MapSink implements BondDataMigration.Sink<Object> {
        private final Map<String, Object> root = new LinkedHashMap<>();
        private final Map<UUID, Map<String, Object>> maids = new LinkedHashMap<>();

        @Override
        public Set<String> rootKeys() {
            return new LinkedHashSet<>(root.keySet());
        }

        @Override
        public Object value(String key) {
            return root.get(key);
        }

        @Override
        public void writeMaidValue(UUID maidUuid, String baseName, Object value) {
            maids.computeIfAbsent(maidUuid, ignored -> new LinkedHashMap<>()).put(baseName, value);
        }

        @Override
        public void removeRootKey(String key) {
            root.remove(key);
        }

        private Map<UUID, Map<String, Object>> copyMaids() {
            Map<UUID, Map<String, Object>> copy = new LinkedHashMap<>();
            maids.forEach((uuid, values) -> copy.put(uuid, new LinkedHashMap<>(values)));
            return copy;
        }
    }

    @Test
    void sinkContractUsesSnapshotKeys() {
        MapSink sink = new MapSink();
        sink.root.put(BondKeys.flatKey(BondKeys.BOND_LEVEL, MAID_A), 5);
        List<String> seen = new ArrayList<>(sink.rootKeys());

        assertEquals(List.of(BondKeys.flatKey(BondKeys.BOND_LEVEL, MAID_A)), seen);
    }
}