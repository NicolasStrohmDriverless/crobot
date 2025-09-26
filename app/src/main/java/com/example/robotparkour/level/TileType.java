// app/src/main/java/com/example/robotparkour/level/TileType.java
package com.example.robotparkour.level;

import android.graphics.Color;

/**
 * Describes the different block types used to build IDE-themed levels.
 */
public enum TileType {
    EMPTY('.', false, Color.TRANSPARENT),
    EDITOR_BLOCK('G', true, Color.parseColor("#1E1E1E")),
    TERMINAL_BLOCK('B', true, Color.parseColor("#252526")),
    DEBUG_BLOCK('Q', true, Color.parseColor("#373277"));

    private final char code;
    private final boolean solid;
    private final int color;

    TileType(char code, boolean solid, int color) {
        this.code = code;
        this.solid = solid;
        this.color = color;
    }

    public char getCode() {
        return code;
    }

    public boolean isSolid() {
        return solid;
    }

    public int getColor() {
        return color;
    }

    public static TileType fromCode(char code) {
        for (TileType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return EMPTY;
    }
}
