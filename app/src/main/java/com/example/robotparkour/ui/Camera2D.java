// app/src/main/java/com/example/robotparkour/ui/Camera2D.java
package com.example.robotparkour.ui;

import com.example.robotparkour.entity.Robot;

/**
 * Simple side-scrolling camera that follows the player with a configurable
 * dead-zone to reduce jitter while keeping upcoming obstacles visible.
 */
public class Camera2D {

    private float viewportWidth;
    private float viewportHeight;
    private float worldWidth;
    private float worldHeight;

    private float x;
    private float y;

    private float horizontalDeadZone;
    private float verticalDeadZone;

    public void setViewport(float width, float height) {
        viewportWidth = width;
        viewportHeight = height;
        horizontalDeadZone = viewportWidth * 0.3f;
        verticalDeadZone = viewportHeight * 0.2f;
    }

    public void setWorldSize(float width, float height) {
        worldWidth = width;
        worldHeight = height;
        clampToWorld();
    }

    public void snapTo(float newX, float newY) {
        x = newX;
        y = newY;
        clampToWorld();
    }

    public void follow(Robot robot) {
        if (robot == null) {
            return;
        }
        float targetX = robot.getCenterX();
        float targetY = robot.getCenterY();

        float deadZoneLeft = x + (viewportWidth / 2f) - horizontalDeadZone;
        float deadZoneRight = x + (viewportWidth / 2f) + horizontalDeadZone;
        if (targetX < deadZoneLeft) {
            x -= (deadZoneLeft - targetX);
        } else if (targetX > deadZoneRight) {
            x += (targetX - deadZoneRight);
        }

        float deadZoneTop = y + (viewportHeight / 2f) - verticalDeadZone;
        float deadZoneBottom = y + (viewportHeight / 2f) + verticalDeadZone;
        if (targetY < deadZoneTop) {
            y -= (deadZoneTop - targetY);
        } else if (targetY > deadZoneBottom) {
            y += (targetY - deadZoneBottom);
        }

        clampToWorld();
    }

    private void clampToWorld() {
        float maxX = Math.max(0f, worldWidth - viewportWidth);
        float maxY = Math.max(0f, worldHeight - viewportHeight);
        if (x < 0f) {
            x = 0f;
        } else if (x > maxX) {
            x = maxX;
        }
        if (y < 0f) {
            y = 0f;
        } else if (y > maxY) {
            y = maxY;
        }
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getViewportWidth() {
        return viewportWidth;
    }

    public float getViewportHeight() {
        return viewportHeight;
    }

    public float getParallaxX(float factor) {
        return x * factor;
    }

    public float getParallaxY(float factor) {
        return y * factor;
    }
}
