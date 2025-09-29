package com.crobot.game.enemy;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import androidx.annotation.NonNull;

import com.example.robotparkour.R;

import java.util.Locale;

/**
 * Factory creating lightweight animated sprites based on the logical enemy type.
 */
public final class EnemyAnimations {

    private static Bitmap spamDroneBitmap;

    private EnemyAnimations() {
    }

    @NonNull
    public static AnimatedEnemy create(@NonNull Context context, @NonNull String typeName) {
        String key = typeName.toLowerCase(Locale.US);
        switch (key) {
            case "bugblob":
                return new AnimatedEnemy(Color.parseColor("#4DA3FF"), Color.parseColor("#FFFFFF"), 4f, 1.4f, 0.45f, false);
            case "keylogger_beetle":
                return new AnimatedEnemy(Color.parseColor("#F06292"), Color.parseColor("#FFE0F0"), 3f, 1.6f, 0.42f, true);
            case "cloud_leech":
                return new AnimatedEnemy(Color.parseColor("#B39DDB"), Color.parseColor("#FFFFFF"), 5f, 1.2f, 0.5f, false);
            case "glitch_saw":
                return new AnimatedEnemy(Color.parseColor("#CE93D8"), Color.parseColor("#FFFFFF"), 6f, 2.4f, 0.35f, true);
            case "bsod_block":
                return new AnimatedEnemy(Color.parseColor("#3F74FF"), Color.parseColor("#FFFFFF"), 2f, 1.0f, 0.4f, false);
            case "patch_golem":
                return new AnimatedEnemy(Color.parseColor("#8D6E63"), Color.parseColor("#FFE0B2"), 3f, 1.1f, 0.48f, false);
            case "lag_bubble":
                return new AnimatedEnemy(Color.parseColor("#BEE7FF"), Color.parseColor("#64B5F6"), 7f, 0.9f, 0.6f, false);
            case "spam_drone":
                return AnimatedEnemy.forBitmap(getSpamDroneBitmap(context), 6f, 1.6f, true);
            case "port_plant":
                return new AnimatedEnemy(Color.parseColor("#4CAF50"), Color.parseColor("#C8E6C9"), 3f, 1.2f, 0.45f, false);
            case "compile_crusher":
                return new AnimatedEnemy(Color.parseColor("#F44336"), Color.parseColor("#FFCDD2"), 5f, 1.5f, 0.4f, true);
            case "phishing_siren":
                return new AnimatedEnemy(Color.parseColor("#64B5F6"), Color.parseColor("#E3F2FD"), 6f, 1.7f, 0.4f, false);
            default:
                return new AnimatedEnemy(Color.parseColor("#9FA8DA"), Color.parseColor("#FFFFFF"), 3f, 1.2f, 0.4f, true);
        }
    }

    @NonNull
    private static synchronized Bitmap getSpamDroneBitmap(@NonNull Context context) {
        if (spamDroneBitmap == null || spamDroneBitmap.isRecycled()) {
            spamDroneBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.spam_drone);
        }
        return spamDroneBitmap;
    }
}
