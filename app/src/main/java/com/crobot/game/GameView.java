// app/src/main/java/com/crobot/game/GameView.java
package com.crobot.game;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.crobot.game.level.LegacyWorldData;
import com.crobot.game.level.LevelModel;
import com.example.robotparkour.audio.GameAudioManager;
import com.example.robotparkour.audio.WorldMusicLibrary;
import com.example.robotparkour.core.WorldInfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SurfaceView responsible for rendering the platformer level and updating the simulation.
 */
public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    public enum Control { LEFT, RIGHT, JUMP }

    public interface LevelCompletionListener {
        void onLevelCompleted(int world, int stage);
    }

    private static final class BackgroundTheme {
        final int backgroundColor;
        final int tabOuterColor;
        final int tabInnerColor;
        final int activeOuterColor;
        final int activeInnerColor;
        final int farHillColor;
        final int midHillColor;
        final int bushOuterColor;
        final int bushInnerColor;
        final int indentColor;
        final int accentGlyphColor;
        final int glintColor;
        final int gutterBaseColor;
        final int gutterLightColor;
        final int gutterTrackColor;
        final int minimapBaseColor;
        final int minimapOutlineColor;
        final int minimapGlowColor;
        final int statusBarColor;

        BackgroundTheme(int backgroundColor,
                        int tabOuterColor,
                        int tabInnerColor,
                        int activeOuterColor,
                        int activeInnerColor,
                        int farHillColor,
                        int midHillColor,
                        int bushOuterColor,
                        int bushInnerColor,
                        int indentColor,
                        int accentGlyphColor,
                        int glintColor,
                        int gutterBaseColor,
                        int gutterLightColor,
                        int gutterTrackColor,
                        int minimapBaseColor,
                        int minimapOutlineColor,
                        int minimapGlowColor,
                        int statusBarColor) {
            this.backgroundColor = backgroundColor;
            this.tabOuterColor = tabOuterColor;
            this.tabInnerColor = tabInnerColor;
            this.activeOuterColor = activeOuterColor;
            this.activeInnerColor = activeInnerColor;
            this.farHillColor = farHillColor;
            this.midHillColor = midHillColor;
            this.bushOuterColor = bushOuterColor;
            this.bushInnerColor = bushInnerColor;
            this.indentColor = indentColor;
            this.accentGlyphColor = accentGlyphColor;
            this.glintColor = glintColor;
            this.gutterBaseColor = gutterBaseColor;
            this.gutterLightColor = gutterLightColor;
            this.gutterTrackColor = gutterTrackColor;
            this.minimapBaseColor = minimapBaseColor;
            this.minimapOutlineColor = minimapOutlineColor;
            this.minimapGlowColor = minimapGlowColor;
            this.statusBarColor = statusBarColor;
        }
    }

    private static final SparseArray<BackgroundTheme> BACKGROUND_THEMES = createBackgroundThemes();
    private static final BackgroundTheme DEFAULT_THEME = BACKGROUND_THEMES.get(1);

    private static final float FIXED_TIME_STEP = 1f / 60f;
    private static final float GRAVITY = 1400f;
    private static final float MOVE_SPEED = 200f;
    private static final float JUMP_VELOCITY = -520f;
    private static final int SAFE_TOP_PX = 64;
    private static final int SAFE_BOTTOM_PX = 48;
    private static final float BASE_SCROLL_SPEED = 120f;

    private final Paint backgroundPaint = new Paint();
    private final Paint entityPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint uiPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tileFallbackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect srcRect = new Rect();
    private final Rect dstRect = new Rect();
    private final RectF tempRectF = new RectF();
    private final RectF flagBounds = new RectF();

    private Thread renderThread;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private boolean surfaceReady;

    @Nullable
    private LevelModel level;
    @Nullable
    private Bitmap tileset;
    private int tilesetColumns;

    private final Player player = new Player();
    private final GameAudioManager audioManager;

    private boolean moveLeft;
    private boolean moveRight;
    private boolean jumpPressed;
    private boolean jumpConsumed;
    private boolean shouldPlayJumpSound;

    private float cameraX;
    private float cameraY;
    private float currentScale = 1f;
    private float parallaxTimer;
    private float animationTimer;
    private int currentWorldNumber = 1;
    private int currentStage = 1;
    @Nullable
    private WorldInfo currentWorldInfo;
    @Nullable
    private BackgroundTheme currentBackgroundTheme = DEFAULT_THEME;
    @Nullable
    private LevelCompletionListener levelCompletionListener;
    private boolean levelCompleted;
    private boolean completionSoundPlayed;

    public GameView(@NonNull Context context) {
        super(context);
        audioManager = new GameAudioManager(context.getApplicationContext());
        init();
    }

    public GameView(@NonNull Context context, @Nullable android.util.AttributeSet attrs) {
        super(context, attrs);
        audioManager = new GameAudioManager(context.getApplicationContext());
        init();
    }

    public void setLevelCompletionListener(@Nullable LevelCompletionListener listener) {
        levelCompletionListener = listener;
    }

    private void init() {
        getHolder().addCallback(this);
        setFocusable(true);
        BackgroundTheme theme = currentBackgroundTheme != null ? currentBackgroundTheme : DEFAULT_THEME;
        if (theme != null) {
            backgroundPaint.setColor(theme.backgroundColor);
        } else {
            backgroundPaint.setColor(Color.rgb(20, 26, 48));
        }
        entityPaint.setColor(Color.YELLOW);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(20f);
        textPaint.setShadowLayer(2f, 1f, 1f, Color.argb(160, 0, 0, 0));
        uiPaint.setStyle(Paint.Style.FILL);
        uiPaint.setColor(Color.WHITE);
        tileFallbackPaint.setStyle(Paint.Style.FILL);
    }

    public void bindLevel(@NonNull LevelModel level, int world, int stage) {
        this.level = level;
        currentWorldNumber = Math.max(1, world);
        currentStage = Math.max(1, stage);
        currentWorldInfo = LegacyWorldData.findWorld(currentWorldNumber);
        currentBackgroundTheme = BACKGROUND_THEMES.get(currentWorldNumber);
        if (currentBackgroundTheme == null) {
            currentBackgroundTheme = DEFAULT_THEME;
        }
        if (currentBackgroundTheme != null) {
            backgroundPaint.setColor(currentBackgroundTheme.backgroundColor);
        }
        cameraX = 0f;
        cameraY = 0f;
        parallaxTimer = 0f;
        animationTimer = 0f;
        player.width = level.getTileWidth() * 0.82f;
        player.height = level.getTileHeight() * 1.65f;
        player.vx = 0f;
        player.vy = 0f;
        player.onGround = false;
        player.facingRight = true;
        shouldPlayJumpSound = false;
        levelCompleted = false;
        completionSoundPlayed = false;
        moveLeft = false;
        moveRight = false;
        jumpPressed = false;
        jumpConsumed = false;

        boolean spawnFound = false;
        for (LevelModel.Entity entity : level.getEntities()) {
            if ("spawn".equalsIgnoreCase(entity.getType())) {
                player.x = entity.getX();
                player.y = entity.getY();
                spawnFound = true;
                break;
            }
        }
        if (!spawnFound) {
            player.x = level.getTileWidth() * 2.5f;
            player.y = level.getPixelHeight() - level.getTileHeight() * 2f;
        }

        loadTilesetBitmap(level.getTilesetAssetPath());
        updateScale(level);
        configureMusic();
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

    private void updateScale(@NonNull LevelModel level) {
        int viewHeight = getHeight();
        if (viewHeight <= 0) {
            return;
        }
        float desiredScale = viewHeight / (float) Math.max(level.getPixelHeight(), 1);
        if (!Float.isFinite(desiredScale) || desiredScale <= 0f) {
            desiredScale = 1f;
        }
        currentScale = desiredScale;
    }

    private void configureMusic() {
        audioManager.stopMusic();
        audioManager.setMusicTrack(WorldMusicLibrary.getTrackFor(currentWorldInfo));
        audioManager.startMusic();
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        surfaceReady = true;
        startRenderThread();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        LevelModel level = this.level;
        if (level != null) {
            updateScale(level);
        }
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
        parallaxTimer += deltaSeconds;
        animationTimer += deltaSeconds;
        updateScale(level);
        if (levelCompleted) {
            updateCamera(level);
            return;
        }
        handleInput(deltaSeconds);
        applyPhysics(deltaSeconds, level);
        if (shouldPlayJumpSound) {
            audioManager.playJump();
            shouldPlayJumpSound = false;
        }
        updateCamera(level);
        checkLevelCompletion(level);
    }

    private void handleInput(float deltaSeconds) {
        player.vx = 0f;
        if (moveLeft && !moveRight) {
            player.vx -= MOVE_SPEED;
            player.facingRight = false;
        } else if (moveRight && !moveLeft) {
            player.vx += MOVE_SPEED;
            player.facingRight = true;
        }
        if (!jumpPressed) {
            jumpConsumed = false;
        }
        if (jumpPressed && !jumpConsumed && player.onGround) {
            player.vy = JUMP_VELOCITY;
            player.onGround = false;
            jumpConsumed = true;
            shouldPlayJumpSound = true;
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
        float scale = currentScale > 0f ? currentScale : 1f;
        float viewWidthWorld = getWidth() / scale;
        float viewHeightWorld = getHeight() / scale;

        float targetX = player.x - viewWidthWorld * 0.4f;
        float maxScrollX = Math.max(0f, level.getPixelWidth() - viewWidthWorld);
        cameraX = clamp(targetX, 0f, maxScrollX);

        float desiredFloorY = Math.max(0f, level.getPixelHeight() - viewHeightWorld);
        float targetY = Math.max(desiredFloorY, player.y - viewHeightWorld * 0.7f);
        float maxScrollY = Math.max(0f, level.getPixelHeight() - viewHeightWorld);
        cameraY = clamp(targetY, 0f, maxScrollY);
    }

    private void checkLevelCompletion(@NonNull LevelModel level) {
        if (levelCompleted) {
            return;
        }
        RectF playerBounds = player.getBounds();
        float tileWidth = level.getTileWidth();
        float tileHeight = level.getTileHeight();
        for (LevelModel.Entity entity : level.getEntities()) {
            String type = entity.getType();
            if (type == null) {
                continue;
            }
            String lowerType = type.toLowerCase(Locale.US);
            if (lowerType.contains("flag")) {
                buildFlagBounds(entity, tileWidth, tileHeight, flagBounds);
                if (RectF.intersects(playerBounds, flagBounds)) {
                    triggerLevelCompleted();
                    break;
                }
            }
        }
    }

    private void buildFlagBounds(@NonNull LevelModel.Entity entity,
                                 float tileWidth,
                                 float tileHeight,
                                 @NonNull RectF outBounds) {
        float baseX = entity.getX();
        float baseY = entity.getY();
        float poleHeight = tileHeight * 3.2f;
        float left = baseX - tileWidth * 0.7f;
        float right = baseX + tileWidth * 1.5f;
        float top = baseY - poleHeight;
        float bottom = baseY + tileHeight * 0.4f;
        if (right < left) {
            float temp = left;
            left = right;
            right = temp;
        }
        if (bottom < top) {
            float temp = top;
            top = bottom;
            bottom = temp;
        }
        outBounds.set(left, top, right, bottom);
    }

    private void triggerLevelCompleted() {
        if (levelCompleted) {
            return;
        }
        levelCompleted = true;
        moveLeft = false;
        moveRight = false;
        jumpPressed = false;
        shouldPlayJumpSound = false;
        player.vx = 0f;
        player.vy = 0f;
        player.onGround = true;
        if (!completionSoundPlayed) {
            audioManager.playVictory();
            completionSoundPlayed = true;
        }
        final int world = currentWorldNumber;
        final int stage = currentStage;
        post(() -> notifyLevelCompleted(world, stage));
    }

    private void notifyLevelCompleted(int world, int stage) {
        LevelCompletionListener listener = levelCompletionListener;
        if (listener != null) {
            listener.onLevelCompleted(world, stage);
        }
    }

    private static SparseArray<BackgroundTheme> createBackgroundThemes() {
        SparseArray<BackgroundTheme> map = new SparseArray<>();
        map.put(1, new BackgroundTheme(
                color("#1E1E1E"),
                color("#252526"),
                color("#2D2D2D"),
                color("#1F1F1F"),
                color("#252526"),
                color("#1B2C33"),
                color("#15252B"),
                color("#1F241F"),
                color("#2A3A29"),
                color("#283238"),
                color("#CE9178"),
                color("#DCDCAA"),
                color("#141414"),
                color("#F14C4C"),
                color("#252526"),
                color("#1B3443"),
                color("#2E4F60"),
                color("#4FC1FF"),
                color("#007ACC")));
        map.put(2, new BackgroundTheme(
                color("#221C24"),
                color("#2E2533"),
                color("#3B3044"),
                color("#38273F"),
                color("#4B3654"),
                color("#3D2E4D"),
                color("#2A2038"),
                color("#3F2B46"),
                color("#5A3A5D"),
                color("#4A3957"),
                color("#E0C38C"),
                color("#F4D8A8"),
                color("#1B1422"),
                color("#FF8E3C"),
                color("#4C3A5A"),
                color("#2D2238"),
                color("#4C3C5F"),
                color("#FFBE6F"),
                color("#9A6BFF")));
        map.put(3, new BackgroundTheme(
                color("#101322"),
                color("#18203A"),
                color("#233055"),
                color("#1F2A47"),
                color("#2E3D6A"),
                color("#1C2850"),
                color("#111C3A"),
                color("#1D2450"),
                color("#2F3A7A"),
                color("#233463"),
                color("#C792EA"),
                color("#A0E8FF"),
                color("#0D1424"),
                color("#7F7BFF"),
                color("#202A4A"),
                color("#142044"),
                color("#263B72"),
                color("#64F5FF"),
                color("#375DFF")));
        map.put(4, new BackgroundTheme(
                color("#1B0F0D"),
                color("#2A1612"),
                color("#3A1F16"),
                color("#3C1A13"),
                color("#532216"),
                color("#401F1A"),
                color("#2C1410"),
                color("#3F1D14"),
                color("#6B2A1C"),
                color("#402620"),
                color("#FFB37A"),
                color("#FFD7A1"),
                color("#1B0C07"),
                color("#FF5E3A"),
                color("#462017"),
                color("#2A1814"),
                color("#4A2B20"),
                color("#FF824A"),
                color("#E2522E")));
        map.put(5, new BackgroundTheme(
                color("#0F1A1F"),
                color("#13242B"),
                color("#1F3943"),
                color("#1A2E36"),
                color("#234550"),
                color("#143C4A"),
                color("#0E2A35"),
                color("#123540"),
                color("#1D4E5A"),
                color("#1A3D4A"),
                color("#8FF7FF"),
                color("#7CFFE6"),
                color("#07171F"),
                color("#2CF9FF"),
                color("#1C3D46"),
                color("#0F2832"),
                color("#1D4C59"),
                color("#5FFFE1"),
                color("#1FA7C6")));
        map.put(6, new BackgroundTheme(
                color("#0B1316"),
                color("#142125"),
                color("#1E2F34"),
                color("#1C2A2F"),
                color("#233C44"),
                color("#16292F"),
                color("#0E1C21"),
                color("#14282C"),
                color("#214045"),
                color("#1B3338"),
                color("#9AD7D3"),
                color("#A8FFF2"),
                color("#061013"),
                color("#4FE3C8"),
                color("#1A3338"),
                color("#11262C"),
                color("#23474E"),
                color("#6CF9D7"),
                color("#2A9C8E")));
        map.put(7, new BackgroundTheme(
                color("#101D16"),
                color("#1A2C22"),
                color("#284237"),
                color("#234534"),
                color("#325C45"),
                color("#1C4734"),
                color("#153427"),
                color("#1E4A33"),
                color("#2E6F4A"),
                color("#2B5A3F"),
                color("#C0FF8F"),
                color("#E2FFB0"),
                color("#0D1A13"),
                color("#7CFF9E"),
                color("#244A35"),
                color("#143926"),
                color("#286046"),
                color("#A4FFAF"),
                color("#3BC976")));
        map.put(8, new BackgroundTheme(
                color("#18140F"),
                color("#261E17"),
                color("#352A1F"),
                color("#352516"),
                color("#4C3622"),
                color("#3A2C1E"),
                color("#271D13"),
                color("#3A2A1A"),
                color("#5B3D25"),
                color("#423123"),
                color("#FFCA7A"),
                color("#FFDFA8"),
                color("#120D08"),
                color("#FF9C3C"),
                color("#3F2C1D"),
                color("#21170F"),
                color("#423320"),
                color("#FFB469"),
                color("#F27E32")));
        map.put(9, new BackgroundTheme(
                color("#0D1018"),
                color("#161C2C"),
                color("#222C44"),
                color("#1D2436"),
                color("#2D3853"),
                color("#202B4A"),
                color("#131B30"),
                color("#1D2A4A"),
                color("#2F3D64"),
                color("#263654"),
                color("#9CF0FF"),
                color("#D0F4FF"),
                color("#070B14"),
                color("#6F7CFF"),
                color("#1F2C47"),
                color("#15223C"),
                color("#2A3F62"),
                color("#7FE0FF"),
                color("#2E6CFF")));
        if (map.get(1) == null) {
            map.put(1, new BackgroundTheme(
                    color("#1E1E1E"),
                    color("#252526"),
                    color("#2D2D2D"),
                    color("#1F1F1F"),
                    color("#252526"),
                    color("#1B2C33"),
                    color("#15252B"),
                    color("#1F241F"),
                    color("#2A3A29"),
                    color("#283238"),
                    color("#CE9178"),
                    color("#DCDCAA"),
                    color("#141414"),
                    color("#F14C4C"),
                    color("#252526"),
                    color("#1B3443"),
                    color("#2E4F60"),
                    color("#4FC1FF"),
                    color("#007ACC")));
        }
        return map;
    }

    private static int color(@NonNull String hex) {
        return Color.parseColor(hex);
    }

    private void render(@NonNull Canvas canvas) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        canvas.drawRect(0, 0, width, height, backgroundPaint);
        LevelModel level = this.level;
        if (level == null) {
            return;
        }
        drawParallaxBackground(canvas);
        drawTiles(canvas, level);
        drawEntities(canvas, level);
        drawPlayer(canvas);
        drawHud(canvas, level);
    }

    private void drawParallaxBackground(@NonNull Canvas canvas) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        canvas.save();
        BackgroundTheme theme = currentBackgroundTheme != null ? currentBackgroundTheme : DEFAULT_THEME;
        if (theme == null) {
            theme = DEFAULT_THEME;
        }
        drawPointerPlainsBackground(canvas, width, height, parallaxTimer, theme);
        canvas.restore();
        drawStatusBar(canvas, width, height, theme);
        drawScanlineOverlay(canvas, width, height);
    }

    private void drawPointerPlainsBackground(@NonNull Canvas canvas,
                                             int width,
                                             int height,
                                             float time,
                                             @NonNull BackgroundTheme theme) {
        paintSolidBackground(canvas, theme.backgroundColor, width, height);

        float tabPeriod = Math.max(width / 4f, 320f);
        float tabOffset = computeLoopOffset(time, BASE_SCROLL_SPEED * 0.2f, tabPeriod);
        for (float x = -tabPeriod; x < width + tabPeriod; x += tabPeriod) {
            float left = x - tabOffset;
            RectF outer = new RectF(left + tabPeriod * 0.08f, 12f,
                    left + tabPeriod * 0.72f, SAFE_TOP_PX - 12f);
            uiPaint.setColor(theme.tabOuterColor);
            canvas.drawRoundRect(outer, 26f, 26f, uiPaint);
            uiPaint.setColor(theme.tabInnerColor);
            canvas.drawRoundRect(new RectF(outer.left + 12f, outer.top + 8f,
                    outer.right - 12f, outer.bottom - 8f), 20f, 20f, uiPaint);
        }
        uiPaint.setColor(theme.activeOuterColor);
        RectF active = new RectF(width * 0.34f, 8f, width * 0.58f, SAFE_TOP_PX - 10f);
        canvas.drawRoundRect(active, 28f, 28f, uiPaint);
        uiPaint.setColor(theme.activeInnerColor);
        canvas.drawRoundRect(new RectF(active.left + 12f, active.top + 10f,
                active.right - 12f, active.bottom - 14f), 22f, 22f, uiPaint);

        float farPeriod = Math.max(width / 3f, 280f);
        drawHillBand(canvas, width, height * 0.58f, height,
                farPeriod, computeLoopOffset(time, BASE_SCROLL_SPEED * 0.2f, farPeriod),
                height * 0.12f, theme.farHillColor);
        drawHillBand(canvas, width, height * 0.68f, height,
                farPeriod * 0.8f, computeLoopOffset(time, BASE_SCROLL_SPEED * 0.35f, farPeriod * 0.8f),
                height * 0.16f, theme.midHillColor);

        float bushPeriod = Math.max(width / 2.6f, 260f);
        float bushOffset = computeLoopOffset(time, BASE_SCROLL_SPEED * 0.5f, bushPeriod);
        for (float x = -bushPeriod; x < width + bushPeriod; x += bushPeriod) {
            float left = x - bushOffset + width * 0.05f;
            RectF bush = new RectF(left, height * 0.62f,
                    left + bushPeriod * 0.64f, height * 0.82f);
            uiPaint.setColor(theme.bushOuterColor);
            canvas.drawRoundRect(bush, 40f, 40f, uiPaint);
            uiPaint.setColor(theme.bushInnerColor);
            canvas.drawRoundRect(new RectF(bush.left + 14f, bush.top + 14f,
                    bush.right - 14f, bush.bottom - 14f), 34f, 34f, uiPaint);
        }

        drawIndentGuides(canvas, width, width * 0.22f, SAFE_TOP_PX + 16f,
                height - SAFE_BOTTOM_PX - 36f, width * 0.05f,
                theme.indentColor,
                computeLoopOffset(time, BASE_SCROLL_SPEED * 0.2f, width * 0.05f));

        uiPaint.setColor(theme.accentGlyphColor);
        uiPaint.setTextAlign(Paint.Align.CENTER);
        uiPaint.setTextSize(height * 0.045f);
        for (int i = 0; i < 7; i++) {
            float px = width * (0.18f + i * 0.12f);
            float py = height * 0.6f + (float) Math.sin(time * 1.4f + i) * 12f;
            canvas.drawText(";", px, py, uiPaint);
        }

        uiPaint.setColor(theme.glintColor);
        uiPaint.setAlpha(120);
        float glintOffset = computeLoopOffset(time, BASE_SCROLL_SPEED * 0.5f, width * 0.18f);
        for (float x = -width * 0.18f; x < width + width * 0.18f; x += width * 0.18f) {
            float left = x - glintOffset;
            canvas.drawRect(left, SAFE_TOP_PX + height * 0.14f,
                    left + width * 0.04f, SAFE_TOP_PX + height * 0.38f, uiPaint);
        }
        uiPaint.setAlpha(255);

        drawGutterRail(canvas, width, height, width * 0.05f, theme.gutterBaseColor,
                theme.gutterLightColor, theme.gutterTrackColor, time, 0.8f, 0.32f);
        drawMinimapColumn(canvas, width, height, theme.minimapBaseColor,
                theme.minimapOutlineColor, theme.minimapGlowColor, time, 0.5f);
    }

    private void paintSolidBackground(@NonNull Canvas canvas, int color, int width, int height) {
        uiPaint.setStyle(Paint.Style.FILL);
        uiPaint.setColor(color);
        canvas.drawRect(0f, 0f, width, height, uiPaint);
    }

    private void drawStatusBar(@NonNull Canvas canvas,
                               int width,
                               int height,
                               @NonNull BackgroundTheme theme) {
        uiPaint.setStyle(Paint.Style.FILL);
        uiPaint.setColor(theme.statusBarColor);
        canvas.drawRect(0f, height - SAFE_BOTTOM_PX, width, height, uiPaint);
    }

    private void drawScanlineOverlay(@NonNull Canvas canvas, int width, int height) {
        uiPaint.setStyle(Paint.Style.FILL);
        uiPaint.setColor(Color.argb(18, 255, 255, 255));
        for (float y = SAFE_TOP_PX; y < height - SAFE_BOTTOM_PX; y += 4f) {
            canvas.drawRect(0f, y, width, y + 1f, uiPaint);
        }
    }

    private float computeLoopOffset(float time, float speed, float period) {
        if (period <= 0f) {
            return 0f;
        }
        float shift = (time * speed) % period;
        if (shift < 0f) {
            shift += period;
        }
        return shift;
    }

    private void drawIndentGuides(@NonNull Canvas canvas,
                                  int width,
                                  float startX,
                                  float top,
                                  float bottom,
                                  float spacing,
                                  int color,
                                  float offset) {
        uiPaint.setStyle(Paint.Style.STROKE);
        uiPaint.setStrokeWidth(2f);
        uiPaint.setColor(color);
        for (float x = startX - spacing; x < width + spacing; x += spacing) {
            float cx = x - offset;
            canvas.drawLine(cx, top, cx, bottom, uiPaint);
        }
        uiPaint.setStyle(Paint.Style.FILL);
    }

    private void drawHillBand(@NonNull Canvas canvas,
                              int width,
                              float baseY,
                              float bottom,
                              float period,
                              float offset,
                              float amplitude,
                              int color) {
        uiPaint.setStyle(Paint.Style.FILL);
        uiPaint.setColor(color);
        Path path = new Path();
        path.moveTo(-width, bottom);
        for (float x = -period; x <= width + period; x += period / 2f) {
            float px = x - offset;
            float py = baseY + (float) Math.sin((px / period) * Math.PI * 2f) * amplitude;
            path.lineTo(px, py);
        }
        path.lineTo(width * 2f, bottom);
        path.close();
        canvas.drawPath(path, uiPaint);
    }

    private void drawGutterRail(@NonNull Canvas canvas,
                                 int width,
                                 int height,
                                 float railWidth,
                                 int baseColor,
                                 int lightColor,
                                 int trackColor,
                                 float time,
                                 float speedFactor,
                                 float glowStrength) {
        float bottom = height - SAFE_BOTTOM_PX;
        uiPaint.setStyle(Paint.Style.FILL);
        uiPaint.setColor(baseColor);
        canvas.drawRect(0f, SAFE_TOP_PX, railWidth, bottom, uiPaint);

        uiPaint.setColor(trackColor);
        float verticalOffset = computeLoopOffset(time, BASE_SCROLL_SPEED * speedFactor, railWidth * 0.6f);
        canvas.drawRect(verticalOffset - railWidth * 0.25f, SAFE_TOP_PX,
                verticalOffset - railWidth * 0.25f + 3f, bottom, uiPaint);

        float blink = (float) ((Math.sin(time * Math.PI / 1.5f) + 1f) * 0.5f);
        int alpha = (int) (80 + 120 * blink * glowStrength);
        uiPaint.setColor(Color.argb(alpha, Color.red(lightColor), Color.green(lightColor), Color.blue(lightColor)));
        float spacing = 68f;
        for (float y = SAFE_TOP_PX + 48f; y < bottom - 24f; y += spacing) {
            float wobble = (float) Math.sin(time * 3f + y * 0.05f) * 2f;
            canvas.drawCircle(railWidth * 0.55f + wobble, y, railWidth * 0.26f, uiPaint);
        }
    }

    private void drawMinimapColumn(@NonNull Canvas canvas,
                                   int width,
                                   int height,
                                   int baseColor,
                                   int accentColor,
                                   int glowColor,
                                   float time,
                                   float shimmerSpeed) {
        float minimapWidth = Math.max(18f, width * 0.028f);
        float left = width - minimapWidth - width * 0.02f;
        RectF column = new RectF(left, SAFE_TOP_PX + 16f,
                left + minimapWidth, height - SAFE_BOTTOM_PX - 16f);
        uiPaint.setStyle(Paint.Style.FILL);
        uiPaint.setColor(baseColor);
        canvas.drawRoundRect(column, 18f, 18f, uiPaint);

        uiPaint.setStyle(Paint.Style.STROKE);
        uiPaint.setStrokeWidth(3f);
        uiPaint.setColor(accentColor);
        canvas.drawRoundRect(new RectF(column.left + 4f, column.top + 6f,
                column.right - 4f, column.bottom - 6f), 16f, 16f, uiPaint);

        uiPaint.setStyle(Paint.Style.FILL);
        float bandHeight = 22f;
        float offset = computeLoopOffset(time, BASE_SCROLL_SPEED * shimmerSpeed, bandHeight * 2f);
        uiPaint.setColor(glowColor);
        uiPaint.setAlpha(160);
        for (float y = column.top - bandHeight; y < column.bottom + bandHeight; y += bandHeight * 2f) {
            float top = y - offset;
            canvas.drawRect(column.left + 6f, top,
                    column.right - 6f, top + bandHeight * 0.6f, uiPaint);
        }
        uiPaint.setAlpha(255);
    }

    private void drawTiles(@NonNull Canvas canvas, @NonNull LevelModel level) {
        int tileWidth = level.getTileWidth();
        int tileHeight = level.getTileHeight();
        float scale = currentScale > 0f ? currentScale : 1f;
        float viewWidthWorld = canvas.getWidth() / scale;
        float viewHeightWorld = canvas.getHeight() / scale;
        int startX = Math.max(0, (int) Math.floor(cameraX / tileWidth));
        int endX = Math.min(level.getWidth() - 1, (int) Math.ceil((cameraX + viewWidthWorld) / tileWidth));
        int startY = Math.max(0, (int) Math.floor(cameraY / tileHeight));
        int endY = Math.min(level.getHeight() - 1, (int) Math.ceil((cameraY + viewHeightWorld) / tileHeight));

        for (int x = startX; x <= endX; x++) {
            for (int y = startY; y <= endY; y++) {
                int gid = level.getTileLayer().getTileId(x, y);
                if (gid <= 0) {
                    continue;
                }
                float left = (x * tileWidth - cameraX) * scale;
                float top = (y * tileHeight - cameraY) * scale;
                float right = left + tileWidth * scale;
                float bottom = top + tileHeight * scale;

                if (tileset != null && tilesetColumns > 0) {
                    int index = gid - 1;
                    int srcX = (index % tilesetColumns) * tileWidth;
                    int srcY = (index / tilesetColumns) * tileHeight;
                    srcRect.set(srcX, srcY, srcX + tileWidth, srcY + tileHeight);
                    dstRect.set(Math.round(left), Math.round(top), Math.round(right), Math.round(bottom));
                    canvas.drawBitmap(tileset, srcRect, dstRect, null);
                } else {
                    drawFallbackTile(canvas, gid, left, top, right, bottom);
                }
            }
        }
    }

    private void drawFallbackTile(@NonNull Canvas canvas,
                                   int gid,
                                   float left,
                                   float top,
                                   float right,
                                   float bottom) {
        tempRectF.set(left, top, right, bottom);
        float width = tempRectF.width();
        float height = tempRectF.height();
        float radius = Math.min(width, height) * 0.16f;
        switch (gid) {
            case 1: // Editor block
                tileFallbackPaint.setColor(Color.parseColor("#1E1E1E"));
                canvas.drawRoundRect(tempRectF, radius, radius, tileFallbackPaint);
                tileFallbackPaint.setColor(Color.parseColor("#2D2D30"));
                canvas.drawRect(tempRectF.left, tempRectF.top,
                        tempRectF.right, tempRectF.top + height * 0.18f, tileFallbackPaint);
                tileFallbackPaint.setColor(Color.parseColor("#3C3C3C"));
                canvas.drawRect(tempRectF.left, tempRectF.top + height * 0.18f,
                        tempRectF.left + width * 0.2f, tempRectF.bottom, tileFallbackPaint);
                tileFallbackPaint.setColor(Color.parseColor("#252526"));
                canvas.drawRect(tempRectF.left + width * 0.22f, tempRectF.top + height * 0.22f,
                        tempRectF.right - width * 0.08f, tempRectF.top + height * 0.42f, tileFallbackPaint);
                break;
            case 2: // Terminal block
                tileFallbackPaint.setColor(Color.parseColor("#252526"));
                canvas.drawRoundRect(tempRectF, radius, radius, tileFallbackPaint);
                tileFallbackPaint.setColor(Color.parseColor("#0E639C"));
                float indicatorRadius = Math.min(width, height) * 0.12f;
                canvas.drawCircle(tempRectF.left + indicatorRadius * 1.8f,
                        tempRectF.top + indicatorRadius * 1.8f, indicatorRadius, tileFallbackPaint);
                tileFallbackPaint.setColor(Color.parseColor("#3C3C3C"));
                canvas.drawRect(tempRectF.left, tempRectF.top,
                        tempRectF.right, tempRectF.top + height * 0.18f, tileFallbackPaint);
                tileFallbackPaint.setColor(Color.parseColor("#1F1F1F"));
                canvas.drawRect(tempRectF.left + width * 0.12f, tempRectF.top + height * 0.26f,
                        tempRectF.right - width * 0.12f, tempRectF.bottom - height * 0.22f, tileFallbackPaint);
                break;
            case 3: // Debug block
                tileFallbackPaint.setColor(Color.parseColor("#373277"));
                canvas.drawRoundRect(tempRectF, radius, radius, tileFallbackPaint);
                tileFallbackPaint.setColor(Color.parseColor("#1E1E1E"));
                float inset = Math.min(width, height) * 0.18f;
                canvas.drawRoundRect(tempRectF.left + inset, tempRectF.top + inset,
                        tempRectF.right - inset, tempRectF.bottom - inset, radius, radius, tileFallbackPaint);
                float dotRadius = Math.min(width, height) * 0.12f;
                tileFallbackPaint.setColor(Color.parseColor("#F14C4C"));
                canvas.drawCircle(tempRectF.left + width * 0.25f, tempRectF.centerY(), dotRadius, tileFallbackPaint);
                tileFallbackPaint.setColor(Color.parseColor("#3794FF"));
                canvas.drawCircle(tempRectF.right - width * 0.25f, tempRectF.centerY(), dotRadius, tileFallbackPaint);
                break;
            default:
                tileFallbackPaint.setColor(Color.parseColor("#1E1E1E"));
                canvas.drawRoundRect(tempRectF, radius, radius, tileFallbackPaint);
                break;
        }
    }

    private void drawEntities(@NonNull Canvas canvas, @NonNull LevelModel level) {
        float scale = currentScale > 0f ? currentScale : 1f;
        for (LevelModel.Entity entity : level.getEntities()) {
            String type = entity.getType();
            if (type == null || "spawn".equalsIgnoreCase(type)) {
                continue;
            }
            float screenX = (entity.getX() - cameraX) * scale;
            float screenY = (entity.getY() - cameraY) * scale;
            float tileWidth = level.getTileWidth() * scale;
            float tileHeight = level.getTileHeight() * scale;
            String lowerType = type.toLowerCase(Locale.US);
            if (lowerType.contains("coin")) {
                drawCoinEntity(canvas, screenX, screenY, tileWidth);
            } else if (lowerType.contains("spike")) {
                drawSpikeEntity(canvas, screenX, screenY, tileWidth, tileHeight);
            } else if (lowerType.contains("flag")) {
                drawFlagEntity(canvas, screenX, screenY, tileWidth, tileHeight);
            } else if (lowerType.contains("enemy")) {
                drawEnemyEntity(canvas, screenX, screenY, tileWidth, tileHeight);
            } else {
                drawGenericEntity(canvas, screenX, screenY, tileWidth, tileHeight, type);
            }
        }
    }

    private void drawPlayer(@NonNull Canvas canvas) {
        RectF bounds = player.getBounds();
        float left = worldToScreenX(bounds.left);
        float right = worldToScreenX(bounds.right);
        float top = worldToScreenY(bounds.top);
        float bottom = worldToScreenY(bounds.bottom);
        drawRobotSprite(canvas, left, top, right, bottom, player.facingRight);
    }

    private void drawHud(@NonNull Canvas canvas, @NonNull LevelModel level) {
        String worldName = currentWorldInfo != null ? currentWorldInfo.getName()
                : String.format(Locale.US, "World %d", currentWorldNumber);
        String header = String.format(Locale.US, "%s  (W%d-%d)", worldName, currentWorldNumber, currentStage);
        canvas.drawText(header, 20f, SAFE_TOP_PX - 18f, textPaint);
        String text = String.format(Locale.US, "x=%1$.0f  y=%2$.0f", player.x, player.y);
        canvas.drawText(text, 20f, SAFE_TOP_PX + 12f, textPaint);
        if (tileset != null && level.getTilesetAssetPath() != null && !level.getTilesetAssetPath().isEmpty()) {
            canvas.drawText(level.getTilesetAssetPath(), 20f, SAFE_TOP_PX + 42f, textPaint);
        }
    }

    private float worldToScreenX(float worldX) {
        float scale = currentScale > 0f ? currentScale : 1f;
        return (worldX - cameraX) * scale;
    }

    private float worldToScreenY(float worldY) {
        float scale = currentScale > 0f ? currentScale : 1f;
        return (worldY - cameraY) * scale;
    }

    private void drawRobotSprite(@NonNull Canvas canvas,
                                 float left,
                                 float top,
                                 float right,
                                 float bottom,
                                 boolean facingRight) {
        float width = right - left;
        float height = bottom - top;

        Paint.Style originalStyle = entityPaint.getStyle();
        int originalColor = entityPaint.getColor();
        Paint.Align originalAlign = entityPaint.getTextAlign();
        float originalTextSize = entityPaint.getTextSize();
        float originalStroke = entityPaint.getStrokeWidth();

        entityPaint.setStyle(Paint.Style.FILL);
        entityPaint.setColor(Color.parseColor("#4A90E2"));
        canvas.drawRoundRect(left + width * 0.12f, top + height * 0.12f,
                right - width * 0.12f, bottom - height * 0.12f, width * 0.18f, width * 0.18f, entityPaint);

        entityPaint.setColor(Color.parseColor("#A1C4FD"));
        canvas.drawRoundRect(left + width * 0.2f, top + height * 0.06f,
                right - width * 0.2f, top + height * 0.45f, width * 0.16f, width * 0.16f, entityPaint);

        entityPaint.setColor(Color.parseColor("#0D1B2A"));
        float eyeY = top + height * 0.24f;
        float eyeOffset = width * 0.12f * (facingRight ? 1f : -1f);
        float eyeRadius = Math.max(3f, width * 0.06f);
        canvas.drawCircle(left + width * 0.5f - eyeOffset, eyeY, eyeRadius, entityPaint);
        canvas.drawCircle(left + width * 0.5f + eyeOffset, eyeY, eyeRadius, entityPaint);

        entityPaint.setColor(Color.parseColor("#3D7ECC"));
        float armLength = width * 0.38f;
        float armHeight = height * 0.08f;
        float armTop = top + height * 0.38f;
        if (facingRight) {
            canvas.drawRoundRect(right - width * 0.12f, armTop,
                    right + armLength, armTop + armHeight, armHeight, armHeight, entityPaint);
            canvas.drawRoundRect(left - armLength, armTop,
                    left + width * 0.12f, armTop + armHeight, armHeight, armHeight, entityPaint);
        } else {
            canvas.drawRoundRect(left - armLength, armTop,
                    left + width * 0.12f, armTop + armHeight, armHeight, armHeight, entityPaint);
            canvas.drawRoundRect(right - width * 0.12f, armTop,
                    right + armLength, armTop + armHeight, armHeight, armHeight, entityPaint);
        }

        entityPaint.setColor(Color.parseColor("#344E9A"));
        float footHeight = height * 0.14f;
        canvas.drawRoundRect(left + width * 0.08f, bottom - footHeight,
                left + width * 0.42f, bottom, footHeight * 0.6f, footHeight * 0.6f, entityPaint);
        canvas.drawRoundRect(right - width * 0.42f, bottom - footHeight,
                right - width * 0.08f, bottom, footHeight * 0.6f, footHeight * 0.6f, entityPaint);

        entityPaint.setColor(Color.parseColor("#0D1B2A"));
        entityPaint.setTextAlign(Paint.Align.CENTER);
        entityPaint.setTextSize(height * 0.22f);
        canvas.drawText("</>", left + width * 0.5f, bottom - height * 0.32f, entityPaint);

        entityPaint.setStyle(originalStyle);
        entityPaint.setColor(originalColor);
        entityPaint.setTextAlign(originalAlign);
        entityPaint.setTextSize(originalTextSize);
        entityPaint.setStrokeWidth(originalStroke);
    }

    private void drawCoinEntity(@NonNull Canvas canvas, float centerX, float centerY, float tileSize) {
        Paint.Style originalStyle = entityPaint.getStyle();
        int originalColor = entityPaint.getColor();
        float originalStroke = entityPaint.getStrokeWidth();

        float size = tileSize * 0.6f;
        float wobble = (float) Math.sin(animationTimer * 6f + centerX * 0.01f) * tileSize * 0.06f;
        float top = centerY - size / 2f + wobble;
        float bottom = centerY + size / 2f + wobble;

        entityPaint.setStyle(Paint.Style.STROKE);
        entityPaint.setStrokeWidth(Math.max(2f, tileSize * 0.08f));
        entityPaint.setColor(Color.parseColor("#FFD166"));
        RectF leftArc = new RectF(centerX - size, top, centerX - size * 0.2f, bottom);
        RectF rightArc = new RectF(centerX + size * 0.2f, top, centerX + size, bottom);
        canvas.drawArc(leftArc, 110, 140, false, entityPaint);
        canvas.drawArc(rightArc, -70, 140, false, entityPaint);

        entityPaint.setStyle(originalStyle);
        entityPaint.setColor(originalColor);
        entityPaint.setStrokeWidth(originalStroke);
    }

    private void drawSpikeEntity(@NonNull Canvas canvas,
                                 float centerX,
                                 float baseY,
                                 float tileWidth,
                                 float tileHeight) {
        Paint.Style originalStyle = entityPaint.getStyle();
        int originalColor = entityPaint.getColor();
        float originalStroke = entityPaint.getStrokeWidth();

        Path path = new Path();
        float halfWidth = tileWidth * 0.45f;
        path.moveTo(centerX - halfWidth, baseY);
        path.lineTo(centerX, baseY - tileHeight * 0.9f);
        path.lineTo(centerX + halfWidth, baseY);
        path.close();

        entityPaint.setStyle(Paint.Style.FILL);
        entityPaint.setColor(Color.parseColor("#C94E4E"));
        canvas.drawPath(path, entityPaint);

        entityPaint.setStyle(Paint.Style.STROKE);
        entityPaint.setStrokeWidth(Math.max(2f, tileWidth * 0.05f));
        entityPaint.setColor(Color.parseColor("#FCD7D7"));
        canvas.drawPath(path, entityPaint);

        entityPaint.setStyle(originalStyle);
        entityPaint.setColor(originalColor);
        entityPaint.setStrokeWidth(originalStroke);
    }

    private void drawFlagEntity(@NonNull Canvas canvas,
                                float baseX,
                                float baseY,
                                float tileWidth,
                                float tileHeight) {
        Paint.Style originalStyle = entityPaint.getStyle();
        int originalColor = entityPaint.getColor();
        float originalStroke = entityPaint.getStrokeWidth();

        float poleHeight = tileHeight * 3.2f;
        float poleTop = baseY - poleHeight;

        entityPaint.setStyle(Paint.Style.STROKE);
        entityPaint.setStrokeWidth(Math.max(2f, tileWidth * 0.08f));
        entityPaint.setColor(Color.parseColor("#C7CDD6"));
        canvas.drawLine(baseX, poleTop, baseX, baseY, entityPaint);

        entityPaint.setStyle(Paint.Style.FILL);
        entityPaint.setColor(Color.parseColor("#4FC1FF"));
        float flagWidth = tileWidth * 1.4f;
        float flagHeight = tileHeight * 0.9f;
        Path flag = new Path();
        flag.moveTo(baseX, poleTop + flagHeight * 0.3f);
        flag.lineTo(baseX + flagWidth, poleTop + flagHeight * 0.6f);
        flag.lineTo(baseX, poleTop + flagHeight);
        flag.close();
        canvas.drawPath(flag, entityPaint);

        entityPaint.setStyle(originalStyle);
        entityPaint.setColor(originalColor);
        entityPaint.setStrokeWidth(originalStroke);
    }

    private void drawEnemyEntity(@NonNull Canvas canvas,
                                 float centerX,
                                 float baseY,
                                 float tileWidth,
                                 float tileHeight) {
        Paint.Style originalStyle = entityPaint.getStyle();
        int originalColor = entityPaint.getColor();
        float originalStroke = entityPaint.getStrokeWidth();

        float bodyWidth = tileWidth * 1.1f;
        float bodyHeight = tileHeight * 0.9f;
        RectF body = new RectF(centerX - bodyWidth / 2f, baseY - bodyHeight,
                centerX + bodyWidth / 2f, baseY);
        entityPaint.setStyle(Paint.Style.FILL);
        entityPaint.setColor(Color.parseColor("#BF6C32"));
        canvas.drawRoundRect(body, bodyWidth * 0.3f, bodyWidth * 0.3f, entityPaint);

        entityPaint.setColor(Color.parseColor("#FCEBD2"));
        float eyeRadius = Math.max(2f, tileWidth * 0.12f);
        canvas.drawCircle(centerX - eyeRadius * 1.6f, body.top + bodyHeight * 0.35f, eyeRadius, entityPaint);
        canvas.drawCircle(centerX + eyeRadius * 1.6f, body.top + bodyHeight * 0.35f, eyeRadius, entityPaint);

        entityPaint.setColor(Color.parseColor("#2D160C"));
        float mouthWidth = bodyWidth * 0.5f;
        float mouthHeight = bodyHeight * 0.12f;
        canvas.drawRect(centerX - mouthWidth / 2f, body.top + bodyHeight * 0.65f,
                centerX + mouthWidth / 2f, body.top + bodyHeight * 0.65f + mouthHeight, entityPaint);

        entityPaint.setStyle(originalStyle);
        entityPaint.setColor(originalColor);
        entityPaint.setStrokeWidth(originalStroke);
    }

    private void drawGenericEntity(@NonNull Canvas canvas,
                                   float centerX,
                                   float baseY,
                                   float tileWidth,
                                   float tileHeight,
                                   @NonNull String label) {
        Paint.Style originalStyle = entityPaint.getStyle();
        int originalColor = entityPaint.getColor();

        entityPaint.setStyle(Paint.Style.FILL);
        entityPaint.setColor(Color.argb(140, 255, 255, 255));
        RectF rect = new RectF(centerX - tileWidth * 0.5f, baseY - tileHeight,
                centerX + tileWidth * 0.5f, baseY);
        canvas.drawRoundRect(rect, tileWidth * 0.2f, tileWidth * 0.2f, entityPaint);

        entityPaint.setStyle(originalStyle);
        entityPaint.setColor(originalColor);

        float originalTextSize = textPaint.getTextSize();
        int originalTextColor = textPaint.getColor();
        textPaint.setTextSize(tileHeight * 0.35f);
        textPaint.setColor(Color.WHITE);
        canvas.drawText(label, rect.left + tileWidth * 0.08f, rect.top + tileHeight * 0.6f, textPaint);
        textPaint.setTextSize(originalTextSize);
        textPaint.setColor(originalTextColor);
    }

    public void onHostResume() {
        if (surfaceReady) {
            startRenderThread();
        }
        audioManager.onResume();
    }

    public void onHostPause() {
        audioManager.onPause();
        stopRenderThread();
    }

    public void onHostDestroy() {
        stopRenderThread();
        if (tileset != null) {
            tileset.recycle();
            tileset = null;
        }
        audioManager.release();
    }

    public void handleButtonTouch(@NonNull Control control, @NonNull MotionEvent event) {
        if (levelCompleted) {
            return;
        }
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

    private float clamp(float value, float min, float max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private static final class Player {
        float x;
        float y;
        float vx;
        float vy;
        float width;
        float height;
        boolean onGround;
        boolean facingRight = true;

        RectF getBounds() {
            float halfWidth = width / 2f;
            return new RectF(x - halfWidth, y - height, x + halfWidth, y);
        }
    }
}
