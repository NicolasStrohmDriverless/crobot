// app/src/main/java/com/example/robotparkour/level/LevelLibrary.java
package com.example.robotparkour.level;

import androidx.annotation.NonNull;

import com.example.robotparkour.core.WorldInfo;

/**
 * Provides tile layouts for every selectable world. Each world receives its own
 * bespoke course so that switching worlds also changes the gameplay.
 */
public final class LevelLibrary {

    private static final String[] POINTER_PLAINS = new String[] {
            "................................................",
            "................................................",
            "................................................",
            "...............C...........C.................F..",
            "..........GGGGGGGGGG................GGGGGGGGGGGG",
            "......C..G........GGGGGGGGGGGGGGGGGG...........G",
            "..R.G...G....C.....................C..........G.",
            "GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG"
    };

    private static final String[] TEMPLATE_TEMPLE = new String[] {
            "................................................",
            ".............C....................C.............",
            "....QQQQQQQQQQQQ...........QQQQQQQQQQQQQF.......",
            "....Q..........Q....C......Q..........Q.........",
            "..RQ..........QGGGGGGGGGGGGQ..........Q.........",
            "..GGGGGGGGGGGGQ......C.....Q....SSSS..Q.........",
            "............C.Q............Q..........Q.........",
            "..........QQQQQQQQQQQQQQQQQQQQQQQQQQQQQQ........",
            "GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG"
    };

    private static final String[] NAMESPACE_NEBULA = new String[] {
            "................................................",
            "...........C......................C.............",
            "....B....BBBBBB.........C.....BBBBBB....F.......",
            ".........B....B....BBBBBBB....B....B............",
            "..R......B....B....B.....B....B....B............",
            "BBBBBBBBBB....BBBBB.....BBBBBBB....BBBBBBBBBBBBB",
            "...............C...................C............",
            "GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG"
    };

    private static final String[] EXCEPTION_VOLCANO = new String[] {
            "................................................",
            ".............C...........S.SS...................",
            "....GGGGGGGGGGGGGGG....SSSSSS....GGGGGGGGGGGF...",
            "..RG..............G....S....S....G..............",
            "..GGGGGGGGGGGGGGGGGGGGG....GGGGGGGGGGGGGGGGGGGGG",
            "........C.........S............C................",
            "GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG"
    };

    private static final String[] STL_CITY = new String[] {
            "................................................",
            ".................C..............C...............",
            "....BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBF........",
            "....B....C.....B..........C.....B.....B.........",
            "..RB....BBBBB..B....BBBBBBBBB...B..C..B.........",
            "..GGGGGGG..B..BB....B......B....B.....B.........",
            "..........CB..BB....B..C...B....BBBBBBB.........",
            "GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG"
    };

    private static final String[] HEAP_CAVERNS = new String[] {
            "................................................",
            ".............C...........C......................",
            "..BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBF....",
            "..B....C.....B..............C.........B.........",
            "..B....BBBBB.BBBBBBBBBBBBBBBBBBBBBB...B.........",
            "..B........B..............C.........B...........",
            "..RBBBBBBB.B....C.................B.............",
            "GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG"
    };

    private static final String[] LAMBDA_GARDENS = new String[] {
            "................................................",
            "...........C.............C..............C.......",
            "....GGGGGGGGGGGG....C....GGGGGGGGGGGGGGGGGF.....",
            "..RG.........C.G.........G..............G.......",
            "..GGGGGGGGGGGGGGGGGGGGGGG....C.........GGGGGGGGG",
            "..............C...............C.................",
            "GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG"
    };

    private static final String[] MULTITHREAD_FOUNDRY = new String[] {
            "................................................",
            "........C...........C.............C.............",
            "....GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGF......",
            "..RG....C.....G....C....G.....C......G..........",
            "..GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG",
            "....C.........C............C....................",
            "GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG"
    };

    private static final String[] NULLPOINTER_NEXUS = new String[] {
            "................................................",
            ".............C....S....C....S....C..............",
            "....GGGGGGGGGGSSSSGGGGGGGGSSSSGGGGGGGGGGF.......",
            "..RG...............G....G...............G.......",
            "..GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG",
            "...........C........C........C..................",
            "GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG"
    };

    private LevelLibrary() {
    }

    @NonNull
    public static String[] getLevelData(WorldInfo worldInfo) {
        if (worldInfo == null) {
            return POINTER_PLAINS.clone();
        }
        switch (worldInfo.getName()) {
            case "Template Temple":
                return TEMPLATE_TEMPLE.clone();
            case "Namespace Nebula":
                return NAMESPACE_NEBULA.clone();
            case "Exception Volcano":
                return EXCEPTION_VOLCANO.clone();
            case "STL City":
                return STL_CITY.clone();
            case "Heap Caverns":
                return HEAP_CAVERNS.clone();
            case "Lambda Gardens":
                return LAMBDA_GARDENS.clone();
            case "Multithread Foundry":
                return MULTITHREAD_FOUNDRY.clone();
            case "NullPointer-Nexus":
                return NULLPOINTER_NEXUS.clone();
            case "Pointer Plains":
            default:
                return POINTER_PLAINS.clone();
        }
    }
}
