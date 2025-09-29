// app/src/main/java/com/example/robotparkour/scene/WorldSelectScene.java
package com.example.robotparkour.scene;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

import com.crobot.game.level.LevelCatalog;
import com.crobot.game.level.LevelDescriptor;
import com.example.robotparkour.R;
import com.example.robotparkour.core.Scene;
import com.example.robotparkour.core.SceneManager;
import com.example.robotparkour.core.SceneOverlayHost;
import com.example.robotparkour.core.SceneType;
import com.example.robotparkour.core.WorldInfo;
import com.example.robotparkour.scene.adapter.WorldSelectorAdapter;
import com.example.robotparkour.storage.WorldCompletionTracker;

import java.util.ArrayList;
import java.util.List;

/**
 * Presents a Material-themed overlay for selecting the dynamically generated labyrinths.
 */
public class WorldSelectScene implements Scene {

    private final Context appContext;
    private final SceneManager sceneManager;
    private final WorldCompletionTracker completionTracker;
    private final LayoutInflater inflater;

    @Nullable
    private SceneOverlayHost overlayHost;
    @Nullable
    private View overlayView;
    @Nullable
    private ViewPager2 viewPager;
    @Nullable
    private LinearLayout indicatorContainer;
    @Nullable
    private View previousButton;
    @Nullable
    private View nextButton;
    @Nullable
    private View closeButton;
    @Nullable
    private WorldSelectorAdapter adapter;

