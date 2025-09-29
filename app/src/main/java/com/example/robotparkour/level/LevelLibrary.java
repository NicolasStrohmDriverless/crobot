package com.example.robotparkour.level;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.crobot.game.level.LevelModel;
import com.example.robotparkour.core.WorldInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates multi-floor platform layouts for every world and exposes additional entities such as
 * the expanded enemy roster.
 */
public final class LevelLibrary {

    private static final int TILE_SIZE = 32;
    private static final int FLOORS = 3;
    private static final int FLOOR_CLEARANCE = 2;
    private static final int TOP_MARGIN = 5;
    private static final int LEVEL_WIDTH = 180;

    private LevelLibrary() {
    }

    @NonNull
    public static LegacyLevelBlueprint getLevelBlueprint(@Nullable WorldInfo worldInfo) {
        int worldNumber = worldInfo != null ? worldInfo.getProgramNumber() : 1;
        switch (worldNumber) {
            case 2:
                return buildPhishingLagoon();
            case 3:
                return buildKernelCatacombs();
            case 4:
                return buildCloudCitadel();
            case 1:
            default:
                return buildRecycleBinRavine();
        }
    }

    @NonNull
    public static String[] getLevelData(@Nullable WorldInfo worldInfo) {
        return getLevelBlueprint(worldInfo).getRows();
    }

    @NonNull
    public static List<LevelModel.Entity> getAdditionalEntities(@Nullable WorldInfo worldInfo) {
        LegacyLevelBlueprint blueprint = getLevelBlueprint(worldInfo);
        List<LevelModel.Entity> entities = new ArrayList<>();
        for (EntitySpec spec : blueprint.getEntities()) {
            entities.add(spec.toLevelEntity());
        }
        return entities;
    }

    @NonNull
    public static LegacyLevelBlueprint createBlueprint(@NonNull String[] rows,
                                                       @NonNull List<EntitySpec> entities) {
        return new LegacyLevelBlueprint(rows, entities);
    }

    private static LegacyLevelBlueprint buildRecycleBinRavine() {
        LevelBuilder builder = new LevelBuilder(LEVEL_WIDTH);
        applyBaseLayout(builder, 'G', 'B', 'Q');

        builder.addSpawn(0, 2f);
        builder.addFlag(2, LEVEL_WIDTH - 4f);

        builder.addCoinCluster(0, 6f, 4);
        builder.addCoinCluster(1, 44f, 4);
        builder.addCoinCluster(2, 70f, 5);
        builder.addCoinCluster(2, 130f, 6);

        builder.addEnemyOnFloor("bugblob", 0, 12f);
        builder.addEnemyOnFloor("keylogger_beetle", 0, 22f);
        builder.addEnemyOnFloor("cookie_crumbler", 0, 36f);
        builder.addEnemyHovering("bit_bat", 1, 54f, 1.4f);
        builder.addEnemyOnFloor("wurm_weasel", 1, 64f);
        builder.addEnemyOnFloor("treiber_drone", 1, 78f);
        builder.addEnemyOnFloor("driver_module", 1, 86f);
        builder.addEnemyOnFloor("port_plant", 0, 104f);
        builder.addEnemyOnFloor("compile_crusher", 2, 114f);
        builder.addEnemyOnFloor("bsod_block", 1, 126f);
        builder.addEnemyOnFloor("patch_golem", 0, 140f);
        builder.addEnemyHovering("glitch_saw", 2, 150f, 0.6f);

        builder.addSpike(0, 58f);
        builder.addSpike(1, 92f);
        builder.addSpike(2, 120f);

        return builder.build();
    }

