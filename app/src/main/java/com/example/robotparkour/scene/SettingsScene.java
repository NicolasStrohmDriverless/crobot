// app/src/main/java/com/example/robotparkour/scene/SettingsScene.java
package com.example.robotparkour.scene;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.example.robotparkour.audio.GameAudioManager;
import com.example.robotparkour.core.Scene;
import com.example.robotparkour.core.SceneManager;
import com.example.robotparkour.core.SceneType;

/**
 * Minimal settings page that toggles audio preferences.
 */
public class SettingsScene implements Scene {

    private final SceneManager sceneManager;
    private final GameAudioManager audioManager;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF soundToggle = new RectF();
    private final RectF musicToggle = new RectF();
    private final RectF backButton = new RectF();

    private int surfaceWidth;
    private int surfaceHeight;

    public SettingsScene(Context context, SceneManager sceneManager) {
        this.sceneManager = sceneManager;
        this.audioManager = sceneManager.getAudioManager();
    }

    @Override
    public SceneType getType() {
        return SceneType.SETTINGS;
    }

    @Override
    public void onEnter() {
        // Nothing specific beyond showing current toggles.
    }

    @Override
    public void onExit() {
        // No-op.
    }

    @Override
    public void update(float deltaSeconds) {
        // Static screen.
    }

    @Override
    public void draw(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#102542"));
        canvas.drawRect(0, 0, surfaceWidth, surfaceHeight, paint);

        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(surfaceWidth * 0.07f);
        canvas.drawText("Settings", surfaceWidth / 2f, surfaceHeight * 0.2f, paint);

        drawToggle(canvas, soundToggle, "Sound FX", audioManager.isSoundEnabled());
        drawToggle(canvas, musicToggle, "Music", audioManager.isMusicEnabled());
        drawButton(canvas, backButton, "Back");
    }

    private void drawToggle(Canvas canvas, RectF bounds, String label, boolean enabled) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#1F3B73"));
        canvas.drawRoundRect(bounds, 20f, 20f, paint);

        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTextSize(bounds.height() * 0.4f);
        canvas.drawText(label, bounds.left + 24f, bounds.centerY() + paint.getTextSize() * 0.3f, paint);

        float indicatorSize = bounds.height() * 0.5f;
        float indicatorLeft = bounds.right - indicatorSize - 24f;
        paint.setColor(enabled ? Color.parseColor("#27AE60") : Color.parseColor("#BDBDBD"));
        canvas.drawRoundRect(indicatorLeft, bounds.centerY() - indicatorSize / 2f, indicatorLeft + indicatorSize, bounds.centerY() + indicatorSize / 2f, 12f, 12f, paint);
    }

    private void drawButton(Canvas canvas, RectF bounds, String text) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#4A90E2"));
        canvas.drawRoundRect(bounds, 24f, 24f, paint);
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(bounds.height() * 0.55f);
        canvas.drawText(text, bounds.centerX(), bounds.centerY() + paint.getTextSize() * 0.3f, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            float x = event.getX();
            float y = event.getY();
            if (soundToggle.contains(x, y)) {
                audioManager.setSoundEnabled(!audioManager.isSoundEnabled());
                return true;
            } else if (musicToggle.contains(x, y)) {
                audioManager.setMusicEnabled(!audioManager.isMusicEnabled());
                return true;
            } else if (backButton.contains(x, y)) {
                sceneManager.switchTo(SceneType.MENU);
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        surfaceWidth = width;
        surfaceHeight = height;
        float toggleWidth = width * 0.7f;
        float toggleHeight = height * 0.1f;
        float centerX = width / 2f;
        float firstY = height * 0.35f;
        soundToggle.set(centerX - toggleWidth / 2f, firstY, centerX + toggleWidth / 2f, firstY + toggleHeight);
        musicToggle.set(centerX - toggleWidth / 2f, firstY + toggleHeight * 1.3f, centerX + toggleWidth / 2f, firstY + toggleHeight * 2.3f);
        float buttonWidth = toggleWidth * 0.5f;
        backButton.set(centerX - buttonWidth / 2f, height * 0.7f, centerX + buttonWidth / 2f, height * 0.7f + toggleHeight);
    }

    @Override
    public boolean onBackPressed() {
        sceneManager.switchTo(SceneType.MENU);
        return true;
    }
}
