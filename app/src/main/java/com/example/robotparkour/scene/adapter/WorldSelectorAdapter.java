package com.example.robotparkour.scene.adapter;

import android.content.res.Resources;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.crobot.game.level.LevelDescriptor;
import com.example.robotparkour.R;
import com.example.robotparkour.core.WorldInfo;
import com.example.robotparkour.storage.WorldCompletionTracker;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Supplies pages of level buttons to the ViewPager in the world select overlay.
 */
public final class WorldSelectorAdapter extends RecyclerView.Adapter<WorldSelectorAdapter.PageViewHolder> {

    public interface OnLevelSelectedListener {
        void onLevelSelected(@NonNull LevelDescriptor descriptor);
    }

    private final LayoutInflater inflater;
    private final WorldCompletionTracker completionTracker;
    private final OnLevelSelectedListener listener;
    private final int buttonMarginPx;

    private List<List<LevelDescriptor>> pages = new ArrayList<>();
    @Nullable
    private WorldInfo highlightedWorld;

    public WorldSelectorAdapter(@NonNull LayoutInflater inflater,
                                @NonNull WorldCompletionTracker completionTracker,
                                @NonNull OnLevelSelectedListener listener) {
        this.inflater = inflater;
        this.completionTracker = completionTracker;
        this.listener = listener;
        Resources res = inflater.getContext().getResources();
        this.buttonMarginPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8,
                res.getDisplayMetrics());
    }

    public void submitPages(@NonNull List<List<LevelDescriptor>> newPages) {
        pages = new ArrayList<>(newPages);
        notifyDataSetChanged();
    }

    public void setHighlightedWorld(@Nullable WorldInfo worldInfo) {
        highlightedWorld = worldInfo;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_level_page, parent, false);
        return new PageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        List<LevelDescriptor> page = position < pages.size() ? pages.get(position) : Collections.emptyList();
        holder.bind(page, highlightedWorld);
    }

    @Override
    public int getItemCount() {
        return pages.size();
    }

    final class PageViewHolder extends RecyclerView.ViewHolder {

        private final GridLayout gridLayout;

        PageViewHolder(@NonNull View itemView) {
            super(itemView);
            gridLayout = itemView.findViewById(R.id.level_grid);
        }

        void bind(@NonNull List<LevelDescriptor> levels, @Nullable WorldInfo activeWorld) {
            gridLayout.removeAllViews();
            if (levels.isEmpty()) {
                return;
            }
            gridLayout.setColumnCount(2);
            for (int i = 0; i < levels.size(); i++) {
                LevelDescriptor descriptor = levels.get(i);
                MaterialButton button = (MaterialButton) inflater.inflate(R.layout.item_level_button,
                        gridLayout, false);
                WorldInfo worldInfo = descriptor.getWorldInfo();
                String title = worldInfo.getName();
                String subtitle = worldInfo.getDescription();
                button.setText(title + "\n" + subtitle);
                button.setIconResource(R.drawable.ic_labyrinth);
                button.setIconTintResource(R.color.accent_gold);
                button.setOnClickListener(v -> listener.onLevelSelected(descriptor));
                boolean completed = completionTracker.isWorldCompleted(descriptor.getWorldNumber());
                button.setChecked(completed);
                boolean isActive = activeWorld != null && activeWorld.equals(worldInfo);
                button.setActivated(isActive);
                button.setAlpha(isActive ? 1f : 0.9f);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = 0;
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                params.columnSpec = GridLayout.spec(i % 2, 1f);
                params.rowSpec = GridLayout.spec(i / 2, 1f);
                params.setMargins(buttonMarginPx, buttonMarginPx, buttonMarginPx, buttonMarginPx);
                gridLayout.addView(button, params);
            }
        }
    }
}
