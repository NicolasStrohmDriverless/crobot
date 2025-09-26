// app/src/main/java/com/example/robotparkour/scene/WorldSelectScene.java
package com.example.robotparkour.scene;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.example.robotparkour.core.Scene;
import com.example.robotparkour.core.SceneManager;
import com.example.robotparkour.core.SceneType;
import com.example.robotparkour.core.WorldInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Lets the player pick which themed "program" (world) to attempt next.
 */
public class WorldSelectScene implements Scene {

    private static final class WorldCard {
        final WorldInfo worldInfo;
        final RectF bounds = new RectF();
        final float relX;
        final float relY;
        final float sizeMultiplier;
        float centerX;
        float centerY;

        WorldCard(WorldInfo worldInfo, float relX, float relY, float sizeMultiplier) {
            this.worldInfo = worldInfo;
            this.relX = relX;
            this.relY = relY;
            this.sizeMultiplier = sizeMultiplier;
        }
    }

    private final SceneManager sceneManager;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF backButton = new RectF();
    private final List<WorldCard> worldCards = new ArrayList<>();

    private int surfaceWidth;
    private int surfaceHeight;
    private WorldInfo highlightedWorld;

    public WorldSelectScene(Context context, SceneManager sceneManager) {
        this.sceneManager = sceneManager;
        populateWorlds();
    }

    private void populateWorlds() {
        worldCards.clear();
        worldCards.add(new WorldCard(
                new WorldInfo(
                        1,
                        "Pointer Plains",
                        "Startwelt, leicht & freundlich"),
                0.16f, 0.40f, 1.12f));
        worldCards.add(new WorldCard(
                new WorldInfo(
                        2,
                        "Template Temple",
                        "komplex, verschachtelt"),
                0.30f, 0.25f, 1.08f));
        worldCards.add(new WorldCard(
                new WorldInfo(
                        3,
                        "Namespace Nebula",
                        "spacey, schwebend"),
                0.52f, 0.18f, 1.02f));
        worldCards.add(new WorldCard(
                new WorldInfo(
                        4,
                        "Exception Volcano",
                        "heiß, leicht bedrohlich – kein Boss, nur Spannung"),
                0.76f, 0.30f, 1.12f));
        worldCards.add(new WorldCard(
                new WorldInfo(
                        5,
                        "STL City",
                        "geschäftig, groovy"),
                0.84f, 0.50f, 1.08f));
        worldCards.add(new WorldCard(
                new WorldInfo(
                        6,
                        "Heap Caverns",
                        "dunkel, hohl, vorsichtig"),
                0.64f, 0.68f, 1.10f));
        worldCards.add(new WorldCard(
                new WorldInfo(
                        7,
                        "Lambda Gardens",
                        "verspielt, naturhaft, \"funky nerdy\""),
                0.38f, 0.74f, 1.16f));
        worldCards.add(new WorldCard(
                new WorldInfo(
                        8,
                        "Multithread Foundry",
                        "antriebsstark, mechanisch"),
                0.22f, 0.60f, 1.02f));
        worldCards.add(new WorldCard(
                new WorldInfo(
                        9,
                        "NullPointer-Nexus",
                        "Der Kernel-Kerker"),
                0.48f, 0.84f, 1.40f));
    }

    @Override
    public SceneType getType() {
        return SceneType.WORLD_SELECT;
    }

    @Override
    public void onEnter() {
        highlightedWorld = sceneManager.getSelectedWorld();
    }

    @Override
    public void onExit() {
        // Nothing to reset currently.
    }

    @Override
    public void update(float deltaSeconds) {
        // Static menu.
    }

    @Override
    public void draw(Canvas canvas) {
        drawMapBackdrop(canvas);
        drawHeader(canvas);
        drawDottedPath(canvas);
        drawWorldCards(canvas);
        drawButton(canvas, backButton, "Back");
    }

    private void drawHeader(Canvas canvas) {
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(surfaceWidth * 0.07f);
        canvas.drawText("Programmierer-Schatzkarte", surfaceWidth / 2f, surfaceHeight * 0.13f, paint);

        paint.setColor(Color.parseColor("#7FB3FF"));
        paint.setTextSize(surfaceWidth * 0.038f);
        canvas.drawText("Folge der gestrichelten Route zum NullPointer-Nexus", surfaceWidth / 2f, surfaceHeight * 0.19f, paint);
    }

    private void drawWorldCards(Canvas canvas) {
        for (WorldCard card : worldCards) {
            boolean isActive = highlightedWorld != null && highlightedWorld.equals(card.worldInfo);
            drawWorldCard(canvas, card, isActive);
        }
    }

