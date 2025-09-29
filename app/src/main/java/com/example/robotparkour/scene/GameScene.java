// app/src/main/java/com/example/robotparkour/scene/GameScene.java
package com.example.robotparkour.scene;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.example.robotparkour.audio.GameAudioManager;
import com.example.robotparkour.audio.WorldMusicLibrary;
import com.example.robotparkour.core.Scene;
import com.example.robotparkour.core.SceneManager;
import com.example.robotparkour.core.SceneType;
import com.example.robotparkour.core.WorldInfo;
import com.example.robotparkour.entity.Coin;
import com.example.robotparkour.entity.Flag;
import com.example.robotparkour.entity.Robot;
import com.example.robotparkour.entity.Spike;
import com.example.robotparkour.entity.Tile;
import com.example.robotparkour.level.Level;
import com.example.robotparkour.level.LevelLibrary;
import com.example.robotparkour.ui.Camera2D;
import com.example.robotparkour.ui.HudOverlay;
import com.example.robotparkour.ui.VirtualButton;
import com.example.robotparkour.util.GameResult;

import java.util.Collections;
import java.util.List;

/**
 * Core gameplay scene that manages entities, physics, and rendering.
 */
public class GameScene implements Scene {

    private static final int INITIAL_LIVES = 3;

    private final SceneManager sceneManager;
    private final GameAudioManager audioManager;
    private final Paint worldPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint uiPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final HudOverlay hud = new HudOverlay();
    private final Camera2D camera = new Camera2D();
    private final VirtualButton leftButton = new VirtualButton("←");
    private final VirtualButton rightButton = new VirtualButton("→");
    private final VirtualButton jumpButton = new VirtualButton("⤒");

    private Level level;
    private Robot robot;
    private List<Tile> tiles = Collections.emptyList();
    private List<Coin> coins = Collections.emptyList();
    private List<Spike> spikes = Collections.emptyList();
    private Flag flag;
    private String activeWorldName = "";

    private float elapsedSeconds;
    private int collectedCoins;
    private int lives;
    private boolean running;

    private boolean leftKeyDown;
    private boolean rightKeyDown;
    private boolean jumpKeyDown;
    private boolean jumpQueued;
    private boolean jumpButtonPreviouslyPressed;

    private int surfaceWidth;
    private int surfaceHeight;
    private float parallaxTimer;

    private static final int SAFE_TOP_PX = 64;
    private static final int SAFE_BOTTOM_PX = 48;
    private static final float BASE_SCROLL_SPEED = 120f;

    public GameScene(Context context, SceneManager sceneManager) {
        this.sceneManager = sceneManager;
        this.audioManager = sceneManager.getAudioManager();
        ensureLevelForSelectedWorld();
        resetForNewRun();
    }

    @Override
    public SceneType getType() {
        return SceneType.GAME;
    }

    public void resetForNewRun() {
        ensureLevelForSelectedWorld();
        audioManager.setMusicTrack(WorldMusicLibrary.getTrackFor(sceneManager.getContext(), sceneManager.getSelectedWorld()));
        elapsedSeconds = 0f;
        collectedCoins = 0;
        lives = INITIAL_LIVES;
        running = true;
        jumpQueued = false;
        leftKeyDown = false;
        rightKeyDown = false;
        jumpKeyDown = false;
        jumpButtonPreviouslyPressed = false;
        if (robot != null) {
            robot.setLives(INITIAL_LIVES);
            robot.setSpawn(level.getSpawnX(), level.getSpawnY() - (robot.getBounds().height() - Level.TILE_SIZE));
            robot.resetToSpawn();
        }
        for (Coin coin : coins) {
            coin.reset();
        }
        if (flag != null) {
            flag.reset();
        }
        audioManager.startMusic();
        if (robot != null && surfaceWidth > 0) {
            camera.snapTo(Math.max(0f, robot.getX() - surfaceWidth / 2f), 0f);
        } else {
            camera.snapTo(0f, 0f);
        }
    }

    private void ensureLevelForSelectedWorld() {
        WorldInfo selectedWorld = resolveSelectedWorld();
        String worldName = selectedWorld.getName();
        if (level != null && robot != null && worldName.equals(activeWorldName)) {
            return;
        }
        activeWorldName = worldName;
        String[] layout = LevelLibrary.getLevelData(selectedWorld);
        level = Level.fromStringMap(layout, Level.TILE_SIZE);
        tiles = level.getTiles();
        coins = level.getCoins();
        spikes = level.getSpikes();
        flag = level.getFlag();
        float robotWidth = Level.TILE_SIZE * 0.8f;
        float robotHeight = Level.TILE_SIZE * 0.95f;
        robot = new Robot(level.getSpawnX(), level.getSpawnY() - (robotHeight - Level.TILE_SIZE), robotWidth, robotHeight);
        float worldHeight = Math.max(level.getPixelHeight(), surfaceHeight > 0 ? surfaceHeight : level.getPixelHeight());
        camera.setWorldSize(level.getPixelWidth(), worldHeight);
    }

    private WorldInfo resolveSelectedWorld() {
        WorldInfo selectedWorld = sceneManager.getSelectedWorld();
        if (selectedWorld == null) {
            selectedWorld = new WorldInfo(1, "Pointer Plains", "Startwelt, leicht & freundlich");
        }
        return selectedWorld;
    }

    @Override
    public void onEnter() {
        running = true;
        ensureLevelForSelectedWorld();
        audioManager.setMusicTrack(WorldMusicLibrary.getTrackFor(sceneManager.getContext(), sceneManager.getSelectedWorld()));
        audioManager.startMusic();
    }

    @Override
    public void onExit() {
        audioManager.stopMusic();
    }

    @Override
    public void update(float deltaSeconds) {
        parallaxTimer += deltaSeconds;
        if (!running) {
            return;
        }
        if (level == null || robot == null) {
            return;
        }
        elapsedSeconds += deltaSeconds;

        boolean moveLeft = leftKeyDown || leftButton.isPressed();
        boolean moveRight = rightKeyDown || rightButton.isPressed();
        boolean jumpFromButton = jumpButton.isPressed();
        if (jumpFromButton && !jumpButtonPreviouslyPressed) {
            jumpQueued = true;
        }
        jumpButtonPreviouslyPressed = jumpFromButton;

        boolean jumpRequest = jumpQueued;
        jumpQueued = false;
        boolean wasGrounded = robot.isGrounded();
        boolean shouldJump = jumpRequest || (jumpKeyDown && wasGrounded);
        robot.update(level, deltaSeconds, moveLeft, moveRight, shouldJump);
        if (shouldJump && wasGrounded) {
            audioManager.playJump();
        }

        for (Coin coin : coins) {
            coin.update(deltaSeconds);
            if (!coin.isCollected() && RectF.intersects(robot.getBounds(), coin.getBounds())) {
                coin.collect();
                collectedCoins++;
                audioManager.playCoin();
            }
        }

        for (Spike spike : spikes) {
            if (RectF.intersects(robot.getBounds(), spike.getBounds())) {
                handlePlayerHitHazard();
                break;
            }
        }

        if (flag != null && !flag.isActivated() && RectF.intersects(robot.getBounds(), flag.getBounds())) {
            flag.activate();
            finishRun(true);
        }

        if (robot.getY() > level.getPixelHeight() + Level.TILE_SIZE) {
            handlePlayerHitHazard();
        }

        camera.follow(robot);
    }

    private void handlePlayerHitHazard() {
        audioManager.playError();
        robot.loseLife();
        lives = robot.getLives();
        if (lives <= 0) {
            finishRun(false);
        } else if (surfaceWidth > 0) {
            camera.snapTo(Math.max(0f, robot.getX() - surfaceWidth / 2f), 0f);
        }
    }

    private void finishRun(boolean victory) {
        running = false;
        audioManager.stopMusic();
        GameResult result = new GameResult(elapsedSeconds, collectedCoins, victory, Math.max(0, lives));
        sceneManager.showGameOver(result);
    }

    @Override
    public void draw(Canvas canvas) {
        drawParallaxBackground(canvas);
        canvas.save();
        canvas.translate(-camera.getX(), -camera.getY());
        drawLevel(canvas);
        canvas.restore();

        drawControls(canvas);
        hud.draw(canvas, collectedCoins, coins.size(), lives, elapsedSeconds, sceneManager.getGameView().getFps());
    }

