// app/src/main/java/com/example/robotparkour/util/TimeFormatter.java
package com.example.robotparkour.util;

import java.util.Locale;

/**
 * Formats floating-point seconds into mm:ss.mmm strings for the HUD and scoreboard.
 */
public final class TimeFormatter {

    private TimeFormatter() {
        // Utility class.
    }

    public static String format(float seconds) {
        if (seconds < 0f) {
            seconds = 0f;
        }
        int totalMillis = Math.round(seconds * 1000f);
        int minutes = totalMillis / 60000;
        int remaining = totalMillis % 60000;
        int secs = remaining / 1000;
        int millis = remaining % 1000;
        return String.format(Locale.US, "%02d:%02d.%03d", minutes, secs, millis);
    }
}
