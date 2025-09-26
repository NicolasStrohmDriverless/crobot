package com.crobot.game.level;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.crobot.game.level.LevelModel.CollisionMap;
import com.crobot.game.level.LevelModel.Entity;
import com.crobot.game.level.LevelModel.TileLayer;
import com.example.robotparkour.core.WorldInfo;
import com.example.robotparkour.level.LevelLibrary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides access to the legacy ASCII based level layouts so that they can be converted into the
 * {@link LevelModel} representation used by the new renderer. This acts as a compatibility layer
 * for worlds that have not yet been ported to the native pipeline.
 */
public final class LegacyWorldData {

    private static final int TILE_SIZE = 32;

    private static final Map<Integer, WorldInfo> WORLD_INFO = createWorldInfoMap();

    private LegacyWorldData() {
        // Utility class.
    }

    @Nullable
    public static WorldInfo findWorld(int worldNumber) {
        return WORLD_INFO.get(worldNumber);
    }

    @Nullable
    public static LevelModel createLevelModel(int worldNumber, int stage) {
        if (stage != 1) {
            return null;
        }
        WorldInfo info = findWorld(worldNumber);
        if (info == null) {
            return null;
        }
        String[] rows = LevelLibrary.getLevelData(info);
        if (rows == null || rows.length == 0) {
            return null;
        }
        return convertToLevelModel(rows);
    }

    @NonNull
    private static LevelModel convertToLevelModel(@NonNull String[] rows) {
        int height = rows.length;
        int width = 0;
        for (String row : rows) {
            if (row != null) {
                width = Math.max(width, row.length());
            }
        }
        if (width == 0) {
            throw new IllegalArgumentException("Legacy level rows must not be empty");
        }

        int[] tileData = new int[width * height];
        List<Entity> entities = new ArrayList<>();

        for (int y = 0; y < height; y++) {
            String row = rows[y] == null ? "" : rows[y];
            for (int x = 0; x < width; x++) {
                char code = x < row.length() ? row.charAt(x) : '.';
                tileData[y * width + x] = gidFor(code);
                maybeAddEntity(entities, code, x, y);
            }
        }

        int[] collisionFlags = new int[4];
        collisionFlags[gidFor('G')] = 0x1;
        collisionFlags[gidFor('B')] = 0x1;
        collisionFlags[gidFor('Q')] = 0x1;

        TileLayer layer = new TileLayer("ground", width, height, tileData);
        CollisionMap collisionMap = new CollisionMap(collisionFlags);
        return new LevelModel(width, height, TILE_SIZE, TILE_SIZE, layer, entities, collisionMap, "");
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

    private static void maybeAddEntity(@NonNull List<Entity> entities, char code, int gridX, int gridY) {
        switch (code) {
            case 'C': {
                int x = Math.round((gridX + 0.5f) * TILE_SIZE);
                int y = Math.round((gridY + 0.4f) * TILE_SIZE);
                entities.add(new Entity("coin", x, y, null));
                break;
            }
            case 'S': {
                int x = Math.round((gridX + 0.5f) * TILE_SIZE);
                int y = Math.round((gridY + 1f) * TILE_SIZE);
                entities.add(new Entity("spike", x, y, null));
                break;
            }
            case 'F': {
                int x = Math.round((gridX + 0.5f) * TILE_SIZE);
                int y = Math.round(gridY * TILE_SIZE);
                entities.add(new Entity("flag", x, y, null));
                break;
            }
            case 'R': {
                Map<String, String> extras = new HashMap<>();
                int x = Math.round((gridX + 0.5f) * TILE_SIZE);
                int y = Math.round((gridY + 1f) * TILE_SIZE);
                extras.put("spawn", "true");
                entities.add(new Entity("spawn", x, y, extras));
                break;
            }
            default:
                break;
        }
    }

    @NonNull
    private static Map<Integer, WorldInfo> createWorldInfoMap() {
        Map<Integer, WorldInfo> map = new HashMap<>();
        map.put(1, new WorldInfo(1, "Pointer Plains", "Startwelt, leicht & freundlich"));
        map.put(2, new WorldInfo(2, "Template Temple", "komplex, verschachtelt"));
        map.put(3, new WorldInfo(3, "Namespace Nebula", "spacey, schwebend"));
        map.put(4, new WorldInfo(4, "Exception Volcano", "heiß, leicht bedrohlich – kein Boss, nur Spannung"));
        map.put(5, new WorldInfo(5, "STL City", "geschäftig, groovy"));
        map.put(6, new WorldInfo(6, "Heap Caverns", "dunkel, hohl, vorsichtig"));
        map.put(7, new WorldInfo(7, "Lambda Gardens", "verspielt, naturhaft, \"funky nerdy\""));
        map.put(8, new WorldInfo(8, "Multithread Foundry", "antriebsstark, mechanisch"));
        map.put(9, new WorldInfo(9, "NullPointer-Nexus", "Der Kernel-Kerker"));
        return map;
    }
}