    private void drawParallaxBackground(Canvas canvas) {
        if (surfaceWidth <= 0 || surfaceHeight <= 0) {
            return;
        }
        WorldInfo world = sceneManager.getSelectedWorld();
        String worldName = world != null ? world.getName() : "";
        canvas.save();
        switch (worldName) {
            case "Template Temple":
                drawTemplateTempleBackground(canvas, parallaxTimer);
                break;
            case "Namespace Nebula":
                drawNamespaceNebulaBackground(canvas, parallaxTimer);
                break;
            case "Exception Volcano":
                drawExceptionVolcanoBackground(canvas, parallaxTimer);
                break;
            case "STL City":
                drawStlCityBackground(canvas, parallaxTimer);
                break;
            case "Heap Caverns":
                drawHeapCavernsBackground(canvas, parallaxTimer);
                break;
            case "Lambda Gardens":
                drawLambdaGardensBackground(canvas, parallaxTimer);
                break;
            case "Multithread Foundry":
                drawMultithreadFoundryBackground(canvas, parallaxTimer);
                break;
            case "NullPointer-Nexus":
                drawNullPointerNexusBackground(canvas, parallaxTimer);
                break;
            case "Pointer Plains":
            default:
                drawPointerPlainsBackground(canvas, parallaxTimer);
                break;
        }
        canvas.restore();
        drawStatusBar(canvas);
        drawScanlineOverlay(canvas);
    }

    private void drawPointerPlainsBackground(Canvas canvas, float time) {
        float width = surfaceWidth;
        float height = surfaceHeight;
        paintSolidBackground(canvas, Color.parseColor("#1E1E1E"));

        // Tabs as drifting code-cloud banners.
        float tabPeriod = Math.max(width / 4f, 320f);
        float tabOffset = computeLoopOffset(time, BASE_SCROLL_SPEED * 0.2f, tabPeriod);
        for (float x = -tabPeriod; x < width + tabPeriod; x += tabPeriod) {
            float left = x - tabOffset;
            RectF outer = new RectF(left + tabPeriod * 0.08f, 12f,
                    left + tabPeriod * 0.72f, SAFE_TOP_PX - 12f);
            uiPaint.setColor(Color.parseColor("#252526"));
            canvas.drawRoundRect(outer, 26f, 26f, uiPaint);
            uiPaint.setColor(Color.parseColor("#2D2D2D"));
            canvas.drawRoundRect(new RectF(outer.left + 12f, outer.top + 8f,
                    outer.right - 12f, outer.bottom - 8f), 20f, 20f, uiPaint);
        }
        uiPaint.setColor(Color.parseColor("#1F1F1F"));
        RectF active = new RectF(width * 0.34f, 8f, width * 0.58f, SAFE_TOP_PX - 10f);
        canvas.drawRoundRect(active, 28f, 28f, uiPaint);
        uiPaint.setColor(Color.parseColor("#252526"));
        canvas.drawRoundRect(new RectF(active.left + 12f, active.top + 10f,
                active.right - 12f, active.bottom - 14f), 22f, 22f, uiPaint);

        // Gently undulating editor "hills" inspired by indent guides.
        float farPeriod = Math.max(width / 3f, 280f);
        drawHillBand(canvas, height * 0.58f, height,
                farPeriod, computeLoopOffset(time, BASE_SCROLL_SPEED * 0.2f, farPeriod),
                height * 0.12f, Color.parseColor("#1B2C33"));
        drawHillBand(canvas, height * 0.68f, height,
                farPeriod * 0.8f, computeLoopOffset(time, BASE_SCROLL_SPEED * 0.35f, farPeriod * 0.8f),
                height * 0.16f, Color.parseColor("#15252B"));

        // Explorer shrubs with folder edges.
        float bushPeriod = Math.max(width / 2.6f, 260f);
        float bushOffset = computeLoopOffset(time, BASE_SCROLL_SPEED * 0.5f, bushPeriod);
        for (float x = -bushPeriod; x < width + bushPeriod; x += bushPeriod) {
            float left = x - bushOffset + width * 0.05f;
            RectF bush = new RectF(left, height * 0.62f,
                    left + bushPeriod * 0.64f, height * 0.82f);
            uiPaint.setColor(Color.parseColor("#1F241F"));
            canvas.drawRoundRect(bush, 40f, 40f, uiPaint);
            uiPaint.setColor(Color.parseColor("#2A3A29"));
            canvas.drawRoundRect(new RectF(bush.left + 14f, bush.top + 14f,
                    bush.right - 14f, bush.bottom - 14f), 34f, 34f, uiPaint);
        }

        // Indent guide ridges.
        drawIndentGuides(canvas, width * 0.22f, SAFE_TOP_PX + 16f,
                height - SAFE_BOTTOM_PX - 36f, width * 0.05f,
                Color.parseColor("#283238"),
                computeLoopOffset(time, BASE_SCROLL_SPEED * 0.2f, width * 0.05f));

        // Semicolon blossoms.
        uiPaint.setColor(Color.parseColor("#CE9178"));
        uiPaint.setTextAlign(Paint.Align.CENTER);
        uiPaint.setTextSize(height * 0.045f);
        for (int i = 0; i < 7; i++) {
            float px = width * (0.18f + i * 0.12f);
            float py = height * 0.6f + (float) Math.sin(time * 1.4f + i) * 12f;
            canvas.drawText(";", px, py, uiPaint);
        }

        // Warm sunlight glints.
        uiPaint.setColor(Color.parseColor("#DCDCAA"));
        uiPaint.setAlpha(120);
        float glintOffset = computeLoopOffset(time, BASE_SCROLL_SPEED * 0.5f, width * 0.18f);
        for (float x = -width * 0.18f; x < width + width * 0.18f; x += width * 0.18f) {
            float left = x - glintOffset;
            canvas.drawRect(left, SAFE_TOP_PX + height * 0.14f,
                    left + width * 0.04f, SAFE_TOP_PX + height * 0.38f, uiPaint);
        }
        uiPaint.setAlpha(255);

        drawGutterRail(canvas, width * 0.05f, Color.parseColor("#141414"),
                Color.parseColor("#F14C4C"), time, 0.8f, 0.32f);
        drawMinimapColumn(canvas, Color.parseColor("#1B3443"),
                Color.parseColor("#2E4F60"), Color.parseColor("#4FC1FF"), time, 0.5f);
    }

    private void drawTemplateTempleBackground(Canvas canvas, float time) {
        float width = surfaceWidth;
        float height = surfaceHeight;
        paintSolidBackground(canvas, Color.parseColor("#1E1E1E"));

        drawTabArchRow(canvas, time, Color.parseColor("#2D2D2D"), Color.parseColor("#1F1F1F"));

        float columnSpacing = Math.max(width * 0.08f, 90f);
        float columnOffset = computeLoopOffset(time, BASE_SCROLL_SPEED * 0.2f, columnSpacing);
        uiPaint.setStyle(Paint.Style.STROKE);
        uiPaint.setStrokeWidth(6f);
        uiPaint.setColor(Color.parseColor("#303236"));
        for (float x = -columnSpacing; x < width + columnSpacing; x += columnSpacing) {
            float cx = x - columnOffset;
            canvas.drawLine(cx, SAFE_TOP_PX, cx, height - SAFE_BOTTOM_PX, uiPaint);
        }
        uiPaint.setStyle(Paint.Style.FILL);

        float archSpacing = Math.max(width * 0.28f, 320f);
        float archOffset = computeLoopOffset(time, BASE_SCROLL_SPEED * 0.2f, archSpacing);
        for (float x = -archSpacing; x < width + archSpacing; x += archSpacing) {
            float left = x - archOffset;
            drawGenericArch(canvas, left, SAFE_TOP_PX + height * 0.08f,
                    archSpacing * 0.82f, height * 0.34f,
                    Color.parseColor("#2A2D30"), Color.parseColor("#1D1F22"));
        }

        uiPaint.setColor(Color.argb(46, 197, 134, 192));
        for (int i = 0; i < 4; i++) {
            float cx = width * (0.22f + i * 0.18f);
            canvas.drawRoundRect(new RectF(cx - 34f, height * 0.36f - height * 0.22f,
                    cx + 34f, height * 0.36f + height * 0.22f), 30f, 30f, uiPaint);
        }

        uiPaint.setColor(Color.parseColor("#2AA198"));
        float bannerSpacing = Math.max(width * 0.18f, 220f);
        float bannerOffset = computeLoopOffset(time, BASE_SCROLL_SPEED * 0.5f, bannerSpacing);
        for (float x = -bannerSpacing; x < width + bannerSpacing; x += bannerSpacing) {
            float left = x - bannerOffset;
            drawFoldTriangle(canvas, left + 26f, height * 0.42f,
                    bannerSpacing * 0.3f, height * 0.08f);
        }

        drawPaneForeground(canvas, Color.parseColor("#1C1C1C"), Color.parseColor("#2F2F2F"), time);

        drawGutterRail(canvas, width * 0.05f, Color.parseColor("#1A1A1A"),
                Color.parseColor("#C586C0"), time, 0.72f, 0.44f);
        drawMinimapColumn(canvas, Color.parseColor("#23262C"),
                Color.parseColor("#31353D"), Color.parseColor("#2AA198"), time, 0.36f);
    }

