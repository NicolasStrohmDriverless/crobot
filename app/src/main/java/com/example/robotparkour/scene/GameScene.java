// app/src/main/java/com/example/robotparkour/scene/GameScene.java
package com.example.robotparkour.scene;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.example.robotparkour.audio.GameAudioManager;
import com.example.robotparkour.audio.WorldMusicLibrary;
import com.example.robotparkour.core.Scene;
import com.example.robotparkour.core.SceneManager;
import com.example.robotparkour.core.SceneType;
import com.example.robotparkour.entity.Coin;
import com.example.robotparkour.entity.Flag;
import com.example.robotparkour.entity.Robot;
import com.example.robotparkour.entity.Spike;
import com.example.robotparkour.entity.Tile;
import com.example.robotparkour.level.Level;
import com.example.robotparkour.ui.Camera2D;
import com.example.robotparkour.ui.HudOverlay;
import com.example.robotparkour.ui.VirtualButton;
import com.example.robotparkour.util.GameResult;

import java.util.List;

/**
 * Core gameplay scene that manages entities, physics, and rendering.
 */
public class GameScene implements Scene {

    private static final String[] LEVEL_DATA = new String[] {
            "................................",
            "..............C.................",
            "..........GGGGGGG...............",
            "..R......................F......",
            "GGGGGGGGGBBBBQQQGGGGGGGGGGGGGGG"
    };

    private static final int INITIAL_LIVES = 3;

    private final SceneManager sceneManager;
    private final GameAudioManager audioManager;
    private final Paint worldPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint uiPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final HudOverlay hud = new HudOverlay();
    private final Camera2D camera = new Camera2D();
    private final VirtualButton leftButton = new VirtualButton("←");
    private final VirtualButton rightButton = new VirtualButton("→");
    private final VirtualButton jumpButton = new VirtualButton("⤒");

    private final Level level;
    private final Robot robot;
    private final List<Tile> tiles;
    private final List<Coin> coins;
    private final List<Spike> spikes;
    private final Flag flag;

    private float elapsedSeconds;
    private int collectedCoins;
    private int lives;
    private boolean running;

    private boolean leftKeyDown;
    private boolean rightKeyDown;
    private boolean jumpKeyDown;
    private boolean jumpQueued;
    private boolean jumpButtonPreviouslyPressed;

    private int surfaceWidth;
    private int surfaceHeight;
    private float parallaxTimer;

    public GameScene(Context context, SceneManager sceneManager) {
        this.sceneManager = sceneManager;
        this.audioManager = sceneManager.getAudioManager();
        this.level = Level.fromStringMap(LEVEL_DATA, Level.TILE_SIZE);
        float robotWidth = Level.TILE_SIZE * 0.8f;
        float robotHeight = Level.TILE_SIZE * 0.95f;
        this.robot = new Robot(level.getSpawnX(), level.getSpawnY() - (robotHeight - Level.TILE_SIZE), robotWidth, robotHeight);
        this.tiles = level.getTiles();
        this.coins = level.getCoins();
        this.spikes = level.getSpikes();
        this.flag = level.getFlag();
        resetForNewRun();
    }

    @Override
    public SceneType getType() {
        return SceneType.GAME;
    }

    public void resetForNewRun() {
        audioManager.setMusicTrack(WorldMusicLibrary.getTrackFor(sceneManager.getSelectedWorld()));
        elapsedSeconds = 0f;
        collectedCoins = 0;
        lives = INITIAL_LIVES;
        running = true;
        jumpQueued = false;
        leftKeyDown = false;
        rightKeyDown = false;
        jumpKeyDown = false;
        jumpButtonPreviouslyPressed = false;
        robot.setLives(INITIAL_LIVES);
        robot.setSpawn(level.getSpawnX(), level.getSpawnY() - (robot.getBounds().height() - Level.TILE_SIZE));
        robot.resetToSpawn();
        for (Coin coin : coins) {
            coin.reset();
        }
        if (flag != null) {
            flag.reset();
        }
        audioManager.startMusic();
        if (surfaceWidth > 0) {
            camera.snapTo(Math.max(0f, robot.getX() - surfaceWidth / 2f), 0f);
        } else {
            camera.snapTo(0f, 0f);
        }
    }

    @Override
    public void onEnter() {
        running = true;
        audioManager.setMusicTrack(WorldMusicLibrary.getTrackFor(sceneManager.getSelectedWorld()));
        audioManager.startMusic();
    }

    @Override
    public void onExit() {
        audioManager.stopMusic();
    }

