// app/src/main/java/com/example/robotparkour/scene/MenuScene.java
package com.example.robotparkour.scene;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.example.robotparkour.core.Scene;
import com.example.robotparkour.core.SceneManager;
import com.example.robotparkour.core.SceneType;

/**
 * Landing page that lets the player jump into the action or inspect records.
 */
public class MenuScene implements Scene {

    private enum StoryScene {
        CITY_ALERT,
        MISSION_BRIEF,
        PARKOUR_RUN,
        BOSS_FIGHT,
        REUNION
    }

    private static final class StoryStage {
        final StoryScene scene;
        final float duration;

        StoryStage(StoryScene scene, float duration) {
            this.scene = scene;
            this.duration = duration;
        }
    }

    private static final StoryStage[] STORY_SEQUENCE = new StoryStage[] {
            new StoryStage(StoryScene.CITY_ALERT, 3.2f),
            new StoryStage(StoryScene.MISSION_BRIEF, 3.0f),
            new StoryStage(StoryScene.PARKOUR_RUN, 3.4f),
            new StoryStage(StoryScene.BOSS_FIGHT, 3.6f),
            new StoryStage(StoryScene.REUNION, 4.0f)
    };

    private static final float PANEL_CORNER = 28f;

    private final SceneManager sceneManager;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF playButton = new RectF();
    private final RectF scoreboardButton = new RectF();
    private final RectF settingsButton = new RectF();

    private float animationTimer;
    private int surfaceWidth;
    private int surfaceHeight;
    private int currentStoryStage;
    private float stageTimer;
    private boolean storyComplete;

