// app/src/main/java/com/crobot/game/level/LevelRepository.java
package com.crobot.game.level;

import android.content.Context;
import android.content.res.AssetManager;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Central access point that loads {@link LevelModel} instances via JNI.
 *
 * <p>The JSON layout consumed by the native decoder follows this structure:
 * <pre>{
 *   "tileWidth": 16,
 *   "tileHeight": 16,
 *   "width": 128,
 *   "height": 15,
 *   "tileset": "tilesets/platformer16.png",
 *   "solidGids": [1, 2, 3],
 *   "layers": [
 *     {
 *       "name": "ground",
 *       "encoding": "csv",
 *       "data": "comma separated 1-based tile GIDs"
 *     }
 *   ],
 *   "entities": [
 *     {"type": "coin", "x": 320, "y": 160}
 *   ]
 * }</pre>
 * The area/object format mirrors the column-oriented SMB structure and uses a
 * <code>columns</code> array with repeatable column definitions. See
 * {@code assets/levels/world1_stage1.area.json} for a documented example.
 */
public final class LevelRepository {

    static {
        System.loadLibrary("crobot_native");
    }

    private static final Object NATIVE_LOCK = new Object();
    private static boolean sNativeReady;

    private final Context appContext;

    public LevelRepository(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        ensureNativeInitialised();
    }

    private void ensureNativeInitialised() {
        synchronized (NATIVE_LOCK) {
            if (!sNativeReady) {
                AssetManager manager = appContext.getAssets();
                nativeSetAssetManager(manager);
                sNativeReady = true;
            }
        }
    }

    @NonNull
    public LevelModel loadLevel(int world, int stage) throws IOException {
        synchronized (NATIVE_LOCK) {
            int[] tileData = nativeLoadTileMap(world, stage);
            if (tileData == null) {
                throw new IOException("nativeLoadTileMap returned null");
            }
            int[] dimensions = nativeGetLevelDimensions();
            if (dimensions == null || dimensions.length < 4) {
                throw new IOException("nativeGetLevelDimensions returned invalid data");
            }
            int width = dimensions[0];
            int height = dimensions[1];
            int tileWidth = dimensions[2];
            int tileHeight = dimensions[3];
            if (tileData.length != width * height) {
                throw new IOException("Tile data count does not match width*height");
            }
            LevelModel.TileLayer layer = new LevelModel.TileLayer("ground", width, height, tileData);

            LevelModel.Entity[] entityArray = nativeLoadEntities(world, stage);
            List<LevelModel.Entity> entities = new ArrayList<>();
            if (entityArray != null) {
                entities.addAll(Arrays.asList(entityArray));
            }

            int[] collisionMask = nativeGetCollisionMask();
            if (collisionMask == null) {
                collisionMask = new int[0];
            }
            LevelModel.CollisionMap collisionMap = new LevelModel.CollisionMap(collisionMask);

            String tilesetPath = nativeGetTilesetPath();
            if (tilesetPath == null) {
                tilesetPath = "";
            }

            return new LevelModel(width, height, tileWidth, tileHeight, layer, entities, collisionMap, tilesetPath);
        }
    }

    /**
     * Add a new level by placing {@code worldX_stageY.json} (or
     * {@code worldX_stageY.area.json}) inside {@code assets/levels/}.
     */
    public LevelModel loadLevel(@NonNull String worldName, int stage) throws IOException {
        int worldNumber;
        try {
            worldNumber = Integer.parseInt(worldName.replaceAll("\\D", ""));
        } catch (NumberFormatException ex) {
            worldNumber = 1;
        }
        return loadLevel(worldNumber, stage);
    }

    private static native void nativeSetAssetManager(AssetManager manager);

    private static native int[] nativeLoadTileMap(int world, int stage);

    private static native LevelModel.Entity[] nativeLoadEntities(int world, int stage);

    private static native int[] nativeGetCollisionMask();

    private static native int[] nativeGetLevelDimensions();

    private static native String nativeGetTilesetPath();
}