    @Override
    public void update(float deltaSeconds) {
        parallaxTimer += deltaSeconds;
        if (!running) {
            return;
        }
        elapsedSeconds += deltaSeconds;

        boolean moveLeft = leftKeyDown || leftButton.isPressed();
        boolean moveRight = rightKeyDown || rightButton.isPressed();
        boolean jumpFromButton = jumpButton.isPressed();
        if (jumpFromButton && !jumpButtonPreviouslyPressed) {
            jumpQueued = true;
        }
        jumpButtonPreviouslyPressed = jumpFromButton;

        boolean jumpRequest = jumpQueued;
        jumpQueued = false;
        boolean wasGrounded = robot.isGrounded();
        boolean shouldJump = jumpRequest || (jumpKeyDown && wasGrounded);
        robot.update(level, deltaSeconds, moveLeft, moveRight, shouldJump);
        if (shouldJump && wasGrounded) {
            audioManager.playJump();
        }

        for (Coin coin : coins) {
            coin.update(deltaSeconds);
            if (!coin.isCollected() && RectF.intersects(robot.getBounds(), coin.getBounds())) {
                coin.collect();
                collectedCoins++;
                audioManager.playCoin();
            }
        }

        for (Spike spike : spikes) {
            if (RectF.intersects(robot.getBounds(), spike.getBounds())) {
                handlePlayerHitHazard();
                break;
            }
        }

        if (flag != null && !flag.isActivated() && RectF.intersects(robot.getBounds(), flag.getBounds())) {
            flag.activate();
            finishRun(true);
        }

        if (robot.getY() > level.getPixelHeight() + Level.TILE_SIZE) {
            handlePlayerHitHazard();
        }

        camera.follow(robot);
    }

    private void handlePlayerHitHazard() {
        audioManager.playError();
        robot.loseLife();
        lives = robot.getLives();
        if (lives <= 0) {
            finishRun(false);
        } else if (surfaceWidth > 0) {
            camera.snapTo(Math.max(0f, robot.getX() - surfaceWidth / 2f), 0f);
        }
    }

    private void finishRun(boolean victory) {
        running = false;
        audioManager.stopMusic();
        GameResult result = new GameResult(elapsedSeconds, collectedCoins, victory, Math.max(0, lives));
        sceneManager.showGameOver(result);
    }

    @Override
    public void draw(Canvas canvas) {
        drawParallaxBackground(canvas);
        canvas.save();
        canvas.translate(-camera.getX(), -camera.getY());
        drawLevel(canvas);
        canvas.restore();

        drawControls(canvas);
        hud.draw(canvas, collectedCoins, coins.size(), lives, elapsedSeconds, sceneManager.getGameView().getFps());
    }

    private void drawParallaxBackground(Canvas canvas) {
        if (surfaceWidth <= 0 || surfaceHeight <= 0) {
            return;
        }
        uiPaint.setStyle(Paint.Style.FILL);
        uiPaint.setColor(Color.parseColor("#1E1E1E"));
        canvas.drawRect(0, 0, surfaceWidth, surfaceHeight, uiPaint);

        // Activity bar on the left
        float activityWidth = surfaceWidth * 0.08f;
        uiPaint.setColor(Color.parseColor("#252526"));
        canvas.drawRect(0, 0, activityWidth, surfaceHeight, uiPaint);

        uiPaint.setColor(Color.parseColor("#007ACC"));
        float iconSpacing = 64f;
        for (int i = 0; i < 6; i++) {
            float cy = 120f + i * iconSpacing;
            canvas.drawCircle(activityWidth * 0.5f, cy, 12f, uiPaint);
        }

        // Title bar across the top
        float titleHeight = surfaceHeight * 0.07f;
        uiPaint.setColor(Color.parseColor("#3C3C3C"));
        canvas.drawRect(0, 0, surfaceWidth, titleHeight, uiPaint);
        uiPaint.setColor(Color.parseColor("#2D2D2D"));
        canvas.drawRect(activityWidth, 0, surfaceWidth, titleHeight, uiPaint);

        uiPaint.setColor(Color.parseColor("#CCCCCC"));
        uiPaint.setTextAlign(Paint.Align.LEFT);
        uiPaint.setTextSize(28f);
        canvas.drawText("CRobot.java", activityWidth + 24f, titleHeight * 0.7f, uiPaint);

        // File explorer panel
        float explorerWidth = surfaceWidth * 0.18f;
        float explorerLeft = activityWidth;
        uiPaint.setColor(Color.parseColor("#1F1F23"));
        canvas.drawRect(explorerLeft, titleHeight, explorerLeft + explorerWidth, surfaceHeight, uiPaint);

        uiPaint.setColor(Color.parseColor("#3A3D41"));
        canvas.drawRect(explorerLeft, titleHeight, explorerLeft + explorerWidth, titleHeight + 56f, uiPaint);
        uiPaint.setColor(Color.parseColor("#CCCCCC"));
        uiPaint.setTextSize(24f);
        canvas.drawText("EXPLORER", explorerLeft + 24f, titleHeight + 36f, uiPaint);

        uiPaint.setColor(Color.parseColor("#C5C5C5"));
        float fileLineHeight = 34f;
        float explorerScroll = (parallaxTimer * 20f) % fileLineHeight;
        for (int i = 0; i < 16; i++) {
            float y = titleHeight + 64f + i * fileLineHeight + explorerScroll;
            float right = explorerLeft + explorerWidth - 16f;
            canvas.drawLine(explorerLeft + 18f, y, right, y, uiPaint);
        }

        // Editor surface
        float editorLeft = explorerLeft + explorerWidth;
        float editorTop = titleHeight;
        uiPaint.setColor(Color.parseColor("#1E1E1E"));
        canvas.drawRect(editorLeft, editorTop, surfaceWidth, surfaceHeight, uiPaint);

        // Animated code lines
        float lineHeight = 24f;
        float scrolling = (parallaxTimer * 45f) % lineHeight;
        uiPaint.setStrokeWidth(2f);
        for (int i = 0; i < (surfaceHeight / lineHeight) + 4; i++) {
            float y = editorTop + 40f + i * lineHeight + scrolling;
            if (y > surfaceHeight - 60f) {
                continue;
            }
            float indent = (float) (Math.sin((parallaxTimer + i) * 0.8f) * 24f);
            uiPaint.setColor(Color.parseColor("#264F78"));
            canvas.drawLine(editorLeft + 32f + indent, y, surfaceWidth - 32f, y, uiPaint);
        }

        // Highlighted current line indicator
        uiPaint.setColor(Color.parseColor("#094771"));
        float highlightY = editorTop + surfaceHeight * 0.25f + (float) Math.sin(parallaxTimer) * 16f;
        canvas.drawRect(editorLeft + 24f, highlightY, surfaceWidth - 32f, highlightY + 26f, uiPaint);
    }

