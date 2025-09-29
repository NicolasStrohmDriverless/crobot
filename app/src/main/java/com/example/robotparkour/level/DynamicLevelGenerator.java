package com.example.robotparkour.level;

import androidx.annotation.NonNull;

import com.crobot.game.level.LevelModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Builds multi-tier labyrinth layouts derived from the first level's structure while adding
 * additional verticality, side rooms and guidance via collectibles and enemies.
 */
public final class DynamicLevelGenerator {

    private static final int TILE_SIZE = 32;
    private static final int FLOORS = 3;
    private static final int FLOOR_CLEARANCE = 2;
    private static final int TOP_MARGIN = 5;
    private static final int LEVEL_WIDTH = 180;

    private DynamicLevelGenerator() {
    }

    @NonNull
    public static LevelLibrary.LegacyLevelBlueprint buildLabyrinth(int variantIndex, @NonNull String seedName) {
        LabyrinthBuilder builder = new LabyrinthBuilder(LEVEL_WIDTH);
        char[] palette = selectPalette(variantIndex);
        PathNode[] mainPath = buildPrimaryPath(builder.getWidth(), variantIndex);
        for (int i = 0; i < mainPath.length - 1; i++) {
            connectSegments(builder, palette, mainPath[i], mainPath[i + 1]);
        }

        builder.addSpawn(mainPath[0].floor, mainPath[0].tileX + 0.5f);
        PathNode goal = mainPath[mainPath.length - 1];
        builder.addFlag(goal.floor, goal.tileX - 0.5f);

        addElevationHighlights(builder, palette, variantIndex);
        addSecretRooms(builder, palette, variantIndex, seedName);
        addGuidance(builder, variantIndex, seedName);

        return builder.build();
    }

    @NonNull
    public static LevelModel convertToModel(@NonNull LevelLibrary.LegacyLevelBlueprint blueprint) {
        String[] rows = blueprint.getRows();
        List<LevelLibrary.EntitySpec> extraEntities = blueprint.getEntities();

        int height = rows.length;
        int width = 0;
        for (String row : rows) {
            if (row != null) {
                width = Math.max(width, row.length());
            }
        }
        if (width == 0) {
            throw new IllegalArgumentException("Blueprint rows must not be empty");
        }

        int[] tileData = new int[width * height];
        List<LevelModel.Entity> entities = new ArrayList<>();

        for (int y = 0; y < height; y++) {
            String row = rows[y] == null ? "" : rows[y];
            for (int x = 0; x < width; x++) {
                char code = x < row.length() ? row.charAt(x) : '.';
                tileData[y * width + x] = gidFor(code);
                maybeAddEntity(entities, code, x, y);
            }
        }

        if (!extraEntities.isEmpty()) {
            for (LevelLibrary.EntitySpec spec : extraEntities) {
                if (spec != null) {
                    entities.add(spec.toLevelEntity());
                }
            }
        }

        int[] collisionFlags = new int[4];
        collisionFlags[gidFor('G')] = 0x1;
        collisionFlags[gidFor('B')] = 0x1;
        collisionFlags[gidFor('Q')] = 0x1;

        LevelModel.TileLayer layer = new LevelModel.TileLayer("ground", width, height, tileData);
        LevelModel.CollisionMap collisionMap = new LevelModel.CollisionMap(collisionFlags);
        return new LevelModel(width, height, TILE_SIZE, TILE_SIZE, layer, entities, collisionMap, "");
    }

    private static char[] selectPalette(int variantIndex) {
        switch (variantIndex % 4) {
            case 1:
                return new char[]{'B', 'G', 'Q'};
            case 2:
                return new char[]{'Q', 'B', 'G'};
            case 3:
                return new char[]{'B', 'Q', 'G'};
            case 0:
            default:
                return new char[]{'G', 'B', 'Q'};
        }
    }

    private static PathNode[] buildPrimaryPath(int width, int variantIndex) {
        int safeWidth = Math.max(width, 120);
        int tail = safeWidth - 8;
        switch (variantIndex % 3) {
            case 1:
                return new PathNode[]{
                        new PathNode(0, 4),
                        new PathNode(0, 32),
                        new PathNode(1, 54),
                        new PathNode(2, 76),
                        new PathNode(2, 106),
                        new PathNode(1, 132),
                        new PathNode(2, tail)
                };
            case 2:
                return new PathNode[]{
                        new PathNode(0, 6),
                        new PathNode(1, 28),
                        new PathNode(1, 56),
                        new PathNode(2, 84),
                        new PathNode(1, 114),
                        new PathNode(0, 138),
                        new PathNode(2, tail)
                };
            case 0:
            default:
                return new PathNode[]{
                        new PathNode(0, 4),
                        new PathNode(0, 38),
                        new PathNode(1, 60),
                        new PathNode(2, 92),
                        new PathNode(1, 120),
                        new PathNode(2, tail)
                };
        }
    }

