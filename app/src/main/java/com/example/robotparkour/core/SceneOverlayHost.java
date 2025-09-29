package com.example.robotparkour.core;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;

/**
 * Allows scenes to request lightweight Android view overlays on top of the SurfaceView.
 */
public interface SceneOverlayHost {

    @NonNull
    Context getContext();

    void showOverlay(@NonNull View view);

    void removeOverlay(@NonNull View view);
}
