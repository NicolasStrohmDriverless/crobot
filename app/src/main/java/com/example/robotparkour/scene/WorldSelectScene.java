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
                0.12f, 0.42f, 1.05f));
        worldCards.add(new WorldCard(
                new WorldInfo(
                        2,
                        "Template Temple",
                        "komplex, verschachtelt"),
                0.28f, 0.26f, 1.0f));
        worldCards.add(new WorldCard(
                new WorldInfo(
                        3,
                        "Namespace Nebula",
                        "spacey, schwebend"),
                0.50f, 0.22f, 0.95f));
        worldCards.add(new WorldCard(
                new WorldInfo(
                        4,
                        "Exception Volcano",
                        "heiß, leicht bedrohlich – kein Boss, nur Spannung"),
                0.78f, 0.30f, 1.05f));
        worldCards.add(new WorldCard(
                new WorldInfo(
                        5,
                        "STL City",
                        "geschäftig, groovy"),
                0.80f, 0.55f, 1.0f));
        worldCards.add(new WorldCard(
                new WorldInfo(
                        6,
                        "Heap Caverns",
                        "dunkel, hohl, vorsichtig"),
                0.58f, 0.72f, 1.0f));
        worldCards.add(new WorldCard(
                new WorldInfo(
                        7,
                        "Lambda Gardens",
                        "verspielt, naturhaft, \"funky nerdy\""),
                0.32f, 0.66f, 1.05f));
        worldCards.add(new WorldCard(
                new WorldInfo(
                        8,
                        "Multithread Foundry",
                        "antriebsstark, mechanisch"),
                0.18f, 0.55f, 0.95f));
        worldCards.add(new WorldCard(
                new WorldInfo(
                        9,
                        "NullPointer-Nexus",
                        "Der Kernel-Kerker"),
                0.50f, 0.82f, 1.35f));
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
        paint.setAlpha(140);
        canvas.drawCircle(surfaceWidth * 0.18f, surfaceHeight * 0.32f, surfaceWidth * 0.12f, paint);
        canvas.drawCircle(surfaceWidth * 0.78f, surfaceHeight * 0.40f, surfaceWidth * 0.16f, paint);
        canvas.drawCircle(surfaceWidth * 0.46f, surfaceHeight * 0.74f, surfaceWidth * 0.18f, paint);
        paint.setAlpha(255);
    }

    private void drawDottedPath(Canvas canvas) {
        if (worldCards.size() < 2) {
            return;
        }

        Path path = new Path();
        boolean started = false;
        for (WorldCard card : worldCards) {
            if (!started) {
                path.moveTo(card.centerX, card.centerY);
                started = true;
            } else {
                path.lineTo(card.centerX, card.centerY);
            }
        }

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(4f, surfaceWidth * 0.004f));
        paint.setColor(Color.parseColor("#66C9E6FF"));
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setPathEffect(new DashPathEffect(new float[]{28f, 20f}, 0));
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
        paint.setStrokeWidth(isActive ? 8f : 4f);
        paint.setColor(isActive ? Color.parseColor("#9CD2FF") : Color.parseColor("#2C4F66"));
        paint.setPathEffect(new DashPathEffect(new float[]{18f, 14f}, 0));
        canvas.drawRoundRect(card.bounds, cornerRadius, cornerRadius, paint);
        paint.setPathEffect(null);

        float centerX = card.centerX;
        float top = card.bounds.top;
        float cardHeight = card.bounds.height();

        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Color.parseColor("#88C7FF"));
        paint.setTextSize(cardHeight * 0.18f);
        String label = card.worldInfo.getProgramNumber() >= 9
                ? "final.class"
                : "Program " + card.worldInfo.getProgramNumber();
        canvas.drawText(label, centerX, top + cardHeight * 0.28f, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(cardHeight * 0.28f);
        canvas.drawText(card.worldInfo.getName(), centerX, top + cardHeight * 0.55f, paint);

        paint.setColor(Color.parseColor("#C9E6FF"));
        paint.setTextSize(cardHeight * 0.20f);
        float textAreaWidth = card.bounds.width() * 0.76f;
        float descStartY = top + cardHeight * 0.70f;
        float lineHeight = paint.getTextSize() * 1.25f;
        descStartY = drawWrappedCenteredText(canvas, card.worldInfo.getDescription(), centerX,
                descStartY, textAreaWidth, lineHeight);

        paint.setColor(Color.parseColor("#7FB3FF"));
        paint.setTextSize(cardHeight * 0.19f);
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
        float baseSize = Math.min(width, height) * 0.17f;
        float usableWidth = width - padding * 2f;
        float usableHeight = height - padding * 2f;

        for (WorldCard card : worldCards) {
            float centerX = padding + card.relX * usableWidth;
            float centerY = padding + card.relY * usableHeight;
            float cardWidth = baseSize * card.sizeMultiplier;
            float cardHeight = cardWidth * 0.78f;
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