    private void drawNamespaceNebulaBackground(Canvas canvas, float time) {
        float width = surfaceWidth;
        float height = surfaceHeight;
        paintSolidBackground(canvas, Color.parseColor("#202431"));

        // Starfield referencing the minimap pixels.
        float starSpacing = Math.max(width * 0.06f, 80f);
        float starOffset = computeLoopOffset(time, BASE_SCROLL_SPEED * 0.2f, starSpacing);
        uiPaint.setColor(Color.parseColor("#4FC1FF"));
        for (float x = -starSpacing; x < width + starSpacing; x += starSpacing) {
            for (int i = 0; i < 5; i++) {
                float sx = x - starOffset + (i * 0.18f * starSpacing);
                float sy = SAFE_TOP_PX + height * 0.08f + i * 36f;
                uiPaint.setAlpha(170 - i * 24);
                canvas.drawCircle(sx, sy, 3f + i, uiPaint);
            }
        }
        uiPaint.setAlpha(255);

        // Tabs as glowing satellite rectangles.
        float satelliteSpacing = Math.max(width * 0.34f, 360f);
        float satelliteOffset = computeLoopOffset(time, BASE_SCROLL_SPEED * 0.2f, satelliteSpacing);
        for (float x = -satelliteSpacing; x < width + satelliteSpacing; x += satelliteSpacing) {
            float cx = x - satelliteOffset;
            RectF body = new RectF(cx, 12f, cx + satelliteSpacing * 0.52f, SAFE_TOP_PX - 12f);
            uiPaint.setColor(Color.parseColor("#1F2230"));
            canvas.drawRoundRect(body, 18f, 18f, uiPaint);
            uiPaint.setStyle(Paint.Style.STROKE);
            uiPaint.setStrokeWidth(4f);
            uiPaint.setColor(Color.parseColor("#4FC1FF"));
            canvas.drawRoundRect(new RectF(body.left + 6f, body.top + 6f,
                    body.right - 6f, body.bottom - 6f), 16f, 16f, uiPaint);
            uiPaint.setStyle(Paint.Style.FILL);
        }

        // Floating namespace banners.
        float ribbonSpacing = Math.max(width * 0.38f, 380f);
        float ribbonOffset = computeLoopOffset(time, BASE_SCROLL_SPEED * 0.5f, ribbonSpacing);
        for (float x = -ribbonSpacing; x < width + ribbonSpacing; x += ribbonSpacing) {
            float left = x - ribbonOffset;
            drawNamespaceRibbon(canvas, left, height * 0.34f,
                    ribbonSpacing * 0.92f, height * 0.16f, time);
        }

        // Bracket constellations.
        uiPaint.setColor(Color.parseColor("#C586C0"));
        uiPaint.setAlpha(140);
        float braceSpacing = Math.max(width * 0.22f, 260f);
        float braceOffset = computeLoopOffset(time, BASE_SCROLL_SPEED * 0.3f, braceSpacing);
        for (float x = -braceSpacing; x < width + braceSpacing; x += braceSpacing) {
            float cx = x - braceOffset + braceSpacing * 0.5f;
            float cy = height * 0.48f + (float) Math.sin(time + cx * 0.01f) * 24f;
            canvas.drawText("{}", cx, cy, uiPaint);
        }
        uiPaint.setAlpha(255);

        // Orbit rings in the foreground.
        uiPaint.setColor(Color.argb(90, 79, 193, 255));
        float ringSpacing = Math.max(width * 0.26f, 320f);
        float ringOffset = computeLoopOffset(time, BASE_SCROLL_SPEED * 0.8f, ringSpacing);
        for (float x = -ringSpacing; x < width + ringSpacing; x += ringSpacing) {
            float cx = x - ringOffset + ringSpacing * 0.5f;
            float cy = height * 0.68f + (float) Math.sin(time + x) * 18f;
            canvas.drawOval(new RectF(cx - 90f, cy - 32f, cx + 90f, cy + 32f), uiPaint);
        }

        // Cursor meteor streaks.
        drawCursorMeteors(canvas, time, Color.parseColor("#4FC1FF"));

        drawGutterRail(canvas, width * 0.05f, Color.parseColor("#181B28"),
                Color.parseColor("#4FC1FF"), time, 0.78f, 0.38f);
        drawMinimapColumn(canvas, Color.parseColor("#1A2034"),
                Color.parseColor("#283154"), Color.parseColor("#4FC1FF"), time, 0.46f);
    }

    private void drawExceptionVolcanoBackground(Canvas canvas, float time) {
        float width = surfaceWidth;
        float height = surfaceHeight;
        paintSolidBackground(canvas, Color.parseColor("#1B1B1B"));

        drawTabAshClouds(canvas, time);

        float squiggleSpacing = Math.max(width * 0.22f, 260f);
        float squiggleOffset = computeLoopOffset(time, BASE_SCROLL_SPEED * 0.3f, squiggleSpacing);
        for (float x = -squiggleSpacing; x < width + squiggleSpacing; x += squiggleSpacing) {
            float left = x - squiggleOffset;
            drawSquiggleFlow(canvas, left, SAFE_TOP_PX + 40f, height * 0.72f,
                    Color.parseColor("#F14C4C"));
        }

        float chimneySpacing = Math.max(width * 0.3f, 340f);
        float chimneyOffset = computeLoopOffset(time, BASE_SCROLL_SPEED * 0.5f, chimneySpacing);
        for (float x = -chimneySpacing; x < width + chimneySpacing; x += chimneySpacing) {
            float cx = x - chimneyOffset + chimneySpacing * 0.5f;
            drawBreakpointChimney(canvas, cx, height * 0.52f, height * 0.26f, time);
        }

        drawTerminalPlate(canvas, time);
        drawHeatDistortion(canvas, time);

        drawGutterRail(canvas, width * 0.05f, Color.parseColor("#141414"),
                Color.parseColor("#CCA700"), time, 0.84f, 0.5f);
        drawMinimapColumn(canvas, Color.parseColor("#381818"),
                Color.parseColor("#552222"), Color.parseColor("#F14C4C"), time, 0.56f);
    }

    private void drawStlCityBackground(Canvas canvas, float time) {
        float width = surfaceWidth;
        float height = surfaceHeight;
        paintSolidBackground(canvas, Color.parseColor("#1E1E1E"));

        drawBillboardTabs(canvas, time);

        float skylineSpacing = Math.max(width * 0.24f, 280f);
        float skylineOffset = computeLoopOffset(time, BASE_SCROLL_SPEED * 0.3f, skylineSpacing);
        for (float x = -skylineSpacing; x < width + skylineSpacing; x += skylineSpacing) {
            float left = x - skylineOffset;
            drawExplorerSkyline(canvas, left, SAFE_TOP_PX + height * 0.12f,
                    skylineSpacing * 0.84f, height * 0.42f, time);
        }

        float tooltipSpacing = Math.max(width * 0.32f, 360f);
        float tooltipOffset = computeLoopOffset(time, BASE_SCROLL_SPEED * 0.5f, tooltipSpacing);
        for (float x = -tooltipSpacing; x < width + tooltipSpacing; x += tooltipSpacing) {
            float left = x - tooltipOffset;
            drawTooltipBillboard(canvas, left + width * 0.1f, height * 0.46f,
                    tooltipSpacing * 0.6f, height * 0.16f, time);
        }

        drawTransitForeground(canvas, time);
        drawGutterRail(canvas, width * 0.05f, Color.parseColor("#141414"),
                Color.parseColor("#DCDCAA"), time, 0.82f, 0.34f);
        drawMinimapColumn(canvas, Color.parseColor("#1C2A36"),
                Color.parseColor("#27506A"), Color.parseColor("#007ACC"), time, 0.4f);
    }

