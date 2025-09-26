// app/src/main/java/com/example/robotparkour/entity/Flag.java
package com.example.robotparkour.entity;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

/**
 * Finish flag styled as a green "Run" button.
 */
public class Flag extends GameObject {

    private final Path trianglePath = new Path();
    private boolean activated;

    public Flag(float x, float y, float size) {
        super(x, y, size, size);
    }

    public void activate() {
        activated = true;
    }

    public void reset() {
        activated = false;
    }

    public boolean isActivated() {
        return activated;
    }

    @Override
    public void draw(Canvas canvas, Paint paint) {
        Paint.Style originalStyle = paint.getStyle();
        int originalColor = paint.getColor();

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(activated ? Color.parseColor("#6FCF97") : Color.parseColor("#27AE60"));
        canvas.drawRoundRect(bounds, 8f, 8f, paint);

        trianglePath.reset();
        trianglePath.moveTo(bounds.left + bounds.width() * 0.4f, bounds.top + bounds.height() * 0.3f);
        trianglePath.lineTo(bounds.right - bounds.width() * 0.35f, bounds.centerY());
        trianglePath.lineTo(bounds.left + bounds.width() * 0.4f, bounds.bottom - bounds.height() * 0.3f);
        trianglePath.close();
        paint.setColor(Color.WHITE);
        canvas.drawPath(trianglePath, paint);

        paint.setStyle(originalStyle);
        paint.setColor(originalColor);
    }
}
