// app/src/main/java/com/example/robotparkour/audio/WorldMusicLibrary.java
package com.example.robotparkour.audio;

import androidx.annotation.RawRes;

import com.example.robotparkour.R;
import com.example.robotparkour.core.WorldInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * Resolves which background track should play for a given world selection.
 */
public final class WorldMusicLibrary {

    private static final Map<Integer, Integer> MUSIC_BY_PROGRAM = createProgramMap();

    private WorldMusicLibrary() {
        // Utility class.
    }

    @RawRes
    public static int getTrackFor(WorldInfo worldInfo) {
        if (worldInfo == null) {
            return R.raw.background;
        }
        Integer resId = MUSIC_BY_PROGRAM.get(worldInfo.getProgramNumber());
        if (resId != null) {
            return resId;
        }
        return R.raw.background;
    }

    private static Map<Integer, Integer> createProgramMap() {
        Map<Integer, Integer> map = new HashMap<>();
        map.put(1, R.raw.pointer_plains);
        map.put(2, R.raw.template_temple);
        map.put(3, R.raw.namespace_nebula);
        map.put(4, R.raw.exception_volcano);
        map.put(5, R.raw.stl_city);
        map.put(6, R.raw.heap_caverns);
        map.put(7, R.raw.lambda_gardens);
        map.put(8, R.raw.multithread_foundry);
        return map;
    }
}
