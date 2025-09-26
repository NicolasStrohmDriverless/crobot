// app/src/main/java/com/example/robotparkour/entity/GameObject.java
package com.example.robotparkour.entity;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

/**
 * Lightweight base class for any object that participates in the world.
 */
public abstract class GameObject {

    protected float x;
    protected float y;
    protected final float width;
    protected final float height;
    protected final RectF bounds = new RectF();

    protected GameObject(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        updateBounds();
    }

    protected void updateBounds() {
        bounds.set(x, y, x + width, y + height);
    }

    public RectF getBounds() {
        return bounds;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getCenterX() {
        return x + width / 2f;
    }

    public float getCenterY() {
        return y + height / 2f;
    }

    public void setPosition(float newX, float newY) {
        x = newX;
        y = newY;
        updateBounds();
    }

    public void offset(float dx, float dy) {
        x += dx;
        y += dy;
        updateBounds();
    }

    public abstract void draw(Canvas canvas, Paint paint);
}