    private final ViewPager2.OnPageChangeCallback pageChangeCallback = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageSelected(int position) {
            updateIndicators(position);
            updateNavigationButtons(position);
        }
    };

    private List<LevelDescriptor> descriptors = new ArrayList<>();
    private List<List<LevelDescriptor>> pages = new ArrayList<>();
    @Nullable
    private WorldInfo highlightedWorld;

    public WorldSelectScene(Context context,
                            SceneManager sceneManager,
                            WorldCompletionTracker completionTracker) {
        this.appContext = context.getApplicationContext();
        this.sceneManager = sceneManager;
        this.completionTracker = completionTracker;
        Context overlayContext = new ContextThemeWrapper(context, R.style.Theme_RobotIdeParkour);
        this.inflater = LayoutInflater.from(overlayContext);
        reloadLevels();
    }

    public void setOverlayHost(@Nullable SceneOverlayHost host) {
        if (overlayHost != null && overlayView != null) {
            overlayHost.removeOverlay(overlayView);
        }
        overlayHost = host;
        if (overlayView != null && host != null) {
            host.showOverlay(overlayView);
        }
    }

    @Override
    public SceneType getType() {
        return SceneType.WORLD_SELECT;
    }

    @Override
    public void onEnter() {
        highlightedWorld = sceneManager.getSelectedWorld();
        reloadLevels();
        attachOverlay();
        if (adapter != null) {
            adapter.setHighlightedWorld(highlightedWorld);
        }
    }

    @Override
    public void onExit() {
        detachOverlay();
    }

    @Override
    public void update(float deltaSeconds) {
        // Static overlay; no simulation updates required.
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawColor(Color.argb(160, 6, 20, 36));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return false;
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
        // Overlay handles its own measurement via layout params.
    }

    @Override
    public boolean onBackPressed() {
        sceneManager.switchTo(SceneType.MENU);
        return true;
    }

    private void attachOverlay() {
        if (overlayHost == null) {
            return;
        }
        if (overlayView == null) {
            overlayView = inflater.inflate(R.layout.view_world_select_pager, null, false);
            viewPager = overlayView.findViewById(R.id.level_pager);
            indicatorContainer = overlayView.findViewById(R.id.pager_indicator);
            previousButton = overlayView.findViewById(R.id.button_previous);
            nextButton = overlayView.findViewById(R.id.button_next);
            closeButton = overlayView.findViewById(R.id.button_close);

            adapter = new WorldSelectorAdapter(inflater, completionTracker, this::onLevelSelected);
            adapter.submitPages(pages);
            adapter.setHighlightedWorld(highlightedWorld);

            if (viewPager != null) {
                viewPager.setAdapter(adapter);
                viewPager.registerOnPageChangeCallback(pageChangeCallback);
                viewPager.setOffscreenPageLimit(1);
            }
            if (previousButton != null) {
                previousButton.setOnClickListener(v -> navigateBy(-1));
            }
            if (nextButton != null) {
                nextButton.setOnClickListener(v -> navigateBy(1));
            }
            if (closeButton != null) {
                closeButton.setOnClickListener(v -> sceneManager.switchTo(SceneType.MENU));
            }
            buildIndicators();
            int initialPage = highlightedWorld != null ? findPageIndex(highlightedWorld) : 0;
            if (viewPager != null) {
                viewPager.setCurrentItem(Math.max(0, initialPage), false);
            }
            updateIndicators(Math.max(0, initialPage));
            updateNavigationButtons(Math.max(0, initialPage));
        }
        overlayHost.showOverlay(overlayView);
        int currentPage = viewPager != null ? viewPager.getCurrentItem() : 0;
        buildIndicators();
        updateIndicators(Math.max(0, currentPage));
        updateNavigationButtons(Math.max(0, currentPage));
    }

    private void detachOverlay() {
        if (overlayHost != null && overlayView != null) {
            overlayHost.removeOverlay(overlayView);
        }
        if (viewPager != null) {
            viewPager.unregisterOnPageChangeCallback(pageChangeCallback);
        }
    }

    private void reloadLevels() {
        LevelCatalog catalog = LevelCatalog.getInstance(appContext);
        descriptors = catalog.getDescriptors();
        pages = new ArrayList<>();
        if (descriptors != null && !descriptors.isEmpty()) {
            pages.add(new ArrayList<>(descriptors));
        }
        if (adapter != null) {
            adapter.submitPages(pages);
            adapter.setHighlightedWorld(highlightedWorld);
        }
        buildIndicators();
    }

    private void onLevelSelected(@NonNull LevelDescriptor descriptor) {
        highlightedWorld = descriptor.getWorldInfo();
        if (adapter != null) {
            adapter.setHighlightedWorld(highlightedWorld);
        }
        sceneManager.startWorld(descriptor.getWorldInfo());
    }

    private void navigateBy(int delta) {
        if (viewPager == null) {
            return;
        }
        int nextIndex = viewPager.getCurrentItem() + delta;
        int maxIndex = Math.max(0, pages.size() - 1);
        nextIndex = Math.max(0, Math.min(nextIndex, maxIndex));
        viewPager.setCurrentItem(nextIndex, true);
    }

    private void buildIndicators() {
        if (indicatorContainer == null) {
            return;
        }
        indicatorContainer.removeAllViews();
        LayoutInflater hostInflater = LayoutInflater.from(overlayHost != null ? overlayHost.getContext() : appContext);
        if (pages.size() <= 1) {
            indicatorContainer.setVisibility(View.GONE);
            return;
        }
        indicatorContainer.setVisibility(View.VISIBLE);
        for (int i = 0; i < pages.size(); i++) {
            View dot = hostInflater.inflate(R.layout.view_pager_dot, indicatorContainer, false);
            indicatorContainer.addView(dot);
        }
    }

    private void updateIndicators(int position) {
        if (indicatorContainer == null) {
            return;
        }
        if (indicatorContainer.getChildCount() == 0) {
            return;
        }
        int count = indicatorContainer.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = indicatorContainer.getChildAt(i);
            child.setSelected(i == position);
        }
    }

    private void updateNavigationButtons(int position) {
        int lastIndex = Math.max(0, pages.size() - 1);
        boolean allowPaging = pages.size() > 1;
        if (previousButton != null) {
            boolean enabled = allowPaging && position > 0;
            previousButton.setEnabled(enabled);
            previousButton.setAlpha(enabled ? 1f : 0.4f);
            previousButton.setVisibility(allowPaging ? View.VISIBLE : View.GONE);
        }
        if (nextButton != null) {
            boolean enabled = allowPaging && position < lastIndex;
            nextButton.setEnabled(enabled);
            nextButton.setAlpha(enabled ? 1f : 0.4f);
            nextButton.setVisibility(allowPaging ? View.VISIBLE : View.GONE);
        }
    }

    private int findPageIndex(@NonNull WorldInfo worldInfo) {
        int targetWorld = worldInfo.getProgramNumber();
        for (int i = 0; i < pages.size(); i++) {
            List<LevelDescriptor> page = pages.get(i);
            for (LevelDescriptor descriptor : page) {
                if (descriptor.getWorldNumber() == targetWorld) {
                    return i;
                }
            }
        }
        return 0;
    }
}
