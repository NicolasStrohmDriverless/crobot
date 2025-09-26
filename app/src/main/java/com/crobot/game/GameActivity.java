// app/src/main/java/com/crobot/game/GameActivity.java
package com.crobot.game;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.crobot.game.level.LevelModel;
import com.crobot.game.level.LevelRepository;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Activity that hosts the platformer {@link GameView} and loads levels through the NDK pipeline.
 */
public class GameActivity extends AppCompatActivity {

    public static final String EXTRA_WORLD = "com.crobot.game.EXTRA_WORLD";
    public static final String EXTRA_STAGE = "com.crobot.game.EXTRA_STAGE";

    private GameView gameView;
    private View loadingView;
    private LevelRepository levelRepository;
    private ExecutorService executorService;
    private Handler mainHandler;

    public static void start(@NonNull Context context, int world, int stage) {
        Intent intent = new Intent(context, GameActivity.class);
        intent.putExtra(EXTRA_WORLD, world);
        intent.putExtra(EXTRA_STAGE, stage);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_platformer);

        gameView = findViewById(R.id.platformer_view);
        loadingView = findViewById(R.id.loading_overlay);

        levelRepository = new LevelRepository(this);
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        int world = getIntent().getIntExtra(EXTRA_WORLD, 1);
        int stage = getIntent().getIntExtra(EXTRA_STAGE, 1);
        loadLevel(world, stage);

        setupControls();
    }

    private void setupControls() {
        View left = findViewById(R.id.button_left);
        View right = findViewById(R.id.button_right);
        View jump = findViewById(R.id.button_jump);

        View.OnTouchListener listener = (view, motionEvent) -> {
            switch (view.getId()) {
                case R.id.button_left:
                    gameView.handleButtonTouch(GameView.Control.LEFT, motionEvent);
                    break;
                case R.id.button_right:
                    gameView.handleButtonTouch(GameView.Control.RIGHT, motionEvent);
                    break;
                case R.id.button_jump:
                    gameView.handleButtonTouch(GameView.Control.JUMP, motionEvent);
                    break;
                default:
                    break;
            }
            return true;
        };

        left.setOnTouchListener(listener);
        right.setOnTouchListener(listener);
        jump.setOnTouchListener(listener);
    }

    private void loadLevel(int world, int stage) {
        if (loadingView != null) {
            loadingView.setVisibility(View.VISIBLE);
        }
        executorService.execute(() -> {
            try {
                LevelModel level = levelRepository.loadLevel(world, stage);
                mainHandler.post(() -> {
                    if (loadingView != null) {
                        loadingView.setVisibility(View.GONE);
                    }
                    gameView.bindLevel(level);
                });
            } catch (IOException | RuntimeException ex) {
                mainHandler.post(() -> {
                    Toast.makeText(GameActivity.this, ex.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameView.onHostResume();
    }

    @Override
    protected void onPause() {
        gameView.onHostPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        gameView.onHostDestroy();
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }
        super.onDestroy();
    }
}