    private void drawHeapCavernsBackground(Canvas canvas, float time) {
        float width = surfaceWidth;
        float height = surfaceHeight;
        paintSolidBackground(canvas, Color.parseColor("#161616"));

        drawTabStalactites(canvas, time);

        float stalSpacing = Math.max(width * 0.12f, 120f);
        float stalOffset = computeLoopOffset(time, BASE_SCROLL_SPEED * 0.25f, stalSpacing);
        uiPaint.setColor(Color.parseColor("#1E1E1E"));
        for (float x = -stalSpacing; x < width + stalSpacing; x += stalSpacing) {
            float cx = x - stalOffset;
            canvas.drawRect(cx, SAFE_TOP_PX, cx + 6f, height * 0.62f, uiPaint);
        }

        float nookSpacing = Math.max(width * 0.3f, 300f);
        float nookOffset = computeLoopOffset(time, BASE_SCROLL_SPEED * 0.4f, nookSpacing);
        uiPaint.setStyle(Paint.Style.STROKE);
        uiPaint.setStrokeWidth(4f);
        uiPaint.setColor(Color.argb(60, 106, 153, 85));
        for (float x = -nookSpacing; x < width + nookSpacing; x += nookSpacing) {
            float left = x - nookOffset;
            RectF rect = new RectF(left + 40f, height * 0.44f,
                    left + nookSpacing * 0.8f, height * 0.68f);
            canvas.drawRoundRect(rect, 18f, 18f, uiPaint);
        }
        uiPaint.setStyle(Paint.Style.FILL);

        float chunkSpacing = Math.max(width * 0.22f, 240f);
        float chunkOffset = computeLoopOffset(time, BASE_SCROLL_SPEED * 0.5f, chunkSpacing);
        for (float x = -chunkSpacing; x < width + chunkSpacing; x += chunkSpacing) {
            float left = x - chunkOffset;
            drawGarbageChunk(canvas, left, height * 0.6f,
                    chunkSpacing * 0.48f, height * 0.16f, time);
        }

        drawCavernFog(canvas, time);
        drawGutterRail(canvas, width * 0.05f, Color.parseColor("#0F0F0F"),
                Color.parseColor("#4FC1FF"), time, 0.7f, 0.26f);
        drawMinimapColumn(canvas, Color.parseColor("#102027"),
                Color.parseColor("#1B323A"), Color.parseColor("#4FC1FF"), time, 0.34f);
    }

    private void drawLambdaGardensBackground(Canvas canvas, float time) {
        float width = surfaceWidth;
        float height = surfaceHeight;
        paintSolidBackground(canvas, Color.parseColor("#1E231B"));

        drawBannerTabs(canvas, time, Color.parseColor("#1F261C"), Color.parseColor("#253420"));

        float treeSpacing = Math.max(width * 0.28f, 320f);
        float treeOffset = computeLoopOffset(time, BASE_SCROLL_SPEED * 0.24f, treeSpacing);
        for (float x = -treeSpacing; x < width + treeSpacing; x += treeSpacing) {
            float baseX = x - treeOffset + treeSpacing * 0.4f;
            drawCurlyTree(canvas, baseX, height * 0.6f, treeSpacing * 0.3f, height * 0.32f);
        }

        float blossomSpacing = Math.max(width * 0.18f, 200f);
        float blossomOffset = computeLoopOffset(time, BASE_SCROLL_SPEED * 0.5f, blossomSpacing);
        uiPaint.setColor(Color.parseColor("#CE9178"));
        uiPaint.setTextAlign(Paint.Align.CENTER);
        uiPaint.setTextSize(height * 0.05f);
        for (float x = -blossomSpacing; x < width + blossomSpacing; x += blossomSpacing) {
            float px = x - blossomOffset + blossomSpacing * 0.5f;
            float py = height * 0.5f + (float) Math.sin(time * 1.3f + px * 0.02f) * 16f;
            canvas.drawText("()", px, py, uiPaint);
        }

        float vineSpacing = Math.max(width * 0.1f, 90f);
        float vineOffset = computeLoopOffset(time, BASE_SCROLL_SPEED * 0.5f, vineSpacing);
        uiPaint.setColor(Color.parseColor("#35503A"));
        for (float x = -vineSpacing; x < width + vineSpacing; x += vineSpacing) {
            float vx = x - vineOffset;
            canvas.drawLine(vx, SAFE_TOP_PX + 60f, vx, height - SAFE_BOTTOM_PX - 90f, uiPaint);
        }

        drawButterflies(canvas, time);
        drawGrassForeground(canvas, time);
        drawGutterRail(canvas, width * 0.05f, Color.parseColor("#162016"),
                Color.parseColor("#DCDCAA"), time, 0.68f, 0.3f);
        drawMinimapColumn(canvas, Color.parseColor("#1F3026"),
                Color.parseColor("#2F4A36"), Color.parseColor("#DCDCAA"), time, 0.42f);
    }

    private void drawMultithreadFoundryBackground(Canvas canvas, float time) {
        float width = surfaceWidth;
        float height = surfaceHeight;
        paintSolidBackground(canvas, Color.parseColor("#2A2A2A"));

        drawIndustrialTabs(canvas, time);

        float frameSpacing = Math.max(width * 0.18f, 200f);
        float frameOffset = computeLoopOffset(time, BASE_SCROLL_SPEED * 0.22f, frameSpacing);
        uiPaint.setColor(Color.parseColor("#1F1F1F"));
        for (float x = -frameSpacing; x < width + frameSpacing; x += frameSpacing) {
            float left = x - frameOffset;
            canvas.drawRect(left, SAFE_TOP_PX, left + 12f, height - SAFE_BOTTOM_PX, uiPaint);
        }

        float gearSpacing = Math.max(width * 0.28f, 320f);
        float gearOffset = computeLoopOffset(time, BASE_SCROLL_SPEED * 0.5f, gearSpacing);
        for (float x = -gearSpacing; x < width + gearSpacing; x += gearSpacing) {
            float cx = x - gearOffset + gearSpacing * 0.5f;
            drawGearRosette(canvas, cx, height * 0.46f, gearSpacing * 0.24f, time);
        }

        drawConveyor(canvas, time);
        drawServoArms(canvas, time);
        drawGutterRail(canvas, width * 0.05f, Color.parseColor("#1A1A1A"),
                Color.parseColor("#2BB9A0"), time, 0.78f, 0.44f);
        drawMinimapColumn(canvas, Color.parseColor("#1C2F2C"),
                Color.parseColor("#28514E"), Color.parseColor("#2BB9A0"), time, 0.48f);
    }

    private void drawNullPointerNexusBackground(Canvas canvas, float time) {
        float width = surfaceWidth;
        float height = surfaceHeight;
        paintSolidBackground(canvas, Color.parseColor("#141414"));

        drawBrokenPanelTabs(canvas, time);

        float panelSpacing = Math.max(width * 0.32f, 360f);
        float panelOffset = computeLoopOffset(time, BASE_SCROLL_SPEED * 0.26f, panelSpacing);
        for (float x = -panelSpacing; x < width + panelSpacing; x += panelSpacing) {
            float left = x - panelOffset;
            drawBsodShard(canvas, left, SAFE_TOP_PX + height * 0.12f,
                    panelSpacing * 0.8f, height * 0.32f);
        }

        uiPaint.setStyle(Paint.Style.STROKE);
        uiPaint.setColor(Color.parseColor("#F14C4C"));
        uiPaint.setStrokeWidth(6f);
        float radius = Math.min(width, height) * 0.36f;
        canvas.drawCircle(width * 0.5f, height * 0.54f,
                radius + (float) Math.sin(time * 0.7f) * 12f, uiPaint);
        uiPaint.setStyle(Paint.Style.FILL);

        float totemSpacing = Math.max(width * 0.24f, 280f);
        float totemOffset = computeLoopOffset(time, BASE_SCROLL_SPEED * 0.5f, totemSpacing);
        for (float x = -totemSpacing; x < width + totemSpacing; x += totemSpacing) {
            float cx = x - totemOffset + totemSpacing * 0.5f;
            drawBossTotem(canvas, cx, height * 0.56f, height * 0.28f, time);
        }

        drawTryCatchShield(canvas, time);
        drawArenaAnchors(canvas, time);
        drawGutterRail(canvas, width * 0.05f, Color.parseColor("#0E0E0E"),
                Color.parseColor("#4FC1FF"), time, 0.82f, 0.52f);
        drawMinimapColumn(canvas, Color.parseColor("#202F3A"),
                Color.parseColor("#30505F"), Color.parseColor("#4FC1FF"), time, 0.58f);
    }

