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
            return R.raw.robot_cpp;
        }
        Integer resId = MUSIC_BY_PROGRAM.get(worldInfo.getProgramNumber());
        if (resId != null) {
            return resId;
        }
        return R.raw.robot_cpp;
    }

    private static Map<Integer, Integer> createProgramMap() {
        Map<Integer, Integer> map = new HashMap<>();
        map.put(1, R.raw.robot_cpp);
        map.put(2, R.raw.template_temple);
        map.put(3, R.raw.namespace_nebula);
        map.put(4, R.raw.boss_fight);
        return map;
    }
}
