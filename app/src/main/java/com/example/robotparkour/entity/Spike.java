// app/src/main/java/com/example/robotparkour/entity/Spike.java
package com.example.robotparkour.entity;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Path;

/**
 * Hazard tile represented as a red syntax error marker.
 */
public class Spike extends GameObject {

    private final Path path = new Path();

    public Spike(float x, float y, float size) {
        super(x, y, size, size);
    }

    @Override
    public void draw(Canvas canvas, Paint paint) {
        Paint.Style originalStyle = paint.getStyle();
        int originalColor = paint.getColor();
        Align originalAlign = paint.getTextAlign();
        float originalTextSize = paint.getTextSize();

        path.reset();
        path.moveTo(bounds.centerX(), bounds.top);
        path.lineTo(bounds.left, bounds.bottom);
        path.lineTo(bounds.right, bounds.bottom);
        path.close();

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#EB5757"));
        canvas.drawPath(path, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(bounds.height() * 0.5f);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("!", bounds.centerX(), bounds.centerY() + paint.getTextSize() * 0.35f, paint);

        paint.setStyle(originalStyle);
        paint.setColor(originalColor);
        paint.setTextAlign(originalAlign);
        paint.setTextSize(originalTextSize);
    }
}
