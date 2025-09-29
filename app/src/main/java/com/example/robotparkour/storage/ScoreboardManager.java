// app/src/main/java/com/example/robotparkour/storage/ScoreboardManager.java
package com.example.robotparkour.storage;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Persists the player's best completion times so they survive app restarts.
 */
public class ScoreboardManager {

    private static final String PREFS_NAME = "robot_parkour_scores";
    private static final String KEY_PREFIX = "best_time_world_";

    private final SharedPreferences preferences;

    public ScoreboardManager(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public synchronized void submitTime(int worldNumber, float seconds) {
        if (worldNumber <= 0) {
            return;
        }
        Float current = getBestTime(worldNumber);
        if (current == null || seconds < current) {
            preferences.edit().putFloat(keyFor(worldNumber), seconds).apply();
        }
    }

    public synchronized Float getBestTime(int worldNumber) {
        float stored = preferences.getFloat(keyFor(worldNumber), -1f);
        return stored > 0f ? stored : null;
    }

    public synchronized void clear() {
        SharedPreferences.Editor editor = preferences.edit();
        Map<String, ?> all = preferences.getAll();
        Iterator<String> iterator = all.keySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            if (key.startsWith(KEY_PREFIX)) {
                editor.remove(key);
            }
        }
        editor.apply();
    }

    public synchronized Map<Integer, Float> getAllBestTimes() {
        Map<Integer, Float> result = new HashMap<>();
        Map<String, ?> all = preferences.getAll();
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith(KEY_PREFIX)) {
                continue;
            }
            Object value = entry.getValue();
            if (!(value instanceof Float)) {
                continue;
            }
            int world = parseWorldNumber(key);
            if (world > 0) {
                result.put(world, (Float) value);
            }
        }
        return result;
    }

    private String keyFor(int worldNumber) {
        return KEY_PREFIX + worldNumber;
    }

    private int parseWorldNumber(String key) {
        try {
            return Integer.parseInt(key.substring(KEY_PREFIX.length()));
        } catch (NumberFormatException | IndexOutOfBoundsException ex) {
            return -1;
        }
    }
}
