package com.crobot.game.level;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.crobot.game.level.LevelModel;
import com.example.robotparkour.core.WorldInfo;

/**
 * Value object representing a generated labyrinth level and its associated metadata.
 */
public final class LevelDescriptor {

    private final int worldNumber;
    private final int stageNumber;
    private final WorldInfo worldInfo;
    private final int musicResId;
    private final com.example.robotparkour.level.LevelLibrary.LegacyLevelBlueprint blueprint;

    @Nullable
    private LevelModel cachedModel;

    public LevelDescriptor(int worldNumber,
                           int stageNumber,
                           @NonNull WorldInfo worldInfo,
                           int musicResId,
                           @NonNull com.example.robotparkour.level.LevelLibrary.LegacyLevelBlueprint blueprint) {
        this.worldNumber = worldNumber;
        this.stageNumber = stageNumber;
        this.worldInfo = worldInfo;
        this.musicResId = musicResId;
        this.blueprint = blueprint;
    }

    public int getWorldNumber() {
        return worldNumber;
    }

    public int getStageNumber() {
        return stageNumber;
    }

    @NonNull
    public WorldInfo getWorldInfo() {
        return worldInfo;
    }

    public int getMusicResId() {
        return musicResId;
    }

    @NonNull
    public com.example.robotparkour.level.LevelLibrary.LegacyLevelBlueprint getBlueprint() {
        return blueprint;
    }

    @Nullable
    public synchronized LevelModel getCachedModel() {
        return cachedModel;
    }

    public synchronized void cacheModel(@NonNull LevelModel model) {
        cachedModel = model;
    }
}
