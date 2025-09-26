// app/src/main/java/com/example/robotparkour/entity/Tile.java
package com.example.robotparkour.entity;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.RectF;

import com.example.robotparkour.level.TileType;

/**
 * Static tile representing one 32x32 IDE-themed block.
 */
public class Tile extends GameObject {

    private static final float CORNER_RADIUS = 4f;

    private final TileType tileType;

    public Tile(float x, float y, float size, TileType tileType) {
        super(x, y, size, size);
        this.tileType = tileType;
    }

    public TileType getTileType() {
        return tileType;
    }

    public boolean isSolid() {
        return tileType.isSolid();
    }

    @Override
    public void draw(Canvas canvas, Paint paint) {
        Paint.Style originalStyle = paint.getStyle();
        int originalColor = paint.getColor();
        Align originalAlign = paint.getTextAlign();
        float originalStrokeWidth = paint.getStrokeWidth();

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(tileType.getColor());
        canvas.drawRoundRect(bounds, CORNER_RADIUS, CORNER_RADIUS, paint);

        switch (tileType) {
            case EDITOR_BLOCK:
                // VS Code editor styling: top tab highlight and faint line numbers
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.parseColor("#2D2D30"));
                canvas.drawRect(bounds.left, bounds.top, bounds.right, bounds.top + 6f, paint);

                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.parseColor("#3C3C3C"));
                float gutterWidth = bounds.width() * 0.18f;
                canvas.drawRect(bounds.left, bounds.top + 6f, bounds.left + gutterWidth, bounds.bottom, paint);

                paint.setColor(Color.parseColor("#6A9955"));
                paint.setTextSize(bounds.height() * 0.28f);
                paint.setTextAlign(Paint.Align.RIGHT);
                canvas.drawText("10", bounds.left + gutterWidth - 4f, bounds.centerY(), paint);

                paint.setTextAlign(Paint.Align.LEFT);
                paint.setColor(Color.parseColor("#D4D4D4"));
                canvas.drawText("if (run)", bounds.left + gutterWidth + 6f, bounds.centerY(), paint);
                break;
            case TERMINAL_BLOCK:
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.parseColor("#0E639C"));
                canvas.drawCircle(bounds.left + 10f, bounds.top + 10f, 3f, paint);
                paint.setColor(Color.parseColor("#3C3C3C"));
                canvas.drawRect(bounds.left, bounds.top, bounds.right, bounds.top + 6f, paint);

                paint.setColor(Color.parseColor("#C5E4FF"));
                paint.setTextAlign(Paint.Align.LEFT);
                paint.setTextSize(bounds.height() * 0.3f);
                canvas.drawText(">", bounds.left + 8f, bounds.centerY(), paint);
                canvas.drawText("npm start", bounds.left + 20f, bounds.centerY(), paint);
                break;
            case DEBUG_BLOCK:
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.parseColor("#1E1E1E"));
                float inset = 5f;
                canvas.drawRoundRect(bounds.left + inset, bounds.top + inset, bounds.right - inset, bounds.bottom - inset, 10f, 10f, paint);

                paint.setColor(Color.parseColor("#F14C4C"));
                canvas.drawCircle(bounds.left + bounds.width() * 0.2f, bounds.centerY(), 6f, paint);
                paint.setColor(Color.parseColor("#3794FF"));
                canvas.drawCircle(bounds.right - bounds.width() * 0.2f, bounds.centerY(), 6f, paint);

                paint.setColor(Color.parseColor("#DCDCAA"));
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setTextSize(bounds.height() * 0.32f);
                canvas.drawText("DBG", bounds.centerX(), bounds.centerY() + paint.getTextSize() * 0.35f, paint);
                break;
            case EMPTY:
            default:
                break;
        }

        paint.setStyle(originalStyle);
        paint.setColor(originalColor);
        paint.setTextAlign(originalAlign);
        paint.setStrokeWidth(originalStrokeWidth);
    }
}
