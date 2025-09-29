package com.crobot.game.level;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.robotparkour.R;
import com.example.robotparkour.core.WorldInfo;
import com.example.robotparkour.level.DynamicLevelGenerator;
import com.example.robotparkour.level.LevelLibrary;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Discovers audio tracks in {@code res/raw} and creates a labyrinth level for each entry.
 */
public final class LevelCatalog {

    private static final long MUSIC_LENGTH_THRESHOLD_BYTES = 160_000L;
    private static final String[] ORDERED_TRACKS = new String[] {
            "background",
            "pointer_plains",
            "lambda_gardens",
            "namespace_nebula",
            "template_temple",
            "multithread_foundry",
            "exception_volcano",
            "heap_caverns",
            "boss_fight"
    };

    private static final Object LOCK = new Object();
    @Nullable
    private static LevelCatalog instance;

    private final Context appContext;
    private final List<LevelDescriptor> descriptors;

    private LevelCatalog(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        this.descriptors = loadDescriptors();
    }

    @NonNull
    public static LevelCatalog getInstance(@NonNull Context context) {
        synchronized (LOCK) {
            if (instance == null) {
                instance = new LevelCatalog(context);
            }
            return instance;
        }
    }

    @NonNull
    public List<LevelDescriptor> getDescriptors() {
        return Collections.unmodifiableList(descriptors);
    }

    @Nullable
    public LevelDescriptor findByWorld(int worldNumber) {
        for (LevelDescriptor descriptor : descriptors) {
            if (descriptor.getWorldNumber() == worldNumber) {
                return descriptor;
            }
        }
        return null;
    }

    @Nullable
    public LevelModel createLevel(int worldNumber, int stage) {
        if (stage != 1) {
            return null;
        }
        LevelDescriptor descriptor = findByWorld(worldNumber);
        if (descriptor == null) {
            return null;
        }
        LevelModel cached = descriptor.getCachedModel();
        if (cached != null) {
            return cached;
        }
        LevelModel model = DynamicLevelGenerator.convertToModel(descriptor.getBlueprint());
        descriptor.cacheModel(model);
        return model;
    }

    private List<LevelDescriptor> loadDescriptors() {
        List<LevelDescriptor> list = new ArrayList<>();
        Resources resources = appContext.getResources();
        String packageName = appContext.getPackageName();
        int worldNumber = 1;
        for (String entryName : ORDERED_TRACKS) {
            int resId = resources.getIdentifier(entryName, "raw", packageName);
            if (resId == 0) {
                continue;
            }
            if (!isMusicResource(resources, resId)) {
                continue;
            }
            String displayName = toDisplayName(entryName);
            String description = buildDescription(entryName);
            WorldInfo worldInfo = new WorldInfo(worldNumber, displayName, description);
            LevelLibrary.LegacyLevelBlueprint blueprint = DynamicLevelGenerator.buildLabyrinth(worldNumber - 1, entryName);
            LevelDescriptor descriptor = new LevelDescriptor(worldNumber, 1, worldInfo, resId, blueprint);
            list.add(descriptor);
            worldNumber++;
        }
        if (list.isEmpty()) {
            // Provide at least one fallback level so the game remains playable.
            WorldInfo fallbackInfo = new WorldInfo(1, "Pointer Plains", "Klassischer Debug-Parcours");
            LevelLibrary.LegacyLevelBlueprint blueprint = DynamicLevelGenerator.buildLabyrinth(0, "robot_cpp");
            list.add(new LevelDescriptor(1, 1, fallbackInfo, R.raw.robot_cpp, blueprint));
        }
        return list;
    }

    private boolean isMusicResource(@NonNull Resources resources, int resId) {
        AssetFileDescriptor descriptor = null;
        try {
            descriptor = resources.openRawResourceFd(resId);
            if (descriptor == null) {
                return false;
            }
            long length = descriptor.getLength();
            return length < 0 || length >= MUSIC_LENGTH_THRESHOLD_BYTES;
        } catch (Resources.NotFoundException ex) {
            return false;
        } finally {
            if (descriptor != null) {
                try {
                    descriptor.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    @NonNull
    private String toDisplayName(@NonNull String entryName) {
        String[] parts = entryName.split("[_-]");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(part.substring(0, 1).toUpperCase(Locale.getDefault()));
            if (part.length() > 1) {
                builder.append(part.substring(1).toLowerCase(Locale.getDefault()));
            }
        }
        return builder.toString();
    }

    @NonNull
    private String buildDescription(@NonNull String entryName) {
        return "Labyrinth synchronisiert mit \"" + toDisplayName(entryName) + "\"";
    }
}
