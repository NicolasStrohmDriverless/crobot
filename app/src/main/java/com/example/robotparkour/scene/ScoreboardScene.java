// app/src/main/java/com/example/robotparkour/scene/ScoreboardScene.java
package com.example.robotparkour.scene;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import com.crobot.game.level.LevelCatalog;
import com.crobot.game.level.LevelDescriptor;
import com.example.robotparkour.core.Scene;
import com.example.robotparkour.core.SceneManager;
import com.example.robotparkour.core.SceneType;
import com.example.robotparkour.storage.ScoreboardManager;
import com.example.robotparkour.util.TimeFormatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Shows the best run times stored in {@link ScoreboardManager}.
 */
public class ScoreboardScene implements Scene {

    private final SceneManager sceneManager;
    private final ScoreboardManager scoreboardManager;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF backButton = new RectF();
    private final float touchSlop;

    private int surfaceWidth;
    private int surfaceHeight;
    private final List<LevelDescriptor> descriptors = new ArrayList<>();
    private Map<Integer, Float> bestTimes = new HashMap<>();
    private float scrollOffset;
    private float maxScroll;
    private boolean dragging;
    private float lastTouchY;
    private float accumulatedDrag;

    public ScoreboardScene(Context context, SceneManager sceneManager) {
        this.sceneManager = sceneManager;
        this.scoreboardManager = sceneManager.getScoreboardManager();
        this.touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    @Override
    public SceneType getType() {
        return SceneType.SCOREBOARD;
    }

    @Override
    public void onEnter() {
        descriptors.clear();
        descriptors.addAll(LevelCatalog.getInstance(sceneManager.getContext()).getDescriptors());
        bestTimes = scoreboardManager.getAllBestTimes();
        scrollOffset = 0f;
        dragging = false;
        accumulatedDrag = 0f;
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
        paint.setColor(Color.parseColor("#12263F"));
        canvas.drawRect(0, 0, surfaceWidth, surfaceHeight, paint);

        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(surfaceWidth * 0.07f);
        canvas.drawText("Bestzeiten", surfaceWidth / 2f, surfaceHeight * 0.18f, paint);

        float startY = surfaceHeight * 0.3f;
        paint.setTextSize(surfaceWidth * 0.042f);
        float lineHeight = paint.getTextSize() * 1.6f;
        float leftX = surfaceWidth * 0.16f;
        float rightX = surfaceWidth * 0.84f;
        paint.setTextAlign(Paint.Align.LEFT);

        float listBottom = backButton.top - surfaceHeight * 0.06f;
        float availableHeight = Math.max(0f, listBottom - startY);
        float contentHeight = descriptors.size() * lineHeight;
        maxScroll = Math.max(0f, contentHeight - availableHeight);
        scrollOffset = clamp(scrollOffset, 0f, maxScroll);
        float drawY = startY - scrollOffset;

        if (descriptors.isEmpty()) {
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Keine Welten gefunden.", surfaceWidth / 2f, drawY, paint);
            paint.setTextAlign(Paint.Align.LEFT);
        } else {
            for (int i = 0; i < descriptors.size(); i++) {
                LevelDescriptor descriptor = descriptors.get(i);
                String title = String.format(Locale.getDefault(), "%d. %s", descriptor.getWorldNumber(), descriptor.getWorldInfo().getName());
                float y = drawY + i * lineHeight;
                if (y < startY - lineHeight || y > listBottom + lineHeight) {
                    continue;
                }
                paint.setTextAlign(Paint.Align.LEFT);
                canvas.drawText(title, leftX, y, paint);
                paint.setTextAlign(Paint.Align.RIGHT);
                Float best = bestTimes != null ? bestTimes.get(descriptor.getWorldNumber()) : null;
                String timeText = best != null ? TimeFormatter.format(best) : "—";
                canvas.drawText(timeText, rightX, y, paint);
            }
            paint.setTextAlign(Paint.Align.LEFT);
        }

        drawButton(canvas, backButton, "Back");
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
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchY = event.getY();
                accumulatedDrag = 0f;
                dragging = true;
                return true;
            case MotionEvent.ACTION_MOVE:
                if (!dragging) {
                    return false;
                }
                float newY = event.getY();
                float dy = newY - lastTouchY;
                lastTouchY = newY;
                scrollOffset = clamp(scrollOffset - dy, 0f, maxScroll);
                accumulatedDrag += Math.abs(dy);
                return true;
            case MotionEvent.ACTION_UP:
                float x = event.getX();
                float y = event.getY();
                boolean wasClick = accumulatedDrag < touchSlop;
                dragging = false;
                if (wasClick && backButton.contains(x, y)) {
                    sceneManager.switchTo(SceneType.MENU);
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                dragging = false;
                return true;
            default:
                return false;
        }
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
        float buttonWidth = width * 0.25f;
        float buttonHeight = height * 0.09f;
        float centerX = width / 2f;
        backButton.set(centerX - buttonWidth / 2f, height * 0.8f, centerX + buttonWidth / 2f, height * 0.8f + buttonHeight);
    }

    @Override
    public boolean onBackPressed() {
        sceneManager.switchTo(SceneType.MENU);
        return true;
    }

    private float clamp(float value, float min, float max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}