    private static void connectSegments(@NonNull LabyrinthBuilder builder,
                                        @NonNull char[] palette,
                                        @NonNull PathNode from,
                                        @NonNull PathNode to) {
        int startX = Math.min(from.tileX, to.tileX);
        int endX = Math.max(from.tileX, to.tileX);
        if (from.floor == to.floor) {
            builder.fillFloorSegment(from.floor, startX, endX + 1, palette[from.floor]);
            return;
        }
        if (from.floor < to.floor) {
            // Walk upwards via a staircase and continue on the higher platform.
            builder.fillFloorSegment(from.floor, from.tileX - 2, from.tileX + 2, palette[from.floor]);
            builder.addStaircase(from.floor, Math.max(startX - 1, from.tileX - 1), builder.getFloorSpacing(), palette[from.floor]);
            builder.fillFloorSegment(to.floor, startX, endX + 1, palette[to.floor]);
            return;
        }
        // Moving down. Place a staircase anchored on the lower floor and provide landing space.
        int stairWidth = builder.getFloorSpacing();
        int stairStart = Math.max(to.tileX - stairWidth, startX);
        builder.fillFloorSegment(from.floor, startX, from.tileX + 2, palette[from.floor]);
        builder.addStaircase(to.floor, stairStart, stairWidth, palette[to.floor]);
        builder.fillFloorSegment(to.floor, stairStart, endX + 1, palette[to.floor]);
    }

    private static void addElevationHighlights(@NonNull LabyrinthBuilder builder,
                                               @NonNull char[] palette,
                                               int variantIndex) {
        int floorSpacing = builder.getFloorSpacing();
        // Build terraces and vertical connectors that double as observation points.
        builder.addFloatingPlatform(1, 18, 26, palette[1]);
        builder.addFloatingPlatform(2, 42, 48, palette[2]);
        builder.addFloatingColumn(70, builder.getFloorRow(1) - floorSpacing / 2, floorSpacing / 2 + 1, palette[1]);
        builder.addFloatingColumn(104, builder.getFloorRow(2) - floorSpacing / 2, floorSpacing / 2 + 1, palette[2]);
        if (variantIndex % 2 == 0) {
            builder.carveGap(0, 44, 52);
            builder.addFloatingPlatform(0, 45, 50, palette[0]);
            builder.addFloatingPlatform(1, 64, 70, palette[1]);
        } else {
            builder.carveGap(1, 82, 88);
            builder.addFloatingPlatform(1, 76, 90, palette[1]);
            builder.addFloatingPlatform(2, 120, 130, palette[2]);
        }
    }

    private static void addSecretRooms(@NonNull LabyrinthBuilder builder,
                                       @NonNull char[] palette,
                                       int variantIndex,
                                       @NonNull String seedName) {
        int branchAnchor = 32 + (Math.abs(seedName.hashCode()) % 18);
        int branchLength = 10 + (seedName.length() % 8);
        builder.fillFloorSegment(0, branchAnchor, branchAnchor + branchLength, palette[0]);
        builder.addCoinCluster(0, branchAnchor + 1.5f, Math.min(6, branchLength / 2));
        builder.addEnemyOnFloor("bugblob", 0, branchAnchor + branchLength - 2f);

        builder.addStaircase(0, branchAnchor + branchLength - 4, builder.getFloorSpacing(), palette[0]);
        builder.fillFloorSegment(1, branchAnchor + branchLength - 4, branchAnchor + branchLength + 6, palette[1]);
        builder.addCoinCluster(1, branchAnchor + branchLength + 1.5f, 4);

        if (variantIndex % 3 == 1) {
            builder.carveGap(2, 126, 132);
            builder.addFloatingPlatform(2, 124, 136, palette[2]);
            builder.addEnemyHovering("cloud_leech", 2, 130f, 1.6f);
        } else {
            builder.addFloatingColumn(146, builder.getFloorRow(1) - 1, builder.getFloorSpacing(), palette[1]);
            builder.addEnemyOnFloor("patch_golem", 1, 148f);
        }
    }

