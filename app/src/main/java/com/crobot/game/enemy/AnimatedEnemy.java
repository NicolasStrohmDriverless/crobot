package com.crobot.game.enemy;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

/**
 * Simple procedural sprite that provides a looping animation for enemies.
 */
public final class AnimatedEnemy {

    private final Paint bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint accentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float bobAmplitude;
    private final float animationSpeed;
    private final float accentScale;
    private final boolean horizontalWave;

    private float timer;

    public AnimatedEnemy(@ColorInt int bodyColor,
                         @ColorInt int accentColor,
                         float bobAmplitude,
                         float animationSpeed,
                         float accentScale,
                         boolean horizontalWave) {
        bodyPaint.setColor(bodyColor);
        accentPaint.setColor(accentColor);
        this.bobAmplitude = bobAmplitude;
        this.animationSpeed = animationSpeed;
        this.accentScale = accentScale;
        this.horizontalWave = horizontalWave;
    }

    public void update(float deltaSeconds) {
        timer += deltaSeconds * animationSpeed;
    }

    public void draw(@NonNull Canvas canvas, @NonNull RectF bounds) {
        float wobble = (float) Math.sin(timer * 6.28318f) * bobAmplitude;
        RectF animatedBounds = new RectF(bounds);
        if (horizontalWave) {
            animatedBounds.left += wobble;
            animatedBounds.right += wobble;
        } else {
            animatedBounds.offset(0f, wobble);
        }
        canvas.drawRoundRect(animatedBounds, animatedBounds.height() * 0.35f,
                animatedBounds.height() * 0.35f, bodyPaint);

        float accentWidth = animatedBounds.width() * accentScale;
        float accentHeight = animatedBounds.height() * accentScale * 0.6f;
        float centerX = animatedBounds.centerX();
        float centerY = animatedBounds.centerY();
        RectF accent = new RectF(centerX - accentWidth / 2f,
                centerY - accentHeight / 2f,
                centerX + accentWidth / 2f,
                centerY + accentHeight / 2f);
        canvas.drawRoundRect(accent, accentHeight * 0.45f, accentHeight * 0.45f, accentPaint);
    }
}
