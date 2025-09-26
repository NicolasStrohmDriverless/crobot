// app/src/main/java/com/example/robotparkour/util/GameResult.java
package com.example.robotparkour.util;

/**
 * Immutable value object describing the outcome of a game round. It is shared
 * between scenes so the UI can display consistent information.
 */
public class GameResult {

    private final float timeSeconds;
    private final int coinsCollected;
    private final boolean victory;
    private final int livesRemaining;

    public GameResult(float timeSeconds, int coinsCollected, boolean victory, int livesRemaining) {
        this.timeSeconds = timeSeconds;
        this.coinsCollected = coinsCollected;
        this.victory = victory;
        this.livesRemaining = livesRemaining;
    }

    public float getTimeSeconds() {
        return timeSeconds;
    }

    public int getCoinsCollected() {
        return coinsCollected;
    }

    public boolean isVictory() {
        return victory;
    }

    public int getLivesRemaining() {
        return livesRemaining;
    }
}