    private static LegacyLevelBlueprint buildPhishingLagoon() {
        LevelBuilder builder = new LevelBuilder(LEVEL_WIDTH);
        applyBaseLayout(builder, 'B', 'G', 'Q');

        builder.carveGap(0, 44, 50);
        builder.carveGap(0, 98, 106);
        builder.addFloatingPlatform(0, 46, 48, 'B');
        builder.addFloatingPlatform(0, 100, 103, 'B');

        builder.addSpawn(0, 3f);
        builder.addFlag(2, LEVEL_WIDTH - 6f);

        builder.addCoinCluster(0, 18f, 5);
        builder.addCoinCluster(1, 62f, 4);
        builder.addCoinCluster(2, 112f, 5);

        builder.addEnemyOnFloor("phish_carp", 0, 48f);
        builder.addEnemyOnFloor("phish_carp", 0, 102f);
        builder.addEnemyHovering("spam_drone", 1, 58f, 1.8f);
        builder.addEnemyHovering("spam_drone", 2, 132f, 1.2f);
        builder.addEnemyHovering("cloud_leech", 2, 84f, 2.0f);
        builder.addEnemyHovering("cloud_leech", 1, 120f, 1.6f);
        builder.addEnemyOnFloor("adware_balloon", 1, 70f);
        builder.addEnemyOnFloor("adware_balloon", 1, 74f);
        builder.addEnemyWithExtras("botnet_bee_leader", 1, 90f, 0.8f,
                Collections.singletonMap("swarm", "alpha"));
        builder.addEnemyWithExtras("botnet_bee_minion", 1, 94f, 0.8f,
                Collections.singletonMap("leader", "alpha"));
        builder.addEnemyWithExtras("botnet_bee_minion", 1, 88f, 0.8f,
                Collections.singletonMap("leader", "alpha"));
        builder.addEnemyOnFloor("packet_hound", 0, 60f);
        builder.addEnemyOnFloor("packet_hound", 0, 116f);
        builder.addEnemyOnFloor("popup_piranha", 1, 138f);
        builder.addEnemyHovering("lag_bubble", 2, 144f, 0.4f);

        builder.addSpike(0, 30f);
        builder.addSpike(1, 108f);

        return builder.build();
    }

    private static LegacyLevelBlueprint buildKernelCatacombs() {
        LevelBuilder builder = new LevelBuilder(LEVEL_WIDTH);
        applyBaseLayout(builder, 'Q', 'B', 'G');

        builder.addSpawn(0, 4f);
        builder.addFlag(2, LEVEL_WIDTH - 5f);

        builder.addCoinCluster(0, 16f, 3);
        builder.addCoinCluster(1, 52f, 4);
        builder.addCoinCluster(2, 104f, 6);

        builder.addEnemyOnFloor("trojan_turret", 0, 20f);
        builder.addEnemyOnFloor("ransom_knight", 0, 28f);
        builder.addEnemyOnFloor("rootkit_raider", 0, 42f);
        builder.addEnemyOnFloor("firewall_guardian", 1, 64f);
        builder.addEnemyHovering("lag_bubble", 1, 70f, 0.2f);
        builder.addEnemyOnFloor("memory_leak_slime", 1, 82f);
        builder.addEnemyOnFloor("captcha_gargoyle", 1, 90f);
        builder.addEnemyOnFloor("patch_golem", 0, 98f);
        builder.addEnemyHovering("glitch_saw", 1, 108f, 0.6f);
        builder.addEnemyOnFloor("compile_crusher", 2, 118f);
        builder.addEnemyOnFloor("bsod_block", 2, 126f);
        builder.addEnemyOnFloor("port_plant", 1, 134f);
        builder.addEnemyOnFloor("packet_hound", 0, 140f);
        builder.addEnemyHovering("cloud_leech", 2, 146f, 1.2f);

        builder.addSpike(0, 56f);
        builder.addSpike(1, 96f);
        builder.addSpike(2, 138f);

        builder.addFloatingColumn(68, builder.getFloorRow(0) - 1, 3, 'Q');
        builder.addFloatingColumn(110, builder.getFloorRow(1) - 2, 2, 'B');

        return builder.build();
    }

    private static LegacyLevelBlueprint buildCloudCitadel() {
        LevelBuilder builder = new LevelBuilder(LEVEL_WIDTH);
        applyBaseLayout(builder, 'B', 'Q', 'G');

        builder.carveGap(2, 60, 66);
        builder.carveGap(2, 140, 146);
        builder.addFloatingPlatform(2, 60, 64, 'G');
        builder.addFloatingPlatform(2, 140, 144, 'G');

        builder.addSpawn(0, 5f);
        builder.addFlag(2, LEVEL_WIDTH - 8f);

        builder.addCoinCluster(0, 18f, 4);
        builder.addCoinCluster(1, 76f, 5);
        builder.addCoinCluster(2, 124f, 6);

        builder.addEnemyHovering("cloud_leech", 2, 52f, 1.4f);
        builder.addEnemyHovering("cloud_leech", 1, 68f, 1.2f);
        builder.addEnemyOnFloor("captcha_gargoyle", 1, 74f);
        builder.addEnemyWithExtras("botnet_bee_leader", 2, 82f, 0.9f,
                Collections.singletonMap("swarm", "omega"));
        builder.addEnemyWithExtras("botnet_bee_minion", 2, 78f, 0.9f,
                Collections.singletonMap("leader", "omega"));
        builder.addEnemyWithExtras("botnet_bee_minion", 2, 86f, 0.9f,
                Collections.singletonMap("leader", "omega"));
        builder.addEnemyOnFloor("garbage_collector", 0, 92f);
        builder.addEnemyHovering("kernel_kobold", 2, 102f, 2.2f);
        builder.addEnemyHovering("vpn_vampire", 1, 110f, 1.6f);
        builder.addEnemyOnFloor("update_ogre", 1, 118f);
        Map<String, String> jumpSync = new HashMap<>();
        jumpSync.put("channel", "gate");
        jumpSync.put("trigger", "jump");
        builder.addEnemyWithExtras("twofa_guardian_jump", 2, 126f, 1f, jumpSync);
        Map<String, String> crouchSync = new HashMap<>();
        crouchSync.put("channel", "gate");
        crouchSync.put("trigger", "duck");
        builder.addEnemyWithExtras("twofa_guardian_dash", 2, 130f, 1f, crouchSync);
        builder.addEnemyOnFloor("checksum_crab", 2, 136f);
        builder.addEnemyHovering("phishing_siren", 2, 144f, 1.4f);

        builder.addSpike(0, 40f);
        builder.addSpike(1, 94f);
        builder.addSpike(2, 152f);

        return builder.build();
    }

