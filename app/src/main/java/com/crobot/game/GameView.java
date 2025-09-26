// app/src/main/java/com/crobot/game/GameView.java
package com.crobot.game;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.crobot.game.level.LevelModel;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SurfaceView responsible for rendering the platformer level and updating the simulation.
 */
public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    public enum Control { LEFT, RIGHT, JUMP }

    private static final float FIXED_TIME_STEP = 1f / 60f;
    private static final float GRAVITY = 1400f;
    private static final float MOVE_SPEED = 200f;
    private static final float JUMP_VELOCITY = -520f;

    private final Paint backgroundPaint = new Paint();
    private final Paint entityPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect srcRect = new Rect();
    private final Rect dstRect = new Rect();

    private Thread renderThread;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private boolean surfaceReady;

    @Nullable
    private LevelModel level;
    @Nullable
    private Bitmap tileset;
    private int tilesetColumns;

    private final Player player = new Player();

    private boolean moveLeft;
    private boolean moveRight;
    private boolean jumpPressed;
    private boolean jumpConsumed;

    private float cameraX;

    public GameView(@NonNull Context context) {
        super(context);
        init();
    }

    public GameView(@NonNull Context context, @Nullable android.util.AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        getHolder().addCallback(this);
        setFocusable(true);
        backgroundPaint.setColor(Color.rgb(32, 48, 92));
        entityPaint.setColor(Color.YELLOW);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(20f);
        textPaint.setShadowLayer(2f, 1f, 1f, Color.WHITE);
    }

    public void bindLevel(@NonNull LevelModel level) {
        this.level = level;
        cameraX = 0f;
        player.width = level.getTileWidth() * 0.8f;
        player.height = level.getTileHeight() * 1.6f;
        player.x = level.getTileWidth() * 2f;
        player.y = level.getPixelHeight() - level.getTileHeight() * 3f;
        player.vx = 0f;
        player.vy = 0f;
        player.onGround = false;
        loadTilesetBitmap(level.getTilesetAssetPath());
    }

    private void loadTilesetBitmap(@NonNull String assetPath) {
        if (assetPath.isEmpty()) {
            tileset = null;
            tilesetColumns = 0;
            return;
        }
        AssetManager assets = getContext().getAssets();
        try (InputStream inputStream = assets.open(assetPath)) {
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            tileset = bitmap;
            tilesetColumns = bitmap.getWidth() / level.getTileWidth();
        } catch (IOException ex) {
            tileset = null;
            tilesetColumns = 0;
        }
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        surfaceReady = true;
        startRenderThread();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        // No-op
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        surfaceReady = false;
        stopRenderThread();
    }

    private void startRenderThread() {
        if (renderThread != null && renderThread.isAlive()) {
            return;
        }
        running.set(true);
        renderThread = new Thread(this, "PlatformerRenderThread");
        renderThread.start();
    }

    private void stopRenderThread() {
        running.set(false);
        if (renderThread != null) {
            try {
                renderThread.join();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            renderThread = null;
        }
    }

    @Override
    public void run() {
        long previous = System.nanoTime();
        double accumulator = 0.0;
        final double step = 1_000_000_000.0 * FIXED_TIME_STEP;
        SurfaceHolder holder = getHolder();
        while (running.get()) {
            long now = System.nanoTime();
            double delta = now - previous;
            previous = now;
            accumulator += delta;

            while (accumulator >= step) {
                update(FIXED_TIME_STEP);
                accumulator -= step;
            }

            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                if (canvas != null) {
                    render(canvas);
                }
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas);
                }
            }
        }
    }

    private void update(float deltaSeconds) {
        LevelModel level = this.level;
        if (level == null) {
            return;
        }
        handleInput(deltaSeconds);
        applyPhysics(deltaSeconds, level);
        updateCamera(level);
    }

    private void handleInput(float deltaSeconds) {
        player.vx = 0f;
        if (moveLeft) {
            player.vx -= MOVE_SPEED;
        }
        if (moveRight) {
            player.vx += MOVE_SPEED;
        }
        if (!jumpPressed) {
            jumpConsumed = false;
        }
        if (jumpPressed && !jumpConsumed && player.onGround) {
            player.vy = JUMP_VELOCITY;
            player.onGround = false;
            jumpConsumed = true;
        }
    }

    private void applyPhysics(float deltaSeconds, @NonNull LevelModel level) {
        player.vy += GRAVITY * deltaSeconds;
        moveHorizontally(player.vx * deltaSeconds, level);
        moveVertically(player.vy * deltaSeconds, level);
    }

    private void moveHorizontally(float delta, @NonNull LevelModel level) {
        if (delta == 0f) {
            return;
        }
        float newX = player.x + delta;
        RectF bounds = player.getBounds();
        bounds.offset(delta, 0f);
        if (delta > 0) {
            int tileRight = (int) Math.floor((bounds.right - 1) / level.getTileWidth());
            int topTile = (int) Math.floor(bounds.top / level.getTileHeight());
            int bottomTile = (int) Math.floor((bounds.bottom - 1) / level.getTileHeight());
            for (int ty = topTile; ty <= bottomTile; ty++) {
                if (isSolid(level, tileRight, ty)) {
                    float tileLeftEdge = tileRight * level.getTileWidth();
                    newX = tileLeftEdge - (bounds.width() / 2f);
                    player.vx = 0f;
                    break;
                }
            }
        } else {
            int tileLeft = (int) Math.floor(bounds.left / level.getTileWidth());
            int topTile = (int) Math.floor(bounds.top / level.getTileHeight());
            int bottomTile = (int) Math.floor((bounds.bottom - 1) / level.getTileHeight());
            for (int ty = topTile; ty <= bottomTile; ty++) {
                if (isSolid(level, tileLeft, ty)) {
                    float tileRightEdge = (tileLeft + 1) * level.getTileWidth();
                    newX = tileRightEdge + (bounds.width() / 2f);
                    player.vx = 0f;
                    break;
                }
            }
        }
        float halfWidth = bounds.width() / 2f;
        float minX = halfWidth;
        float maxX = Math.max(minX, level.getPixelWidth() - halfWidth);
        if (newX < minX) {
            newX = minX;
            player.vx = 0f;
        } else if (newX > maxX) {
            newX = maxX;
            player.vx = 0f;
        }
        player.x = newX;
    }

    private void moveVertically(float delta, @NonNull LevelModel level) {
        if (delta == 0f) {
            return;
        }
        float newY = player.y + delta;
        RectF bounds = player.getBounds();
        bounds.offset(0f, delta);
        if (delta > 0) {
            int tileBottom = (int) Math.floor((bounds.bottom - 1) / level.getTileHeight());
            int leftTile = (int) Math.floor(bounds.left / level.getTileWidth());
            int rightTile = (int) Math.floor((bounds.right - 1) / level.getTileWidth());
            for (int tx = leftTile; tx <= rightTile; tx++) {
                if (isSolid(level, tx, tileBottom)) {
                    float tileWorldBottom = tileBottom * level.getTileHeight();
                    newY = tileWorldBottom;
                    player.vy = 0f;
                    player.onGround = true;
                    break;
                }
            }
        } else {
            int tileTop = (int) Math.floor(bounds.top / level.getTileHeight());
            int leftTile = (int) Math.floor(bounds.left / level.getTileWidth());
            int rightTile = (int) Math.floor((bounds.right - 1) / level.getTileWidth());
            for (int tx = leftTile; tx <= rightTile; tx++) {
                if (isSolid(level, tx, tileTop)) {
                    float tileWorldTop = (tileTop + 1) * level.getTileHeight();
                    newY = tileWorldTop + player.height;
                    player.vy = 0f;
                    break;
                }
            }
        }
        float minY = player.height;
        float maxY = level.getPixelHeight();
        if (newY < minY) {
            newY = minY;
            player.vy = 0f;
        } else if (newY > maxY) {
            newY = maxY;
            player.vy = 0f;
            player.onGround = true;
        }
        player.y = newY;
        if (delta < 0 && player.vy < 0f) {
            player.onGround = false;
        }
    }

    private boolean isSolid(@NonNull LevelModel level, int tileX, int tileY) {
        int gid = level.getTileLayer().getTileId(tileX, tileY);
        return level.getCollisionMap().isSolid(gid);
    }

    private void updateCamera(@NonNull LevelModel level) {
        int viewWidth = getWidth();
        float target = player.x - viewWidth * 0.4f;
        target = Math.max(target, 0f);
        float maxScroll = Math.max(0, level.getPixelWidth() - viewWidth);
        cameraX = Math.max(0f, Math.min(target, maxScroll));
    }

    private void render(@NonNull Canvas canvas) {
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), backgroundPaint);
        LevelModel level = this.level;
        if (level == null) {
            return;
        }
        drawTiles(canvas, level);
        drawEntities(canvas, level);
        drawPlayer(canvas);
        drawHud(canvas, level);
    }

    private void drawTiles(@NonNull Canvas canvas, @NonNull LevelModel level) {
        if (tileset == null || tilesetColumns <= 0) {
            return;
        }
        int tileWidth = level.getTileWidth();
        int tileHeight = level.getTileHeight();
        int viewWidth = canvas.getWidth();
        int viewHeight = canvas.getHeight();
        int startX = Math.max(0, (int) Math.floor(cameraX / tileWidth));
        int endX = Math.min(level.getWidth() - 1, (int) Math.ceil((cameraX + viewWidth) / tileWidth));
        int endY = Math.min(level.getHeight() - 1, (int) Math.ceil((viewHeight) / tileHeight));

        for (int x = startX; x <= endX; x++) {
            for (int y = 0; y <= endY; y++) {
                int gid = level.getTileLayer().getTileId(x, y);
                if (gid <= 0) {
                    continue;
                }
                int index = gid - 1;
                int srcX = (index % tilesetColumns) * tileWidth;
                int srcY = (index / tilesetColumns) * tileHeight;
                srcRect.set(srcX, srcY, srcX + tileWidth, srcY + tileHeight);
                int screenX = (int) (x * tileWidth - cameraX);
                int screenY = y * tileHeight;
                dstRect.set(screenX, screenY, screenX + tileWidth, screenY + tileHeight);
                canvas.drawBitmap(tileset, srcRect, dstRect, null);
            }
        }
    }

    private void drawEntities(@NonNull Canvas canvas, @NonNull LevelModel level) {
        entityPaint.setColor(Color.argb(160, 255, 255, 255));
        for (LevelModel.Entity entity : level.getEntities()) {
            float width = level.getTileWidth();
            float height = level.getTileHeight();
            float left = entity.getX() - cameraX;
            float top = entity.getY() - height;
            RectF rect = new RectF(left, top, left + width, top + height);
            canvas.drawRoundRect(rect, 6f, 6f, entityPaint);
            String label = entity.getType();
            canvas.drawText(label, left + 4f, top + height / 2f, textPaint);
        }
    }

    private void drawPlayer(@NonNull Canvas canvas) {
        RectF bounds = player.getBounds();
        float left = bounds.left - cameraX;
        float right = bounds.right - cameraX;
        float top = bounds.top;
        float bottom = bounds.bottom;
        entityPaint.setColor(Color.rgb(255, 120, 64));
        canvas.drawRoundRect(new RectF(left, top, right, bottom), 8f, 8f, entityPaint);
    }

    private void drawHud(@NonNull Canvas canvas, @NonNull LevelModel level) {
        String text = String.format(Locale.US, "x=%1$.0f  y=%2$.0f", player.x, player.y);
        canvas.drawText(text, 20f, 30f, textPaint);
        String tilesetInfo = level.getTilesetAssetPath();
        canvas.drawText(tilesetInfo, 20f, 60f, textPaint);
    }

    public void onHostResume() {
        if (surfaceReady) {
            startRenderThread();
        }
    }

    public void onHostPause() {
        stopRenderThread();
    }

    public void onHostDestroy() {
        stopRenderThread();
        if (tileset != null) {
            tileset.recycle();
            tileset = null;
        }
    }

    public void handleButtonTouch(@NonNull Control control, @NonNull MotionEvent event) {
        boolean pressed = event.getActionMasked() != MotionEvent.ACTION_UP
                && event.getActionMasked() != MotionEvent.ACTION_CANCEL;
        switch (control) {
            case LEFT:
                moveLeft = pressed;
                break;
            case RIGHT:
                moveRight = pressed;
                break;
            case JUMP:
                jumpPressed = pressed;
                if (!pressed) {
                    jumpConsumed = false;
                }
                break;
        }
    }

    private static final class Player {
        float x;
        float y;
        float vx;
        float vy;
        float width;
        float height;
        boolean onGround;

        RectF getBounds() {
            float halfWidth = width / 2f;
            return new RectF(x - halfWidth, y - height, x + halfWidth, y);
        }
    }
}
