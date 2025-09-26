// app/src/main/java/com/example/robotparkour/core/WorldInfo.java
package com.example.robotparkour.core;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Immutable value object describing a selectable world in the world-select scene.
 */
public final class WorldInfo {

    private final int programNumber;
    private final String name;
    private final String description;

    public WorldInfo(int programNumber, @NonNull String name, @NonNull String description) {
        this.programNumber = programNumber;
        this.name = name;
        this.description = description;
    }

    public int getProgramNumber() {
        return programNumber;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof WorldInfo)) {
            return false;
        }
        WorldInfo other = (WorldInfo) obj;
        return programNumber == other.programNumber
                && name.equals(other.name)
                && description.equals(other.description);
    }

    @Override
    public int hashCode() {
        int result = Integer.hashCode(programNumber);
        result = 31 * result + name.hashCode();
        result = 31 * result + description.hashCode();
        return result;
    }

    @Override
    @NonNull
    public String toString() {
        return "WorldInfo{" +
                "Program " + programNumber +
                ": " + name +
                ", description='" + description + "'" +
                '}';
    }
}