    public MenuScene(Context context, SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    @Override
    public SceneType getType() {
        return SceneType.MENU;
    }

    @Override
    public void onEnter() {
        sceneManager.getAudioManager().startMusic();
        resetStory();
    }

    @Override
    public void onExit() {
        // Menu currently shares the same looping track as gameplay, so nothing extra is required.
    }

    @Override
    public void update(float deltaSeconds) {
        animationTimer += deltaSeconds;
        if (!storyComplete && STORY_SEQUENCE.length > 0) {
            stageTimer += deltaSeconds;
            StoryStage stage = STORY_SEQUENCE[currentStoryStage];
            if (stageTimer >= stage.duration) {
                advanceStoryStage();
            }
        }
    }

    @Override
    public void draw(Canvas canvas) {
        if (surfaceWidth == 0 || surfaceHeight == 0) {
            return;
        }
        // Background inspired by a dimmed IDE workspace.
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#1E1E1E"));
        canvas.drawRect(0, 0, surfaceWidth, surfaceHeight, paint);
        paint.setColor(Color.parseColor("#252526"));
        canvas.drawRect(0, 0, surfaceWidth, surfaceHeight * 0.08f, paint);

        // Animated backdrop lines.
        paint.setColor(Color.parseColor("#264F78"));
        float lineSpacing = 34f;
        float offset = (float) Math.sin(animationTimer * 0.8f) * 12f;
        for (int i = 0; i < 10; i++) {
            float y = surfaceHeight * 0.12f + i * lineSpacing + offset;
            canvas.drawRect(surfaceWidth * 0.16f, y, surfaceWidth * 0.84f, y + 3f, paint);
        }

        drawTitle(canvas);
        drawStoryPanel(canvas);

        drawButton(canvas, playButton, "Play");
        drawButton(canvas, scoreboardButton, "Scoreboard");
        drawButton(canvas, settingsButton, "Settings");
    }

    private void drawButton(Canvas canvas, RectF bounds, String text) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#007ACC"));
        canvas.drawRoundRect(bounds, 28f, 28f, paint);
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(bounds.height() * 0.45f);
        canvas.drawText(text, bounds.centerX(), bounds.centerY() + paint.getTextSize() * 0.3f, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!storyComplete && STORY_SEQUENCE.length > 0 && event.getAction() == MotionEvent.ACTION_DOWN) {
            skipToNextStage();
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (!storyComplete) {
                return true;
            }
            float x = event.getX();
            float y = event.getY();
            if (playButton.contains(x, y)) {
                sceneManager.showWorldSelect();
                return true;
            } else if (scoreboardButton.contains(x, y)) {
                sceneManager.showScoreboard();
                return true;
            } else if (settingsButton.contains(x, y)) {
                sceneManager.showSettings();
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        surfaceWidth = width;
        surfaceHeight = height;
        float buttonWidth = width * 0.6f;
        float buttonHeight = height * 0.1f;
        float centerX = width / 2f;
        float firstY = height * 0.72f;
        playButton.set(centerX - buttonWidth / 2f, firstY, centerX + buttonWidth / 2f, firstY + buttonHeight);
        scoreboardButton.set(centerX - buttonWidth / 2f, firstY + buttonHeight * 1.2f, centerX + buttonWidth / 2f, firstY + buttonHeight * 2.2f);
        settingsButton.set(centerX - buttonWidth / 2f, firstY + buttonHeight * 2.4f, centerX + buttonWidth / 2f, firstY + buttonHeight * 3.4f);
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    private void drawTitle(Canvas canvas) {
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTextSize(surfaceWidth * 0.065f);
        float titleX = surfaceWidth * 0.12f;
        float titleY = surfaceHeight * 0.18f;
        canvas.drawText("Robot IDE Parkour", titleX, titleY, paint);

        paint.setColor(Color.parseColor("#94C8FF"));
        paint.setTextSize(surfaceWidth * 0.032f);
        canvas.drawText("Run. Debug. Deploy.", titleX, titleY + surfaceHeight * 0.05f, paint);
    }

    private void drawStoryPanel(Canvas canvas) {
        float panelWidth = surfaceWidth * 0.76f;
        float panelLeft = (surfaceWidth - panelWidth) / 2f;
        float panelTop = surfaceHeight * 0.28f;
        float panelHeight = surfaceHeight * 0.36f;
        RectF panel = new RectF(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(220, 18, 18, 24));
        canvas.drawRoundRect(panel, PANEL_CORNER, PANEL_CORNER, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f);
        paint.setColor(Color.parseColor("#007ACC"));
        canvas.drawRoundRect(panel, PANEL_CORNER, PANEL_CORNER, paint);

        if (STORY_SEQUENCE.length == 0) {
            return;
        }

        StoryStage stage = storyComplete
                ? STORY_SEQUENCE[STORY_SEQUENCE.length - 1]
                : STORY_SEQUENCE[currentStoryStage];
        float duration = Math.max(0.1f, stage.duration);
        float progress = storyComplete ? 1f : Math.min(1f, stageTimer / duration);

        drawStoryScene(canvas, panel, stage.scene, progress);
        drawSpeechBubble(canvas, panel, stage.scene, progress);
    }

    private void drawStoryScene(Canvas canvas, RectF panel, StoryScene scene, float progress) {
        float pixel = Math.min(panel.width(), panel.height()) / 48f;
        switch (scene) {
            case CITY_ALERT:
                drawCityAlert(canvas, panel, pixel, progress);
                break;
            case MISSION_BRIEF:
                drawMissionBrief(canvas, panel, pixel, progress);
                break;
            case PARKOUR_RUN:
                drawParkourRun(canvas, panel, pixel, progress);
                break;
            case BOSS_FIGHT:
                drawBossFight(canvas, panel, pixel, progress);
                break;
            case REUNION:
                drawReunion(canvas, panel, pixel, progress);
                break;
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(1f);
    }

    private void drawSpeechBubble(Canvas canvas, RectF panel, StoryScene scene, float progress) {
        String[] lines = getBubbleLines(scene);
        if (lines == null || lines.length == 0) {
            return;
        }
        float appear = storyComplete ? 1f : Math.min(1f, progress * 1.5f);
        if (appear <= 0.01f) {
            return;
        }

        float pixel = Math.min(panel.width(), panel.height()) / 48f;
        float margin = panel.width() * 0.04f;
        float bubbleWidth = panel.width() * 0.38f;
        float bubbleHeight = panel.height() * 0.28f;
        RectF bubble = new RectF(
                panel.right - bubbleWidth - margin,
                panel.top + margin,
                panel.right - margin,
                panel.top + margin + bubbleHeight);

        int alpha = (int) (appear * 220);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb((int) (alpha * 0.92f), 25, 34, 52));
        canvas.drawRoundRect(bubble, pixel * 3f, pixel * 3f, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(pixel * 0.9f);
        paint.setColor(Color.argb(alpha, 0, 122, 204));
        canvas.drawRoundRect(bubble, pixel * 3f, pixel * 3f, paint);

        // Tail pointing towards the panel center
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb((int) (alpha * 0.92f), 25, 34, 52));
        Path tail = new Path();
        float tailBaseX = bubble.left + bubble.width() * 0.3f;
        float tailBaseY = bubble.bottom;
        tail.moveTo(tailBaseX, tailBaseY);
        tail.lineTo(tailBaseX + pixel * 4f, tailBaseY);
        tail.lineTo(tailBaseX + pixel * 1.6f, tailBaseY + pixel * 5f);
        tail.close();
        canvas.drawPath(tail, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(pixel * 0.9f);
        paint.setColor(Color.argb(alpha, 0, 122, 204));
        canvas.drawPath(tail, paint);

        // Text
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setColor(Color.argb(alpha, 240, 245, 255));
        float textSize = bubble.height() / 4.6f;
        paint.setTextSize(textSize);
        float textX = bubble.left + pixel * 3.5f;
        float textY = bubble.top + textSize + pixel * 3f;
        float lineSpacing = textSize + pixel * 1.6f;
        for (String line : lines) {
            canvas.drawText(line, textX, textY, paint);
            textY += lineSpacing;
        }
    }

    private String[] getBubbleLines(StoryScene scene) {
        switch (scene) {
            case CITY_ALERT:
                return new String[] {"NullPointer greift", "Pixelstadt an!"};
            case MISSION_BRIEF:
                return new String[] {"ALARM! Clara++", "ist entführt!"};
            case PARKOUR_RUN:
                return new String[] {"Code-Parkour durch", "Bugfallen!"};
            case BOSS_FIGHT:
                return new String[] {"Try/Catch gegen", "NullPointer!"};
            case REUNION:
                return new String[] {"Clara++ ist frei,", "Refactor-Zeit!"};
            default:
                return null;
        }
    }

    private void drawCityAlert(Canvas canvas, RectF panel, float pixel, float progress) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#111421"));
        canvas.drawRoundRect(panel.left + pixel, panel.top + pixel, panel.right - pixel, panel.bottom - pixel, 16f, 16f, paint);

        // Night sky with twinkling bits
        paint.setColor(Color.parseColor("#1F2A44"));
        for (int i = 0; i < 40; i++) {
            float flicker = (float) Math.abs(Math.sin(animationTimer * 2.0f + i));
            if (flicker > 0.7f) {
                float sx = panel.left + pixel * (4 + (i * 3) % 40);
                float sy = panel.top + pixel * (3 + (i * 7) % 18);
                canvas.drawRect(sx, sy, sx + pixel, sy + pixel, paint);
            }
        }

        int[] buildingHeights = {18, 12, 22, 16, 26, 14, 20, 24};
        for (int i = 0; i < buildingHeights.length; i++) {
            float left = panel.left + pixel * (6 + i * 5);
            float right = left + pixel * 4;
            float bottom = panel.bottom - pixel * 6;
            float top = bottom - pixel * buildingHeights[i];
            paint.setColor(Color.parseColor(i % 2 == 0 ? "#1B1E30" : "#16192A"));
            canvas.drawRect(left, top, right, bottom, paint);
            paint.setColor(Color.parseColor("#232840"));
            for (int w = 0; w < buildingHeights[i] / 2; w++) {
                float wy = bottom - pixel * (w * 2 + 1.5f);
                canvas.drawRect(left + pixel, wy, left + pixel * 1.8f, wy + pixel * 0.8f, paint);
            }
        }

        // Clara++ distress call
        float claraCenterX = panel.left + panel.width() * 0.5f;
        float claraBaseY = panel.top + panel.height() * 0.56f;
        drawClaraSprite(canvas, claraCenterX, claraBaseY, pixel);

        float frameWidth = pixel * 8.5f;
        float frameHeight = pixel * 5.6f;
        float frameLeft = claraCenterX - frameWidth / 2f;
        float frameTop = claraBaseY - frameHeight;
        float screenLeft = frameLeft + pixel * 1.0f;
        float screenTop = frameTop + pixel * 0.9f;
        float screenRight = frameLeft + frameWidth - pixel * 1.0f;
        float screenBottom = frameTop + frameHeight - pixel * 1.8f;

        float glitch = (float) Math.abs(Math.sin(animationTimer * 5.0f));
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb((int) (120 + glitch * 90), 244, 71, 71));
        canvas.drawRoundRect(screenLeft, screenTop + pixel * 1.0f,
                screenRight, screenBottom - pixel * 0.8f, pixel, pixel, paint);

        paint.setColor(Color.argb((int) (140 + glitch * 60), 255, 255, 255));
        canvas.drawRect(screenLeft + pixel * 0.6f, screenTop + pixel * 2.6f,
                screenRight - pixel * 0.6f, screenTop + pixel * 3.1f, paint);

        // Malware surge creeping in
        float surgeWidth = panel.width() * 0.76f * progress;
        paint.setColor(Color.argb(160, 244, 71, 71));
        canvas.drawRect(panel.left + pixel * 2f, panel.bottom - pixel * 6f,
                panel.left + pixel * 2f + surgeWidth, panel.bottom - pixel * 4f, paint);
    }

    private void drawMissionBrief(Canvas canvas, RectF panel, float pixel, float progress) {
        float baseY = panel.bottom - pixel * 10f;
        float bounce = (float) Math.sin(animationTimer * 3.0f) * pixel;
        float robotX = panel.left + panel.width() * 0.18f;
        drawRobotSprite(canvas, robotX, baseY + bounce, pixel, true);

        // Dramatic siren backdrop
        float epicLeft = panel.left + panel.width() * 0.42f;
        float epicTop = panel.top + pixel * 6f;
        float epicRight = panel.right - pixel * 4f;
        float epicBottom = panel.bottom - pixel * 6f;

        float alarmPhase = (animationTimer * 3.6f) % 1f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(180, 18, 22, 40));
        canvas.drawRoundRect(epicLeft, epicTop, epicRight, epicBottom, pixel * 1.2f, pixel * 1.2f, paint);

        // Flashing red bands
        paint.setColor(Color.argb(140, 244, 71, 71));
        float bandHeight = pixel * 2.2f;
        for (int i = -1; i < 6; i++) {
            float offset = (alarmPhase * bandHeight * 6f);
            float top = epicTop + i * bandHeight * 1.6f + offset;
            canvas.drawRect(epicLeft + pixel * 1.2f, top, epicRight - pixel * 1.2f, top + bandHeight, paint);
        }

        // Rotating siren arcs
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(pixel * 0.9f);
        for (int i = 0; i < 3; i++) {
            float t = (alarmPhase + i * 0.25f) % 1f;
            int alpha = (int) (180 * (1f - t));
            paint.setColor(Color.argb(alpha, 244, 71, 71));
            float radius = pixel * (6f + 9f * t);
            canvas.drawArc(epicLeft + pixel * 8f - radius, epicTop + pixel * 6f - radius,
                    epicLeft + pixel * 8f + radius, epicTop + pixel * 6f + radius,
                    200f - t * 40f, 60f, false, paint);
        }

        // Distress waveform
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(200, 121, 192, 255));
        float waveLeft = epicLeft + pixel * 2.5f;
        float waveTop = epicTop + pixel * 5f;
        float waveHeight = pixel * 4.5f;
        float waveWidth = epicRight - epicLeft - pixel * 5f;
        Path waveform = new Path();
        waveform.moveTo(waveLeft, waveTop + waveHeight / 2f);
        int samples = 32;
        for (int i = 0; i <= samples; i++) {
            float tt = i / (float) samples;
            float amp = (float) Math.sin((tt * 6f + animationTimer * 8f)) * 0.5f;
            float spike = (float) Math.sin((progress * 3f + tt * 12f)) * 0.3f;
            float y = waveTop + waveHeight * (0.5f - amp - spike);
            float x = waveLeft + waveWidth * tt;
            waveform.lineTo(x, y);
        }
        waveform.lineTo(waveLeft + waveWidth, waveTop + waveHeight);
        waveform.lineTo(waveLeft, waveTop + waveHeight);
        waveform.close();
        canvas.drawPath(waveform, paint);

        // Pulsing exclamation in sky
        paint.setColor(Color.argb(220, 255, 215, 0));
        float exWidth = pixel * 3.6f;
        float exHeight = pixel * 10f;
        float exX = epicRight - pixel * 6f;
        float exY = epicTop + pixel * 3f;
        float pulse = 0.8f + 0.2f * (float) Math.sin(animationTimer * 6f);
        canvas.drawRoundRect(exX, exY, exX + exWidth * pulse, exY + exHeight * pulse, pixel * 0.8f, pixel * 0.8f, paint);
        paint.setColor(Color.parseColor("#111C2B"));
        canvas.drawRoundRect(exX + pixel * 0.8f, exY + pixel * 1.4f, exX + exWidth * pulse - pixel * 0.8f,
                exY + exHeight * pulse - pixel * 2.6f, pixel * 0.5f, pixel * 0.5f, paint);
        canvas.drawRoundRect(exX + pixel * 1.2f, exY + exHeight * pulse - pixel * 1.8f,
                exX + exWidth * pulse - pixel * 1.2f, exY + exHeight * pulse - pixel * 0.8f, pixel * 0.4f, pixel * 0.4f, paint);
    }