    private static void addGuidance(@NonNull LabyrinthBuilder builder,
                                    int variantIndex,
                                    @NonNull String seedName) {
        List<Float> rhythm = buildRhythm(seedName);
        int floor = 0;
        float cursor = 12f;
        for (Float beat : rhythm) {
            cursor += beat;
            builder.addCoin(floor == 0 ? builder.getFloorRow(0) - 1.6f : builder.getFloorRow(floor) - 1.2f, cursor);
            if (beat > 3f) {
                builder.addEnemyOnFloor(floor == 0 ? "keylogger_beetle" : "wurm_weasel", floor, cursor + 1.2f);
            }
            floor = (floor + 1) % FLOORS;
        }
        builder.addSpike(0, 58f);
        builder.addSpike(1, 98f);
        builder.addSpike(2, 138f);
        builder.addEnemyHovering("glitch_saw", 2, 112f, 0.7f);
        builder.addEnemyHovering("spam_drone", 1, 84f, 1.4f);
        builder.addEnemyOnFloor("compile_crusher", 2, 142f);
        builder.addEnemyOnFloor("bsod_block", 1, 150f);
    }

    @NonNull
    private static List<Float> buildRhythm(@NonNull String seedName) {
        if (seedName.isEmpty()) {
            return Collections.singletonList(8f);
        }
        List<Float> rhythm = new ArrayList<>();
        char[] chars = seedName.toLowerCase(Locale.getDefault()).toCharArray();
        for (char c : chars) {
            if (Character.isDigit(c)) {
                rhythm.add(2f + (c - '0') * 0.3f);
            } else if (c >= 'a' && c <= 'z') {
                rhythm.add(1.5f + (c - 'a') * 0.15f);
            }
        }
        if (rhythm.isEmpty()) {
            rhythm.add(6f);
        }
        return rhythm;
    }

    private static int gidFor(char code) {
        switch (code) {
            case 'G':
                return 1;
            case 'B':
                return 2;
            case 'Q':
                return 3;
            default:
                return 0;
        }
    }

    private static void maybeAddEntity(@NonNull List<LevelModel.Entity> entities, char code, int gridX, int gridY) {
        switch (code) {
            case 'C': {
                int x = Math.round((gridX + 0.5f) * TILE_SIZE);
                int y = Math.round((gridY + 0.4f) * TILE_SIZE);
                entities.add(new LevelModel.Entity("coin", x, y, null));
                break;
            }
            case 'S': {
                int x = Math.round((gridX + 0.5f) * TILE_SIZE);
                int y = Math.round((gridY + 1f) * TILE_SIZE);
                entities.add(new LevelModel.Entity("spike", x, y, null));
                break;
            }
            default:
                break;
        }
    }

    private static final class PathNode {
        final int floor;
        final int tileX;

        PathNode(int floor, int tileX) {
            this.floor = Math.max(0, Math.min(FLOORS - 1, floor));
            this.tileX = Math.max(2, tileX);
        }
    }

    private static final class LabyrinthBuilder {
        private final int width;
        private final int height;
        private final int floorSpacing;
        private final int[] floorRows;
        private final char[][] tiles;
        private final List<LevelLibrary.EntitySpec> entities = new ArrayList<>();

        LabyrinthBuilder(int width) {
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

        int getFloorSpacing() {
            return floorSpacing;
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
            int clampedSteps = Math.max(3, Math.min(steps, floorSpacing + 2));
            for (int i = 0; i < clampedSteps; i++) {
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
                addCoin(getFloorRow(floorIndex) - 1.6f, startTileX + i * 2f);
            }
        }

        void addCoin(float tileY, float tileX) {
            entities.add(new LevelLibrary.EntitySpec("coin", tileX, tileY, 0.5f, 0.4f, null));
        }

        void addSpawn(int floorIndex, float tileX) {
            Map<String, String> extras = Collections.singletonMap("spawn", "true");
            entities.add(new LevelLibrary.EntitySpec("spawn", tileX, getFloorRow(floorIndex), 0.5f, 1f, extras));
        }

        void addFlag(int floorIndex, float tileX) {
            entities.add(new LevelLibrary.EntitySpec("flag", tileX, getFloorRow(floorIndex), 0.5f, 0f, null));
        }

        void addSpike(int floorIndex, float tileX) {
            entities.add(new LevelLibrary.EntitySpec("spike", tileX, getFloorRow(floorIndex), 0.5f, 1f, null));
        }

        void addEnemyOnFloor(String type, int floorIndex, float tileX) {
            entities.add(new LevelLibrary.EntitySpec(type, tileX, getFloorRow(floorIndex), 0.5f, 1f, null));
        }

        void addEnemyHovering(String type, int floorIndex, float tileX, float verticalOffset) {
            float tileY = getFloorRow(floorIndex) - verticalOffset;
            entities.add(new LevelLibrary.EntitySpec(type, tileX, tileY, 0.5f, 0.6f, null));
        }

        LevelLibrary.LegacyLevelBlueprint build() {
            String[] rows = new String[height];
            for (int y = 0; y < height; y++) {
                rows[y] = new String(tiles[y]);
            }
            return LevelLibrary.createBlueprint(rows, entities);
        }
    }
}
