package com.example.sdamgia.data;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.VibrationEffect;
import android.os.Vibrator;

import com.example.sdamgia.R;

import java.util.HashMap;
import java.util.Map;

public class SoundManager {

    private final SoundPool soundPool;
    private final Map<String, Integer> soundIds = new HashMap<>();
    private MediaPlayer musicPlayer;
    private int sleepStreamId = 0;
    private final Vibrator vibrator;
    private final Context context;

    private float currentMusicVolume = 0.5f;
    private boolean musicEnabled = true;
    private boolean soundEffectsEnabled = true;

    public SoundManager(Context context) {
        this.context = context;

        AudioAttributes attrs = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build();

        soundPool = new SoundPool.Builder()
            .setMaxStreams(6)
            .setAudioAttributes(attrs)
            .build();

        vibrator = context.getSystemService(Vibrator.class);

        loadSounds(context);
    }

    private void loadSounds(Context context) {
        Map<String, Integer> sounds = new HashMap<>();
        sounds.put("feed", R.raw.eat);
        sounds.put("play", R.raw.play);
        sounds.put("bath", R.raw.swim);
        sounds.put("wake_up", R.raw.miu);
        sounds.put("correct_answer", R.raw.correct);
        sounds.put("wrong_answer", R.raw.wrong);
        sounds.put("level_up", R.raw.correct);
        sounds.put("pet_die", R.raw.die);
        sounds.put("sleep", R.raw.sleep);

        for (Map.Entry<String, Integer> entry : sounds.entrySet()) {
            int id = soundPool.load(context, entry.getValue(), 1);
            soundIds.put(entry.getKey(), id);
        }
    }

    public void playSound(String name) {
        if (!soundEffectsEnabled) return;
        Integer id = soundIds.get(name);
        if (id == null) return;
        soundPool.play(id, 1f, 1f, 1, 0, 1f);
    }

    public void playSleepSound() {
        if (!soundEffectsEnabled) return;
        try {
            Integer id = soundIds.get("sleep");
            if (id == null) return;
            if (sleepStreamId != 0) {
                soundPool.stop(sleepStreamId);
                sleepStreamId = 0;
            }
            int streamId = soundPool.play(id, 0.7f, 0.7f, 1, -1, 1f);
            if (streamId > 0) sleepStreamId = streamId;
        } catch (Exception ignored) {}
    }

    public void stopSleepSound() {
        try {
            if (sleepStreamId != 0) {
                soundPool.stop(sleepStreamId);
                sleepStreamId = 0;
            }
        } catch (Exception ignored) {}
    }

    public void vibrate(long durationMs) {
        try {
            if (vibrator != null) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        } catch (Exception ignored) {}
    }

    public void loadMusic() {
        try {
            if (musicPlayer != null) {
                musicPlayer.release();
                musicPlayer = null;
            }
            musicPlayer = MediaPlayer.create(context, R.raw.fone);
            if (musicPlayer != null) {
                musicPlayer.setLooping(true);
                musicPlayer.setVolume(currentMusicVolume, currentMusicVolume);
            }
        } catch (Exception ignored) {
            musicPlayer = null;
        }
    }

    public void playMusic() {
        if (!musicEnabled) return;
        try {
            if (musicPlayer == null) loadMusic();
            if (musicPlayer != null && !musicPlayer.isPlaying()) {
                musicPlayer.start();
            }
        } catch (Exception ignored) {}
    }

    public void pauseMusic() {
        try {
            if (musicPlayer != null) musicPlayer.pause();
        } catch (Exception ignored) {}
    }

    public void stopMusic() {
        try {
            if (musicPlayer != null) {
                musicPlayer.stop();
                musicPlayer.release();
                musicPlayer = null;
            }
        } catch (Exception ignored) {}
    }

    public void setMusicVolume(float volume) {
        currentMusicVolume = Math.max(0f, Math.min(1f, volume));
        if (musicPlayer != null) {
            musicPlayer.setVolume(currentMusicVolume, currentMusicVolume);
        }
    }

    public boolean toggleMusic() {
        musicEnabled = !musicEnabled;
        if (musicEnabled) playMusic();
        else pauseMusic();
        return musicEnabled;
    }

    public boolean toggleSoundEffects() {
        soundEffectsEnabled = !soundEffectsEnabled;
        return soundEffectsEnabled;
    }

    public void release() {
        stopMusic();
        stopSleepSound();
        soundPool.release();
    }

    public boolean isSoundEffectsEnabled() { return soundEffectsEnabled; }
    public void setSoundEffectsEnabled(boolean enabled) { this.soundEffectsEnabled = enabled; }
    public boolean isMusicEnabled() { return musicEnabled; }
    public void setMusicEnabled(boolean enabled) { this.musicEnabled = enabled; }
    public float getCurrentMusicVolume() { return currentMusicVolume; }
}
