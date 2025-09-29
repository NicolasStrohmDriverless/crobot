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
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private Map<Integer, Float> bestTimes = new HashMap<>();

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
        sceneManager.getAudioManager().setMusicTrack(R.raw.robot_cpp);
        sceneManager.getAudioManager().startMusic();
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

            adapter = new WorldSelectorAdapter(inflater, completionTracker, this::onLevelSelected);
            adapter.submitPages(pages);
            adapter.setHighlightedWorld(highlightedWorld);
            adapter.setBestTimes(bestTimes);

            if (viewPager != null) {
                viewPager.setAdapter(adapter);
                viewPager.registerOnPageChangeCallback(pageChangeCallback);
                viewPager.setOffscreenPageLimit(1);
            }
            if (previousButton != null) {
                previousButton.setOnClickListener(v -> handlePrevious());
            }
            if (nextButton != null) {
                nextButton.setOnClickListener(v -> navigateBy(1));
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
        pages = paginateDescriptors(descriptors);
        Map<Integer, Float> storedTimes = sceneManager.getScoreboardManager().getAllBestTimes();
        if (storedTimes == null) {
            bestTimes = new HashMap<>();
        } else {
            bestTimes = new HashMap<>(storedTimes);
        }
        if (adapter != null) {
            adapter.submitPages(pages);
            adapter.setHighlightedWorld(highlightedWorld);
            adapter.setBestTimes(bestTimes);
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
        boolean hasPages = !pages.isEmpty();
        if (previousButton != null) {
            previousButton.setVisibility(hasPages ? View.VISIBLE : View.GONE);
            previousButton.setEnabled(hasPages);
            if (previousButton instanceof MaterialButton) {
                MaterialButton button = (MaterialButton) previousButton;
                int textRes = position == 0 ? R.string.world_select_back_to_menu : R.string.world_select_back;
                button.setText(textRes);
            }
        }
        if (nextButton != null) {
            if (!allowPaging || position >= lastIndex) {
                nextButton.setVisibility(View.GONE);
                nextButton.setEnabled(false);
            } else {
                nextButton.setVisibility(View.VISIBLE);
                nextButton.setEnabled(true);
                if (nextButton instanceof MaterialButton) {
                    MaterialButton button = (MaterialButton) nextButton;
                    int textRes = position == 0 ? R.string.world_select_page_two : R.string.world_select_page_three;
                    button.setText(textRes);
                }
            }
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

    private void handlePrevious() {
        if (viewPager == null) {
            sceneManager.switchTo(SceneType.MENU);
            return;
        }
        int current = viewPager.getCurrentItem();
        if (current <= 0) {
            sceneManager.switchTo(SceneType.MENU);
        } else {
            viewPager.setCurrentItem(current - 1, true);
        }
    }

    private List<List<LevelDescriptor>> paginateDescriptors(List<LevelDescriptor> descriptors) {
        List<List<LevelDescriptor>> result = new ArrayList<>();
        if (descriptors == null || descriptors.isEmpty()) {
            return result;
        }
        int index = 0;
        int[] firstPages = new int[] {4, 4};
        for (int size : firstPages) {
            if (index >= descriptors.size()) {
                break;
            }
            int end = Math.min(descriptors.size(), index + size);
            result.add(new ArrayList<>(descriptors.subList(index, end)));
            index = end;
        }
        if (index < descriptors.size()) {
            result.add(new ArrayList<>(descriptors.subList(index, descriptors.size())));
        }
        return result;
    }
}
