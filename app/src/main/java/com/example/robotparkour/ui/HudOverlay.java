// app/src/main/java/com/example/robotparkour/ui/HudOverlay.java
package com.example.robotparkour.ui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.example.robotparkour.util.TimeFormatter;

/**
 * Heads-up display rendered on top of gameplay to show player state.
 */
public class HudOverlay {

    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public void draw(Canvas canvas,
                     int collectedCoins,
                     int totalCoins,
                     int lives,
                     float elapsedSeconds,
                     float fps) {
        float scale = canvas.getWidth() / 1080f;
        float clampedScale = Math.max(0.6f, scale);
        float barHeight = 64f * clampedScale;
        float barTop = canvas.getHeight() - barHeight;

        iconPaint.setStyle(Paint.Style.FILL);
        iconPaint.setColor(Color.parseColor("#0E639C"));
        canvas.drawRect(0, barTop, canvas.getWidth(), canvas.getHeight(), iconPaint);

        iconPaint.setColor(Color.parseColor("#1B4F72"));
        canvas.drawRect(0, barTop - 4f, canvas.getWidth(), barTop, iconPaint);

        textPaint.setColor(Color.parseColor("#F8F8F8"));
        float textSize = 28f * clampedScale;
        textPaint.setTextSize(textSize);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextAlign(Paint.Align.LEFT);

        float baseline = barTop + barHeight * 0.62f;
        float x = 32f * clampedScale;

        x = drawSegment(canvas, "CRobot.java", x, baseline, clampedScale);
        x = drawSplitter(canvas, x, barTop, barHeight * 0.6f, clampedScale);

        String coinsText = "COINS " + collectedCoins + "/" + totalCoins;
        x = drawSegment(canvas, coinsText, x, baseline, clampedScale);
        x = drawSplitter(canvas, x, barTop, barHeight * 0.6f, clampedScale);

        String timeText = "TIME " + TimeFormatter.format(elapsedSeconds);
        x = drawSegment(canvas, timeText, x, baseline, clampedScale);
        x = drawSplitter(canvas, x, barTop, barHeight * 0.6f, clampedScale);

        String livesText = "LIVES";
        x = drawSegment(canvas, livesText, x, baseline, clampedScale);

        float iconSize = 18f * clampedScale;
        float iconSpacing = 8f * clampedScale;
        float iconX = x + iconSpacing;
        float iconY = baseline - iconSize * 0.8f;
        for (int i = 0; i < Math.max(lives, 1); i++) {
            iconPaint.setColor(i < lives ? Color.parseColor("#F48771") : Color.parseColor("#144B6C"));
            canvas.drawCircle(iconX + i * (iconSize + iconSpacing), iconY, iconSize * 0.5f, iconPaint);
        }

        textPaint.setTextAlign(Paint.Align.RIGHT);
        String fpsText = String.format("FPS %.0f", fps);
        canvas.drawText(fpsText, canvas.getWidth() - 32f * clampedScale, baseline, textPaint);

        textPaint.setTextAlign(Paint.Align.LEFT);
    }

    private float drawSegment(Canvas canvas, String text, float startX, float baseline, float scale) {
        float width = textPaint.measureText(text);
        canvas.drawText(text, startX, baseline, textPaint);
        return startX + width + 32f * scale;
    }

    private float drawSplitter(Canvas canvas, float x, float barTop, float height, float scale) {
        iconPaint.setStyle(Paint.Style.FILL);
        iconPaint.setColor(Color.parseColor("#F8F8F8"));
        float center = x + 8f * scale;
        canvas.drawRect(center, barTop + 12f * scale, center + 2f, barTop + 12f * scale + height, iconPaint);
        return center + 18f * scale;
    }
}