    private void drawLevel(Canvas canvas) {
        for (Tile tile : tiles) {
            tile.draw(canvas, worldPaint);
        }
        for (Coin coin : coins) {
            coin.draw(canvas, worldPaint);
        }
        for (Spike spike : spikes) {
            spike.draw(canvas, worldPaint);
        }
        if (flag != null) {
            flag.draw(canvas, worldPaint);
        }
        robot.draw(canvas, worldPaint);
    }

    private void drawControls(Canvas canvas) {
        uiPaint.setStyle(Paint.Style.FILL);
        uiPaint.setColor(Color.argb(200, 30, 30, 30));
        canvas.drawRect(0, surfaceHeight * 0.7f, surfaceWidth, surfaceHeight, uiPaint);

        uiPaint.setColor(Color.argb(220, 0, 122, 204));
        canvas.drawRect(0, surfaceHeight * 0.7f, surfaceWidth, surfaceHeight * 0.7f + 4f, uiPaint);

        leftButton.draw(canvas, uiPaint);
        rightButton.draw(canvas, uiPaint);
        jumpButton.draw(canvas, uiPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
                updateVirtualButtons(event);
                break;
            default:
                break;
        }
        return true;
    }

    private void updateVirtualButtons(MotionEvent event) {
        leftButton.setPressed(false);
        rightButton.setPressed(false);
        jumpButton.setPressed(false);
        for (int i = 0; i < event.getPointerCount(); i++) {
            float x = event.getX(i);
            float y = event.getY(i);
            if (leftButton.contains(x, y)) {
                leftButton.setPressed(true);
            }
            if (rightButton.contains(x, y)) {
                rightButton.setPressed(true);
            }
            if (jumpButton.contains(x, y)) {
                jumpButton.setPressed(true);
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            leftKeyDown = true;
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            rightKeyDown = true;
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_SPACE) {
            if (!jumpKeyDown) {
                jumpQueued = true;
            }
            jumpKeyDown = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            leftKeyDown = false;
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            rightKeyDown = false;
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_SPACE) {
            jumpKeyDown = false;
            return true;
        }
        return false;
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        surfaceWidth = width;
        surfaceHeight = height;
        camera.setViewport(width, height);
        camera.setWorldSize(level.getPixelWidth(), Math.max(level.getPixelHeight(), height));

        float buttonSize = height * 0.18f;
        float margin = 32f;
        leftButton.setBounds(margin, height - buttonSize - margin, margin + buttonSize, height - margin);
        rightButton.setBounds(margin + buttonSize + 24f, height - buttonSize - margin, margin + buttonSize * 2f + 24f, height - margin);
        jumpButton.setBounds(width - buttonSize - margin, height - buttonSize - margin, width - margin, height - margin);
        if (running) {
            camera.snapTo(Math.max(0f, robot.getX() - width / 2f), Math.max(0f, robot.getY() - height / 2f));
        }
    }

    @Override
    public boolean onBackPressed() {
        sceneManager.switchTo(SceneType.MENU);
        return true;
    }
}