    private void drawParkourRun(Canvas canvas, RectF panel, float pixel, float progress) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#101424"));
        canvas.drawRect(panel.left + pixel, panel.top + pixel, panel.right - pixel, panel.bottom - pixel, paint);

        // Platforms
        paint.setColor(Color.parseColor("#2D2D30"));
        for (int i = 0; i < 5; i++) {
            float platWidth = pixel * (10 + i * 2);
            float platLeft = panel.left + pixel * (4 + i * 10);
            float platTop = panel.bottom - pixel * (8 + i * 3);
            canvas.drawRect(platLeft, platTop, platLeft + platWidth, platTop + pixel * 2f, paint);
        }

        float playableLeft = panel.left + pixel * 4f;
        float playableRight = panel.right - pixel * 4f;
        float playableTop = panel.top + pixel * 4f;
        float playableBottom = panel.bottom - pixel * 8f;

        float[] keyTimes = {0f, 0.24f, 0.48f, 0.72f, 1f};
        float[] posX = {0.05f, 0.24f, 0.48f, 0.7f, 0.92f};
        float[] posY = {0.15f, 0.58f, 0.30f, 0.62f, 0.35f};
        float[] arcStrength = {0.36f, 0.42f, 0.38f, 0.28f};

        float phase = (progress * 1.2f + animationTimer * 0.7f) % 1f;
        if (phase < 0f) {
            phase += 1f;
        }

