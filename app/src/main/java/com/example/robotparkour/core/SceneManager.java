// app/src/main/java/com/example/robotparkour/core/SceneManager.java
package com.example.robotparkour.core;

import android.content.Context;
import android.graphics.Canvas;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.crobot.game.ui.MenuIntegration;
import com.example.robotparkour.audio.GameAudioManager;
import com.example.robotparkour.scene.GameOverScene;
import com.example.robotparkour.scene.GameScene;
import com.example.robotparkour.scene.MenuScene;
import com.example.robotparkour.scene.ScoreboardScene;
import com.example.robotparkour.scene.SettingsScene;
import com.example.robotparkour.scene.WorldSelectScene;
import com.example.robotparkour.storage.ScoreboardManager;
import com.example.robotparkour.util.GameResult;

import java.util.EnumMap;
import java.util.Map;

/**
 * Central orchestrator that owns every scene and routes input and lifecycle
 * events to the currently active one.
 */
public class SceneManager {

    private final Context appContext;
    private final GameView gameView;
    private final ScoreboardManager scoreboardManager;
    private final GameAudioManager audioManager;
    private final Map<SceneType, Scene> scenes = new EnumMap<>(SceneType.class);

    private Scene currentScene;
    private int surfaceWidth;
    private int surfaceHeight;

    private GameScene gameScene;
    private GameOverScene gameOverScene;
    private ScoreboardScene scoreboardScene;
    private WorldSelectScene worldSelectScene;

    private WorldInfo selectedWorld;

    public SceneManager(Context context,
                        GameView gameView,
                        ScoreboardManager scoreboardManager,
                        GameAudioManager audioManager) {
        this.appContext = context.getApplicationContext();
        this.gameView = gameView;
        this.scoreboardManager = scoreboardManager;
        this.audioManager = audioManager;
        createScenes();
    }

    private void createScenes() {
        MenuScene menuScene = new MenuScene(appContext, this);
        registerScene(menuScene);

        gameScene = new GameScene(appContext, this);
        registerScene(gameScene);

        gameOverScene = new GameOverScene(appContext, this);
        registerScene(gameOverScene);

        scoreboardScene = new ScoreboardScene(appContext, this);
        registerScene(scoreboardScene);

        SettingsScene settingsScene = new SettingsScene(appContext, this);
        registerScene(settingsScene);

        worldSelectScene = new WorldSelectScene(appContext, this);
        registerScene(worldSelectScene);

        if (selectedWorld == null) {
            selectedWorld = new WorldInfo(1, "Pointer Plains", "Startwelt, leicht & freundlich");
        }

        switchTo(SceneType.MENU);
    }

    private void registerScene(Scene scene) {
        scenes.put(scene.getType(), scene);
        if (surfaceWidth > 0 && surfaceHeight > 0) {
            scene.onSurfaceChanged(surfaceWidth, surfaceHeight);
        }
    }

    public void switchTo(SceneType type) {
        Scene nextScene = scenes.get(type);
        if (nextScene == null) {
            return;
        }
        if (currentScene != null) {
            currentScene.onExit();
        }
        currentScene = nextScene;
        currentScene.onEnter();
    }

    public void startNewGame() {
        if (selectedWorld == null) {
            selectedWorld = new WorldInfo(1, "Pointer Plains", "Startwelt, leicht & freundlich");
        }
        int worldNumber = selectedWorld.getProgramNumber();
        if (MenuIntegration.launchGameActivity(worldNumber, 1)) {
            return;
        }
        if (gameScene != null) {
            gameScene.resetForNewRun();
        }
        switchTo(SceneType.GAME);
    }

    public void startWorld(WorldInfo worldInfo) {
        if (worldInfo != null) {
            selectedWorld = worldInfo;
        }
        startNewGame();
    }

    public void showWorldSelect() {
        switchTo(SceneType.WORLD_SELECT);
    }

    public void showScoreboard() {
        switchTo(SceneType.SCOREBOARD);
    }

    public void showSettings() {
        switchTo(SceneType.SETTINGS);
    }

    public void showGameOver(GameResult result) {
        if (result != null && result.isVictory()) {
            scoreboardManager.submitTime(result.getTimeSeconds());
        }
        if (gameOverScene != null) {
            gameOverScene.setResult(result);
        }
        switchTo(SceneType.GAME_OVER);
    }

    public void update(float deltaSeconds) {
        if (currentScene != null) {
            currentScene.update(deltaSeconds);
        }
    }

    public void draw(Canvas canvas) {
        if (currentScene != null) {
            currentScene.draw(canvas);
        }
    }

    public boolean onTouchEvent(MotionEvent event) {
        return currentScene != null && currentScene.onTouchEvent(event);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return currentScene != null && currentScene.onKeyDown(keyCode, event);
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return currentScene != null && currentScene.onKeyUp(keyCode, event);
    }

    public void onSurfaceChanged(int width, int height) {
        surfaceWidth = width;
        surfaceHeight = height;
        for (Scene scene : scenes.values()) {
            scene.onSurfaceChanged(width, height);
        }
    }

    public boolean onBackPressed() {
        return currentScene != null && currentScene.onBackPressed();
    }

    public Context getContext() {
        return appContext;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public GameAudioManager getAudioManager() {
        return audioManager;
    }

    public GameView getGameView() {
        return gameView;
    }

    public Scene getCurrentScene() {
        return currentScene;
    }

    public WorldInfo getSelectedWorld() {
        return selectedWorld;
    }
}
