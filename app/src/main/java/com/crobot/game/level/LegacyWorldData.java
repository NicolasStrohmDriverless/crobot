package com.crobot.game.level;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.robotparkour.core.WorldInfo;
import com.example.robotparkour.level.DynamicLevelGenerator;
import com.example.robotparkour.level.LevelLibrary;

/**
 * Provides access to the legacy ASCII based level layouts so that they can be converted into the
 * {@link LevelModel} representation used by the new renderer. This acts as a compatibility layer
 * for worlds that have not yet been ported to the native pipeline.
 */
public final class LegacyWorldData {

    @Nullable
    private static LevelCatalog sCatalog;

    private LegacyWorldData() {
        // Utility class.
    }

    public static void initialize(@NonNull Context context) {
        if (sCatalog == null) {
            sCatalog = LevelCatalog.getInstance(context.getApplicationContext());
        }
    }

    @Nullable
    public static WorldInfo findWorld(int worldNumber) {
        LevelCatalog catalog = sCatalog;
        if (catalog == null) {
            return null;
        }
        LevelDescriptor descriptor = catalog.findByWorld(worldNumber);
        return descriptor != null ? descriptor.getWorldInfo() : null;
    }

    @Nullable
    public static LevelModel createLevelModel(int worldNumber, int stage) {
        LevelCatalog catalog = sCatalog;
        if (catalog == null) {
            return null;
        }
        LevelModel level = catalog.createLevel(worldNumber, stage);
        if (level != null) {
            return level;
        }
        WorldInfo info = findWorld(worldNumber);
        if (info == null) {
            return null;
        }
        LevelLibrary.LegacyLevelBlueprint blueprint = LevelLibrary.getLevelBlueprint(info);
        return DynamicLevelGenerator.convertToModel(blueprint);
    }
}
