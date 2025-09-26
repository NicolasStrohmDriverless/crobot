// app/src/main/java/com/example/robotparkour/core/Scene.java
package com.example.robotparkour.core;

import android.graphics.Canvas;
import android.view.KeyEvent;
import android.view.MotionEvent;

/**
 * Basic contract implemented by every scene (menu, gameplay, etc.).
 */
public interface Scene {

    /**
     * @return the unique type so the manager can identify the scene.
     */
    SceneType getType();

    /**
     * Called whenever the scene becomes the active scene.
     */
    void onEnter();

    /**
     * Called when the scene is replaced by another one.
     */
    void onExit();

    /**
     * Steps the scene logic using a fixed delta-time from the game loop.
     */
    void update(float deltaSeconds);

    /**
     * Renders the scene onto the supplied canvas.
     */
    void draw(Canvas canvas);

    /**
     * Handles touch interaction. Return {@code true} if the event was used.
     */
    boolean onTouchEvent(MotionEvent event);

    /**
     * Handles key down events (for emulator / hardware keys).
     */
    boolean onKeyDown(int keyCode, KeyEvent event);

    /**
     * Handles key up events (for emulator / hardware keys).
     */
    boolean onKeyUp(int keyCode, KeyEvent event);

    /**
     * Provides the new viewport size whenever the surface changes.
     */
    void onSurfaceChanged(int width, int height);

    /**
     * Gives the scene a chance to consume the back button.
     */
    boolean onBackPressed();
}
