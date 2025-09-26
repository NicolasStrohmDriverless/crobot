// app/src/main/java/com/example/robotparkour/level/Level.java
package com.example.robotparkour.level;

import android.graphics.RectF;

import com.example.robotparkour.entity.Coin;
import com.example.robotparkour.entity.Flag;
import com.example.robotparkour.entity.Spike;
import com.example.robotparkour.entity.Tile;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable description of a single tile-based level, including all entities
 * that should spawn in it.
 */
public class Level {

    public static final float TILE_SIZE = 32f;

    private final int width;
    private final int height;
    private final float tileSize;

    private final Tile[][] tileGrid;
    private final List<Tile> tiles;
    private final List<Coin> coins;
    private final List<Spike> spikes;
    private final Flag flag;
    private final float spawnX;
    private final float spawnY;

    private Level(int width,
                  int height,
                  float tileSize,
                  Tile[][] tileGrid,
                  List<Tile> tiles,
                  List<Coin> coins,
                  List<Spike> spikes,
                  Flag flag,
                  float spawnX,
                  float spawnY) {
        this.width = width;
        this.height = height;
        this.tileSize = tileSize;
        this.tileGrid = tileGrid;
        this.tiles = tiles;
        this.coins = coins;
        this.spikes = spikes;
        this.flag = flag;
        this.spawnX = spawnX;
        this.spawnY = spawnY;
    }

    public static Level fromStringMap(String[] rows, float tileSize) {
        if (rows == null || rows.length == 0) {
            throw new IllegalArgumentException("Level requires at least one row");
        }
        int height = rows.length;
        int width = 0;
        for (String row : rows) {
            if (row != null) {
                width = Math.max(width, row.length());
            }
        }
        if (width == 0) {
            throw new IllegalArgumentException("Level rows must contain at least one column");
        }
        Tile[][] grid = new Tile[height][width];
        List<Tile> tiles = new ArrayList<>();
        List<Coin> coins = new ArrayList<>();
        List<Spike> spikes = new ArrayList<>();
        Flag flag = null;
        float spawnX = tileSize;
        float spawnY = tileSize;

        for (int row = 0; row < height; row++) {
            String line = rows[row] == null ? "" : rows[row];
            for (int col = 0; col < width; col++) {
                char code = col < line.length() ? line.charAt(col) : '.';
                float x = col * tileSize;
                float y = row * tileSize;
                switch (code) {
                    case 'G':
                    case 'B':
                    case 'Q':
                        TileType type = TileType.fromCode(code);
                        Tile tile = new Tile(x, y, tileSize, type);
                        grid[row][col] = tile;
                        tiles.add(tile);
                        break;
                    case 'C':
                        Coin coin = new Coin(x + tileSize * 0.2f, y + tileSize * 0.15f, tileSize * 0.6f);
                        coins.add(coin);
                        break;
                    case 'S':
                        Spike spike = new Spike(x, y, tileSize);
                        spikes.add(spike);
                        break;
                    case 'F':
                        flag = new Flag(x, y, tileSize);
                        break;
                    case 'R':
                        spawnX = x;
                        spawnY = y;
                        break;
                    case '.':
                    default:
                        break;
                }
            }
        }
        return new Level(width, height, tileSize, grid, tiles, coins, spikes, flag, spawnX, spawnY);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public float getPixelWidth() {
        return width * tileSize;
    }

    public float getPixelHeight() {
        return height * tileSize;
    }

    public float getTileSize() {
        return tileSize;
    }

    public List<Tile> getTiles() {
        return tiles;
    }

    public List<Coin> getCoins() {
        return coins;
    }

    public List<Spike> getSpikes() {
        return spikes;
    }

    public Flag getFlag() {
        return flag;
    }

    public float getSpawnX() {
        return spawnX;
    }

    public float getSpawnY() {
        return spawnY;
    }

    public Tile getTile(int gridX, int gridY) {
        if (gridX < 0 || gridX >= width || gridY < 0 || gridY >= height) {
            return null;
        }
        return tileGrid[gridY][gridX];
    }

    /**
     * Populates {@code result} with every solid tile intersecting the given rectangle.
     */
    public void querySolidTiles(RectF area, List<Tile> result) {
        result.clear();
        int startX = Math.max(0, (int) Math.floor(area.left / tileSize));
        int endX = Math.min(width - 1, (int) Math.floor(area.right / tileSize));
        int startY = Math.max(0, (int) Math.floor(area.top / tileSize));
        int endY = Math.min(height - 1, (int) Math.floor(area.bottom / tileSize));
        for (int y = startY; y <= endY; y++) {
            for (int x = startX; x <= endX; x++) {
                Tile tile = tileGrid[y][x];
                if (tile != null && tile.isSolid()) {
                    result.add(tile);
                }
            }
        }
    }
}
