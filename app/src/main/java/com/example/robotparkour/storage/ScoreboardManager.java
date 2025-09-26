// app/src/main/java/com/example/robotparkour/storage/ScoreboardManager.java
package com.example.robotparkour.storage;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Persists the player's best completion times so they survive app restarts.
 */
public class ScoreboardManager {

    private static final String PREFS_NAME = "robot_parkour_scores";
    private static final String KEY_TIMES = "times";
    private static final int MAX_ENTRIES = 10;

    private final SharedPreferences preferences;

    public ScoreboardManager(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public synchronized void submitTime(float seconds) {
        List<Float> times = loadTimes();
        times.add(seconds);
        Collections.sort(times);
        if (times.size() > MAX_ENTRIES) {
            times = new ArrayList<>(times.subList(0, MAX_ENTRIES));
        }
        saveTimes(times);
    }

    public synchronized List<Float> getTopTimes() {
        return loadTimes();
    }

    public synchronized void clear() {
        preferences.edit().remove(KEY_TIMES).apply();
    }

    private List<Float> loadTimes() {
        String raw = preferences.getString(KEY_TIMES, "");
        List<Float> result = new ArrayList<>();
        if (raw == null || raw.isEmpty()) {
            return result;
        }
        String[] pieces = raw.split(",");
        for (String piece : pieces) {
            try {
                result.add(Float.parseFloat(piece));
            } catch (NumberFormatException ignored) {
                // Skip malformed entries.
            }
        }
        Collections.sort(result);
        if (result.size() > MAX_ENTRIES) {
            return new ArrayList<>(result.subList(0, MAX_ENTRIES));
        }
        return result;
    }

    private void saveTimes(List<Float> times) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < times.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(String.format(Locale.US, "%.3f", times.get(i)));
        }
        preferences.edit().putString(KEY_TIMES, builder.toString()).apply();
    }
}