        int seg = keyTimes.length - 2;
        for (int i = 0; i < keyTimes.length - 1; i++) {
            if (phase < keyTimes[i + 1]) {
                seg = i;
                break;
            }
        }

        float segSpan = keyTimes[seg + 1] - keyTimes[seg];
        float segT = segSpan > 0f ? (phase - keyTimes[seg]) / segSpan : 0f;
        segT = Math.max(0f, Math.min(1f, segT));

        float baseXNorm = lerp(posX[seg], posX[seg + 1], segT);
        float baseYNorm = lerp(posY[seg], posY[seg + 1], segT);
        float jump = arcStrength[seg] * (float) Math.sin(segT * Math.PI);
        float yNorm = Math.max(0f, Math.min(1f, baseYNorm - jump));

        float robotX = lerp(playableLeft, playableRight, baseXNorm);
        float robotY = lerp(playableTop, playableBottom, yNorm);

        float dxNormDt = (posX[seg + 1] - posX[seg]) / Math.max(segSpan, 0.0001f);
        float dyNormDt = (posY[seg + 1] - posY[seg]) / Math.max(segSpan, 0.0001f)
                - arcStrength[seg] * (float) Math.PI * (float) Math.cos(segT * Math.PI);
        float dx = dxNormDt * (playableRight - playableLeft);
        float dy = dyNormDt * (playableBottom - playableTop);
        float lean = (float) Math.toDegrees(Math.atan2(-dy, dx));
        lean = Math.max(-26f, Math.min(26f, lean));

