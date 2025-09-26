// app/src/main/java/com/example/robotparkour/ui/VirtualButton.java
package com.example.robotparkour.ui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

/**
 * Basic on-screen button used for touch controls.
 */
public class VirtualButton {

    private final RectF bounds = new RectF();
    private final String label;

    private boolean pressed;

    public VirtualButton(String label) {
        this.label = label;
    }

    public void setBounds(float left, float top, float right, float bottom) {
        bounds.set(left, top, right, bottom);
    }

    public RectF getBounds() {
        return bounds;
    }

    public boolean contains(float x, float y) {
        return bounds.contains(x, y);
    }

    public void setPressed(boolean pressed) {
        this.pressed = pressed;
    }

    public boolean isPressed() {
        return pressed;
    }

    public void draw(Canvas canvas, Paint paint) {
        int originalColor = paint.getColor();
        Paint.Style originalStyle = paint.getStyle();
        float originalTextSize = paint.getTextSize();
        Paint.Align originalAlign = paint.getTextAlign();
        float originalStrokeWidth = paint.getStrokeWidth();

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(pressed ? Color.parseColor("#094771") : Color.parseColor("#333333"));
        canvas.drawRoundRect(bounds, 24f, 24f, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        paint.setColor(Color.parseColor("#007ACC"));
        canvas.drawRoundRect(bounds, 24f, 24f, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#F3F3F3"));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(bounds.height() * 0.5f);
        canvas.drawText(label, bounds.centerX(), bounds.centerY() + paint.getTextSize() * 0.32f, paint);

        paint.setColor(originalColor);
        paint.setStyle(originalStyle);
        paint.setTextSize(originalTextSize);
        paint.setTextAlign(originalAlign);
        paint.setStrokeWidth(originalStrokeWidth);
    }
}
