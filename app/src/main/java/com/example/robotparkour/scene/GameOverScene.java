// app/src/main/java/com/example/robotparkour/scene/GameOverScene.java
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
import com.example.robotparkour.util.GameResult;
import com.example.robotparkour.util.TimeFormatter;

/**
 * Post-run screen that celebrates a finish or shows defeat.
 */
public class GameOverScene implements Scene {

    private final SceneManager sceneManager;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF retryButton = new RectF();
    private final RectF menuButton = new RectF();
    private final RectF scoresButton = new RectF();

    private GameResult result;
    private int surfaceWidth;
    private int surfaceHeight;

    public GameOverScene(Context context, SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    public void setResult(GameResult result) {
        this.result = result;
    }

    @Override
    public SceneType getType() {
        return SceneType.GAME_OVER;
    }

    @Override
    public void onEnter() {
        sceneManager.getAudioManager().stopMusic();
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
        paint.setColor(Color.parseColor("#1B2735"));
        canvas.drawRect(0, 0, surfaceWidth, surfaceHeight, paint);

        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(surfaceWidth * 0.08f);
        String title = (result != null && result.isVictory()) ? "Run Complete!" : "Robot Down";
        canvas.drawText(title, surfaceWidth / 2f, surfaceHeight * 0.22f, paint);

        paint.setTextSize(surfaceWidth * 0.045f);
        float infoY = surfaceHeight * 0.35f;
        if (result != null) {
            canvas.drawText("Time: " + TimeFormatter.format(result.getTimeSeconds()), surfaceWidth / 2f, infoY, paint);
            canvas.drawText("Coins: " + result.getCoinsCollected(), surfaceWidth / 2f, infoY + paint.getTextSize() * 1.4f, paint);
            canvas.drawText("Lives Left: " + result.getLivesRemaining(), surfaceWidth / 2f, infoY + paint.getTextSize() * 2.8f, paint);
        }

        drawButton(canvas, retryButton, "Retry");
        drawButton(canvas, menuButton, "Menu");
        drawButton(canvas, scoresButton, "Scoreboard");
    }

    private void drawButton(Canvas canvas, RectF bounds, String text) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#4A90E2"));
        canvas.drawRoundRect(bounds, 24f, 24f, paint);
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(bounds.height() * 0.4f);
        canvas.drawText(text, bounds.centerX(), bounds.centerY() + paint.getTextSize() * 0.3f, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            float x = event.getX();
            float y = event.getY();
            if (retryButton.contains(x, y)) {
                sceneManager.startNewGame();
                return true;
            } else if (menuButton.contains(x, y)) {
                sceneManager.switchTo(SceneType.MENU);
                return true;
            } else if (scoresButton.contains(x, y)) {
                sceneManager.showScoreboard();
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
        float firstY = height * 0.6f;
        retryButton.set(centerX - buttonWidth / 2f, firstY, centerX + buttonWidth / 2f, firstY + buttonHeight);
        menuButton.set(centerX - buttonWidth / 2f, firstY + buttonHeight * 1.2f, centerX + buttonWidth / 2f, firstY + buttonHeight * 2.2f);
        scoresButton.set(centerX - buttonWidth / 2f, firstY + buttonHeight * 2.4f, centerX + buttonWidth / 2f, firstY + buttonHeight * 3.4f);
    }

    @Override
    public boolean onBackPressed() {
        sceneManager.switchTo(SceneType.MENU);
        return true;
    }
}