        // Motion streaks
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(110, 121, 192, 255));
        for (int i = 1; i <= 3; i++) {
            float rewind = phase - i * 0.06f;
            if (rewind < 0f) {
                rewind += 1f;
            }
            int ghostSeg = keyTimes.length - 2;
            for (int idx = 0; idx < keyTimes.length - 1; idx++) {
                if (rewind < keyTimes[idx + 1]) {
                    ghostSeg = idx;
                    break;
                }
            }
            float ghostSpan = keyTimes[ghostSeg + 1] - keyTimes[ghostSeg];
            float ghostT = ghostSpan > 0f ? (rewind - keyTimes[ghostSeg]) / ghostSpan : 0f;
            ghostT = Math.max(0f, Math.min(1f, ghostT));
            float ghostXNorm = lerp(posX[ghostSeg], posX[ghostSeg + 1], ghostT);
            float ghostYNorm = lerp(posY[ghostSeg], posY[ghostSeg + 1], ghostT)
                    - arcStrength[ghostSeg] * (float) Math.sin(ghostT * Math.PI);
            float gx = lerp(playableLeft, playableRight, ghostXNorm);
            float gy = lerp(playableTop, playableBottom, Math.max(0f, Math.min(1f, ghostYNorm)));
            canvas.drawCircle(gx, gy, pixel * 0.7f / i, paint);
        }

        drawRobotSprite(canvas, robotX, robotY, pixel * 0.92f, false, lean);

        // Spinning coins
        paint.setColor(Color.parseColor("#FDD835"));
        for (int i = 0; i < 4; i++) {
            float coinX = panel.left + pixel * (12 + i * 12);
            float coinY = panel.bottom - pixel * (20 + (i % 2) * 4);
            float scale = 0.6f + 0.3f * (float) Math.sin(animationTimer * 4f + i);
            canvas.drawRect(coinX - pixel * scale, coinY - pixel * scale,
                    coinX + pixel * scale, coinY + pixel * scale, paint);
        }

        // Spike hazards
        paint.setColor(Color.parseColor("#C7515A"));
        float spikeBaseY = panel.bottom - pixel * 6f;
        for (int i = 0; i < 6; i++) {
            float sx = panel.left + pixel * (8 + i * 6);
            canvas.drawRect(sx, spikeBaseY - pixel * 2f, sx + pixel, spikeBaseY, paint);
        }
    }

    private void drawBossFight(Canvas canvas, RectF panel, float pixel, float progress) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#1A0F1F"));
        canvas.drawRect(panel.left + pixel, panel.top + pixel, panel.right - pixel, panel.bottom - pixel, paint);

        float lavaTop = panel.bottom - pixel * 6f;
        paint.setColor(Color.parseColor("#7A1633"));
        canvas.drawRect(panel.left + pixel, lavaTop, panel.right - pixel, panel.bottom - pixel, paint);

        float dodgePhase = (progress * 1.2f + animationTimer * 0.8f) % 1f;
        if (dodgePhase < 0f) {
            dodgePhase += 1f;
        }
        float dodgeSin = (float) Math.sin(dodgePhase * Math.PI * 2f);
        float dodgeLift = (float) Math.abs(Math.sin(dodgePhase * Math.PI));
        float baseRobotX = lerp(panel.left + pixel * 9f, panel.left + pixel * 13f, (dodgeSin + 1f) * 0.5f);
        float baseRobotY = lavaTop - pixel * (4f + dodgeLift * 3f);
        float baseRobotLean = -dodgeSin * 20f;

        // NullPointer boss
        float bossSize = pixel * 14f;
        float bossLeft = panel.right - pixel * 18f;
        float bossTop = lavaTop - bossSize - pixel * 2f;
        float finalPhase = Math.max(0f, Math.min(1f, (progress - 0.82f) / 0.18f));
        float bossShake = finalPhase > 0f ? (float) Math.sin(animationTimer * 24f) * pixel * 1.5f * finalPhase : 0f;
        bossLeft += bossShake;

        float robotX = baseRobotX;
        float robotY = baseRobotY;
        float robotLean = baseRobotLean;
        if (finalPhase > 0f) {
            robotX = lerp(baseRobotX, bossLeft - pixel * 1.8f, finalPhase);
            robotY = lerp(baseRobotY, lavaTop - pixel * 6.6f, finalPhase);
            robotLean = 12f * (1f - finalPhase);
        }
        drawRobotSprite(canvas, robotX, robotY, pixel, false, robotLean);
        paint.setColor(Color.parseColor("#5B1224"));
        canvas.drawRect(bossLeft, bossTop, bossLeft + bossSize, bossTop + bossSize, paint);
        int coreAlpha = (int) (255 - finalPhase * 120f);
        paint.setColor(Color.argb(coreAlpha, 244, 71, 71));
        canvas.drawRect(bossLeft + pixel * 2f, bossTop + pixel * 2f, bossLeft + bossSize - pixel * 2f, bossTop + bossSize - pixel * 2f, paint);
        paint.setColor(Color.BLACK);
        canvas.drawRect(bossLeft + pixel * 3f, bossTop + pixel * 4f, bossLeft + pixel * 6f, bossTop + pixel * 7f, paint);
        canvas.drawRect(bossLeft + bossSize - pixel * 6f, bossTop + pixel * 4f, bossLeft + bossSize - pixel * 3f, bossTop + pixel * 7f, paint);
        canvas.drawRect(bossLeft + pixel * 4f, bossTop + pixel * 10f, bossLeft + bossSize - pixel * 4f, bossTop + pixel * 11f, paint);
        if (finalPhase > 0f) {
            paint.setColor(Color.argb((int) (80 + finalPhase * 120f), 255, 255, 255));
            float crackWidth = pixel * (1.2f + finalPhase * 1.8f);
            for (int i = 0; i < 4; i++) {
                float cx = bossLeft + pixel * (4f + i * 2.5f);
                canvas.drawRect(cx, bossTop + pixel * 3f, cx + crackWidth / 2f, bossTop + bossSize - pixel * 3f, paint);
            }
        }

        float duelCycle = (animationTimer * 0.9f + progress * 0.5f) % 1f;
        if (duelCycle < 0f) {
            duelCycle += 1f;
        }

        if (duelCycle < 0.5f) {
            // Robot counter-attack
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(pixel * 0.9f);
            paint.setColor(Color.argb(210, 121, 192, 255));
            float counterT = duelCycle / 0.5f;
            for (int i = 0; i < 3; i++) {
                float sway = (float) Math.sin((counterT + i * 0.2f) * Math.PI * 2f) * pixel * 1.2f;
                float startX = robotX + pixel * (1.4f + i * 0.4f);
                float startY = robotY - pixel * (3.0f - i * 0.6f);
                float endX = bossLeft + bossSize * 0.3f + sway;
                float endY = bossTop + bossSize * (0.3f + i * 0.2f);
                canvas.drawLine(startX, startY, endX, endY, paint);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(endX, endY, pixel * (0.9f - i * 0.15f), paint);
                paint.setStyle(Paint.Style.STROKE);
            }
            if (finalPhase > 0.4f) {
                float impactX = bossLeft + bossSize * 0.35f;
                float impactY = bossTop + bossSize * 0.32f;
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.argb((int) (220 * finalPhase), 255, 255, 255));
                for (int i = 0; i < 6; i++) {
                    float angle = i * 60f + animationTimer * 180f;
                    float rad = (float) Math.toRadians(angle);
                    float ix = impactX + (float) Math.cos(rad) * pixel * (2.4f + finalPhase * 2.2f);
                    float iy = impactY + (float) Math.sin(rad) * pixel * (2.4f + finalPhase * 2.2f);
                    canvas.drawCircle(ix, iy, pixel * (0.6f + finalPhase * 0.2f), paint);
                }
            }
        } else {
            // Boss projectile volley
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.parseColor("#FDD835"));
            for (int i = 0; i < 3; i++) {
                float t = ((progress * 2f + i * 0.33f + animationTimer * 0.6f) % 1f);
                float startX = bossLeft - pixel * (1.2f + i * 0.4f);
                float startY = bossTop + bossSize * (0.35f + i * 0.18f);
                float pxPos = lerp(startX, robotX + pixel * 0.8f, t);
                float pyPos = lerp(startY, robotY - pixel * (2.5f - i), t)
                        + (float) Math.sin(t * Math.PI) * pixel * 2.2f;
                canvas.drawRect(pxPos, pyPos, pxPos + pixel * 1.4f, pyPos + pixel * 1.4f, paint);
            }
        }

        // Shield effect
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(pixel * 0.8f);
        paint.setColor(Color.parseColor("#79C0FF"));
        float shieldRadius = pixel * 5f + (float) Math.sin(animationTimer * 3f) * pixel;
        canvas.drawCircle(robotX, robotY - pixel * 2.6f, shieldRadius, paint);

        if (progress > 0.7f) {
            float finale = Math.min(1f, (progress - 0.7f) / 0.3f);
            float pulse = (float) Math.sin((animationTimer + finale) * 6f) * pixel;
            float auraRadius = pixel * (12f + finale * 20f) + pulse;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(pixel * (1.2f + finale * 1.8f));
            paint.setColor(Color.argb((int) (160 + finale * 80), 121, 192, 255));
            canvas.drawCircle(robotX, robotY - pixel * 2.6f, auraRadius, paint);

            paint.setColor(Color.argb((int) (140 + finale * 100), 244, 71, 71));
            canvas.drawCircle(bossLeft + bossSize / 2f, bossTop + bossSize / 2f,
                    pixel * (6f + finale * 12f), paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setColor(Color.argb((int) (230 * finale), 255, 255, 255));
            paint.setTextSize(pixel * (3.6f + finale * 2.2f));
            canvas.drawText("FINAL REGEX", robotX, robotY - auraRadius - pixel * 2f, paint);

            paint.setTextSize(pixel * (2.4f + finale));
            canvas.drawText("/.*NullPointer.*/", bossLeft + bossSize / 2f,
                    bossTop - pixel * (3f + finale * 4f), paint);

            if (finale > 0.8f) {
                float collapse = (finale - 0.8f) / 0.2f;
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.argb((int) (200 * collapse), 255, 255, 255));
                float debrisBaseX = bossLeft + bossSize / 2f;
                float debrisBaseY = bossTop + bossSize / 2f;
                for (int i = 0; i < 10; i++) {
                    float angle = (float) (i / 10f * Math.PI * 2f + animationTimer * 3f);
                    float dist = collapse * pixel * (8f + i * 1.2f);
                    float dx = (float) Math.cos(angle) * dist;
                    float dy = (float) Math.sin(angle) * dist;
                    canvas.drawCircle(debrisBaseX + dx, debrisBaseY + dy, pixel * (0.6f - 0.04f * i), paint);
                }
            }
        }
    }

    private void drawReunion(Canvas canvas, RectF panel, float pixel, float progress) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#101B2A"));
        canvas.drawRect(panel.left + pixel, panel.top + pixel, panel.right - pixel, panel.bottom - pixel, paint);

        float centerY = panel.bottom - pixel * 8f;
        float robotX = panel.left + panel.width() * 0.35f;
        float computerX = panel.right - panel.width() * 0.35f;
        float handshakeProgress = Math.min(1f, progress * 1.2f);

        drawRobotSprite(canvas, lerp(robotX, (robotX + computerX) / 2f - pixel * 6f, handshakeProgress), centerY, pixel, true);
        drawClaraSprite(canvas, lerp(computerX, (robotX + computerX) / 2f + pixel * 6f, handshakeProgress), centerY, pixel);

        // Connection beam
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(pixel * 0.9f);
        paint.setColor(Color.parseColor("#94C8FF"));
        float beamStart = lerp(robotX + pixel * 2f, (robotX + computerX) / 2f, handshakeProgress);
        float beamEnd = lerp(computerX - pixel * 2f, (robotX + computerX) / 2f, handshakeProgress);
        canvas.drawLine(beamStart, centerY - pixel * 2f, beamEnd, centerY - pixel * 2f, paint);

        // Hearts rising
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#F26BAA"));
        for (int i = 0; i < 4; i++) {
            float t = (progress * 2f + i * 0.2f + animationTimer * 0.5f) % 1f;
            float hx = (robotX + computerX) / 2f + (float) Math.sin(t * Math.PI * 2f + i) * pixel * 4f;
            float hy = centerY - pixel * (6f + t * 12f);
            drawHeart(canvas, hx, hy, pixel * (1.2f - t * 0.4f));
        }

        // Hello World banner
        paint.setColor(Color.parseColor("#0E639C"));
        float bannerLeft = panel.left + panel.width() * 0.3f;
        float bannerTop = panel.top + pixel * 4f;
        float bannerRight = panel.right - panel.width() * 0.3f;
        float bannerBottom = bannerTop + pixel * 5f;
        canvas.drawRect(bannerLeft, bannerTop, bannerRight, bannerBottom, paint);
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(pixel * 3.2f);
        canvas.drawText("HELLO WORLD", (bannerLeft + bannerRight) / 2f, bannerBottom - pixel * 1.2f, paint);
    }

    private void resetStory() {
        currentStoryStage = 0;
        stageTimer = 0f;
        storyComplete = STORY_SEQUENCE.length == 0;
    }

    private void advanceStoryStage() {
        if (storyComplete) {
            return;
        }
        currentStoryStage++;
        if (currentStoryStage >= STORY_SEQUENCE.length) {
            storyComplete = true;
            currentStoryStage = STORY_SEQUENCE.length - 1;
            stageTimer = STORY_SEQUENCE.length > 0 ? STORY_SEQUENCE[currentStoryStage].duration : 0f;
        } else {
            stageTimer = 0f;
        }
    }

    private void skipToNextStage() {
        if (storyComplete || STORY_SEQUENCE.length == 0) {
            return;
        }
        stageTimer = STORY_SEQUENCE[currentStoryStage].duration;
        advanceStoryStage();
    }

    private void drawRobotSprite(Canvas canvas, float centerX, float baseY, float pixel, boolean bob) {
        drawRobotSprite(canvas, centerX, baseY, pixel, bob, 0f);
    }

    private void drawRobotSprite(Canvas canvas, float centerX, float baseY, float pixel, boolean bob, float leanDegrees) {
        canvas.save();
        canvas.translate(centerX, baseY);
        canvas.rotate(leanDegrees);
        canvas.translate(-centerX, -baseY);

        float bobOffset = bob ? (float) Math.sin(animationTimer * 3.4f) * pixel * 0.8f : 0f;

        float torsoWidth = pixel * 6f;
        float torsoHeight = pixel * 6.4f;
        float torsoLeft = centerX - torsoWidth / 2f;
        float torsoTop = baseY - torsoHeight + bobOffset;

        // Legs with joints
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#2B3B55"));
        float legWidth = pixel * 1.4f;
        float legHeight = pixel * 2.4f;
        float legGap = pixel * 1.1f;
        float legTop = baseY - legHeight + pixel * 0.4f;
        canvas.drawRoundRect(centerX - legGap - legWidth, legTop, centerX - legGap, legTop + legHeight, pixel * 0.6f, pixel * 0.6f, paint);
        canvas.drawRoundRect(centerX + legGap, legTop, centerX + legGap + legWidth, legTop + legHeight, pixel * 0.6f, pixel * 0.6f, paint);

        paint.setColor(Color.parseColor("#94C8FF"));
        canvas.drawCircle(centerX - legGap - legWidth / 2f, legTop + pixel * 0.6f, pixel * 0.6f, paint);
        canvas.drawCircle(centerX + legGap + legWidth / 2f, legTop + pixel * 0.6f, pixel * 0.6f, paint);

        // Torso plating
        paint.setColor(Color.parseColor("#9FB8D1"));
        canvas.drawRoundRect(torsoLeft, torsoTop, torsoLeft + torsoWidth, torsoTop + torsoHeight, pixel * 1.4f, pixel * 1.4f, paint);

        paint.setColor(Color.parseColor("#6284A8"));
        canvas.drawRoundRect(torsoLeft + pixel * 0.5f, torsoTop + pixel * 0.8f,
                torsoLeft + torsoWidth - pixel * 0.5f, torsoTop + torsoHeight - pixel * 0.8f,
                pixel * 1.2f, pixel * 1.2f, paint);

        // Chest panel
        paint.setColor(Color.parseColor("#0E639C"));
        float panelTop = torsoTop + pixel * 2f;
        float panelBottom = panelTop + pixel * 1.8f;
        canvas.drawRoundRect(centerX - pixel * 2.4f, panelTop, centerX + pixel * 2.4f, panelBottom, pixel, pixel, paint);
        paint.setColor(Color.parseColor("#94C8FF"));
        canvas.drawRect(centerX - pixel * 1.6f, panelTop + pixel * 0.4f, centerX - pixel * 0.6f, panelBottom - pixel * 0.4f, paint);
        paint.setColor(Color.parseColor("#FDD835"));
        canvas.drawRect(centerX + pixel * 0.6f, panelTop + pixel * 0.4f, centerX + pixel * 1.6f, panelBottom - pixel * 0.4f, paint);

        // Head
        float headHeight = pixel * 2.8f;
        float headWidth = pixel * 4.4f;
        float headBottom = torsoTop - pixel * 0.4f;
        float headTop = headBottom - headHeight;
        paint.setColor(Color.parseColor("#D9E4FF"));
        canvas.drawRoundRect(centerX - headWidth / 2f, headTop, centerX + headWidth / 2f, headBottom, pixel * 1.2f, pixel * 1.2f, paint);

        // Eyes and mouth
        paint.setColor(Color.parseColor("#1F2937"));
        canvas.drawRoundRect(centerX - pixel * 1.8f, headTop + pixel * 1.2f,
                centerX - pixel * 0.8f, headTop + pixel * 2.2f, pixel * 0.4f, pixel * 0.4f, paint);
        canvas.drawRoundRect(centerX + pixel * 0.8f, headTop + pixel * 1.2f,
                centerX + pixel * 1.8f, headTop + pixel * 2.2f, pixel * 0.4f, pixel * 0.4f, paint);
        paint.setColor(Color.parseColor("#4ADEDE"));
        canvas.drawCircle(centerX - pixel * 1.3f, headTop + pixel * 1.7f, pixel * 0.4f, paint);
        canvas.drawCircle(centerX + pixel * 1.3f, headTop + pixel * 1.7f, pixel * 0.4f, paint);

        paint.setColor(Color.parseColor("#2B3B55"));
        canvas.drawRoundRect(centerX - pixel, headTop + pixel * 2.4f, centerX + pixel, headTop + pixel * 2.8f, pixel * 0.4f, pixel * 0.4f, paint);

        // Antenna with light
        paint.setColor(Color.parseColor("#2B3B55"));
        canvas.drawRect(centerX - pixel * 0.3f, headTop - pixel * 1.6f, centerX + pixel * 0.3f, headTop, paint);
        paint.setColor(Color.parseColor("#FDD835"));
        canvas.drawCircle(centerX, headTop - pixel * 1.8f, pixel * 0.8f, paint);

        // Arms with joints
        float shoulderY = torsoTop + pixel * 1.6f;
        float armLength = pixel * 3.6f;
        float armWidth = pixel * 1.1f;
        float swing = (float) Math.sin(animationTimer * 4.2f) * pixel * 0.8f;
        paint.setColor(Color.parseColor("#9FB8D1"));
        canvas.drawRoundRect(torsoLeft - armWidth, shoulderY + swing, torsoLeft, shoulderY + armLength + swing, pixel * 0.6f, pixel * 0.6f, paint);
        canvas.drawRoundRect(torsoLeft + torsoWidth, shoulderY - swing, torsoLeft + torsoWidth + armWidth, shoulderY + armLength - swing, pixel * 0.6f, pixel * 0.6f, paint);

        paint.setColor(Color.parseColor("#2B3B55"));
        canvas.drawCircle(torsoLeft - armWidth / 2f, shoulderY + swing + armLength * 0.35f, pixel * 0.6f, paint);
        canvas.drawCircle(torsoLeft + torsoWidth + armWidth / 2f, shoulderY - swing + armLength * 0.35f, pixel * 0.6f, paint);

        paint.setColor(Color.parseColor("#FDD835"));
        canvas.drawCircle(torsoLeft - armWidth / 2f, shoulderY + swing + armLength + pixel * 0.1f, pixel * 0.5f, paint);
        canvas.drawCircle(torsoLeft + torsoWidth + armWidth / 2f, shoulderY - swing + armLength + pixel * 0.1f, pixel * 0.5f, paint);

        // Outline
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(pixel * 0.6f);
        paint.setColor(Color.parseColor("#1F2937"));
        canvas.drawRoundRect(torsoLeft, torsoTop, torsoLeft + torsoWidth, torsoTop + torsoHeight, pixel * 1.4f, pixel * 1.4f, paint);

        canvas.restore();
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawClaraSprite(Canvas canvas, float centerX, float baseY, float pixel) {
        float frameWidth = pixel * 8.5f;
        float frameHeight = pixel * 5.6f;
        float frameLeft = centerX - frameWidth / 2f;
        float frameTop = baseY - frameHeight;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#2B2F3A"));
        canvas.drawRoundRect(frameLeft, frameTop, frameLeft + frameWidth, frameTop + frameHeight, pixel * 1.2f, pixel * 1.2f, paint);

        // Hair (blonde)
        paint.setColor(Color.parseColor("#F7D774"));
        float hairHeight = pixel * 1.6f;
        canvas.drawRoundRect(frameLeft + pixel * 0.6f, frameTop - hairHeight,
                frameLeft + frameWidth - pixel * 0.6f, frameTop + pixel * 0.2f,
                pixel * 1.2f, pixel * 1.2f, paint);
        Path hairFringe = new Path();
        hairFringe.moveTo(frameLeft + pixel * 1.0f, frameTop);
        hairFringe.lineTo(frameLeft + pixel * 2.6f, frameTop + pixel * 1.2f);
        hairFringe.lineTo(frameLeft + pixel * 4.2f, frameTop);
        hairFringe.lineTo(frameLeft + pixel * 5.8f, frameTop + pixel * 1.2f);
        hairFringe.lineTo(frameLeft + frameWidth - pixel * 1.0f, frameTop);
        hairFringe.close();
        canvas.drawPath(hairFringe, paint);

        // Screen area
        paint.setColor(Color.parseColor("#0E639C"));
        float screenLeft = frameLeft + pixel * 1.0f;
        float screenTop = frameTop + pixel * 0.9f;
        float screenRight = frameLeft + frameWidth - pixel * 1.0f;
        float screenBottom = frameTop + frameHeight - pixel * 1.8f;
        canvas.drawRoundRect(screenLeft, screenTop, screenRight, screenBottom, pixel, pixel, paint);

        // C++ title and friendly face
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(pixel * 2.6f);
        canvas.drawText("C++", (screenLeft + screenRight) / 2f, screenTop + pixel * 2.8f, paint);

        paint.setColor(Color.parseColor("#94C8FF"));
        canvas.drawCircle(screenLeft + pixel * 2.0f, screenTop + pixel * 4.0f, pixel * 0.7f, paint);
        canvas.drawCircle(screenRight - pixel * 2.0f, screenTop + pixel * 4.0f, pixel * 0.7f, paint);
        paint.setColor(Color.WHITE);
        canvas.drawRoundRect((screenLeft + screenRight) / 2f - pixel * 1.2f,
                screenTop + pixel * 5.0f,
                (screenLeft + screenRight) / 2f + pixel * 1.2f,
                screenTop + pixel * 5.5f,
                pixel * 0.6f, pixel * 0.6f, paint);

        // Frame outline
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(pixel * 0.6f);
        paint.setColor(Color.parseColor("#16191F"));
        canvas.drawRoundRect(frameLeft, frameTop, frameLeft + frameWidth, frameTop + frameHeight, pixel * 1.2f, pixel * 1.2f, paint);
        paint.setStyle(Paint.Style.FILL);

        // Stand
        paint.setColor(Color.parseColor("#1E1E1E"));
        float standTop = frameTop + frameHeight - pixel * 0.6f;
        float standBottom = standTop + pixel * 2.8f;
        float standWidth = frameWidth * 0.4f;
        canvas.drawRoundRect(centerX - standWidth / 2f, standTop, centerX + standWidth / 2f, standBottom, pixel * 0.8f, pixel * 0.8f, paint);
        canvas.drawRoundRect(centerX - standWidth / 1.6f, standBottom - pixel * 0.6f,
                centerX + standWidth / 1.6f, standBottom + pixel * 0.8f, pixel * 0.6f, pixel * 0.6f, paint);

        // Status LED
        paint.setColor(Color.parseColor("#FDD835"));
        canvas.drawCircle(screenRight - pixel * 1.2f, screenTop + pixel * 1.4f, pixel * 0.4f, paint);
    }

    private void drawHeart(Canvas canvas, float centerX, float centerY, float size) {
        float half = size / 2f;
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(centerX - half * 0.6f, centerY - half * 0.2f, half * 0.6f, paint);
        canvas.drawCircle(centerX + half * 0.6f, centerY - half * 0.2f, half * 0.6f, paint);
        float bottom = centerY + half * 0.8f;
        canvas.drawRect(centerX - half, centerY - half * 0.2f, centerX + half, bottom, paint);
        Path path = new Path();
        path.moveTo(centerX - half, centerY + half * 0.2f);
        path.lineTo(centerX + half, centerY + half * 0.2f);
        path.lineTo(centerX, bottom + half * 0.6f);
        path.close();
        canvas.drawPath(path, paint);
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
