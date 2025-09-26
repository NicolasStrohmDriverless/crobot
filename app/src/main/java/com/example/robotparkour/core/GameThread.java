// app/src/main/java/com/example/robotparkour/core/GameThread.java
package com.example.robotparkour.core;

import android.graphics.Canvas;
import android.util.Log;
import android.view.SurfaceHolder;

/**
 * Dedicated render/update thread that drives the game at a fixed timestep (~60 FPS).
 * The implementation uses a standard accumulator loop so the simulation step remains
 * deterministic even when rendering hiccups occur.
 */
public class GameThread extends Thread {

    private static final String TAG = "GameThread";
    private static final double TARGET_FPS = 60.0;
    private static final double STEP_SECONDS = 1.0 / TARGET_FPS;

    private final SurfaceHolder surfaceHolder;
    private final GameView gameView;

    private volatile boolean running = false;

    public GameThread(SurfaceHolder surfaceHolder, GameView gameView) {
        this.surfaceHolder = surfaceHolder;
        this.gameView = gameView;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    @Override
    public void run() {
        double accumulator = 0.0;
        long previousTime = System.nanoTime();

        while (running) {
            long currentTime = System.nanoTime();
            double frameTime = (currentTime - previousTime) / 1_000_000_000.0;
            if (frameTime > 0.25) {
                // Clamp ridiculously long pauses (e.g., debugger attached) to keep the simulation stable.
                frameTime = 0.25;
            }
            previousTime = currentTime;
            accumulator += frameTime;

            while (accumulator >= STEP_SECONDS) {
                gameView.update((float) STEP_SECONDS);
                accumulator -= STEP_SECONDS;
            }

            Canvas canvas = null;
            try {
                canvas = surfaceHolder.lockCanvas();
                if (canvas != null) {
                    gameView.render(canvas);
                }
            } catch (IllegalArgumentException exception) {
                Log.w(TAG, "Unable to lock canvas", exception);
            } finally {
                if (canvas != null) {
                    try {
                        surfaceHolder.unlockCanvasAndPost(canvas);
                        gameView.onFramePresented();
                    } catch (IllegalStateException unlockException) {
                        Log.w(TAG, "Failed to unlock canvas", unlockException);
                    }
                }
            }

            double sleepTime = STEP_SECONDS - accumulator;
            if (sleepTime > 0.0) {
                try {
                    Thread.sleep((long) (sleepTime * 1000));
                } catch (InterruptedException ignored) {
                    // Restore interrupt so higher-level code can react if needed.
                    interrupt();
                }
            }
        }
    }
}
