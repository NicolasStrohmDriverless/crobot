// app/src/main/java/com/crobot/game/level/LevelModel.java
package com.crobot.game.level;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable representation of a decoded level.
 */
public final class LevelModel {

    private final int width;
    private final int height;
    private final int tileWidth;
    private final int tileHeight;
    @NonNull
    private final TileLayer tileLayer;
    @NonNull
    private final List<Entity> entities;
    @NonNull
    private final CollisionMap collisionMap;
    @NonNull
    private final String tilesetAssetPath;

    public LevelModel(int width,
                      int height,
                      int tileWidth,
                      int tileHeight,
                      @NonNull TileLayer tileLayer,
                      @NonNull List<Entity> entities,
                      @NonNull CollisionMap collisionMap,
                      @NonNull String tilesetAssetPath) {
        this.width = width;
        this.height = height;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.tileLayer = tileLayer;
        this.entities = Collections.unmodifiableList(new ArrayList<>(entities));
        this.collisionMap = collisionMap;
        this.tilesetAssetPath = tilesetAssetPath;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getTileWidth() {
        return tileWidth;
    }

    public int getTileHeight() {
        return tileHeight;
    }

    @NonNull
    public TileLayer getTileLayer() {
        return tileLayer;
    }

    @NonNull
    public List<Entity> getEntities() {
        return entities;
    }

    @NonNull
    public CollisionMap getCollisionMap() {
        return collisionMap;
    }

    @NonNull
    public String getTilesetAssetPath() {
        return tilesetAssetPath;
    }

    public int getPixelWidth() {
        return width * tileWidth;
    }

    public int getPixelHeight() {
        return height * tileHeight;
    }

    /**
     * Represents a single tile layer with CSV encoded data.
     */
    public static final class TileLayer {
        @NonNull
        private final String name;
        private final int width;
        private final int height;
        @NonNull
        private final int[] data;

        public TileLayer(@NonNull String name, int width, int height, @NonNull int[] data) {
            this.name = name;
            this.width = width;
            this.height = height;
            this.data = data;
        }

        @NonNull
        public String getName() {
            return name;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public int getTileId(int x, int y) {
            if (x < 0 || x >= width || y < 0 || y >= height) {
                return 0;
            }
            return data[y * width + x];
        }

        @NonNull
        public int[] getData() {
            return data;
        }
    }

    /**
     * Entity definition passed from native code.
     */
    public static final class Entity {
        @NonNull
        private final String type;
        private final int x;
        private final int y;
        @NonNull
        private final Map<String, String> extras;

        public Entity(@NonNull String type, int x, int y, @Nullable Map<String, String> extras) {
            this.type = type;
            this.x = x;
            this.y = y;
            if (extras == null || extras.isEmpty()) {
                this.extras = Collections.emptyMap();
            } else {
                this.extras = Collections.unmodifiableMap(new HashMap<>(extras));
            }
        }

        @NonNull
        public String getType() {
            return type;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        @NonNull
        public Map<String, String> getExtras() {
            return extras;
        }
    }

    /**
     * Collision information per GID.
     */
    public static final class CollisionMap {
        @NonNull
        private final int[] gidFlags;

        public CollisionMap(@NonNull int[] gidFlags) {
            this.gidFlags = gidFlags;
        }

        public boolean isSolid(int gid) {
            if (gid < 0 || gid >= gidFlags.length) {
                return false;
            }
            return (gidFlags[gid] & 0x1) != 0;
        }

        public int[] getRawFlags() {
            return gidFlags;
        }
    }
}
