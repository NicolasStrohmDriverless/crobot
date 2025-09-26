// app/src/main/java/com/example/robotparkour/storage/WorldCompletionTracker.java
package com.example.robotparkour.storage;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Persists which worlds have been cleared so the map can display completion badges.
 */
public class WorldCompletionTracker {

    private static final String PREFS_NAME = "robot_parkour_progress";
    private static final String KEY_COMPLETED = "completed_worlds";

    private final SharedPreferences preferences;

    public WorldCompletionTracker(@NonNull Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public synchronized void markCompleted(int world, int stage) {
        Set<String> entries = load();
        if (entries.add(encode(world, stage))) {
            save(entries);
        }
    }

    public synchronized boolean isCompleted(int world, int stage) {
        return load().contains(encode(world, stage));
    }

    public synchronized boolean isWorldCompleted(int world) {
        return isCompleted(world, 1);
    }

    public synchronized Set<Integer> getCompletedWorlds() {
        Set<String> entries = load();
        Set<Integer> result = new HashSet<>();
        for (String entry : entries) {
            int separator = entry.indexOf(':');
            String worldPart = separator >= 0 ? entry.substring(0, separator) : entry;
            try {
                result.add(Integer.parseInt(worldPart));
            } catch (NumberFormatException ignored) {
                // Skip malformed entries.
            }
        }
        return result;
    }

    public synchronized void clear() {
        preferences.edit().remove(KEY_COMPLETED).apply();
    }

    @NonNull
    private Set<String> load() {
        Set<String> stored = preferences.getStringSet(KEY_COMPLETED, Collections.emptySet());
        if (stored == null || stored.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(stored);
    }

    private void save(@NonNull Set<String> entries) {
        preferences.edit().putStringSet(KEY_COMPLETED, new HashSet<>(entries)).apply();
    }

    private String encode(int world, int stage) {
        return world + ":" + stage;
    }
}