    private static void applyBaseLayout(@NonNull LevelBuilder builder,
                                        char lowerTile,
                                        char middleTile,
                                        char upperTile) {
        builder.fillFloorSegment(0, 0, 56, lowerTile);
        builder.fillFloorSegment(0, 60, 120, lowerTile);
        builder.fillFloorSegment(0, 124, builder.getWidth(), lowerTile);

        builder.fillFloorSegment(1, 0, 38, middleTile);
        builder.fillFloorSegment(1, 42, 90, middleTile);
        builder.fillFloorSegment(1, 96, builder.getWidth(), middleTile);

        builder.fillFloorSegment(2, 0, 48, upperTile);
        builder.fillFloorSegment(2, 54, 110, upperTile);
        builder.fillFloorSegment(2, 116, builder.getWidth(), upperTile);

        builder.addStaircase(0, 48, 4, lowerTile);
        builder.carveGap(1, 48, 52);

        builder.addStaircase(1, 84, 4, middleTile);
        builder.carveGap(2, 84, 88);

        builder.addStaircase(0, 118, 4, lowerTile);
        builder.carveGap(1, 118, 122);

        builder.addStaircase(1, 134, 4, middleTile);
        builder.carveGap(2, 134, 138);

        builder.addFloatingColumn(32, builder.getFloorRow(0) - 1, 2, lowerTile);
        builder.addFloatingColumn(68, builder.getFloorRow(1) - 2, 2, middleTile);
        builder.addFloatingColumn(142, builder.getFloorRow(2) - 2, 2, upperTile);
    }

    public static final class LegacyLevelBlueprint {
        private final String[] rows;
        private final List<EntitySpec> entities;

        LegacyLevelBlueprint(@NonNull String[] rows, @NonNull List<EntitySpec> entities) {
            this.rows = rows.clone();
            this.entities = Collections.unmodifiableList(new ArrayList<>(entities));
        }

        @NonNull
        public String[] getRows() {
            return rows.clone();
        }

        @NonNull
        public List<EntitySpec> getEntities() {
            return entities;
        }
    }

    public static final class EntitySpec {
        private final String type;
        private final float tileX;
        private final float tileY;
        private final float offsetX;
        private final float offsetY;
        private final Map<String, String> extras;

        EntitySpec(@NonNull String type,
                   float tileX,
                   float tileY,
                   float offsetX,
                   float offsetY,
                   @Nullable Map<String, String> extras) {
            this.type = type;
            this.tileX = tileX;
            this.tileY = tileY;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            if (extras == null || extras.isEmpty()) {
                this.extras = Collections.emptyMap();
            } else {
                this.extras = Collections.unmodifiableMap(new HashMap<>(extras));
            }
        }

        public LevelModel.Entity toLevelEntity() {
            int pixelX = Math.round((tileX + offsetX) * TILE_SIZE);
            int pixelY = Math.round((tileY + offsetY) * TILE_SIZE);
            return new LevelModel.Entity(type, pixelX, pixelY, extras);
        }
    }

    private static final class LevelBuilder {
        private final int width;
        private final int height;
        private final int floorSpacing;
        private final int[] floorRows;
        private final char[][] tiles;
        private final List<EntitySpec> entities = new ArrayList<>();