    private void drawMapBackdrop(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#041521"));
        canvas.drawRect(0, 0, surfaceWidth, surfaceHeight, paint);

        paint.setColor(Color.parseColor("#0B2737"));
        canvas.drawRect(surfaceWidth * 0.04f, surfaceHeight * 0.09f,
                surfaceWidth * 0.96f, surfaceHeight * 0.92f, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(surfaceWidth * 0.0025f);
        paint.setColor(Color.parseColor("#113B52"));
        float gridStep = surfaceWidth * 0.10f;
        for (float x = surfaceWidth * 0.04f; x <= surfaceWidth * 0.96f; x += gridStep) {
            canvas.drawLine(x, surfaceHeight * 0.09f, x, surfaceHeight * 0.92f, paint);
        }
        for (float y = surfaceHeight * 0.09f; y <= surfaceHeight * 0.92f; y += gridStep) {
            canvas.drawLine(surfaceWidth * 0.04f, y, surfaceWidth * 0.96f, y, paint);
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#06202F"));
        paint.setAlpha(150);
        drawCurvyIsland(canvas, surfaceWidth * 0.22f, surfaceHeight * 0.36f,
                surfaceWidth * 0.13f, surfaceHeight * 0.10f, 0.24f);
        drawCurvyIsland(canvas, surfaceWidth * 0.76f, surfaceHeight * 0.38f,
                surfaceWidth * 0.17f, surfaceHeight * 0.13f, 0.20f);
        drawCurvyIsland(canvas, surfaceWidth * 0.48f, surfaceHeight * 0.72f,
                surfaceWidth * 0.19f, surfaceHeight * 0.14f, 0.26f);
        paint.setAlpha(255);
    }

    private void drawCurvyIsland(Canvas canvas, float centerX, float centerY,
                                 float radiusX, float radiusY, float wobble) {
        Path islandPath = new Path();
        int segments = 6;
        for (int i = 0; i <= segments; i++) {
            double angle = (Math.PI * 2.0 / segments) * i;
            float sin = (float) Math.sin(angle);
            float cos = (float) Math.cos(angle);
            float radialX = radiusX * (1f + wobble * (float) Math.sin(angle * 2.1f));
            float radialY = radiusY * (1f + wobble * (float) Math.cos(angle * 2.1f));
            float x = centerX + cos * radialX;
            float y = centerY + sin * radialY;
            if (i == 0) {
                islandPath.moveTo(x, y);
            } else {
                double controlAngle = angle - (Math.PI * 2.0 / segments) / 2.0;
                float controlRadialX = radiusX * (1f + wobble * 0.6f * (float) Math.sin(controlAngle * 2.1f));
                float controlRadialY = radiusY * (1f + wobble * 0.6f * (float) Math.cos(controlAngle * 2.1f));
                float controlX = centerX + (float) Math.cos(controlAngle) * controlRadialX;
                float controlY = centerY + (float) Math.sin(controlAngle) * controlRadialY;
                islandPath.quadTo(controlX, controlY, x, y);
            }
        }
        islandPath.close();
        canvas.drawPath(islandPath, paint);
    }

    private void drawDottedPath(Canvas canvas) {
        if (worldCards.size() < 2) {
            return;
        }

        Path path = new Path();
        WorldCard previous = null;
        int segmentIndex = 0;
        for (WorldCard card : worldCards) {
            if (previous == null) {
                path.moveTo(card.centerX, card.centerY);
                previous = card;
                continue;
            }

            float dx = card.centerX - previous.centerX;
            float dy = card.centerY - previous.centerY;
            float distance = (float) Math.hypot(dx, dy);
            if (distance == 0f) {
                previous = card;
                continue;
            }

            float perpX = -dy / distance;
            float perpY = dx / distance;
            float arcStrength = Math.min(distance * 0.22f, surfaceWidth * 0.14f);
            float wiggle = (segmentIndex % 2 == 0) ? 1f : -1f;
            float controlOffsetX = perpX * arcStrength * wiggle;
            float controlOffsetY = perpY * arcStrength * wiggle;

            float control1X = previous.centerX + dx * 0.33f + controlOffsetX;
            float control1Y = previous.centerY + dy * 0.33f + controlOffsetY;
            float control2X = previous.centerX + dx * 0.66f + controlOffsetX;
            float control2Y = previous.centerY + dy * 0.66f + controlOffsetY;

            path.cubicTo(control1X, control1Y, control2X, control2Y, card.centerX, card.centerY);

            previous = card;
            segmentIndex++;
        }

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(4f, surfaceWidth * 0.0035f));
        paint.setColor(Color.parseColor("#66C9E6FF"));
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setPathEffect(new DashPathEffect(new float[]{34f, 26f}, 0));
        canvas.drawPath(path, paint);
        paint.setPathEffect(null);
        paint.setStrokeCap(Paint.Cap.BUTT);
    }

    private void drawWorldCard(Canvas canvas, WorldCard card, boolean isActive) {
        float cornerRadius = card.bounds.height() * 0.45f;

        // Shadow halo for "island"
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#071823"));
        canvas.drawRoundRect(expand(card.bounds, card.bounds.width() * 0.08f), cornerRadius, cornerRadius, paint);

        paint.setColor(isActive ? Color.parseColor("#1B4D6B") : Color.parseColor("#12324A"));
        canvas.drawRoundRect(card.bounds, cornerRadius, cornerRadius, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(isActive ? 7f : 4f);
        paint.setColor(isActive ? Color.parseColor("#9CD2FF") : Color.parseColor("#2C4F66"));
        paint.setPathEffect(new DashPathEffect(new float[]{24f, 16f}, 0));
        canvas.drawRoundRect(card.bounds, cornerRadius, cornerRadius, paint);
        paint.setPathEffect(null);

        float centerX = card.centerX;
        float top = card.bounds.top;
        float cardHeight = card.bounds.height();

        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Color.parseColor("#88C7FF"));
        paint.setTextSize(cardHeight * 0.16f);
        String label = card.worldInfo.getProgramNumber() >= 9
                ? "final.class"
                : "Program " + card.worldInfo.getProgramNumber();
        canvas.drawText(label, centerX, top + cardHeight * 0.28f, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(cardHeight * 0.24f);
        canvas.drawText(card.worldInfo.getName(), centerX, top + cardHeight * 0.55f, paint);

        paint.setColor(Color.parseColor("#C9E6FF"));
        paint.setTextSize(cardHeight * 0.18f);
        float textAreaWidth = card.bounds.width() * 0.80f;
        float descStartY = top + cardHeight * 0.70f;
        float lineHeight = paint.getTextSize() * 1.20f;
        descStartY = drawWrappedCenteredText(canvas, card.worldInfo.getDescription(), centerX,
                descStartY, textAreaWidth, lineHeight);

        paint.setColor(Color.parseColor("#7FB3FF"));
        paint.setTextSize(cardHeight * 0.17f);
        String cta = isActive ? "tap(); // starten" : "load();";
        canvas.drawText(cta, centerX, Math.min(card.bounds.bottom - cardHeight * 0.12f, descStartY + lineHeight), paint);
    }

    private float drawWrappedCenteredText(Canvas canvas, String text, float centerX,
                                          float startY, float maxWidth, float lineHeight) {
        String[] words = text.split(" ");
        if (words.length == 0) {
            return startY;
        }

        StringBuilder lineBuilder = new StringBuilder();
        float y = startY;
        for (String word : words) {
            String candidate = lineBuilder.length() == 0
                    ? word
                    : lineBuilder.toString() + " " + word;
            if (paint.measureText(candidate) <= maxWidth || lineBuilder.length() == 0) {
                lineBuilder.setLength(0);
                lineBuilder.append(candidate);
            } else {
                canvas.drawText(lineBuilder.toString(), centerX, y, paint);
                y += lineHeight;
                lineBuilder.setLength(0);
                lineBuilder.append(word);
            }
        }

        if (lineBuilder.length() > 0) {
            canvas.drawText(lineBuilder.toString(), centerX, y, paint);
        }

        return y;
    }

    private RectF expand(RectF rect, float amount) {
        return new RectF(
                rect.left - amount,
                rect.top - amount,
                rect.right + amount,
                rect.bottom + amount);
    }

    private void drawButton(Canvas canvas, RectF bounds, String text) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#4A90E2"));
        canvas.drawRoundRect(bounds, 24f, 24f, paint);
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(bounds.height() * 0.45f);
        canvas.drawText(text, bounds.centerX(), bounds.centerY() + paint.getTextSize() * 0.3f, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            float x = event.getX();
            float y = event.getY();

            if (backButton.contains(x, y)) {
                sceneManager.switchTo(SceneType.MENU);
                return true;
            }

            for (WorldCard card : worldCards) {
                if (card.bounds.contains(x, y)) {
                    highlightedWorld = card.worldInfo;
                    sceneManager.startWorld(card.worldInfo);
                    return true;
                }
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

        float padding = width * 0.08f;
        float baseSize = Math.min(width, height) * 0.20f;
        float usableWidth = width - padding * 2f;
        float usableHeight = height - padding * 2f;

        for (WorldCard card : worldCards) {
            float centerX = padding + card.relX * usableWidth;
            float centerY = padding + card.relY * usableHeight;
            float cardWidth = baseSize * card.sizeMultiplier;
            float cardHeight = cardWidth * 0.88f;
            card.centerX = centerX;
            card.centerY = centerY;
            card.bounds.set(
                    centerX - cardWidth / 2f,
                    centerY - cardHeight / 2f,
                    centerX + cardWidth / 2f,
                    centerY + cardHeight / 2f);
        }

        float buttonWidth = width * 0.2f;
        float buttonHeight = height * 0.08f;
        backButton.set(padding,
                padding * 0.8f,
                padding + buttonWidth,
                padding * 0.8f + buttonHeight);
    }

    @Override
    public boolean onBackPressed() {
        sceneManager.switchTo(SceneType.MENU);
        return true;
    }
}
