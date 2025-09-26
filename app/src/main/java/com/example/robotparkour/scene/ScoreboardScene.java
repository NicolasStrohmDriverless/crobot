// app/src/main/java/com/example/robotparkour/scene/ScoreboardScene.java
package com.example.robotparkour.scene;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.example.robotparkour.core.Scene;
import com.example.robotparkour.core.SceneManager;
import com.example.robotparkour.core.SceneType;
import com.example.robotparkour.storage.ScoreboardManager;
import com.example.robotparkour.util.TimeFormatter;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows the best run times stored in {@link ScoreboardManager}.
 */
public class ScoreboardScene implements Scene {

    private final SceneManager sceneManager;
    private final ScoreboardManager scoreboardManager;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF backButton = new RectF();

    private int surfaceWidth;
    private int surfaceHeight;
    private List<Float> cachedTimes = new ArrayList<>();

    public ScoreboardScene(Context context, SceneManager sceneManager) {
        this.sceneManager = sceneManager;
        this.scoreboardManager = sceneManager.getScoreboardManager();
    }

    @Override
    public SceneType getType() {
        return SceneType.SCOREBOARD;
    }

    @Override
    public void onEnter() {
        cachedTimes = scoreboardManager.getTopTimes();
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
        canvas.drawText("Top Times", surfaceWidth / 2f, surfaceHeight * 0.18f, paint);

        paint.setTextSize(surfaceWidth * 0.04f);
        float startY = surfaceHeight * 0.3f;
        float lineHeight = paint.getTextSize() * 1.4f;
        if (cachedTimes.isEmpty()) {
            canvas.drawText("Run the parkour to record your best time!", surfaceWidth / 2f, startY, paint);
        } else {
            for (int i = 0; i < cachedTimes.size(); i++) {
                String line = String.format("%d. %s", i + 1, TimeFormatter.format(cachedTimes.get(i)));
                canvas.drawText(line, surfaceWidth / 2f, startY + i * lineHeight, paint);
            }
        }

        drawButton(canvas, backButton, "Back");
    }

    private void drawButton(Canvas canvas, RectF bounds, String text) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#4A90E2"));
        canvas.drawRoundRect(bounds, 24f, 24f, paint);
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(bounds.height() * 0.45f);
        canvas.drawText(text, bounds.centerX(), bounds.centerY() + paint.getTextSize() * 0.3f, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            float x = event.getX();
            float y = event.getY();
            if (backButton.contains(x, y)) {
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
        float buttonWidth = width * 0.5f;
        float buttonHeight = height * 0.09f;
        float centerX = width / 2f;
        backButton.set(centerX - buttonWidth / 2f, height * 0.8f, centerX + buttonWidth / 2f, height * 0.8f + buttonHeight);
    }

    @Override
    public boolean onBackPressed() {
        sceneManager.switchTo(SceneType.MENU);
        return true;
    }
}
