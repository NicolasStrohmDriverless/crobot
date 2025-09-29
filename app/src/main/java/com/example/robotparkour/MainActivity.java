// app/src/main/java/com/example/robotparkour/MainActivity.java
package com.example.robotparkour;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.crobot.game.ui.MenuIntegration;
import com.example.robotparkour.R;
import com.example.robotparkour.core.SceneOverlayHost;
import com.example.robotparkour.core.GameView;

/**
 * Hosts the {@link GameView} and bridges Android lifecycle callbacks to the game engine.
 */
public class MainActivity extends AppCompatActivity {

    private GameView gameView;
    private ViewGroup overlayContainer;
    private SceneOverlayHost overlayHost;
    private OnBackPressedCallback backPressedCallback;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);
        gameView = findViewById(R.id.game_view);
        overlayContainer = findViewById(R.id.overlay_container);
        MenuIntegration.registerHost(this);

        if (gameView != null) {
            overlayHost = new SceneOverlayHost() {
                @Override
                public android.content.Context getContext() {
                    return MainActivity.this;
                }

                @Override
                public void showOverlay(@NonNull View view) {
                    if (overlayContainer == null) {
                        return;
                    }
                    if (view.getLayoutParams() == null) {
                        view.setLayoutParams(new ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT));
                    }
                    if (view.getParent() != overlayContainer) {
                        overlayContainer.addView(view);
                    }
                    view.setVisibility(View.VISIBLE);
                }

                @Override
                public void removeOverlay(@NonNull View view) {
                    if (overlayContainer == null) {
                        return;
                    }
                    overlayContainer.removeView(view);
                }
            };
            gameView.getSceneManager().setOverlayHost(overlayHost);
        }

        backPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (gameView == null || !gameView.handleBackPressed()) {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, backPressedCallback);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameView != null) {
            gameView.resumeGame();
        }
    }

    @Override
    protected void onPause() {
        if (gameView != null) {
            gameView.pauseGame();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (gameView != null) {
            gameView.releaseResources();
            gameView.getSceneManager().setOverlayHost(null);
        }
        MenuIntegration.unregisterHost(this);
        if (backPressedCallback != null) {
            backPressedCallback.remove();
            backPressedCallback = null;
        }
        super.onDestroy();
    }
}
