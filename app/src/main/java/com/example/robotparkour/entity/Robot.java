// app/src/main/java/com/example/robotparkour/entity/Robot.java
package com.example.robotparkour.entity;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.RectF;

import com.example.robotparkour.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Player-controlled robot with simple platforming physics.
 */
public class Robot extends GameObject {

    private static final float MOVE_ACCEL = 780f;
    private static final float MAX_MOVE_SPEED = 260f;
    private static final float GROUND_FRICTION = 680f;
    private static final float AIR_FRICTION = 240f;
    private static final float GRAVITY = 2000f;
    private static final float MAX_FALL_SPEED = 720f;
    private static final float JUMP_VELOCITY = -690f;

    private final RectF workingRect = new RectF();
    private final List<Tile> collisionTiles = new ArrayList<>(8);

    private float velocityX;
    private float velocityY;
    private boolean grounded;
    private boolean facingRight = true;
    private int lives = 3;
    private float spawnX;
    private float spawnY;

    public Robot(float x, float y, float width, float height) {
        super(x, y, width, height);
        spawnX = x;
        spawnY = y;
    }

    public void resetToSpawn() {
        setPosition(spawnX, spawnY);
        velocityX = 0f;
        velocityY = 0f;
        grounded = false;
    }

    public void setSpawn(float x, float y) {
        spawnX = x;
        spawnY = y;
        resetToSpawn();
    }

    public void setLives(int lives) {
        this.lives = lives;
    }

    public int getLives() {
        return lives;
    }

    public void loseLife() {
        lives = Math.max(0, lives - 1);
        resetToSpawn();
    }

    public boolean isGrounded() {
        return grounded;
    }

    public boolean isFacingRight() {
        return facingRight;
    }

    public void update(Level level,
                       float deltaSeconds,
                       boolean movingLeft,
                       boolean movingRight,
                       boolean jumpRequested) {
        float accelerationX = 0f;
        if (movingLeft) {
            accelerationX -= MOVE_ACCEL;
            facingRight = false;
        }
        if (movingRight) {
            accelerationX += MOVE_ACCEL;
            facingRight = true;
        }
        velocityX += accelerationX * deltaSeconds;

        float friction = grounded ? GROUND_FRICTION : AIR_FRICTION;
        if (!movingLeft && !movingRight) {
            if (velocityX > 0f) {
                velocityX = Math.max(0f, velocityX - friction * deltaSeconds);
            } else if (velocityX < 0f) {
                velocityX = Math.min(0f, velocityX + friction * deltaSeconds);
            }
        }

        if (velocityX > MAX_MOVE_SPEED) {
            velocityX = MAX_MOVE_SPEED;
        } else if (velocityX < -MAX_MOVE_SPEED) {
            velocityX = -MAX_MOVE_SPEED;
        }

        if (jumpRequested && grounded) {
            velocityY = JUMP_VELOCITY;
            grounded = false;
        }

        velocityY += GRAVITY * deltaSeconds;
        if (velocityY > MAX_FALL_SPEED) {
            velocityY = MAX_FALL_SPEED;
        }

        moveAlongAxis(level, 0f, velocityY * deltaSeconds);
        moveAlongAxis(level, velocityX * deltaSeconds, 0f);
    }

    private void moveAlongAxis(Level level, float dx, float dy) {
        if (dx == 0f && dy == 0f) {
            return;
        }
        workingRect.set(bounds);
        workingRect.offset(dx, dy);
        level.querySolidTiles(workingRect, collisionTiles);

        if (dy != 0f) {
            grounded = false;
        }

        for (Tile tile : collisionTiles) {
            if (!RectF.intersects(workingRect, tile.getBounds())) {
                continue;
            }
            if (dy > 0f) {
                workingRect.offsetTo(workingRect.left, tile.getBounds().top - height);
                velocityY = 0f;
                grounded = true;
            } else if (dy < 0f) {
                workingRect.offsetTo(workingRect.left, tile.getBounds().bottom);
                velocityY = 0f;
            } else if (dx > 0f) {
                workingRect.offsetTo(tile.getBounds().left - width, workingRect.top);
                velocityX = 0f;
            } else if (dx < 0f) {
                workingRect.offsetTo(tile.getBounds().right, workingRect.top);
                velocityX = 0f;
            }
        }
        setPosition(workingRect.left, workingRect.top);
    }

    @Override
    public void draw(Canvas canvas, Paint paint) {
        Paint.Style originalStyle = paint.getStyle();
        int originalColor = paint.getColor();
        Align originalAlign = paint.getTextAlign();

        // Body
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#4A90E2"));
        canvas.drawRoundRect(bounds.left + 4, bounds.top + 6, bounds.right - 4, bounds.bottom - 6, 10f, 10f, paint);

        // Head display
        paint.setColor(Color.parseColor("#A1C4FD"));
        canvas.drawRoundRect(bounds.left + 8, bounds.top + 2, bounds.right - 8, bounds.top + height * 0.45f, 8f, 8f, paint);

        // Eyes (simple coding caret style)
        paint.setColor(Color.parseColor("#0D1B2A"));
        float eyeY = bounds.top + height * 0.22f;
        float eyeSpacing = facingRight ? 6f : -6f;
        canvas.drawCircle(bounds.centerX() - eyeSpacing, eyeY, 3f, paint);
        canvas.drawCircle(bounds.centerX() + eyeSpacing, eyeY, 3f, paint);

        // Arms
        paint.setColor(Color.parseColor("#3D7ECC"));
        float armLength = height * 0.35f;
        if (facingRight) {
            canvas.drawRoundRect(bounds.right - 6, bounds.top + 14, bounds.right + armLength, bounds.top + 20, 6f, 6f, paint);
            canvas.drawRoundRect(bounds.left - armLength, bounds.top + 14, bounds.left + 6, bounds.top + 20, 6f, 6f, paint);
        } else {
            canvas.drawRoundRect(bounds.left - armLength, bounds.top + 14, bounds.left + 6, bounds.top + 20, 6f, 6f, paint);
            canvas.drawRoundRect(bounds.right - 6, bounds.top + 14, bounds.right + armLength, bounds.top + 20, 6f, 6f, paint);
        }

        // Feet
        paint.setColor(Color.parseColor("#344E9A"));
        float footHeight = height * 0.12f;
        canvas.drawRoundRect(bounds.left + 4, bounds.bottom - footHeight, bounds.left + width * 0.45f, bounds.bottom, 4f, 4f, paint);
        canvas.drawRoundRect(bounds.right - width * 0.45f, bounds.bottom - footHeight, bounds.right - 4, bounds.bottom, 4f, 4f, paint);

        // Chest display with </>
        paint.setColor(Color.parseColor("#0D1B2A"));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(height * 0.22f);
        canvas.drawText("</>", bounds.centerX(), bounds.top + height * 0.63f, paint);

        paint.setStyle(originalStyle);
        paint.setColor(originalColor);
        paint.setTextAlign(originalAlign);
    }
}
