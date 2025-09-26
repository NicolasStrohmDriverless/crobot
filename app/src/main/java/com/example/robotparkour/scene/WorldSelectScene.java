// app/src/main/java/com/example/robotparkour/scene/WorldSelectScene.java
package com.example.robotparkour.scene;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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

        WorldCard(WorldInfo worldInfo) {
            this.worldInfo = worldInfo;
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
        worldCards.add(new WorldCard(new WorldInfo(
                1,
                "Pointer Plains",
                "Startwelt, leicht & freundlich")));
        worldCards.add(new WorldCard(new WorldInfo(
                2,
                "Lambda Lagoon",
                "Funktionale Strände für ruhige Runs")));
        worldCards.add(new WorldCard(new WorldInfo(
                3,
                "Stacktrace Summit",
                "Für Profis – steile Lernkurve inklusive")));
        worldCards.add(new WorldCard(new WorldInfo(
                4,
                "Bytecode Bazaar",
                "Schnelle Gegner, viele Power-Ups")));
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
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#0B1F2E"));
        canvas.drawRect(0, 0, surfaceWidth, surfaceHeight, paint);

        drawHeader(canvas);
        drawWorldCards(canvas);
        drawButton(canvas, backButton, "Back");
    }

    private void drawHeader(Canvas canvas) {
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(surfaceWidth * 0.075f);
        canvas.drawText("Choose Your Program", surfaceWidth / 2f, surfaceHeight * 0.14f, paint);

        paint.setColor(Color.parseColor("#7FB3FF"));
        paint.setTextSize(surfaceWidth * 0.04f);
        canvas.drawText("Wähle eine Simulation für den nächsten Run", surfaceWidth / 2f, surfaceHeight * 0.21f, paint);
    }

    private void drawWorldCards(Canvas canvas) {
        paint.setTextAlign(Paint.Align.LEFT);
        for (WorldCard card : worldCards) {
            boolean isActive = highlightedWorld != null && highlightedWorld.equals(card.worldInfo);
            drawWorldCard(canvas, card, isActive);
        }
    }

    private void drawWorldCard(Canvas canvas, WorldCard card, boolean isActive) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(isActive ? Color.parseColor("#1F5F8B") : Color.parseColor("#12324A"));
        canvas.drawRoundRect(card.bounds, 26f, 26f, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(isActive ? 6f : 3f);
        paint.setColor(isActive ? Color.parseColor("#9CD2FF") : Color.parseColor("#274863"));
        canvas.drawRoundRect(card.bounds, 26f, 26f, paint);

        float padding = card.bounds.height() * 0.18f;
        float textX = card.bounds.left + padding;
        float textY = card.bounds.top + padding * 1.8f;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setTextSize(card.bounds.height() * 0.32f);
        canvas.drawText(card.worldInfo.getName(), textX, textY, paint);

        paint.setColor(Color.parseColor("#C9E6FF"));
        paint.setTextSize(card.bounds.height() * 0.22f);
        float descY = textY + paint.getTextSize() * 1.5f;
        canvas.drawText(card.worldInfo.getDescription(), textX, descY, paint);

        paint.setColor(Color.parseColor("#7FB3FF"));
        paint.setTextSize(card.bounds.height() * 0.24f);
        String cta = isActive ? "Tippe zum Starten" : "Tippe zum Laden";
        canvas.drawText(cta, textX, card.bounds.bottom - padding * 0.6f, paint);
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

        float cardWidth = width * 0.76f;
        float cardHeight = height * 0.16f;
        float spacing = cardHeight * 0.55f;
        float startY = height * 0.28f;
        float centerX = width / 2f;

        for (int i = 0; i < worldCards.size(); i++) {
            WorldCard card = worldCards.get(i);
            float top = startY + i * (cardHeight + spacing);
            card.bounds.set(
                    centerX - cardWidth / 2f,
                    top,
                    centerX + cardWidth / 2f,
                    top + cardHeight);
        }

        float buttonWidth = width * 0.45f;
        float buttonHeight = height * 0.09f;
        backButton.set(centerX - buttonWidth / 2f,
                height * 0.82f,
                centerX + buttonWidth / 2f,
                height * 0.82f + buttonHeight);
    }

    @Override
    public boolean onBackPressed() {
        sceneManager.switchTo(SceneType.MENU);
        return true;
    }
}
