// app/src/main/java/com/example/robotparkour/core/GameView.java
package com.example.robotparkour.core;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.robotparkour.audio.GameAudioManager;
import com.example.robotparkour.storage.ScoreboardManager;

/**
 * Custom {@link SurfaceView} that owns the game loop thread and delegates
 * runtime behaviour to the active {@link Scene}.
 */
public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    private final Paint clearPaint = new Paint();
    private final ScoreboardManager scoreboardManager;
    private final GameAudioManager audioManager;
    private final SceneManager sceneManager;

    private GameThread gameThread;
    private float fps;
    private long fpsWindowStartNanos;
    private int framesRendered;

    public GameView(@NonNull Context context) {
        this(context, null);
    }

    public GameView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GameView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        clearPaint.setColor(Color.BLACK);
        getHolder().addCallback(this);
        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();

        scoreboardManager = new ScoreboardManager(context.getApplicationContext());
        audioManager = new GameAudioManager(context.getApplicationContext());
        sceneManager = new SceneManager(context.getApplicationContext(), this, scoreboardManager, audioManager);
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        startThreadIfNeeded();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        sceneManager.onSurfaceChanged(width, height);
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        stopThread();
    }

    private void startThreadIfNeeded() {
        if (gameThread == null || !gameThread.isAlive()) {
            gameThread = new GameThread(getHolder(), this);
            gameThread.setRunning(true);
            gameThread.start();
        }
    }

    private void stopThread() {
        if (gameThread != null) {
            gameThread.setRunning(false);
            boolean retry = true;
            while (retry) {
                try {
                    gameThread.join();
                    retry = false;
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
            gameThread = null;
        }
    }

    /**
     * Called by {@link GameThread} to run a single simulation update.
     */
    public void update(float deltaSeconds) {
        sceneManager.update(deltaSeconds);
    }

    /**
     * Called by {@link GameThread} whenever a new frame must be rendered.
     */
    public void render(Canvas canvas) {
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), clearPaint);
        sceneManager.draw(canvas);
    }

    /**
     * Tracks frames-per-second for HUD diagnostics.
     */
    public void onFramePresented() {
        framesRendered++;
        long now = System.nanoTime();
        if (fpsWindowStartNanos == 0L) {
            fpsWindowStartNanos = now;
        }
        long elapsed = now - fpsWindowStartNanos;
        if (elapsed >= 1_000_000_000L) {
            fps = framesRendered * 1_000_000_000f / elapsed;
            framesRendered = 0;
            fpsWindowStartNanos = now;
        }
    }

    public float getFps() {
        return fps;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return sceneManager.onTouchEvent(event) || super.onTouchEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return sceneManager.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return sceneManager.onKeyUp(keyCode, event) || super.onKeyUp(keyCode, event);
    }

    public void resumeGame() {
        startThreadIfNeeded();
        audioManager.onResume();
    }

    public void pauseGame() {
        audioManager.onPause();
        stopThread();
    }

    public void releaseResources() {
        audioManager.release();
    }

    public boolean handleBackPressed() {
        return sceneManager.onBackPressed();
    }

    public SceneManager getSceneManager() {
        return sceneManager;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public GameAudioManager getAudioManager() {
        return audioManager;
    }
}
