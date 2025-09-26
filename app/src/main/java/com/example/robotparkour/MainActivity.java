// app/src/main/java/com/example/robotparkour/MainActivity.java
package com.example.robotparkour;

import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.crobot.game.ui.MenuIntegration;
import com.example.robotparkour.core.GameView;

/**
 * Hosts the {@link GameView} and bridges Android lifecycle callbacks to the game engine.
 */
public class MainActivity extends AppCompatActivity {

    private GameView gameView;
    private OnBackPressedCallback backPressedCallback;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);
        gameView = findViewById(R.id.game_view);
        MenuIntegration.registerHost(this);

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
        }
        MenuIntegration.unregisterHost(this);
        if (backPressedCallback != null) {
            backPressedCallback.remove();
            backPressedCallback = null;
        }
        super.onDestroy();
    }
}