    private void paintSolidBackground(Canvas canvas, int color) {
        uiPaint.setStyle(Paint.Style.FILL);
        uiPaint.setColor(color);
        canvas.drawRect(0f, 0f, surfaceWidth, surfaceHeight, uiPaint);
    }

    private void drawStatusBar(Canvas canvas) {
        uiPaint.setStyle(Paint.Style.FILL);
        uiPaint.setColor(Color.parseColor("#007ACC"));
        canvas.drawRect(0f, surfaceHeight - SAFE_BOTTOM_PX, surfaceWidth, surfaceHeight, uiPaint);
    }

    private void drawScanlineOverlay(Canvas canvas) {
        uiPaint.setStyle(Paint.Style.FILL);
        uiPaint.setColor(Color.argb(18, 255, 255, 255));
        for (float y = SAFE_TOP_PX; y < surfaceHeight - SAFE_BOTTOM_PX; y += 4f) {
            canvas.drawRect(0f, y, surfaceWidth, y + 1f, uiPaint);
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

    private void drawIndentGuides(Canvas canvas, float startX, float top, float bottom,
                                  float spacing, int color, float offset) {
        uiPaint.setStyle(Paint.Style.STROKE);
        uiPaint.setStrokeWidth(2f);
        uiPaint.setColor(color);
        for (float x = startX - spacing; x < surfaceWidth + spacing; x += spacing) {
            float cx = x - offset;
            canvas.drawLine(cx, top, cx, bottom, uiPaint);
        }
        uiPaint.setStyle(Paint.Style.FILL);
    }

    private void drawHillBand(Canvas canvas, float baseY, float bottom, float period,
                              float offset, float amplitude, int color) {
        uiPaint.setStyle(Paint.Style.FILL);
        uiPaint.setColor(color);
        Path path = new Path();
        path.moveTo(-surfaceWidth, bottom);
        for (float x = -period; x <= surfaceWidth + period; x += period / 2f) {
            float px = x - offset;
            float py = baseY + (float) Math.sin((px / period) * Math.PI * 2f) * amplitude;
            path.lineTo(px, py);
        }
        path.lineTo(surfaceWidth * 2f, bottom);
        path.close();
        canvas.drawPath(path, uiPaint);
    }

    private void drawGutterRail(Canvas canvas, float width, int baseColor, int lightColor,
                                 float time, float speedFactor, float glowStrength) {
        float bottom = surfaceHeight - SAFE_BOTTOM_PX;
        uiPaint.setStyle(Paint.Style.FILL);
        uiPaint.setColor(baseColor);
        canvas.drawRect(0f, SAFE_TOP_PX, width, bottom, uiPaint);

        uiPaint.setColor(Color.parseColor("#252526"));
        float verticalOffset = computeLoopOffset(time, BASE_SCROLL_SPEED * speedFactor, width * 0.6f);
        canvas.drawRect(verticalOffset - width * 0.25f, SAFE_TOP_PX,
                verticalOffset - width * 0.25f + 3f, bottom, uiPaint);

        float blink = (float) ((Math.sin(time * Math.PI / 1.5f) + 1f) * 0.5f);
        int alpha = (int) (80 + 120 * blink * glowStrength);
        uiPaint.setColor(Color.argb(alpha, Color.red(lightColor), Color.green(lightColor), Color.blue(lightColor)));
        float spacing = 68f;
        for (float y = SAFE_TOP_PX + 48f; y < bottom - 24f; y += spacing) {
            float wobble = (float) Math.sin(time * 3f + y * 0.05f) * 2f;
            canvas.drawCircle(width * 0.55f + wobble, y, width * 0.26f, uiPaint);
        }
    }

    private void drawMinimapColumn(Canvas canvas, int baseColor, int accentColor,
                                   int glowColor, float time, float shimmerSpeed) {
        float width = surfaceWidth;
        float height = surfaceHeight;
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

    private void drawTabArchRow(Canvas canvas, float time, int baseColor, int accentColor) {
        float width = surfaceWidth;
        float period = Math.max(width / 4f, 320f);
        float offset = computeLoopOffset(time, BASE_SCROLL_SPEED * 0.18f, period);
        for (float x = -period; x < width + period; x += period) {
            float left = x - offset;
            RectF outer = new RectF(left + period * 0.1f, 10f,
                    left + period * 0.74f, SAFE_TOP_PX - 12f);
            uiPaint.setColor(baseColor);
            canvas.drawRoundRect(outer, 22f, 22f, uiPaint);
            uiPaint.setColor(accentColor);
            canvas.drawRoundRect(new RectF(outer.left + 12f, outer.top + 10f,
                    outer.right - 12f, outer.bottom - 10f), 18f, 18f, uiPaint);
        }
        uiPaint.setColor(Color.parseColor("#C586C0"));
        uiPaint.setTextAlign(Paint.Align.CENTER);
        uiPaint.setTextSize(SAFE_TOP_PX * 0.36f);
        canvas.drawText("<>", width * 0.5f, SAFE_TOP_PX * 0.7f, uiPaint);
    }

    private void drawGenericArch(Canvas canvas, float left, float top, float width,
                                 float height, int outerColor, int innerColor) {
        RectF rect = new RectF(left, top, left + width, top + height);
        uiPaint.setStyle(Paint.Style.FILL);
        uiPaint.setColor(outerColor);
        canvas.drawRoundRect(rect, width * 0.42f, width * 0.42f, uiPaint);
        uiPaint.setColor(innerColor);
        RectF inner = new RectF(rect.left + width * 0.08f, rect.top + width * 0.08f,
                rect.right - width * 0.08f, rect.bottom);
        canvas.drawRoundRect(inner, width * 0.38f, width * 0.38f, uiPaint);
    }

    private void drawFoldTriangle(Canvas canvas, float left, float top,
                                  float width, float height) {
        Path path = new Path();
        path.moveTo(left, top);
        path.lineTo(left + width, top);
        path.lineTo(left + width * 0.5f, top + height);
        path.close();
        canvas.drawPath(path, uiPaint);
    }

    private void drawPaneForeground(Canvas canvas, int darkColor, int lightColor, float time) {
        float width = surfaceWidth;
        float height = surfaceHeight;
        uiPaint.setStyle(Paint.Style.STROKE);
        uiPaint.setStrokeWidth(6f);
        uiPaint.setColor(darkColor);
        float offset = computeLoopOffset(time, BASE_SCROLL_SPEED * 0.72f, width * 0.42f);
        for (float x = -width; x < width + width; x += width * 0.42f) {
            float cx = x - offset;
            canvas.drawLine(cx, SAFE_TOP_PX, cx, height - SAFE_BOTTOM_PX, uiPaint);
        }
        uiPaint.setColor(lightColor);
        canvas.drawLine(0f, height * 0.52f, width, height * 0.52f, uiPaint);
        uiPaint.setStyle(Paint.Style.FILL);
    }

    private void drawNamespaceRibbon(Canvas canvas, float left, float centerY,
                                     float width, float height, float time) {
        Path path = new Path();
        float wave = (float) Math.sin(time + left * 0.01f) * height * 0.28f;
        path.moveTo(left, centerY - height * 0.5f);
        path.quadTo(left + width * 0.33f, centerY - height * 0.5f + wave,
                left + width * 0.5f, centerY);
        path.quadTo(left + width * 0.66f, centerY + height * 0.5f - wave,
                left + width, centerY + height * 0.5f);
        path.lineTo(left + width, centerY + height * 0.5f + 14f);
        path.quadTo(left + width * 0.66f, centerY + height * 0.5f + 14f - wave,
                left + width * 0.5f, centerY + 14f);
        path.quadTo(left + width * 0.33f, centerY - height * 0.5f + wave + 14f,
                left, centerY - height * 0.5f + 14f);
        path.close();
        uiPaint.setColor(Color.parseColor("#C586C0"));
        uiPaint.setAlpha(180);
        canvas.drawPath(path, uiPaint);
        uiPaint.setAlpha(255);
    }

    private void drawCursorMeteors(Canvas canvas, float time, int color) {
        uiPaint.setStyle(Paint.Style.STROKE);
        uiPaint.setStrokeWidth(2f);
        uiPaint.setColor(color);
        for (int i = 0; i < 4; i++) {
            float progress = (time * 0.2f + i * 0.25f) % 1f;
            float x = surfaceWidth * (0.2f + progress * 0.6f);
            float y = SAFE_TOP_PX + surfaceHeight * 0.2f + (float) Math.sin(time * 1.6f + i) * 40f;
            canvas.drawLine(x, y, x + 14f, y + 2f, uiPaint);
        }
        uiPaint.setStyle(Paint.Style.FILL);
    }

    private void drawTabAshClouds(Canvas canvas, float time) {
        float width = surfaceWidth;
        float period = Math.max(width / 3.6f, 260f);
        float offset = computeLoopOffset(time, BASE_SCROLL_SPEED * 0.22f, period);
        for (float x = -period; x < width + period; x += period) {
            float left = x - offset;
            RectF cloud = new RectF(left + period * 0.05f, 12f,
                    left + period * 0.8f, SAFE_TOP_PX - 8f);
            uiPaint.setColor(Color.parseColor("#2B2B2B"));
            canvas.drawRoundRect(cloud, 28f, 28f, uiPaint);
            uiPaint.setColor(Color.parseColor("#3A1F1F"));
            canvas.drawRoundRect(new RectF(cloud.left + 10f, cloud.top + 6f,
                    cloud.right - 10f, cloud.bottom - 12f), 24f, 24f, uiPaint);
        }
    }

    private void drawSquiggleFlow(Canvas canvas, float left, float top, float height, int color) {
        Path path = new Path();
        path.moveTo(left, top);
        float amplitude = height * 0.12f;
        int segments = 12;
        for (int i = 1; i <= segments; i++) {
            float x = left + (i / (float) segments) * (surfaceWidth * 0.18f);
            float y = top + i * (height / segments);
            float controlX = left + (i - 0.5f) * (surfaceWidth * 0.18f / segments);
            float controlY = top + (i - 0.5f) * (height / segments) +
                    (float) Math.sin((i * 0.8f) + top * 0.01f) * amplitude;
            path.quadTo(controlX, controlY, x, y);
        }
        uiPaint.setStyle(Paint.Style.STROKE);
        uiPaint.setStrokeWidth(6f);
        uiPaint.setColor(color);
        canvas.drawPath(path, uiPaint);
        uiPaint.setStyle(Paint.Style.FILL);
    }

    private void drawBreakpointChimney(Canvas canvas, float centerX, float centerY,
                                       float height, float time) {
        uiPaint.setStyle(Paint.Style.FILL);
        uiPaint.setColor(Color.parseColor("#2B1A1A"));
        RectF base = new RectF(centerX - 28f, centerY - height * 0.5f,
                centerX + 28f, centerY + height * 0.5f);
        canvas.drawRoundRect(base, 22f, 22f, uiPaint);
        float glow = (float) ((Math.sin(time * 1.5f + centerX * 0.01f) + 1f) * 0.5f);
        uiPaint.setColor(Color.argb((int) (120 + 80 * glow), 241, 76, 76));
        canvas.drawCircle(centerX, base.top + height * 0.2f, 22f, uiPaint);
        uiPaint.setColor(Color.argb((int) (100 + 60 * glow), 204, 167, 0));
        canvas.drawCircle(centerX, base.top + height * 0.5f, 12f, uiPaint);
    }

    private void drawTerminalPlate(Canvas canvas, float time) {
        float width = surfaceWidth;
        float height = surfaceHeight;
        uiPaint.setStyle(Paint.Style.FILL);
        uiPaint.setColor(Color.parseColor("#242424"));
        canvas.drawRect(width * 0.18f, height * 0.78f,
                width * 0.92f, height - SAFE_BOTTOM_PX - 12f, uiPaint);
        uiPaint.setColor(Color.parseColor("#333333"));
        float gridSpacing = 36f;
        float offset = computeLoopOffset(time, BASE_SCROLL_SPEED * 0.6f, gridSpacing);
        for (float x = width * 0.18f; x < width * 0.92f; x += gridSpacing) {
            float gx = x - offset;
            canvas.drawLine(gx, height * 0.78f, gx, height - SAFE_BOTTOM_PX - 12f, uiPaint);
        }
    }

    private void drawHeatDistortion(Canvas canvas, float time) {
        uiPaint.setStyle(Paint.Style.FILL);
        uiPaint.setColor(Color.argb(40, 255, 120, 0));
        for (int i = 0; i < 5; i++) {
            float phase = time * 0.6f + i * 0.7f;
            float top = SAFE_TOP_PX + surfaceHeight * 0.34f + (float) Math.sin(phase) * 12f;
            canvas.drawRect(0f, top, surfaceWidth, top + 6f, uiPaint);
        }
    }

    private void drawBillboardTabs(Canvas canvas, float time) {
        float width = surfaceWidth;
        float period = Math.max(width / 4f, 320f);
        float offset = computeLoopOffset(time, BASE_SCROLL_SPEED * 0.22f, period);
        for (float x = -period; x < width + period; x += period) {
            float left = x - offset;
            RectF tab = new RectF(left + period * 0.08f, 12f,
                    left + period * 0.75f, SAFE_TOP_PX - 10f);
            uiPaint.setColor(Color.parseColor("#1F1F1F"));
            canvas.drawRoundRect(tab, 20f, 20f, uiPaint);
            uiPaint.setColor(Color.parseColor("#007ACC"));
            canvas.drawRect(tab.left + 14f, tab.bottom - 16f, tab.right - 14f, tab.bottom - 8f, uiPaint);
        }
    }

    private void drawExplorerSkyline(Canvas canvas, float left, float top, float width,
                                     float height, float time) {
        uiPaint.setStyle(Paint.Style.FILL);
        float buildingWidth = width / 6f;
        for (int i = 0; i < 6; i++) {
            float bx = left + i * buildingWidth;
            float hFactor = 0.4f + (i % 3) * 0.18f;
            float buildingHeight = height * hFactor;
            RectF building = new RectF(bx, top + height - buildingHeight,
                    bx + buildingWidth * 0.72f, top + height);
            uiPaint.setColor(Color.parseColor("#252526"));
            canvas.drawRect(building, uiPaint);
            uiPaint.setColor(Color.parseColor("#333333"));
            float windowHeight = 14f;
            float offset = computeLoopOffset(time, BASE_SCROLL_SPEED * 0.5f, windowHeight * 3f);
            for (float y = building.top + 18f; y < building.bottom - 12f; y += windowHeight * 3f) {
                float wy = y - offset;
                canvas.drawRect(building.left + 12f, wy,
                        building.right - 12f, wy + windowHeight, uiPaint);
            }
        }
    }

    private void drawTooltipBillboard(Canvas canvas, float left, float centerY,
                                      float width, float height, float time) {
        RectF rect = new RectF(left, centerY - height * 0.5f, left + width, centerY + height * 0.5f);
        uiPaint.setStyle(Paint.Style.FILL);
        uiPaint.setColor(Color.parseColor("#2A2D33"));
        canvas.drawRoundRect(rect, 18f, 18f, uiPaint);
        uiPaint.setColor(Color.parseColor("#3A3D44"));
        canvas.drawRoundRect(new RectF(rect.left + 10f, rect.top + 10f,
                rect.right - 10f, rect.bottom - 10f), 16f, 16f, uiPaint);
        uiPaint.setColor(Color.parseColor("#DCDCAA"));
        uiPaint.setTextAlign(Paint.Align.LEFT);
        uiPaint.setTextSize(height * 0.28f);
        canvas.drawText("intellisense", rect.left + 18f,
                rect.centerY() + (float) Math.sin(time + left) * 6f, uiPaint);
    }

    private void drawTransitForeground(Canvas canvas, float time) {
        float height = surfaceHeight;
        uiPaint.setStyle(Paint.Style.FILL);
        uiPaint.setColor(Color.parseColor("#202020"));
        canvas.drawRect(0f, height * 0.74f, surfaceWidth, height * 0.74f + 18f, uiPaint);
        uiPaint.setColor(Color.parseColor("#007ACC"));
        float segmentWidth = 48f;
        float offset = computeLoopOffset(time, BASE_SCROLL_SPEED * 1.2f, segmentWidth * 2f);
        for (float x = -segmentWidth; x < surfaceWidth + segmentWidth; x += segmentWidth * 2f) {
            float left = x - offset;
            canvas.drawRect(left, height * 0.74f, left + segmentWidth, height * 0.74f + 18f, uiPaint);
        }
        uiPaint.setColor(Color.argb(160, 220, 220, 220));
        float lightSpacing = 72f;
        float lightOffset = computeLoopOffset(time, BASE_SCROLL_SPEED * 0.6f, lightSpacing);
        for (float x = -lightSpacing; x < surfaceWidth + lightSpacing; x += lightSpacing) {
            float cx = x - lightOffset;
            canvas.drawCircle(cx, height * 0.7f, 6f, uiPaint);
        }
    }

    private void drawTabStalactites(Canvas canvas, float time) {
        float width = surfaceWidth;
        float period = Math.max(width / 4.5f, 240f);
        float offset = computeLoopOffset(time, BASE_SCROLL_SPEED * 0.18f, period);
        for (float x = -period; x < width + period; x += period) {
            float left = x - offset;
            Path path = new Path();
            path.moveTo(left + period * 0.1f, 12f);
            path.lineTo(left + period * 0.4f, SAFE_TOP_PX - 18f);
            path.lineTo(left + period * 0.7f, 12f);
            path.close();
            uiPaint.setColor(Color.parseColor("#202020"));
            canvas.drawPath(path, uiPaint);
        }
    }

    private void drawGarbageChunk(Canvas canvas, float left, float baseline,
                                  float width, float height, float time) {
        RectF rect = new RectF(left, baseline - height * 0.6f,
                left + width, baseline + height * 0.4f);
        uiPaint.setColor(Color.parseColor("#202020"));
        canvas.drawRoundRect(rect, 12f, 12f, uiPaint);
        uiPaint.setColor(Color.parseColor("#2A2A2A"));
        canvas.drawRoundRect(new RectF(rect.left + 8f, rect.top + 8f,
                rect.right - 8f, rect.bottom - 8f), 10f, 10f, uiPaint);
        uiPaint.setColor(Color.argb(140, 79, 193, 255));
        float blink = (float) ((Math.sin(time * 2f + left * 0.01f) + 1f) * 0.5f);
        canvas.drawCircle(rect.centerX(), rect.top + height * 0.1f, 6f + blink * 4f, uiPaint);
    }

    private void drawCavernFog(Canvas canvas, float time) {
        uiPaint.setStyle(Paint.Style.FILL);
        uiPaint.setColor(Color.argb(70, 20, 40, 56));
        for (int i = 0; i < 4; i++) {
            float offset = (float) Math.sin(time * 0.6f + i) * 40f;
            canvas.drawRect(0f, SAFE_TOP_PX + surfaceHeight * 0.4f + offset,
                    surfaceWidth, SAFE_TOP_PX + surfaceHeight * 0.5f + offset, uiPaint);
        }
    }

    private void drawBannerTabs(Canvas canvas, float time, int baseColor, int accentColor) {
        float width = surfaceWidth;
        float period = Math.max(width / 4f, 300f);
        float offset = computeLoopOffset(time, BASE_SCROLL_SPEED * 0.18f, period);
        for (float x = -period; x < width + period; x += period) {
            float left = x - offset;
            RectF banner = new RectF(left + period * 0.08f, 14f,
                    left + period * 0.7f, SAFE_TOP_PX - 8f);
            uiPaint.setColor(baseColor);
            canvas.drawRoundRect(banner, 20f, 20f, uiPaint);
            uiPaint.setColor(accentColor);
            canvas.drawRoundRect(new RectF(banner.left + 14f, banner.top + 10f,
                    banner.right - 14f, banner.bottom - 12f), 16f, 16f, uiPaint);
        }
    }

    private void drawCurlyTree(Canvas canvas, float centerX, float baseY,
                                float width, float height) {
        uiPaint.setStyle(Paint.Style.STROKE);
        uiPaint.setColor(Color.parseColor("#2F422E"));
        uiPaint.setStrokeWidth(8f);
        Path trunk = new Path();
        trunk.moveTo(centerX, baseY);
        trunk.cubicTo(centerX - width * 0.2f, baseY - height * 0.2f,
                centerX + width * 0.2f, baseY - height * 0.6f,
                centerX, baseY - height);
        canvas.drawPath(trunk, uiPaint);
        uiPaint.setStyle(Paint.Style.FILL);
        uiPaint.setColor(Color.parseColor("#6A9955"));
        canvas.drawCircle(centerX - width * 0.2f, baseY - height * 0.6f, width * 0.28f, uiPaint);
        canvas.drawCircle(centerX + width * 0.2f, baseY - height * 0.7f, width * 0.24f, uiPaint);
        canvas.drawCircle(centerX, baseY - height * 0.85f, width * 0.3f, uiPaint);
    }

    private void drawButterflies(Canvas canvas, float time) {
        uiPaint.setColor(Color.parseColor("#DCDCAA"));
        uiPaint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < 6; i++) {
            float progress = (time * 0.3f + i * 0.15f) % 1f;
            float x = surfaceWidth * (0.1f + progress * 0.8f);
            float y = SAFE_TOP_PX + surfaceHeight * 0.3f + (float) Math.sin(time * 2f + i) * 12f;
            canvas.drawCircle(x - 6f, y, 6f, uiPaint);
            canvas.drawCircle(x + 6f, y, 6f, uiPaint);
        }
    }

    private void drawGrassForeground(Canvas canvas, float time) {
        uiPaint.setColor(Color.parseColor("#2F4A36"));
        for (int i = 0; i < 40; i++) {
            float x = i / 39f * surfaceWidth;
            float sway = (float) Math.sin(time * 1.2f + i * 0.4f) * 6f;
            canvas.drawLine(x, surfaceHeight - SAFE_BOTTOM_PX - 10f,
                    x + sway, surfaceHeight - SAFE_BOTTOM_PX - 42f, uiPaint);
        }
    }

    private void drawIndustrialTabs(Canvas canvas, float time) {
        float width = surfaceWidth;
        float period = Math.max(width / 4f, 320f);
        float offset = computeLoopOffset(time, BASE_SCROLL_SPEED * 0.2f, period);
        for (float x = -period; x < width + period; x += period) {
            float left = x - offset;
            RectF plate = new RectF(left + period * 0.08f, 12f,
                    left + period * 0.7f, SAFE_TOP_PX - 10f);
            uiPaint.setColor(Color.parseColor("#333333"));
            canvas.drawRoundRect(plate, 18f, 18f, uiPaint);
            uiPaint.setColor(Color.parseColor("#2BB9A0"));
            canvas.drawCircle(plate.left + 20f, plate.centerY(), 6f, uiPaint);
            canvas.drawCircle(plate.right - 20f, plate.centerY(), 6f, uiPaint);
        }
    }

    private void drawGearRosette(Canvas canvas, float cx, float cy, float radius, float time) {
        uiPaint.setStyle(Paint.Style.STROKE);
        uiPaint.setStrokeWidth(6f);
        uiPaint.setColor(Color.parseColor("#2BB9A0"));
        for (int i = 0; i < 6; i++) {
            float angle = (float) (i * Math.PI / 3f + time * 0.6f);
            float x = cx + (float) Math.cos(angle) * radius;
            float y = cy + (float) Math.sin(angle) * radius;
            canvas.drawLine(cx, cy, x, y, uiPaint);
        }
        uiPaint.setStyle(Paint.Style.FILL);
        uiPaint.setColor(Color.parseColor("#1F1F1F"));
        canvas.drawCircle(cx, cy, radius * 0.4f, uiPaint);
    }

    private void drawConveyor(Canvas canvas, float time) {
        float height = surfaceHeight;
        uiPaint.setColor(Color.parseColor("#1F3A43"));
        float bandTop = height * 0.78f;
        canvas.drawRect(0f, bandTop, surfaceWidth, bandTop + 28f, uiPaint);
        uiPaint.setColor(Color.parseColor("#2BB9A0"));
        float segment = 64f;
        float offset = computeLoopOffset(time, BASE_SCROLL_SPEED * 1.1f, segment * 2f);
        for (float x = -segment; x < surfaceWidth + segment; x += segment * 2f) {
            float left = x - offset;
            canvas.drawRect(left, bandTop, left + segment, bandTop + 28f, uiPaint);
        }
    }

    private void drawServoArms(Canvas canvas, float time) {
        uiPaint.setStyle(Paint.Style.STROKE);
        uiPaint.setStrokeWidth(6f);
        uiPaint.setColor(Color.parseColor("#CCA700"));
        for (int i = 0; i < 4; i++) {
            float baseX = surfaceWidth * (0.2f + i * 0.2f);
            float baseY = SAFE_TOP_PX + surfaceHeight * 0.2f;
            float swing = (float) Math.sin(time * 2f + i) * 24f;
            canvas.drawLine(baseX, baseY, baseX + swing, baseY + 90f, uiPaint);
        }
        uiPaint.setStyle(Paint.Style.FILL);
    }

    private void drawBrokenPanelTabs(Canvas canvas, float time) {
        float width = surfaceWidth;
        float period = Math.max(width / 4f, 320f);
        float offset = computeLoopOffset(time, BASE_SCROLL_SPEED * 0.24f, period);
        for (float x = -period; x < width + period; x += period) {
            float left = x - offset;
            RectF panel = new RectF(left + period * 0.08f, 10f,
                    left + period * 0.74f, SAFE_TOP_PX - 12f);
            uiPaint.setColor(Color.parseColor("#1F1F1F"));
            canvas.drawRoundRect(panel, 18f, 18f, uiPaint);
            uiPaint.setColor(Color.parseColor("#2C2C2C"));
            canvas.drawRect(panel.left + 12f, panel.top + 10f,
                    panel.right - 32f, panel.bottom - 10f, uiPaint);
            uiPaint.setColor(Color.parseColor("#F14C4C"));
            canvas.drawRect(panel.right - 28f, panel.top + 10f,
                    panel.right - 12f, panel.bottom - 10f, uiPaint);
        }
    }

    private void drawBsodShard(Canvas canvas, float left, float top,
                                float width, float height) {
        Path path = new Path();
        path.moveTo(left, top + height * 0.1f);
        path.lineTo(left + width * 0.4f, top);
        path.lineTo(left + width, top + height * 0.4f);
        path.lineTo(left + width * 0.6f, top + height);
        path.close();
        uiPaint.setColor(Color.parseColor("#1F2F44"));
        canvas.drawPath(path, uiPaint);
        uiPaint.setColor(Color.parseColor("#274B6B"));
        canvas.drawLine(left, top + height * 0.1f, left + width * 0.6f, top + height, uiPaint);
    }

    private void drawBossTotem(Canvas canvas, float cx, float cy, float height, float time) {
        uiPaint.setColor(Color.parseColor("#1E1E1E"));
        RectF body = new RectF(cx - 20f, cy - height * 0.5f,
                cx + 20f, cy + height * 0.5f);
        canvas.drawRoundRect(body, 14f, 14f, uiPaint);
        uiPaint.setColor(Color.parseColor("#F14C4C"));
        float glow = (float) ((Math.sin(time * 2f + cx * 0.02f) + 1f) * 0.5f);
        canvas.drawCircle(cx, body.top + height * 0.2f, 14f + glow * 6f, uiPaint);
        uiPaint.setColor(Color.parseColor("#4FC1FF"));
        canvas.drawCircle(cx, body.bottom - height * 0.2f, 10f + glow * 4f, uiPaint);
    }

    private void drawTryCatchShield(Canvas canvas, float time) {
        uiPaint.setStyle(Paint.Style.STROKE);
        uiPaint.setColor(Color.argb(140, 79, 193, 255));
        uiPaint.setStrokeWidth(5f);
        float radius = Math.min(surfaceWidth, surfaceHeight) * 0.3f;
        for (int i = 0; i < 3; i++) {
            float angle = time * 0.6f + i * 0.9f;
            float cx = surfaceWidth * 0.5f + (float) Math.cos(angle) * 24f;
            float cy = surfaceHeight * 0.58f + (float) Math.sin(angle) * 14f;
            canvas.drawArc(new RectF(cx - radius, cy - radius, cx + radius, cy + radius),
                    200f, 140f, false, uiPaint);
        }
        uiPaint.setStyle(Paint.Style.FILL);
    }

    private void drawArenaAnchors(Canvas canvas, float time) {
        uiPaint.setStyle(Paint.Style.FILL);
        uiPaint.setColor(Color.argb(180, 79, 193, 255));
        float[] anchors = new float[] {40f, surfaceWidth - 40f};
        for (float x : anchors) {
            canvas.drawRect(x - 8f, SAFE_TOP_PX + 10f, x + 8f, SAFE_TOP_PX + 50f, uiPaint);
            canvas.drawRect(x - 8f, surfaceHeight - SAFE_BOTTOM_PX - 50f,
                    x + 8f, surfaceHeight - SAFE_BOTTOM_PX - 10f, uiPaint);
        }
        uiPaint.setColor(Color.parseColor("#CCA700"));
        float pulse = (float) ((Math.sin(time * 1.5f) + 1f) * 0.5f);
        canvas.drawCircle(surfaceWidth * 0.5f, SAFE_TOP_PX + 30f, 8f + pulse * 4f, uiPaint);
        canvas.drawCircle(surfaceWidth * 0.5f, surfaceHeight - SAFE_BOTTOM_PX - 30f, 8f + pulse * 4f, uiPaint);
    }

    private void drawLevel(Canvas canvas) {
        if (robot == null) {
            return;
        }
        for (Tile tile : tiles) {
            tile.draw(canvas, worldPaint);
        }
        for (Coin coin : coins) {
            coin.draw(canvas, worldPaint);
        }
        for (Spike spike : spikes) {
            spike.draw(canvas, worldPaint);
        }
        if (flag != null) {
            flag.draw(canvas, worldPaint);
        }
        robot.draw(canvas, worldPaint);
    }

    private void drawControls(Canvas canvas) {
        uiPaint.setStyle(Paint.Style.FILL);
        uiPaint.setColor(Color.argb(200, 30, 30, 30));
        canvas.drawRect(0, surfaceHeight * 0.7f, surfaceWidth, surfaceHeight, uiPaint);

        uiPaint.setColor(Color.argb(220, 0, 122, 204));
        canvas.drawRect(0, surfaceHeight * 0.7f, surfaceWidth, surfaceHeight * 0.7f + 4f, uiPaint);

        leftButton.draw(canvas, uiPaint);
        rightButton.draw(canvas, uiPaint);
        jumpButton.draw(canvas, uiPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
                updateVirtualButtons(event);
                break;
            default:
                break;
        }
        return true;
    }

    private void updateVirtualButtons(MotionEvent event) {
        leftButton.setPressed(false);
        rightButton.setPressed(false);
        jumpButton.setPressed(false);
        for (int i = 0; i < event.getPointerCount(); i++) {
            float x = event.getX(i);
            float y = event.getY(i);
            if (leftButton.contains(x, y)) {
                leftButton.setPressed(true);
            }
            if (rightButton.contains(x, y)) {
                rightButton.setPressed(true);
            }
            if (jumpButton.contains(x, y)) {
                jumpButton.setPressed(true);
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            leftKeyDown = true;
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            rightKeyDown = true;
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_SPACE) {
            if (!jumpKeyDown) {
                jumpQueued = true;
            }
            jumpKeyDown = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            leftKeyDown = false;
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            rightKeyDown = false;
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_SPACE) {
            jumpKeyDown = false;
            return true;
        }
        return false;
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        surfaceWidth = width;
        surfaceHeight = height;
        camera.setViewport(width, height);
        ensureLevelForSelectedWorld();
        if (level != null) {
            camera.setWorldSize(level.getPixelWidth(), Math.max(level.getPixelHeight(), height));
        }

        float buttonSize = height * 0.18f;
        float margin = 32f;
        leftButton.setBounds(margin, height - buttonSize - margin, margin + buttonSize, height - margin);
        rightButton.setBounds(margin + buttonSize + 24f, height - buttonSize - margin, margin + buttonSize * 2f + 24f, height - margin);
        jumpButton.setBounds(width - buttonSize - margin, height - buttonSize - margin, width - margin, height - margin);
        if (running) {
            camera.snapTo(Math.max(0f, robot.getX() - width / 2f), Math.max(0f, robot.getY() - height / 2f));
        }
    }

    @Override
    public boolean onBackPressed() {
        sceneManager.switchTo(SceneType.MENU);
        return true;
    }
}
