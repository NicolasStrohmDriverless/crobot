// app/src/main/java/com/example/robotparkour/audio/GameAudioManager.java
package com.example.robotparkour.audio;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;

import androidx.annotation.RawRes;

import com.example.robotparkour.R;

/**
 * Loads and plays the small sound palette for the game. SoundPool is used for
 * short effects while MediaPlayer handles the looping background track.
 */
public class GameAudioManager {

    private final Context context;

    private SoundPool soundPool;
    private MediaPlayer mediaPlayer;

    private int jumpSoundId;
    private int coinSoundId;
    private int errorSoundId;

    private boolean soundEnabled = true;
    private boolean musicEnabled = true;

    public GameAudioManager(Context context) {
        this.context = context.getApplicationContext();
        createSoundPool();
        loadEffects();
        prepareMediaPlayer();
    }

    private void createSoundPool() {
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setAudioAttributes(attributes)
                .setMaxStreams(4)
                .build();
    }

    private void loadEffects() {
        jumpSoundId = loadSound(R.raw.jump);
        coinSoundId = loadSound(R.raw.coin);
        errorSoundId = loadSound(R.raw.error);
    }

    private int loadSound(@RawRes int resId) {
        if (soundPool == null) {
            return -1;
        }
        return soundPool.load(context, resId, 1);
    }

    private void prepareMediaPlayer() {
        mediaPlayer = MediaPlayer.create(context, R.raw.background);
        if (mediaPlayer != null) {
            mediaPlayer.setLooping(true);
            mediaPlayer.setVolume(0.4f, 0.4f);
        }
    }

    public void playJump() {
        playEffect(jumpSoundId);
    }

    public void playCoin() {
        playEffect(coinSoundId);
    }

    public void playError() {
        playEffect(errorSoundId);
    }

    private void playEffect(int soundId) {
        if (!soundEnabled || soundPool == null || soundId <= 0) {
            return;
        }
        soundPool.play(soundId, 0.8f, 0.8f, 1, 0, 1f);
    }

    public void startMusic() {
        if (!musicEnabled || mediaPlayer == null) {
            return;
        }
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    public void stopMusic() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    public void onPause() {
        stopMusic();
    }

    public void onResume() {
        startMusic();
    }

    public void release() {
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    public void setSoundEnabled(boolean enabled) {
        soundEnabled = enabled;
    }

    public void setMusicEnabled(boolean enabled) {
        musicEnabled = enabled;
        if (musicEnabled) {
            startMusic();
        } else {
            stopMusic();
        }
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public boolean isMusicEnabled() {
        return musicEnabled;
    }
}