        LevelBuilder(int width) {
            this.width = width;
            this.floorSpacing = FLOOR_CLEARANCE + 1;
            this.height = TOP_MARGIN + FLOORS * floorSpacing + 1;
            this.floorRows = new int[FLOORS];
            int baseRow = height - 1;
            for (int i = 0; i < FLOORS; i++) {
                floorRows[i] = baseRow - i * floorSpacing;
            }
            this.tiles = new char[height][width];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    tiles[y][x] = '.';
                }
            }
        }

        int getWidth() {
            return width;
        }

        int getFloorRow(int floorIndex) {
            return floorRows[Math.max(0, Math.min(floorIndex, floorRows.length - 1))];
        }

        void fillFloorSegment(int floorIndex, int startX, int endX, char tile) {
            int row = floorRows[Math.max(0, Math.min(floorIndex, floorRows.length - 1))];
            int safeStart = Math.max(0, Math.min(startX, width));
            int safeEnd = Math.max(safeStart, Math.min(endX, width));
            for (int x = safeStart; x < safeEnd; x++) {
                tiles[row][x] = tile;
            }
        }

        void carveGap(int floorIndex, int startX, int endX) {
            int row = floorRows[Math.max(0, Math.min(floorIndex, floorRows.length - 1))];
            int safeStart = Math.max(0, Math.min(startX, width));
            int safeEnd = Math.max(safeStart, Math.min(endX, width));
            for (int x = safeStart; x < safeEnd; x++) {
                tiles[row][x] = '.';
            }
        }

        void addStaircase(int floorIndex, int startX, int steps, char tile) {
            int baseRow = floorRows[Math.max(0, Math.min(floorIndex, floorRows.length - 1))];
            int safeStart = Math.max(0, Math.min(startX, width - 1));
            for (int i = 0; i < steps; i++) {
                int x = safeStart + i;
                if (x >= width) {
                    break;
                }
                for (int h = 0; h <= i && h <= floorSpacing; h++) {
                    int y = baseRow - h;
                    if (y >= 0 && y < height) {
                        tiles[y][x] = tile;
                    }
                }
            }
        }

        void addFloatingPlatform(int floorIndex, int startX, int endX, char tile) {
            int baseRow = floorRows[Math.max(0, Math.min(floorIndex, floorRows.length - 1))] - (FLOOR_CLEARANCE + 1);
            int safeStart = Math.max(0, Math.min(startX, width));
            int safeEnd = Math.max(safeStart, Math.min(endX, width));
            for (int x = safeStart; x < safeEnd; x++) {
                if (baseRow >= 0 && baseRow < height) {
                    tiles[baseRow][x] = tile;
                }
            }
        }

        void addFloatingColumn(int x, int startRow, int heightUnits, char tile) {
            int safeX = Math.max(0, Math.min(x, width - 1));
            int top = Math.max(0, startRow - heightUnits + 1);
            for (int y = startRow; y >= top; y--) {
                if (y >= 0 && y < this.height) {
                    tiles[y][safeX] = tile;
                }
            }
        }

        void addCoinCluster(int floorIndex, float startTileX, int count) {
            for (int i = 0; i < count; i++) {
                addCoin(floorRows[floorIndex] - 1.6f, startTileX + i * 2f);
            }
        }

        void addCoin(float tileY, float tileX) {
            entities.add(new EntitySpec("coin", tileX, tileY, 0.5f, 0.4f, null));
        }

        void addSpawn(int floorIndex, float tileX) {
            Map<String, String> extras = Collections.singletonMap("spawn", "true");
            entities.add(new EntitySpec("spawn", tileX, floorRows[floorIndex], 0.5f, 1f, extras));
        }

        void addFlag(int floorIndex, float tileX) {
            entities.add(new EntitySpec("flag", tileX, floorRows[floorIndex], 0.5f, 0f, null));
        }

        void addSpike(int floorIndex, float tileX) {
            entities.add(new EntitySpec("spike", tileX, floorRows[floorIndex], 0.5f, 1f, null));
        }

        void addEnemyOnFloor(String type, int floorIndex, float tileX) {
            entities.add(new EntitySpec(type, tileX, floorRows[floorIndex], 0.5f, 1f, null));
        }

        void addEnemyHovering(String type, int floorIndex, float tileX, float verticalOffset) {
            float tileY = floorRows[floorIndex] - verticalOffset;
            entities.add(new EntitySpec(type, tileX, tileY, 0.5f, 0.6f, null));
        }

        void addEnemyWithExtras(String type, int floorIndex, float tileX, float bottomOffset,
                                 @Nullable Map<String, String> extras) {
            float tileY = floorRows[floorIndex];
            entities.add(new EntitySpec(type, tileX, tileY, 0.5f, bottomOffset, extras));
        }

        void addEnemyWithExtras(String type, float tileX, float tileY, float bottomOffset,
                                 @Nullable Map<String, String> extras) {
            entities.add(new EntitySpec(type, tileX, tileY, 0.5f, bottomOffset, extras));
        }

        LegacyLevelBlueprint build() {
            String[] rows = new String[height];
            for (int y = 0; y < height; y++) {
                rows[y] = new String(tiles[y]);
            }
            return new LegacyLevelBlueprint(rows, entities);
        }
    }
}

