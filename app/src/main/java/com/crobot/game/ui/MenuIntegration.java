// app/src/main/java/com/crobot/game/ui/MenuIntegration.java
package com.crobot.game.ui;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.crobot.game.GameActivity;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Bridges the legacy crobot menus with the new {@link GameActivity} platformer flow.
 */
public final class MenuIntegration {

    private static final String TAG = "MenuIntegration";
    private static final AtomicReference<WeakReference<Activity>> HOST_ACTIVITY = new AtomicReference<>();

    private MenuIntegration() {
    }

    /**
     * Must be invoked by the host {@link Activity} (MainActivity) so that menu callbacks can
     * launch {@link GameActivity} with the correct context.
     */
    public static void registerHost(@NonNull Activity activity) {
        HOST_ACTIVITY.set(new WeakReference<>(activity));
    }

    /**
     * Should be called from {@link Activity#onDestroy()} to avoid leaking the reference.
     */
    public static void unregisterHost(@NonNull Activity activity) {
        WeakReference<Activity> current = HOST_ACTIVITY.get();
        Activity stored = current != null ? current.get() : null;
        if (stored == activity) {
            HOST_ACTIVITY.set(new WeakReference<>((Activity) null));
        }
    }

    @Nullable
    private static Activity resolveActivity() {
        WeakReference<Activity> ref = HOST_ACTIVITY.get();
        return ref != null ? ref.get() : null;
    }

    /**
     * Requests the platformer activity to launch. Returns {@code true} when the call could be
     * scheduled. If no host activity is registered, the caller should continue with the
     * previous behaviour.
     */
    public static boolean launchGameActivity(int world, int stage) {
        Activity activity = resolveActivity();
        if (activity == null) {
            Log.w(TAG, "No host activity registered; cannot start platformer");
            return false;
        }
        activity.runOnUiThread(() -> GameActivity.start(activity, world, stage));
        return true;
    }

    /**
     * Convenience overload when only a {@link Context} is available.
     */
    public static void launchGameActivity(@NonNull Context context, int world, int stage) {
        if (!launchGameActivity(world, stage)) {
            GameActivity.start(context, world, stage);
        }
    }
}
