// app/src/main/java/com/example/robotparkour/entity/Coin.java
package com.example.robotparkour.entity;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

/**
 * Collectible coin shaped like a golden pair of curly braces.
 */
public class Coin extends GameObject {

    private boolean collected;
    private float animationTimer;

    public Coin(float x, float y, float size) {
        super(x, y, size, size);
    }

    public void update(float deltaSeconds) {
        if (!collected) {
            animationTimer += deltaSeconds;
        }
    }

    public boolean isCollected() {
        return collected;
    }

    public void collect() {
        collected = true;
    }

    public void reset() {
        collected = false;
        animationTimer = 0f;
    }

    @Override
    public void draw(Canvas canvas, Paint paint) {
        if (collected) {
            return;
        }
        Paint.Style originalStyle = paint.getStyle();
        int originalColor = paint.getColor();
        float originalStroke = paint.getStrokeWidth();

        float wobble = (float) Math.sin(animationTimer * 6.0f) * 2f;
        float top = bounds.top + wobble;
        float bottom = bounds.bottom + wobble;

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
        paint.setColor(Color.parseColor("#FFD166"));
        float braceHeight = bottom - top;
        float braceWidth = braceHeight * 0.4f;
        RectF leftArc = new RectF(bounds.centerX() - braceWidth * 1.5f, top, bounds.centerX() - braceWidth * 0.5f, bottom);
        RectF rightArc = new RectF(bounds.centerX() + braceWidth * 0.5f, top, bounds.centerX() + braceWidth * 1.5f, bottom);
        canvas.drawArc(leftArc, 110, 140, false, paint);
        canvas.drawArc(rightArc, -70, 140, false, paint);

        paint.setStyle(originalStyle);
        paint.setColor(originalColor);
        paint.setStrokeWidth(originalStroke);
    }
}
