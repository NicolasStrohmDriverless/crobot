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
import android.graphics.Typeface;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.crobot.game.enemy.AnimatedEnemy;
import com.crobot.game.enemy.EnemyAnimations;
import com.crobot.game.level.LegacyWorldData;
import com.crobot.game.level.LevelModel;
import com.example.robotparkour.audio.GameAudioManager;
import com.example.robotparkour.audio.WorldMusicLibrary;
import com.example.robotparkour.core.WorldInfo;
import com.example.robotparkour.util.TimeFormatter;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SurfaceView responsible for rendering the platformer level and updating the simulation.
 */
public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    public enum Control { LEFT, RIGHT, JUMP, DUCK }

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
    private static final int SAFE_TOP_PX = 64;
    private static final int SAFE_BOTTOM_PX = 48;
    private static final float BASE_SCROLL_SPEED = 120f;
    private static final float BOSS_MESSAGE_DURATION = 4.5f;
    private static final float BOSS_MESSAGE_FADE = 0.8f;
    private static final String BOSS_NAME = "KoopaByte";

    private final Paint backgroundPaint = new Paint();
    private final Paint entityPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint uiPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tileFallbackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint timerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bossTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
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
    private final List<EnemyInstance> enemies = new ArrayList<>();
    private final List<Projectile> projectiles = new ArrayList<>();
    private final List<DebugPlatform> debugPlatforms = new ArrayList<>();
    private final Map<String, GuardianGate> guardianGates = new HashMap<>();
    private final Random random = new Random();

    private boolean moveLeft;
    private boolean moveRight;
    private boolean jumpPressed;
    private boolean jumpConsumed;
    private boolean shouldPlayJumpSound;
    private boolean duckPressed;
    private boolean playerRespawnedThisFrame;
    private boolean isBossWorld;
    private float runTimerSeconds;
    private float screenShakeTimer;
    private float screenShakeDuration;
    private float screenShakeMagnitude;
    private float shakeOffsetX;
    private float shakeOffsetY;
    private boolean bossMessageVisible;
    private float bossMessageTimer;

    private float previousVx;

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

    private PlayerAction lastPlayerAction = PlayerAction.IDLE;

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
        timerPaint.setColor(Color.WHITE);
        timerPaint.setTextSize(32f);
        timerPaint.setShadowLayer(3f, 0f, 2f, Color.argb(180, 0, 0, 0));
        timerPaint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
        timerPaint.setTextAlign(Paint.Align.LEFT);
        bossTextPaint.setColor(Color.parseColor("#FF4C4C"));
        bossTextPaint.setTextSize(64f);
        bossTextPaint.setShadowLayer(8f, 0f, 0f, Color.argb(200, 0, 0, 0));
        bossTextPaint.setTextAlign(Paint.Align.CENTER);
        bossTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD));
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
        String worldName = currentWorldInfo != null ? currentWorldInfo.getName() : null;
        isBossWorld = worldName != null && worldName.toLowerCase(Locale.US).contains("boss");
        runTimerSeconds = 0f;
        screenShakeTimer = 0f;
        screenShakeDuration = 0f;
        screenShakeMagnitude = 0f;
        shakeOffsetX = 0f;
        shakeOffsetY = 0f;
        bossMessageVisible = false;
        bossMessageTimer = 0f;
        cameraX = 0f;
        cameraY = 0f;
        parallaxTimer = 0f;
        animationTimer = 0f;
        player.width = level.getTileWidth() * 0.82f;
        player.height = level.getTileHeight() * 1.65f;
        player.vx = 0f;
        previousVx = 0f;
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
        duckPressed = false;
        lastPlayerAction = PlayerAction.IDLE;

        boolean spawnFound = false;
        for (LevelModel.Entity entity : level.getEntities()) {
            if ("spawn".equalsIgnoreCase(entity.getType())) {
                player.x = entity.getX();
                player.y = entity.getY();
                player.spawnX = player.x;
                player.spawnY = player.y;
                spawnFound = true;
                break;
            }
        }
        if (!spawnFound) {
            player.x = level.getTileWidth() * 2.5f;
            player.y = level.getPixelHeight() - level.getTileHeight() * 2f;
            player.spawnX = player.x;
            player.spawnY = player.y;
        }

        player.standingHeight = level.getTileHeight() * 1.9f;
        player.crouchHeight = level.getTileHeight() * 0.95f;
        player.height = player.standingHeight;
        player.jumpVelocity = computeJumpVelocity(level);
        player.crouching = false;
        player.stickyTimer = 0f;
        player.slipTimer = 0f;
        player.jumpCooldownTimer = 0f;
        player.timeSlowTimer = 0f;
        player.touchedDebugSymbol = false;

        enemies.clear();
        projectiles.clear();
        debugPlatforms.clear();
        guardianGates.clear();
        buildEnemyInstances(level);

        if (isBossWorld) {
            triggerBossIntro(level);
        }

        loadTilesetBitmap(level.getTilesetAssetPath());
        updateScale(level);
        configureMusic();
    }

    private float computeJumpVelocity(@NonNull LevelModel level) {
        float tileHeight = Math.max(1f, level.getTileHeight());
        float desiredJumpHeight = tileHeight * 3f;
        return (float) -Math.sqrt(2f * GRAVITY * desiredJumpHeight);
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
        audioManager.setMusicTrack(WorldMusicLibrary.getTrackFor(getContext(), currentWorldInfo));
        audioManager.startMusic();
    }

    private void buildEnemyInstances(@NonNull LevelModel level) {
        enemies.clear();
        projectiles.clear();
        debugPlatforms.clear();
        guardianGates.clear();

        float tileWidth = Math.max(1f, level.getTileWidth());
        float tileHeight = Math.max(1f, level.getTileHeight());

        Map<String, EnemyInstance> leadersById = new HashMap<>();
        for (LevelModel.Entity entity : level.getEntities()) {
            String type = entity.getType();
            if (type == null) {
                continue;
            }
            EnemyKind kind = EnemyKind.fromType(type);
            if (kind == null) {
                continue;
            }
            AnimatedEnemy animatedSprite = EnemyAnimations.create(kind.typeName);
            EnemyInstance instance = new EnemyInstance(kind, entity.getX(), entity.getY(),
                    tileWidth, tileHeight, entity.getExtras(), animatedSprite);
            if (instance.kind == EnemyKind.PACKET_HOUND && !isBossWorld) {
                continue;
            }
            if (instance.kind == EnemyKind.PACKET_HOUND && isBossWorld) {
                instance.width = tileWidth * 1.6f;
                instance.height = tileHeight * 1.9f;
                instance.animatedSprite = null;
            }
            enemies.add(instance);
            if (kind == EnemyKind.BOTNET_BEE_LEADER && instance.swarmId != null) {
                leadersById.put(instance.swarmId, instance);
            }
            if ((kind == EnemyKind.TWOFA_GUARDIAN_JUMP || kind == EnemyKind.TWOFA_GUARDIAN_DASH)
                    && instance.channel != null && !guardianGates.containsKey(instance.channel)) {
                guardianGates.put(instance.channel, new GuardianGate());
            }
        }

        for (EnemyInstance enemy : enemies) {
            if (enemy.kind == EnemyKind.BOTNET_BEE_MINION && enemy.leaderId != null) {
                enemy.leader = leadersById.get(enemy.leaderId);
            }
        }
    }

    private void triggerBossIntro(@NonNull LevelModel level) {
        screenShakeDuration = 1.2f;
        screenShakeTimer = screenShakeDuration;
        float tileSize = Math.max(level.getTileWidth(), level.getTileHeight());
        screenShakeMagnitude = Math.max(tileSize * 0.6f, 18f);
        bossMessageVisible = true;
        bossMessageTimer = 0f;
    }

    private void updateEnemies(float deltaSeconds, @NonNull LevelModel level) {
        float levelWidth = level.getPixelWidth();
        float levelHeight = level.getPixelHeight();
        Iterator<EnemyInstance> iterator = enemies.iterator();
        while (iterator.hasNext()) {
            EnemyInstance enemy = iterator.next();
            if (!enemy.active) {
                iterator.remove();
                continue;
            }
            if (enemy.animatedSprite != null) {
                enemy.animatedSprite.update(deltaSeconds);
            }
            enemy.timer += deltaSeconds;
            enemy.stateTimer += deltaSeconds;

            switch (enemy.kind) {
                case BUGBLOB:
                    updateHopper(enemy, deltaSeconds, level,
                            new float[] { enemy.tileHeight * 0.9f, enemy.tileHeight * 1.6f, enemy.tileHeight * 0.9f });
                    break;
                case KEYLOGGER_BEETLE:
                    updateKeylogger(enemy, deltaSeconds, level);
                    break;
                case COOKIE_CRUMBLER:
                    updateGroundPatrol(enemy, deltaSeconds, level, 48f);
                    break;
                case BIT_BAT:
                    updateFlyer(enemy, deltaSeconds, 36f, enemy.tileHeight * 1.2f, 1.6f);
                    break;
                case PHISH_CARP:
                    updatePopper(enemy, deltaSeconds, enemy.tileHeight * 2.4f, 1.8f, false);
                    break;
                case SPAM_DRONE:
                    updateSpamDrone(enemy, deltaSeconds);
                    break;
                case CLOUD_LEECH:
                    updateCloudLeech(enemy, deltaSeconds);
                    break;
                case TROJAN_TURRET:
                    updateTrojanTurret(enemy, deltaSeconds, level);
                    break;
                case RANSOM_KNIGHT:
                    updateGroundPatrol(enemy, deltaSeconds, level, 56f);
                    break;
                case ROOTKIT_RAIDER:
                    updateRootkit(enemy, deltaSeconds);
                    break;
                case FIREWALL_GUARDIAN:
                    updateFirewallGuardian(enemy, deltaSeconds);
                    break;
                case POPUP_PIRANHA:
                    updatePopper(enemy, deltaSeconds, enemy.tileHeight * 2.2f, 1.4f, true);
                    break;
                case LAG_BUBBLE:
                    updateLagBubble(enemy, deltaSeconds);
                    break;
                case MEMORY_LEAK_SLIME:
                    updateMemoryLeak(enemy, deltaSeconds);
                    break;
                case CAPTCHA_GARGOYLE:
                    updateCaptcha(enemy, deltaSeconds);
                    break;
                case PACKET_HOUND:
                    updatePacketHound(enemy, deltaSeconds, level);
                    break;
                case BSOD_BLOCK:
                    updateBsodBlock(enemy, deltaSeconds);
                    break;
                case PATCH_GOLEM:
                    updatePatchGolem(enemy, deltaSeconds);
                    break;
                case GLITCH_SAW:
                    updateGlitchSaw(enemy, deltaSeconds);
                    break;
                case ADWARE_BALLOON:
                    updateAdwareBalloon(enemy, deltaSeconds);
                    break;
                case BOTNET_BEE_LEADER:
                    updateBeeLeader(enemy, deltaSeconds);
                    break;
                case BOTNET_BEE_MINION:
                    updateBeeMinion(enemy, deltaSeconds);
                    break;
                case WURM_WEASEL:
                    updateGroundPatrol(enemy, deltaSeconds, level, 72f);
                    break;
                case TREIBER_DRONE:
                    updateTreiberDrone(enemy, deltaSeconds, level);
                    break;
                case DRIVER_MODULE:
                    updateDriverModule(enemy, deltaSeconds, level);
                    break;
                case PORT_PLANT:
                    updatePopper(enemy, deltaSeconds, enemy.tileHeight * 2.0f, 1.2f, true);
                    break;
                case COMPILE_CRUSHER:
                    updateCompileCrusher(enemy, deltaSeconds);
                    break;
                case GARBAGE_COLLECTOR:
                    updateGarbageCollector(enemy, deltaSeconds);
                    break;
                case KERNEL_KOBOLD:
                    updateKernelKobold(enemy, deltaSeconds);
                    break;
                case VPN_VAMPYRE:
                    updateVpnVampire(enemy, deltaSeconds);
                    break;
                case UPDATE_OGRE:
                    updateUpdateOgre(enemy, deltaSeconds, level);
                    break;
                case TWOFA_GUARDIAN_JUMP:
                case TWOFA_GUARDIAN_DASH:
                    updateGuardian(enemy);
                    break;
                case CHECKSUM_CRAB:
                    updateChecksumCrab(enemy, deltaSeconds, level);
                    break;
                case PHISHING_SIREN:
                    updatePhishingSiren(enemy, deltaSeconds);
                    break;
            }

            enemy.x = clamp(enemy.x, enemy.width / 2f, Math.max(enemy.width / 2f, levelWidth - enemy.width / 2f));
            if (enemy.kind == EnemyKind.LAG_BUBBLE || enemy.kind == EnemyKind.GARBAGE_COLLECTOR) {
                enemy.y = clamp(enemy.y, enemy.height, levelHeight - enemy.height * 0.5f);
            } else if (enemy.kind != EnemyKind.PHISH_CARP && enemy.kind != EnemyKind.POPUP_PIRANHA && enemy.kind != EnemyKind.PORT_PLANT) {
                enemy.y = Math.min(enemy.y, levelHeight + enemy.height * 2f);
            }
            if (enemy.y > levelHeight + enemy.height * 2f) {
                enemy.active = false;
            }
        }
        enemies.removeIf(instance -> !instance.active);
    }

    private void updateProjectiles(float deltaSeconds, @NonNull LevelModel level) {
        float levelWidth = level.getPixelWidth();
        float levelHeight = level.getPixelHeight();
        Iterator<Projectile> iterator = projectiles.iterator();
        RectF playerBounds = player.getBounds();
        while (iterator.hasNext()) {
            Projectile projectile = iterator.next();
            projectile.lifetime -= deltaSeconds;
            if (projectile.gravity) {
                projectile.vy += GRAVITY * deltaSeconds;
            }
            projectile.x += projectile.vx * deltaSeconds;
            projectile.y += projectile.vy * deltaSeconds;
            if (projectile.lifetime <= 0f
                    || projectile.x < -projectile.radius
                    || projectile.x > levelWidth + projectile.radius
                    || projectile.y > levelHeight + projectile.radius * 2f) {
                iterator.remove();
                continue;
            }
            if (circleIntersects(playerBounds, projectile.x, projectile.y, projectile.radius)) {
                duckPressed = false;
                lastPlayerAction = PlayerAction.IDLE;
                player.respawn();
                onPlayerRespawned();
                iterator.remove();
            }
        }
    }

    private void updateDebugPlatforms(float deltaSeconds) {
        Iterator<DebugPlatform> iterator = debugPlatforms.iterator();
        while (iterator.hasNext()) {
            DebugPlatform platform = iterator.next();
            platform.lifetime -= deltaSeconds;
            if (platform.lifetime <= 0f) {
                iterator.remove();
            }
        }
    }

    private void updateHopper(@NonNull EnemyInstance enemy,
                              float deltaSeconds,
                              @NonNull LevelModel level,
                              @NonNull float[] jumpPattern) {
        float cadence = Math.max(0.4f, getExtraFloat(enemy, "cadence", 0.6f));
        enemy.stateTimer += deltaSeconds;
        if (enemy.onGround && enemy.stateTimer >= cadence) {
            int index = ((int) enemy.state) % jumpPattern.length;
            float desired = Math.max(level.getTileHeight() * 0.6f, jumpPattern[index]);
            enemy.vy = (float) -Math.sqrt(2f * GRAVITY * desired);
            enemy.onGround = false;
            enemy.stateTimer = 0f;
            enemy.state = (enemy.state + 1f) % jumpPattern.length;
        }
        if (!enemy.onGround) {
            enemy.vy += GRAVITY * deltaSeconds;
            enemy.y += enemy.vy * deltaSeconds;
            if (enemy.y >= enemy.baseY) {
                enemy.y = enemy.baseY;
                enemy.vy = 0f;
                enemy.onGround = true;
            }
        }
    }

    private void updateGroundPatrol(@NonNull EnemyInstance enemy,
                                    float deltaSeconds,
                                    @NonNull LevelModel level,
                                    float speed) {
        float patrolRange = Math.max(enemy.tileWidth * 2f,
                getExtraFloat(enemy, "range", enemy.tileWidth * 3.5f));
        float minX = enemy.baseX - patrolRange;
        float maxX = enemy.baseX + patrolRange;
        enemy.x += enemy.direction * speed * deltaSeconds;
        if (enemy.x <= minX) {
            enemy.x = minX;
            enemy.direction = 1;
        } else if (enemy.x >= maxX) {
            enemy.x = maxX;
            enemy.direction = -1;
        }
        enemy.onGround = true;
        enemy.baseY = Math.max(enemy.baseY, level.getTileHeight());
        enemy.y = enemy.baseY;
    }

    private void updateKeylogger(@NonNull EnemyInstance enemy,
                                 float deltaSeconds,
                                 @NonNull LevelModel level) {
        updateGroundPatrol(enemy, deltaSeconds, level, 52f);
        if (lastPlayerAction == PlayerAction.JUMP && enemy.onGround && enemy.stateTimer > 0.3f) {
            float hopHeight = level.getTileHeight() * 1.2f;
            enemy.vy = (float) -Math.sqrt(2f * GRAVITY * hopHeight);
            enemy.onGround = false;
            enemy.stateTimer = 0f;
        }
        if (!enemy.onGround) {
            enemy.vy += GRAVITY * deltaSeconds;
            enemy.y += enemy.vy * deltaSeconds;
            if (enemy.y >= enemy.baseY) {
                enemy.y = enemy.baseY;
                enemy.vy = 0f;
                enemy.onGround = true;
            }
        }
    }

    private void updateFlyer(@NonNull EnemyInstance enemy,
                             float deltaSeconds,
                             float speed,
                             float amplitude,
                             float periodSeconds) {
        enemy.timer += deltaSeconds;
        float frequency = (float) (Math.PI * 2f / Math.max(0.1f, periodSeconds));
        enemy.y = enemy.baseY + (float) Math.sin(enemy.timer * frequency) * amplitude;
        enemy.x += enemy.direction * speed * deltaSeconds;
        float range = Math.max(enemy.tileWidth * 3f, getExtraFloat(enemy, "range", enemy.tileWidth * 4f));
        if (enemy.x > enemy.baseX + range) {
            enemy.x = enemy.baseX + range;
            enemy.direction = -1;
        } else if (enemy.x < enemy.baseX - range) {
            enemy.x = enemy.baseX - range;
            enemy.direction = 1;
        }
    }

    private void updatePopper(@NonNull EnemyInstance enemy,
                               float deltaSeconds,
                               float riseHeight,
                               float riseSpeedMultiplier,
                               boolean waitForPlayer) {
        float cycle = Math.max(2.6f, getExtraFloat(enemy, "cycle", 3.4f));
        float riseSpeed = Math.max(36f, riseSpeedMultiplier * enemy.tileHeight);
        float restY = enemy.baseY;
        float peakY = restY - riseHeight;
        enemy.stateTimer += deltaSeconds;
        boolean trigger = !waitForPlayer || Math.abs(player.x - enemy.x) < enemy.tileWidth * 3.5f;
        if (!trigger) {
            enemy.stateTimer = 0f;
            enemy.y = restY;
            enemy.visible = false;
            return;
        }
        enemy.visible = true;
        float localTime = enemy.stateTimer % cycle;
        float riseDuration = Math.min(cycle * 0.35f, Math.max(0.4f, riseHeight / riseSpeed));
        if (localTime < riseDuration) {
            enemy.y = restY - (riseHeight * (localTime / riseDuration));
        } else if (localTime < riseDuration + 0.8f) {
            enemy.y = peakY;
        } else {
            float fallProgress = (localTime - riseDuration - 0.8f)
                    / Math.max(0.2f, cycle - (riseDuration + 0.8f));
            enemy.y = peakY + riseHeight * clamp(fallProgress, 0f, 1f);
        }
    }

    private void updateSpamDrone(@NonNull EnemyInstance enemy, float deltaSeconds) {
        enemy.timer += deltaSeconds;
        float angularSpeed = (float) (Math.PI * 0.6f);
        float radius = Math.max(enemy.tileWidth * 2.2f, getExtraFloat(enemy, "radius", enemy.tileWidth * 2.8f));
        float centerX = player.x;
        float centerY = player.y - player.height * 1.6f;
        float angle = enemy.timer * angularSpeed;
        enemy.x = centerX + (float) Math.cos(angle) * radius;
        enemy.y = centerY + (float) Math.sin(angle) * radius * 0.4f;
        float dropInterval = Math.max(1.2f, getExtraFloat(enemy, "drop_interval", 1.8f));
        if (enemy.timer - enemy.state >= dropInterval) {
            enemy.state = enemy.timer;
            spawnProjectile(enemy, enemy.x, enemy.y, 0f, 160f, enemy.tileWidth * 0.4f, 4f, true);
        }
    }

    private void updateCloudLeech(@NonNull EnemyInstance enemy, float deltaSeconds) {
        float followSpeed = Math.max(40f, getExtraFloat(enemy, "speed", 60f));
        float targetX = player.x;
        float targetY = player.y - player.height * 1.8f;
        enemy.x += clamp(targetX - enemy.x, -followSpeed, followSpeed) * deltaSeconds;
        float baseY = enemy.y + clamp(targetY - enemy.y, -followSpeed, followSpeed) * deltaSeconds;
        enemy.timer += deltaSeconds;
        enemy.y = baseY + (float) Math.sin(enemy.timer * 1.6f) * enemy.tileHeight * 0.3f;
    }

    private void updateTrojanTurret(@NonNull EnemyInstance enemy,
                                    float deltaSeconds,
                                    @NonNull LevelModel level) {
        float triggerDistance = Math.max(enemy.tileWidth * 4f, getExtraFloat(enemy, "trigger", enemy.tileWidth * 5f));
        float distanceToPlayer = distance(enemy.x, enemy.y, player.x, player.y - player.height * 0.5f);
        enemy.stateTimer += deltaSeconds;
        if (distanceToPlayer < triggerDistance && enemy.stateTimer > 2.2f) {
            enemy.stateTimer = 0f;
            float spread = enemy.tileWidth * 0.4f;
            for (int i = -1; i <= 1; i++) {
                float vx = i * 40f;
                float vy = -180f - Math.abs(i) * 40f;
                spawnProjectile(enemy, enemy.x + i * spread, enemy.y - enemy.height * 0.6f,
                        vx, vy, enemy.tileWidth * 0.32f, 3.5f, true);
            }
        }
        enemy.y = enemy.baseY;
        enemy.x = enemy.baseX;
    }

    private void updateRootkit(@NonNull EnemyInstance enemy, float deltaSeconds) {
        enemy.stateTimer += deltaSeconds;
        switch ((int) enemy.state) {
            case 0:
                enemy.x = enemy.baseX;
                enemy.y = enemy.baseY;
                enemy.visible = true;
                if (enemy.stateTimer > 2.4f) {
                    enemy.state = 1f;
                    enemy.stateTimer = 0f;
                    enemy.visible = false;
                }
                break;
            case 1:
                if (enemy.stateTimer > 0.7f) {
                    enemy.state = 2f;
                    enemy.stateTimer = 0f;
                    enemy.visible = true;
                    float offset = enemy.tileWidth * 2.2f;
                    enemy.x = player.x + (player.facingRight ? -offset : offset);
                    enemy.y = player.y;
                }
                break;
            case 2:
                if (enemy.stateTimer > 1.6f) {
                    enemy.state = 0f;
                    enemy.stateTimer = 0f;
                    enemy.x = enemy.baseX;
                    enemy.y = enemy.baseY;
                    enemy.visible = true;
                }
                break;
            default:
                enemy.state = 0f;
                enemy.stateTimer = 0f;
                enemy.visible = true;
                break;
        }
    }

    private void updateFirewallGuardian(@NonNull EnemyInstance enemy, float deltaSeconds) {
        enemy.stateTimer += deltaSeconds;
        float cadence = Math.max(2f, getExtraFloat(enemy, "cadence", 2.6f));
        if (enemy.stateTimer >= cadence) {
            enemy.stateTimer = 0f;
            for (int i = 0; i < 6; i++) {
                float angle = (float) (i * Math.PI * 2f / 6f);
                float vx = (float) Math.cos(angle) * 140f;
                float vy = (float) Math.sin(angle) * 140f;
                spawnProjectile(enemy, enemy.x, enemy.y - enemy.height * 0.5f, vx, vy,
                        enemy.tileWidth * 0.35f, 3f, false);
            }
        }
        enemy.x = enemy.baseX;
        enemy.y = enemy.baseY;
    }

    private void updateLagBubble(@NonNull EnemyInstance enemy, float deltaSeconds) {
        enemy.timer += deltaSeconds;
        enemy.y = enemy.baseY + (float) Math.sin(enemy.timer * 0.4f) * enemy.tileHeight * 0.6f;
        enemy.x = enemy.baseX + (float) Math.cos(enemy.timer * 0.2f) * enemy.tileWidth * 0.4f;
    }

    private void updateMemoryLeak(@NonNull EnemyInstance enemy, float deltaSeconds) {
        float growRate = Math.max(0.2f, getExtraFloat(enemy, "growth", 0.3f));
        enemy.height = clamp(enemy.height + enemy.tileHeight * growRate * deltaSeconds,
                enemy.tileHeight * 0.6f, enemy.tileHeight * 3.5f);
        enemy.y = enemy.baseY;
    }

    private void updateCaptcha(@NonNull EnemyInstance enemy, float deltaSeconds) {
        enemy.stateTimer += deltaSeconds;
        if (enemy.stateTimer > 2f) {
            enemy.stateTimer = 0f;
            enemy.state = (enemy.state + 1f) % 4f;
        }
        enemy.visible = enemy.stateTimer > 0.5f;
    }

    private void updatePacketHound(@NonNull EnemyInstance enemy,
                                   float deltaSeconds,
                                   @NonNull LevelModel level) {
        float chaseSpeed = Math.max(80f, getExtraFloat(enemy, "speed", 92f));
        if (isBossWorld) {
            chaseSpeed = Math.max(chaseSpeed, 140f);
        }
        float direction = player.x - enemy.x;
        int dir = direction < 0f ? -1 : 1;
        if (Math.abs(direction) < 1f) {
            dir = enemy.direction;
        }
        if (dir == 0) {
            dir = 1;
        }
        enemy.direction = dir;
        enemy.x += enemy.direction * chaseSpeed * deltaSeconds;
        float minX = enemy.tileWidth * 0.5f;
        float maxX = Math.max(minX, level.getPixelWidth() - enemy.tileWidth * 0.5f);
        enemy.x = clamp(enemy.x, minX, maxX);
        enemy.onGround = true;
        enemy.y = enemy.baseY;
    }

    private void updateBsodBlock(@NonNull EnemyInstance enemy, float deltaSeconds) {
        float holdTime = Math.max(0.6f, getExtraFloat(enemy, "freeze", 0.8f));
        if (enemy.state == 0f) {
            enemy.stateTimer += deltaSeconds;
            enemy.vy = 0f;
            enemy.y = enemy.baseY;
            if (enemy.stateTimer > holdTime) {
                enemy.state = 1f;
                enemy.stateTimer = 0f;
            }
        } else {
            enemy.vy += GRAVITY * deltaSeconds;
            enemy.y += enemy.vy * deltaSeconds;
            if (enemy.y > enemy.baseY + enemy.tileHeight * 8f) {
                enemy.active = false;
            }
        }
    }

    private void updatePatchGolem(@NonNull EnemyInstance enemy, float deltaSeconds) {
        float walkSpeed = Math.max(28f, getExtraFloat(enemy, "speed", 32f));
        enemy.x += enemy.direction * walkSpeed * deltaSeconds;
        float range = Math.max(enemy.tileWidth * 3f, getExtraFloat(enemy, "range", enemy.tileWidth * 4f));
        if (enemy.x >= enemy.baseX + range) {
            enemy.x = enemy.baseX + range;
            enemy.direction = -1;
        } else if (enemy.x <= enemy.baseX - range) {
            enemy.x = enemy.baseX - range;
            enemy.direction = 1;
        }
        enemy.stateTimer += deltaSeconds;
        if (enemy.stateTimer > 3.5f) {
            enemy.stateTimer = 0f;
            spawnDebugPlatform(enemy.x, enemy.baseY, enemy.tileWidth * 2.6f,
                    enemy.tileHeight * 0.8f, 3f);
            enemy.platformCarrier = true;
        } else if (enemy.platformCarrier && enemy.stateTimer > 2.8f) {
            enemy.platformCarrier = false;
        }
        enemy.y = enemy.baseY;
    }

    private void updateGlitchSaw(@NonNull EnemyInstance enemy, float deltaSeconds) {
        enemy.timer += deltaSeconds;
        float range = Math.max(enemy.tileWidth * 4f, getExtraFloat(enemy, "range", enemy.tileWidth * 5f));
        float speed = Math.max(90f, getExtraFloat(enemy, "speed", 120f));
        enemy.x = enemy.baseX + (float) Math.sin(enemy.timer * speed / range) * range;
        enemy.y = enemy.baseY;
        enemy.visible = ((int) (enemy.timer * 10f)) % 2 == 0;
    }

    private void updateAdwareBalloon(@NonNull EnemyInstance enemy, float deltaSeconds) {
        enemy.timer += deltaSeconds;
        enemy.x = enemy.baseX + (float) Math.sin(enemy.timer * 1.3f) * enemy.tileWidth * 2f;
        enemy.y = enemy.baseY + (float) Math.cos(enemy.timer * 1.6f) * enemy.tileHeight * 1.4f;
    }

    private void updateBeeLeader(@NonNull EnemyInstance enemy, float deltaSeconds) {
        enemy.timer += deltaSeconds;
        float circleRadius = Math.max(enemy.tileWidth * 2.2f, getExtraFloat(enemy, "radius", enemy.tileWidth * 2.4f));
        float angularSpeed = Math.max(1.6f, getExtraFloat(enemy, "omega", 2.2f));
        float angle = enemy.timer * angularSpeed;
        enemy.x = enemy.baseX + (float) Math.cos(angle) * circleRadius;
        enemy.y = enemy.baseY + (float) Math.sin(angle) * circleRadius * 0.7f;
    }

    private void updateBeeMinion(@NonNull EnemyInstance enemy, float deltaSeconds) {
        EnemyInstance leader = enemy.leader;
        if (leader == null || !leader.active) {
            updateFlyer(enemy, deltaSeconds, 60f, enemy.tileHeight, 2f);
            return;
        }
        float followLag = Math.max(0.2f, getExtraFloat(enemy, "lag", 0.35f));
        enemy.x += (leader.x - enemy.x) * clamp(deltaSeconds / followLag, 0f, 1f);
        enemy.y += (leader.y - enemy.y) * clamp(deltaSeconds / followLag, 0f, 1f);
    }

    private void updateTreiberDrone(@NonNull EnemyInstance enemy,
                                    float deltaSeconds,
                                    @NonNull LevelModel level) {
        updateGroundPatrol(enemy, deltaSeconds, level, 64f);
    }

    private void updateDriverModule(@NonNull EnemyInstance enemy,
                                    float deltaSeconds,
                                    @NonNull LevelModel level) {
        float speed = Math.max(120f, getExtraFloat(enemy, "speed", 150f));
        enemy.x += enemy.direction * speed * deltaSeconds;
        float minX = enemy.tileWidth * 0.5f;
        float maxX = Math.max(minX, level.getPixelWidth() - enemy.tileWidth * 0.5f);
        if (enemy.x <= minX || enemy.x >= maxX) {
            enemy.active = false;
        }
        enemy.y = enemy.baseY;
    }

    private void updateCompileCrusher(@NonNull EnemyInstance enemy, float deltaSeconds) {
        enemy.stateTimer += deltaSeconds;
        if (enemy.state == 0f && (lastPlayerAction == PlayerAction.MOVE_LEFT
                || lastPlayerAction == PlayerAction.MOVE_RIGHT)
                && Math.abs(player.x - enemy.x) < enemy.tileWidth * 2.5f) {
            enemy.state = 1f;
            enemy.stateTimer = 0f;
        }
        if (enemy.state == 1f) {
            enemy.vy += GRAVITY * deltaSeconds;
            enemy.y += enemy.vy * deltaSeconds;
            if (enemy.y > enemy.baseY + enemy.tileHeight * 6f) {
                enemy.active = false;
            }
        }
    }

    private void updateGarbageCollector(@NonNull EnemyInstance enemy, float deltaSeconds) {
        float pullRadius = Math.max(enemy.tileWidth * 5f, getExtraFloat(enemy, "radius", enemy.tileWidth * 6f));
        float dist = distance(enemy.x, enemy.y, player.x, player.y - player.height * 0.5f);
        if (dist < pullRadius) {
            float pullStrength = Math.max(30f, getExtraFloat(enemy, "pull", 60f));
            player.vx += clamp(enemy.x - player.x, -pullStrength, pullStrength) * deltaSeconds;
        }
        enemy.stateTimer += deltaSeconds;
        if (enemy.stateTimer > 4.5f) {
            enemy.stateTimer = 0f;
            for (EnemyInstance instance : enemies) {
                if (instance != enemy && instance.active
                        && distance(enemy.x, enemy.y, instance.x, instance.y) < pullRadius) {
                    instance.active = false;
                    spawnProjectile(enemy, enemy.x, enemy.y - enemy.height * 0.4f,
                            random.nextFloat() * 240f - 120f,
                            -220f - random.nextFloat() * 60f,
                            enemy.tileWidth * 0.3f, 3.2f, true);
                }
            }
        }
    }

    private void updateKernelKobold(@NonNull EnemyInstance enemy, float deltaSeconds) {
        enemy.stateTimer += deltaSeconds;
        if (enemy.stateTimer > 2.2f) {
            enemy.stateTimer = 0f;
            enemy.state = (enemy.state + 1f) % 3f;
        }
        float offset = enemy.tileWidth * 3.5f;
        int index = (int) enemy.state;
        enemy.x = enemy.baseX + (index - 1) * offset;
        enemy.y = enemy.baseY - (index == 1 ? enemy.tileHeight * 1.5f : 0f);
    }

    private void updateVpnVampire(@NonNull EnemyInstance enemy, float deltaSeconds) {
        enemy.stateTimer += deltaSeconds;
        float visibilityCycle = Math.max(3f, getExtraFloat(enemy, "cycle", 4.5f));
        float phase = enemy.stateTimer % visibilityCycle;
        enemy.visible = phase < visibilityCycle * 0.6f;
        enemy.x = enemy.baseX;
        enemy.y = enemy.baseY;
    }

    private void updateUpdateOgre(@NonNull EnemyInstance enemy,
                                  float deltaSeconds,
                                  @NonNull LevelModel level) {
        enemy.stateTimer += deltaSeconds;
        float pause = Math.max(1.5f, getExtraFloat(enemy, "pause", 2.2f));
        if (enemy.stateTimer > pause) {
            enemy.stateTimer = 0f;
            enemy.state = (enemy.state + 1f) % 2f;
        }
        if (enemy.state == 0f) {
            updateGroundPatrol(enemy, deltaSeconds, level, 40f);
        } else {
            enemy.onGround = true;
            enemy.vx = 0f;
        }
    }

    private void updateGuardian(@NonNull EnemyInstance enemy) {
        GuardianGate gate = guardianGates.get(enemy.channel);
        if (gate != null && gate.isOpen()) {
            enemy.deactivated = true;
            enemy.visible = false;
            enemy.active = false;
        }
        enemy.x = enemy.baseX;
        enemy.y = enemy.baseY;
    }

    private void updateChecksumCrab(@NonNull EnemyInstance enemy,
                                    float deltaSeconds,
                                    @NonNull LevelModel level) {
        float mirrorSpeed = Math.max(48f, getExtraFloat(enemy, "speed", 54f));
        if (lastPlayerAction == PlayerAction.MOVE_LEFT) {
            enemy.direction = -1;
        } else if (lastPlayerAction == PlayerAction.MOVE_RIGHT) {
            enemy.direction = 1;
        }
        enemy.x += enemy.direction * mirrorSpeed * deltaSeconds;
        float minX = enemy.tileWidth * 0.5f;
        float maxX = Math.max(minX, level.getPixelWidth() - enemy.tileWidth * 0.5f);
        if (enemy.x <= minX || enemy.x >= maxX) {
            enemy.direction *= -1;
        }
        enemy.y = enemy.baseY;
    }

    private void updatePhishingSiren(@NonNull EnemyInstance enemy, float deltaSeconds) {
        enemy.stateTimer += deltaSeconds;
        float cycle = Math.max(3f, getExtraFloat(enemy, "cycle", 3.8f));
        if (enemy.stateTimer > cycle) {
            enemy.stateTimer = 0f;
            enemy.state = (enemy.state + 1f) % 2f;
        }
        enemy.visible = enemy.state == 0f || enemy.stateTimer > cycle * 0.25f;
        enemy.x = enemy.baseX + (enemy.state == 0f ? 0f : enemy.tileWidth * 1.2f);
    }

    private void applyEnemyEffect(@NonNull EnemyInstance enemy,
                                  @NonNull RectF enemyBounds,
                                  @NonNull RectF playerBounds) {
        boolean stomp = player.vy > 0f && playerBounds.bottom <= enemyBounds.top + enemy.tileHeight * 0.45f;
        switch (enemy.kind) {
            case BUGBLOB:
            case COOKIE_CRUMBLER:
            case WURM_WEASEL:
            case BOTNET_BEE_MINION:
                if (stomp) {
                    defeatEnemy(enemy);
                    bouncePlayer();
                    return;
                }
                break;
            case TREIBER_DRONE:
                if (stomp) {
                    convertToDriverModule(enemy);
                    bouncePlayer();
                    return;
                }
                break;
            case DRIVER_MODULE:
                enemy.active = false;
                spawnDebugPlatform(enemy.x, enemy.y, enemy.tileWidth * 1.5f, enemy.tileHeight,
                        2.5f);
                return;
            case PATCH_GOLEM:
                if (stomp) {
                    bouncePlayer();
                    return;
                }
                break;
            case ADWARE_BALLOON:
                player.vx += (player.x < enemy.x ? -1f : 1f) * 120f;
                return;
            case LAG_BUBBLE:
                player.timeSlowTimer = Math.max(player.timeSlowTimer, 1.2f);
                return;
            case MEMORY_LEAK_SLIME:
                player.stickyTimer = Math.max(player.stickyTimer, 2.4f);
                return;
            case FIREWALL_GUARDIAN:
            case SPAM_DRONE:
            case TROJAN_TURRET:
            case ROOTKIT_RAIDER:
            case PHISH_CARP:
            case POPUP_PIRANHA:
            case BIT_BAT:
            case CLOUD_LEECH:
            case PHISHING_SIREN:
            case RANSOM_KNIGHT:
            case PACKET_HOUND:
            case BSOD_BLOCK:
            case GLITCH_SAW:
            case BOTNET_BEE_LEADER:
            case PORT_PLANT:
            case COMPILE_CRUSHER:
            case GARBAGE_COLLECTOR:
            case KERNEL_KOBOLD:
            case VPN_VAMPYRE:
            case UPDATE_OGRE:
            case TWOFA_GUARDIAN_JUMP:
            case TWOFA_GUARDIAN_DASH:
            case CHECKSUM_CRAB:
                break;
        }
        playerRespawnedThisFrame = true;
        player.respawn();
        onPlayerRespawned();
        previousVx = 0f;
        lastPlayerAction = PlayerAction.IDLE;
    }

    private void defeatEnemy(@NonNull EnemyInstance enemy) {
        enemy.active = false;
        enemy.visible = false;
    }

    private void convertToDriverModule(@NonNull EnemyInstance enemy) {
        enemy.kind = EnemyKind.DRIVER_MODULE;
        enemy.state = 0f;
        enemy.stateTimer = 0f;
        enemy.direction = player.facingRight ? 1 : -1;
        enemy.vx = enemy.direction * 180f;
        enemy.height = enemy.tileHeight * EnemyKind.DRIVER_MODULE.heightScale;
        enemy.width = enemy.tileWidth * EnemyKind.DRIVER_MODULE.widthScale;
        enemy.baseX = enemy.x;
        enemy.baseY = enemy.y;
        enemy.onGround = true;
    }

    private void bouncePlayer() {
        player.vy = player.jumpVelocity * 0.6f;
        player.onGround = false;
    }

    private void drawEnemySprite(@NonNull Canvas canvas,
                                 @NonNull EnemyInstance enemy,
                                 float left,
                                 float top,
                                 float right,
                                 float bottom) {
        AnimatedEnemy sprite = enemy.animatedSprite;
        if (sprite != null) {
            tempRectF.set(left, top, right, bottom);
            sprite.draw(canvas, tempRectF);
            if (!enemy.visible) {
                entityPaint.setColor(Color.argb(100, 255, 255, 255));
                canvas.drawRoundRect(tempRectF, tempRectF.height() * 0.35f,
                        tempRectF.height() * 0.35f, entityPaint);
            }
            return;
        }
        int color;
        switch (enemy.kind) {
            case BUGBLOB:
                color = Color.parseColor("#6EC6FF");
                break;
            case KEYLOGGER_BEETLE:
                color = Color.parseColor("#F06292");
                break;
            case COOKIE_CRUMBLER:
                color = Color.parseColor("#A1887F");
                break;
            case BIT_BAT:
                color = Color.parseColor("#9575CD");
                break;
            case PHISH_CARP:
                color = Color.parseColor("#4DD0E1");
                break;
            case SPAM_DRONE:
                color = Color.parseColor("#FFB74D");
                break;
            case CLOUD_LEECH:
                color = Color.parseColor("#B39DDB");
                break;
            case TROJAN_TURRET:
                color = Color.parseColor("#FF8A65");
                break;
            case RANSOM_KNIGHT:
                color = Color.parseColor("#90CAF9");
                break;
            case ROOTKIT_RAIDER:
                color = Color.parseColor("#FF7043");
                break;
            case FIREWALL_GUARDIAN:
                color = Color.parseColor("#FF5252");
                break;
            case POPUP_PIRANHA:
                color = Color.parseColor("#81C784");
                break;
            case LAG_BUBBLE:
                color = Color.parseColor("#E0F7FA");
                break;
            case MEMORY_LEAK_SLIME:
                color = Color.parseColor("#66BB6A");
                break;
            case CAPTCHA_GARGOYLE:
                color = Color.parseColor("#BDBDBD");
                break;
            case PACKET_HOUND:
                if (isBossWorld) {
                    drawKoopaByte(canvas, enemy, left, top, right, bottom);
                    return;
                }
                color = Color.parseColor("#A5D6A7");
                break;
            case BSOD_BLOCK:
                color = Color.parseColor("#2196F3");
                break;
            case PATCH_GOLEM:
                color = Color.parseColor("#8D6E63");
                break;
            case GLITCH_SAW:
                color = Color.parseColor("#CE93D8");
                break;
            case ADWARE_BALLOON:
                color = Color.parseColor("#FFF176");
                break;
            case BOTNET_BEE_LEADER:
            case BOTNET_BEE_MINION:
                color = Color.parseColor("#FFD54F");
                break;
            case WURM_WEASEL:
                color = Color.parseColor("#AEEA00");
                break;
            case TREIBER_DRONE:
                color = Color.parseColor("#FFAB40");
                break;
            case DRIVER_MODULE:
                color = Color.parseColor("#FFCC80");
                break;
            case PORT_PLANT:
                color = Color.parseColor("#4CAF50");
                break;
            case COMPILE_CRUSHER:
                color = Color.parseColor("#F44336");
                break;
            case GARBAGE_COLLECTOR:
                color = Color.parseColor("#455A64");
                break;
            case KERNEL_KOBOLD:
                color = Color.parseColor("#9CCC65");
                break;
            case VPN_VAMPYRE:
                color = Color.parseColor("#AB47BC");
                break;
            case UPDATE_OGRE:
                color = Color.parseColor("#8BC34A");
                break;
            case TWOFA_GUARDIAN_JUMP:
            case TWOFA_GUARDIAN_DASH:
                color = Color.parseColor("#26C6DA");
                break;
            case CHECKSUM_CRAB:
                color = Color.parseColor("#FF7043");
                break;
            case PHISHING_SIREN:
                color = Color.parseColor("#64B5F6");
                break;
            default:
                color = Color.MAGENTA;
                break;
        }
        entityPaint.setColor(color);
        canvas.drawRoundRect(new RectF(left, top, right, bottom), 12f, 12f, entityPaint);
        if (!enemy.visible) {
            entityPaint.setColor(Color.argb(100, 255, 255, 255));
            canvas.drawRect(left, top, right, bottom, entityPaint);
        }
    }

    private void drawKoopaByte(@NonNull Canvas canvas,
                               @NonNull EnemyInstance enemy,
                               float left,
                               float top,
                               float right,
                               float bottom) {
        float width = right - left;
        float height = bottom - top;
        Paint.Style originalStyle = entityPaint.getStyle();
        int originalColor = entityPaint.getColor();
        entityPaint.setStyle(Paint.Style.FILL);

        float pulse = (float) Math.sin(animationTimer * 6f) * 0.08f;

        entityPaint.setColor(Color.parseColor("#5C0A0A"));
        canvas.drawRoundRect(left + width * 0.05f, top + height * 0.24f,
                right - width * 0.05f, bottom, width * 0.18f, width * 0.18f, entityPaint);

        entityPaint.setColor(Color.parseColor("#C62828"));
        canvas.drawRoundRect(left + width * 0.12f, top + height * 0.08f,
                right - width * 0.12f, top + height * (0.6f + pulse * 0.05f), width * 0.25f, width * 0.25f, entityPaint);

        entityPaint.setColor(Color.argb(190, 255, 120, 64));
        canvas.drawRoundRect(left + width * 0.2f, top + height * (0.18f + pulse * 0.04f),
                right - width * 0.2f, top + height * 0.52f, width * 0.18f, width * 0.18f, entityPaint);

        entityPaint.setColor(Color.parseColor("#FFE082"));
        for (int i = 0; i < 4; i++) {
            float cx = left + width * (0.2f + 0.2f * i);
            drawSpike(canvas, cx, top + height * 0.04f, width * 0.08f, height * 0.24f);
        }
        entityPaint.setColor(Color.parseColor("#FFAB40"));
        drawSpike(canvas, left + width * 0.1f, top + height * 0.28f, width * 0.07f, height * 0.18f);
        drawSpike(canvas, right - width * 0.1f, top + height * 0.28f, width * 0.07f, height * 0.18f);

        entityPaint.setColor(Color.parseColor("#4E0707"));
        canvas.drawRoundRect(left + width * 0.18f, top + height * 0.34f,
                right - width * 0.18f, bottom - height * 0.15f, width * 0.1f, width * 0.1f, entityPaint);

        entityPaint.setColor(Color.parseColor("#FFD7D7"));
        float eyeY = top + height * 0.48f;
        float eyeRadius = width * 0.09f;
        canvas.drawCircle(left + width * 0.34f, eyeY, eyeRadius, entityPaint);
        canvas.drawCircle(right - width * 0.34f, eyeY, eyeRadius, entityPaint);
        entityPaint.setColor(Color.parseColor("#390000"));
        canvas.drawCircle(left + width * 0.34f, eyeY, eyeRadius * 0.55f, entityPaint);
        canvas.drawCircle(right - width * 0.34f, eyeY, eyeRadius * 0.55f, entityPaint);
        entityPaint.setColor(Color.argb(180, 255, 70, 90));
        canvas.drawCircle(left + width * 0.34f, eyeY, eyeRadius * 0.3f, entityPaint);
        canvas.drawCircle(right - width * 0.34f, eyeY, eyeRadius * 0.3f, entityPaint);

        entityPaint.setColor(Color.parseColor("#2B0000"));
        canvas.drawRoundRect(left + width * 0.28f, top + height * 0.58f,
                right - width * 0.28f, top + height * 0.7f, width * 0.08f, width * 0.08f, entityPaint);
        entityPaint.setColor(Color.parseColor("#FFB74D"));
        canvas.drawRect(left + width * 0.3f, top + height * 0.62f,
                left + width * 0.42f, top + height * 0.66f, entityPaint);
        canvas.drawRect(right - width * 0.42f, top + height * 0.62f,
                right - width * 0.3f, top + height * 0.66f, entityPaint);

        entityPaint.setColor(Color.parseColor("#FF6F61"));
        drawClaw(canvas, left + width * 0.2f, bottom - height * 0.08f, width * 0.12f, height * 0.22f, true);
        drawClaw(canvas, right - width * 0.2f, bottom - height * 0.08f, width * 0.12f, height * 0.22f, false);

        entityPaint.setColor(Color.argb(130, 255, 64, 192));
        float stripWidth = width * 0.18f;
        float stripHeight = height * 0.08f;
        float offset = (float) Math.sin(animationTimer * 9f) * width * 0.1f;
        canvas.drawRect(left + width * 0.3f + offset, top + height * 0.32f,
                left + width * 0.3f + offset + stripWidth, top + height * 0.32f + stripHeight, entityPaint);
        canvas.drawRect(left + width * 0.25f - offset, top + height * 0.46f,
                left + width * 0.25f - offset + stripWidth, top + height * 0.46f + stripHeight * 0.6f, entityPaint);

        entityPaint.setStyle(originalStyle);
        entityPaint.setColor(originalColor);
    }

    private void drawSpike(@NonNull Canvas canvas, float centerX, float tipY, float halfWidth, float height) {
        Path path = new Path();
        path.moveTo(centerX, tipY);
        path.lineTo(centerX - halfWidth, tipY + height);
        path.lineTo(centerX + halfWidth, tipY + height);
        path.close();
        canvas.drawPath(path, entityPaint);
    }

    private void drawClaw(@NonNull Canvas canvas,
                          float baseX,
                          float baseY,
                          float width,
                          float height,
                          boolean leftHanded) {
        Path path = new Path();
        float direction = leftHanded ? -1f : 1f;
        path.moveTo(baseX, baseY - height * 0.4f);
        path.lineTo(baseX + direction * width, baseY);
        path.lineTo(baseX, baseY + height * 0.2f);
        path.close();
        canvas.drawPath(path, entityPaint);
    }

    private boolean circleIntersects(@NonNull RectF rect,
                                     float cx,
                                     float cy,
                                     float radius) {
        float closestX = clamp(cx, rect.left, rect.right);
        float closestY = clamp(cy, rect.top, rect.bottom);
        float dx = cx - closestX;
        float dy = cy - closestY;
        return dx * dx + dy * dy <= radius * radius;
    }

    private void spawnProjectile(@NonNull EnemyInstance source,
                                 float x,
                                 float y,
                                 float vx,
                                 float vy,
                                 float radius,
                                 float lifetime,
                                 boolean gravity) {
        Projectile projectile = new Projectile();
        projectile.x = x;
        projectile.y = y;
        projectile.vx = vx;
        projectile.vy = vy;
        projectile.radius = radius;
        projectile.lifetime = lifetime;
        projectile.gravity = gravity;
        projectile.source = source.kind;
        projectiles.add(projectile);
    }

    private void spawnDebugPlatform(float centerX,
                                    float baseY,
                                    float width,
                                    float height,
                                    float lifetime) {
        debugPlatforms.add(new DebugPlatform(centerX, baseY, width, height, lifetime));
    }

    private float distance(float ax, float ay, float bx, float by) {
        float dx = ax - bx;
        float dy = ay - by;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private void handleGuardian(@NonNull EnemyInstance enemy,
                                @NonNull RectF enemyBounds,
                                @NonNull RectF playerBounds) {
        GuardianGate gate = guardianGates.get(enemy.channel);
        if (gate == null) {
            return;
        }
        if (RectF.intersects(enemyBounds, playerBounds)) {
            if (enemy.kind == EnemyKind.TWOFA_GUARDIAN_JUMP
                    && lastPlayerAction == PlayerAction.JUMP) {
                gate.jumpSatisfied = true;
            } else if (enemy.kind == EnemyKind.TWOFA_GUARDIAN_DASH
                    && lastPlayerAction == PlayerAction.CROUCH) {
                gate.duckSatisfied = true;
            }
        }
        if (gate.isOpen()) {
            enemy.deactivated = true;
            enemy.visible = false;
            enemy.active = false;
        }
    }

    private float getExtraFloat(@NonNull EnemyInstance enemy, @NonNull String key, float defaultValue) {
        String value = enemy.extras.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private void resolveEnemyInteractions(@NonNull LevelModel level) {
        RectF playerBounds = player.getBounds();
        boolean groundedByPlatform = false;
        playerRespawnedThisFrame = false;

        for (DebugPlatform platform : debugPlatforms) {
            if (platform.bounds.right < playerBounds.left || platform.bounds.left > playerBounds.right) {
                continue;
            }
            float platformTop = platform.bounds.top;
            if (player.vy >= 0f
                    && playerBounds.bottom >= platformTop - 6f
                    && playerBounds.bottom <= platform.bounds.bottom + 6f) {
                player.y = platform.bounds.bottom;
                player.onGround = true;
                player.vy = Math.min(0f, player.vy);
                groundedByPlatform = true;
                player.touchedDebugSymbol = true;
            }
        }

        for (EnemyInstance enemy : enemies) {
            if (!enemy.active) {
                continue;
            }
            if (enemy.kind == EnemyKind.LAG_BUBBLE) {
                float dist = distance(player.x, player.y, enemy.x, enemy.y - enemy.height * 0.5f);
                if (dist < enemy.tileWidth * 2.5f) {
                    player.timeSlowTimer = Math.max(player.timeSlowTimer, 0.9f);
                }
            }
            RectF enemyBounds = enemy.getBounds();

            if (enemy.kind == EnemyKind.PATCH_GOLEM && enemy.platformCarrier) {
                if (player.vy >= 0f
                        && playerBounds.bottom >= enemyBounds.top - 6f
                        && playerBounds.bottom <= enemyBounds.top + enemy.tileHeight * 0.4f
                        && playerBounds.right > enemyBounds.left
                        && playerBounds.left < enemyBounds.right) {
                    player.y = enemyBounds.bottom;
                    player.onGround = true;
                    player.vy = Math.min(0f, player.vy);
                    groundedByPlatform = true;
                    continue;
                }
            }

            if (enemy.kind == EnemyKind.TWOFA_GUARDIAN_JUMP
                    || enemy.kind == EnemyKind.TWOFA_GUARDIAN_DASH) {
                handleGuardian(enemy, enemyBounds, playerBounds);
            }

            if (enemy.deactivated || !enemy.visible) {
                continue;
            }

            if (RectF.intersects(enemyBounds, playerBounds)) {
                applyEnemyEffect(enemy, enemyBounds, playerBounds);
                if (playerRespawnedThisFrame) {
                    playerBounds = player.getBounds();
                    playerRespawnedThisFrame = false;
                }
            }
        }

        if (!groundedByPlatform && !player.onGround) {
            player.touchedDebugSymbol = false;
        }
    }

    private void drawEnemies(@NonNull Canvas canvas, @NonNull LevelModel level) {
        float scale = currentScale > 0f ? currentScale : 1f;
        for (EnemyInstance enemy : enemies) {
            if (!enemy.active || (!enemy.visible && enemy.kind != EnemyKind.VPN_VAMPYRE)) {
                continue;
            }
            RectF bounds = enemy.getBounds();
            float left = worldToScreenX(bounds.left);
            float top = worldToScreenY(bounds.top);
            float right = worldToScreenX(bounds.right);
            float bottom = worldToScreenY(bounds.bottom);
            drawEnemySprite(canvas, enemy, left, top, right, bottom);
        }
    }

    private void drawProjectiles(@NonNull Canvas canvas) {
        Paint.Style originalStyle = entityPaint.getStyle();
        int originalColor = entityPaint.getColor();
        for (Projectile projectile : projectiles) {
            float x = worldToScreenX(projectile.x);
            float y = worldToScreenY(projectile.y);
            float radius = Math.max(3f, projectile.radius * (currentScale > 0f ? currentScale : 1f));
            entityPaint.setStyle(Paint.Style.FILL);
            entityPaint.setColor(Color.parseColor("#FF8A65"));
            canvas.drawCircle(x, y, radius, entityPaint);
        }
        entityPaint.setStyle(originalStyle);
        entityPaint.setColor(originalColor);
    }

    private void drawDebugPlatforms(@NonNull Canvas canvas) {
        Paint.Style originalStyle = entityPaint.getStyle();
        int originalColor = entityPaint.getColor();
        entityPaint.setStyle(Paint.Style.FILL);
        for (DebugPlatform platform : debugPlatforms) {
            float alpha = Math.max(0.2f, Math.min(1f, platform.lifetime));
            entityPaint.setColor(Color.argb((int) (alpha * 180), 255, 229, 127));
            float left = worldToScreenX(platform.bounds.left);
            float top = worldToScreenY(platform.bounds.top);
            float right = worldToScreenX(platform.bounds.right);
            float bottom = worldToScreenY(platform.bounds.bottom);
            canvas.drawRoundRect(left, top, right, bottom, (right - left) * 0.1f, (bottom - top) * 0.1f, entityPaint);
        }
        entityPaint.setStyle(originalStyle);
        entityPaint.setColor(originalColor);
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
            updateBossEffects(deltaSeconds);
            return;
        }
        float timeScale = player.timeSlowTimer > 0f ? 0.6f : 1f;
        float effectiveDelta = deltaSeconds * timeScale;
        updateStatusEffects(deltaSeconds);
        handleInput(level);
        applyPhysics(effectiveDelta, level);
        updateEnemies(effectiveDelta, level);
        updateProjectiles(effectiveDelta, level);
        updateDebugPlatforms(deltaSeconds);
        resolveEnemyInteractions(level);
        if (shouldPlayJumpSound) {
            audioManager.playJump();
            shouldPlayJumpSound = false;
        }
        runTimerSeconds += deltaSeconds;
        updateCamera(level);
        checkLevelCompletion(level);
        updateBossEffects(deltaSeconds);
    }

    private void updateStatusEffects(float deltaSeconds) {
        if (player.stickyTimer > 0f) {
            player.stickyTimer = Math.max(0f, player.stickyTimer - deltaSeconds);
        }
        if (player.slipTimer > 0f) {
            player.slipTimer = Math.max(0f, player.slipTimer - deltaSeconds);
        }
        if (player.jumpCooldownTimer > 0f) {
            player.jumpCooldownTimer = Math.max(0f, player.jumpCooldownTimer - deltaSeconds);
        }
        if (player.timeSlowTimer > 0f) {
            player.timeSlowTimer = Math.max(0f, player.timeSlowTimer - deltaSeconds);
        }
    }

    private void updateBossEffects(float deltaSeconds) {
        if (screenShakeTimer > 0f) {
            screenShakeTimer = Math.max(0f, screenShakeTimer - deltaSeconds);
            float progress = screenShakeDuration > 0f ? screenShakeTimer / screenShakeDuration : 0f;
            float intensity = screenShakeMagnitude * progress * progress;
            shakeOffsetX = (random.nextFloat() * 2f - 1f) * intensity;
            shakeOffsetY = (random.nextFloat() * 2f - 1f) * intensity;
        } else {
            shakeOffsetX = 0f;
            shakeOffsetY = 0f;
        }
        if (bossMessageVisible) {
            bossMessageTimer += deltaSeconds;
            if (bossMessageTimer >= BOSS_MESSAGE_DURATION) {
                bossMessageVisible = false;
            }
        }
    }

    private void onPlayerRespawned() {
        runTimerSeconds = 0f;
        shakeOffsetX = 0f;
        shakeOffsetY = 0f;
        screenShakeTimer = 0f;
    }

    private void handleInput(@NonNull LevelModel level) {
        float movementSpeed = MOVE_SPEED;
        if (player.stickyTimer > 0f) {
            movementSpeed *= 0.5f;
        }
        if (player.crouching) {
            movementSpeed *= 0.6f;
        }
        player.vx = 0f;
        PlayerAction action = PlayerAction.IDLE;
        if (moveLeft && !moveRight) {
            player.vx -= movementSpeed;
            player.facingRight = false;
            action = PlayerAction.MOVE_LEFT;
        } else if (moveRight && !moveLeft) {
            player.vx += movementSpeed;
            player.facingRight = true;
            action = PlayerAction.MOVE_RIGHT;
        } else if (player.slipTimer > 0f) {
            player.vx = previousVx * 0.94f;
        }

        if (!duckPressed) {
            tryStand(level);
        } else if (!player.crouching && player.onGround) {
            player.crouching = true;
            player.height = player.crouchHeight;
            action = PlayerAction.CROUCH;
        } else if (player.crouching) {
            action = PlayerAction.CROUCH;
        }

        if (!jumpPressed) {
            jumpConsumed = false;
        }
        if (player.jumpCooldownTimer > 0f) {
            jumpConsumed = true;
        }
        if (jumpPressed && !jumpConsumed && player.onGround && player.jumpCooldownTimer <= 0f) {
            player.vy = player.jumpVelocity;
            player.onGround = false;
            jumpConsumed = true;
            shouldPlayJumpSound = true;
            action = PlayerAction.JUMP;
        }
        lastPlayerAction = action;
        previousVx = player.vx;
    }

    private void applyPhysics(float deltaSeconds, @NonNull LevelModel level) {
        player.vy += GRAVITY * deltaSeconds;
        moveHorizontally(player.vx * deltaSeconds, level);
        moveVertically(player.vy * deltaSeconds, level);
    }

    private void tryStand(@NonNull LevelModel level) {
        if (!player.crouching) {
            return;
        }
        float targetHeight = player.standingHeight;
        if (targetHeight <= player.height) {
            player.height = targetHeight;
            player.crouching = false;
            return;
        }
        RectF bounds = player.getBounds();
        float newTop = player.y - targetHeight;
        int leftTile = (int) Math.floor(bounds.left / level.getTileWidth());
        int rightTile = (int) Math.floor((bounds.right - 1f) / level.getTileWidth());
        int topTile = (int) Math.floor(newTop / level.getTileHeight());
        int bottomTile = (int) Math.floor((player.y - 1f) / level.getTileHeight());
        boolean blocked = false;
        for (int tx = leftTile; tx <= rightTile && !blocked; tx++) {
            for (int ty = topTile; ty <= bottomTile; ty++) {
                if (isSolid(level, tx, ty)) {
                    blocked = true;
                    break;
                }
            }
        }
        if (!blocked) {
            player.height = targetHeight;
            player.crouching = false;
        }
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
        bossMessageVisible = false;
        screenShakeTimer = 0f;
        shakeOffsetX = 0f;
        shakeOffsetY = 0f;
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
        drawDebugPlatforms(canvas);
        drawEnemies(canvas, level);
        drawProjectiles(canvas);
        drawPlayer(canvas);
        drawHud(canvas, level);
        drawBossMessage(canvas);
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
        float cameraX = this.cameraX + shakeOffsetX;
        float cameraY = this.cameraY + shakeOffsetY;
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
        float cameraX = this.cameraX + shakeOffsetX;
        float cameraY = this.cameraY + shakeOffsetY;
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
            if (EnemyKind.fromType(lowerType) != null) {
                continue;
            }
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
        drawRobotSprite(canvas, left, top, right, bottom, player.facingRight, player.crouching);
    }

    private void drawHud(@NonNull Canvas canvas, @NonNull LevelModel level) {
        float padding = 24f;
        float timerSize = Math.max(30f, canvas.getWidth() * 0.03f);
        timerPaint.setTextSize(timerSize);
        String timerText = "Zeit " + TimeFormatter.format(Math.max(0f, runTimerSeconds));
        float timerBaseline = padding + timerSize;
        canvas.drawText(timerText, padding, timerBaseline, timerPaint);

        float originalSize = textPaint.getTextSize();
        textPaint.setTextSize(originalSize * 0.85f);
        String worldName = currentWorldInfo != null ? currentWorldInfo.getName()
                : String.format(Locale.US, "World %d", currentWorldNumber);
        String header = String.format(Locale.US, "%s  (W%d-%d)", worldName, currentWorldNumber, currentStage);
        float infoBaseline = timerBaseline + timerSize * 0.55f;
        canvas.drawText(header, padding, infoBaseline, textPaint);
        textPaint.setTextSize(originalSize);
    }

    private void drawBossMessage(@NonNull Canvas canvas) {
        if (!bossMessageVisible) {
            return;
        }
        float fadeIn = Math.min(1f, bossMessageTimer / 0.4f);
        float fadeOut = bossMessageTimer > (BOSS_MESSAGE_DURATION - BOSS_MESSAGE_FADE)
                ? Math.max(0f, (BOSS_MESSAGE_DURATION - bossMessageTimer) / BOSS_MESSAGE_FADE)
                : 1f;
        float alphaFactor = Math.max(0f, Math.min(fadeIn, fadeOut));
        int originalAlpha = bossTextPaint.getAlpha();
        float baseSize = bossTextPaint.getTextSize();
        bossTextPaint.setAlpha((int) (alphaFactor * 255));
        float centerX = canvas.getWidth() / 2f;
        float centerY = canvas.getHeight() * 0.28f;
        canvas.drawText(BOSS_NAME + " ist aufgetaucht!", centerX, centerY, bossTextPaint);
        bossTextPaint.setTextSize(baseSize * 0.55f);
        canvas.drawText("Ein glühender Systemfehler jagt dich!", centerX, centerY + baseSize * 0.85f, bossTextPaint);
        bossTextPaint.setTextSize(baseSize);
        bossTextPaint.setAlpha(originalAlpha);
    }

    private float worldToScreenX(float worldX) {
        float scale = currentScale > 0f ? currentScale : 1f;
        return (worldX - (cameraX + shakeOffsetX)) * scale;
    }

    private float worldToScreenY(float worldY) {
        float scale = currentScale > 0f ? currentScale : 1f;
        return (worldY - (cameraY + shakeOffsetY)) * scale;
    }

    private void drawRobotSprite(@NonNull Canvas canvas,
                                 float left,
                                 float top,
                                 float right,
                                 float bottom,
                                 boolean facingRight,
                                 boolean crouching) {
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
        float visorTop = crouching ? top + height * 0.12f : top + height * 0.06f;
        float visorBottom = crouching ? top + height * 0.46f : top + height * 0.45f;
        canvas.drawRoundRect(left + width * 0.2f, visorTop,
                right - width * 0.2f, visorBottom, width * 0.16f, width * 0.16f, entityPaint);

        entityPaint.setColor(Color.parseColor("#0D1B2A"));
        float eyeY = crouching ? top + height * 0.3f : top + height * 0.24f;
        float eyeOffset = width * 0.12f * (facingRight ? 1f : -1f);
        float eyeRadius = Math.max(3f, width * 0.06f);
        canvas.drawCircle(left + width * 0.5f - eyeOffset, eyeY, eyeRadius, entityPaint);
        canvas.drawCircle(left + width * 0.5f + eyeOffset, eyeY, eyeRadius, entityPaint);

        entityPaint.setColor(Color.parseColor("#3D7ECC"));
        float armLength = width * 0.38f;
        float armHeight = height * 0.08f;
        float armTop = crouching ? top + height * 0.46f : top + height * 0.38f;
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
        float footHeight = crouching ? height * 0.2f : height * 0.14f;
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
                if (pressed) {
                    duckPressed = false;
                    LevelModel currentLevel = this.level;
                    if (currentLevel != null) {
                        tryStand(currentLevel);
                    }
                }
                if (!pressed) {
                    jumpConsumed = false;
                }
                break;
            case DUCK:
                duckPressed = pressed;
                if (pressed) {
                    jumpPressed = false;
                    jumpConsumed = true;
                }
                if (!pressed) {
                    LevelModel currentLevel = this.level;
                    if (currentLevel != null) {
                        tryStand(currentLevel);
                    }
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

    private enum PlayerAction {
        IDLE,
        MOVE_LEFT,
        MOVE_RIGHT,
        JUMP,
        CROUCH
    }

    private enum EnemyKind {
        BUGBLOB("bugblob", 0.9f, 1.0f),
        KEYLOGGER_BEETLE("keylogger_beetle", 0.9f, 0.9f),
        COOKIE_CRUMBLER("cookie_crumbler", 1.0f, 0.9f),
        BIT_BAT("bit_bat", 0.8f, 0.8f),
        PHISH_CARP("phish_carp", 0.9f, 1.0f),
        SPAM_DRONE("spam_drone", 0.9f, 0.9f),
        CLOUD_LEECH("cloud_leech", 1.0f, 1.0f),
        TROJAN_TURRET("trojan_turret", 1.2f, 1.2f),
        RANSOM_KNIGHT("ransom_knight", 1.1f, 1.4f),
        ROOTKIT_RAIDER("rootkit_raider", 1.0f, 1.0f),
        FIREWALL_GUARDIAN("firewall_guardian", 1.2f, 1.6f),
        POPUP_PIRANHA("popup_piranha", 0.9f, 1.2f),
        LAG_BUBBLE("lag_bubble", 1.4f, 1.4f),
        MEMORY_LEAK_SLIME("memory_leak_slime", 1.0f, 1.0f),
        CAPTCHA_GARGOYLE("captcha_gargoyle", 1.1f, 1.6f),
        PACKET_HOUND("packet_hound", 1.0f, 0.9f),
        BSOD_BLOCK("bsod_block", 1.4f, 1.0f),
        PATCH_GOLEM("patch_golem", 1.6f, 1.0f),
        GLITCH_SAW("glitch_saw", 1.2f, 1.2f),
        ADWARE_BALLOON("adware_balloon", 1.0f, 1.0f),
        BOTNET_BEE_LEADER("botnet_bee_leader", 0.8f, 0.8f),
        BOTNET_BEE_MINION("botnet_bee_minion", 0.6f, 0.6f),
        WURM_WEASEL("wurm_weasel", 0.9f, 0.9f),
        TREIBER_DRONE("treiber_drone", 1.0f, 1.0f),
        DRIVER_MODULE("driver_module", 0.9f, 0.9f),
        PORT_PLANT("port_plant", 1.0f, 1.4f),
        COMPILE_CRUSHER("compile_crusher", 1.6f, 1.8f),
        GARBAGE_COLLECTOR("garbage_collector", 1.8f, 1.6f),
        KERNEL_KOBOLD("kernel_kobold", 1.0f, 1.2f),
        VPN_VAMPYRE("vpn_vampire", 1.0f, 1.4f),
        UPDATE_OGRE("update_ogre", 1.6f, 1.8f),
        TWOFA_GUARDIAN_JUMP("twofa_guardian_jump", 1.4f, 2.2f),
        TWOFA_GUARDIAN_DASH("twofa_guardian_dash", 1.4f, 2.2f),
        CHECKSUM_CRAB("checksum_crab", 1.1f, 1.0f),
        PHISHING_SIREN("phishing_siren", 1.2f, 1.4f);

        private final String typeName;
        private final float widthScale;
        private final float heightScale;

        EnemyKind(@NonNull String typeName, float widthScale, float heightScale) {
            this.typeName = typeName;
            this.widthScale = widthScale;
            this.heightScale = heightScale;
        }

        @Nullable
        static EnemyKind fromType(@NonNull String type) {
            String key = type.toLowerCase(Locale.US);
            return LOOKUP.get(key);
        }

        private static Map<String, EnemyKind> createLookup() {
            Map<String, EnemyKind> map = new HashMap<>();
            for (EnemyKind kind : values()) {
                map.put(kind.typeName, kind);
            }
            return map;
        }

        private static final Map<String, EnemyKind> LOOKUP = createLookup();
    }

    private static final class EnemyInstance {
        EnemyKind kind;
        final float tileWidth;
        final float tileHeight;
        float width;
        float height;
        float x;
        float y;
        float baseX;
        float baseY;
        float vx;
        float vy;
        float timer;
        float state;
        float stateTimer;
        int direction = 1;
        boolean active = true;
        boolean deactivated;
        boolean onGround;
        boolean visible = true;
        boolean platformCarrier;
        String swarmId;
        String leaderId;
        EnemyInstance leader;
        String channel;
        String trigger;
        Map<String, String> extras;
        AnimatedEnemy animatedSprite;

        EnemyInstance(@NonNull EnemyKind kind,
                      float pixelX,
                      float pixelY,
                      float tileWidth,
                      float tileHeight,
                      @Nullable Map<String, String> extras,
                      @Nullable AnimatedEnemy animatedSprite) {
            this.kind = kind;
            this.tileWidth = tileWidth;
            this.tileHeight = tileHeight;
            this.width = tileWidth * kind.widthScale;
            this.height = tileHeight * kind.heightScale;
            this.x = pixelX;
            this.y = pixelY;
            this.baseX = pixelX;
            this.baseY = pixelY;
            this.vx = 0f;
            this.vy = 0f;
            this.timer = 0f;
            this.state = 0f;
            this.stateTimer = 0f;
            this.direction = 1;
            if (extras == null || extras.isEmpty()) {
                this.extras = Collections.emptyMap();
            } else {
                this.extras = Collections.unmodifiableMap(new HashMap<>(extras));
            }
            this.swarmId = this.extras.get("swarm");
            this.leaderId = this.extras.get("leader");
            this.channel = this.extras.get("channel");
            this.trigger = this.extras.get("trigger");
            this.animatedSprite = animatedSprite;
        }

        @NonNull
        RectF getBounds() {
            return new RectF(x - width / 2f, y - height, x + width / 2f, y);
        }
    }

    private static final class Projectile {
        float x;
        float y;
        float vx;
        float vy;
        float radius;
        float lifetime;
        boolean gravity;
        EnemyKind source;
    }

    private static final class DebugPlatform {
        final RectF bounds = new RectF();
        float lifetime;

        DebugPlatform(float centerX, float baseY, float width, float height, float lifetime) {
            bounds.set(centerX - width / 2f, baseY - height, centerX + width / 2f, baseY);
            this.lifetime = lifetime;
        }
    }

    private static final class GuardianGate {
        boolean jumpSatisfied;
        boolean duckSatisfied;

        boolean isOpen() {
            return jumpSatisfied && duckSatisfied;
        }
    }

    private static final class Player {
        float x;
        float y;
        float vx;
        float vy;
        float width;
        float height;
        float standingHeight;
        float crouchHeight;
        float jumpVelocity;
        float spawnX;
        float spawnY;
        float stickyTimer;
        float slipTimer;
        float jumpCooldownTimer;
        float timeSlowTimer;
        boolean touchedDebugSymbol;
        boolean onGround;
        boolean facingRight = true;
        boolean crouching;

        RectF getBounds() {
            float halfWidth = width / 2f;
            return new RectF(x - halfWidth, y - height, x + halfWidth, y);
        }

        void respawn() {
            x = spawnX;
            y = spawnY;
            vx = 0f;
            vy = 0f;
            onGround = false;
            crouching = false;
            height = standingHeight;
            stickyTimer = 0f;
            slipTimer = 0f;
            jumpCooldownTimer = 0f;
            timeSlowTimer = 0f;
        }
    }
}
