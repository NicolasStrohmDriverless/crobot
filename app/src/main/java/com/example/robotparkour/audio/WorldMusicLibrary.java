// app/src/main/java/com/example/robotparkour/audio/WorldMusicLibrary.java
package com.example.robotparkour.audio;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;

import com.crobot.game.level.LevelCatalog;
import com.crobot.game.level.LevelDescriptor;
import com.example.robotparkour.R;
import com.example.robotparkour.core.WorldInfo;

import java.util.List;

/**
 * Resolves which background track should play for a given world selection.
 */
public final class WorldMusicLibrary {

    private WorldMusicLibrary() {
        // Utility class.
    }

    @RawRes
    public static int getTrackFor(@NonNull Context context, @Nullable WorldInfo worldInfo) {
        LevelCatalog catalog = LevelCatalog.getInstance(context.getApplicationContext());
        if (worldInfo != null) {
            LevelDescriptor descriptor = catalog.findByWorld(worldInfo.getProgramNumber());
            if (descriptor != null) {
                return descriptor.getMusicResId();
            }
        }
        List<LevelDescriptor> descriptors = catalog.getDescriptors();
        if (!descriptors.isEmpty()) {
            return descriptors.get(0).getMusicResId();
        }
        return R.raw.robot_cpp;
    }
}
