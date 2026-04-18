package com.jyu.weathergrow;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.audio.Music;

public class MusicManager {
    private static final String PREFS_NAME = "MusicPrefs";
    private static final String KEY_MUSIC_ENABLED = "musicEnabled";
    private static final String KEY_MUSIC_VOLUME = "musicVolume";
    
    private Music backgroundMusic;
    private Preferences prefs;
    
    private boolean isEnabled = true;
    private float volume = 0.5f; // Default volume 50%
    
    public MusicManager() {
        prefs = Gdx.app.getPreferences(PREFS_NAME);
        loadSettings();
        loadMusic();
    }
    
    private void loadSettings() {
        isEnabled = prefs.getBoolean(KEY_MUSIC_ENABLED, true);
        volume = prefs.getFloat(KEY_MUSIC_VOLUME, 0.5f);
        
        // Clamp volume to valid range
        volume = Math.max(0f, Math.min(1f, volume));
        
        System.out.println("MusicManager: Loaded settings - enabled: " + isEnabled + ", volume: " + volume);
    }
    
    private void loadMusic() {
        try {
            backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("music/music.mp3"));
            if (backgroundMusic != null) {
                backgroundMusic.setLooping(true);
                backgroundMusic.setVolume(volume);
                
                if (isEnabled) {
                    backgroundMusic.play();
                    System.out.println("MusicManager: Music loaded and playing");
                } else {
                    System.out.println("MusicManager: Music loaded but disabled");
                }
            } else {
                System.err.println("MusicManager: Failed to load music - null returned");
            }
        } catch (Exception e) {
            System.err.println("MusicManager: Failed to load music file: " + e.getMessage());
        }
    }
    
    public void update(float delta) {
        // Update music volume if needed
        if (backgroundMusic != null) {
            backgroundMusic.setVolume(volume);
        }
    }
    
    public void play() {
        if (backgroundMusic != null && !backgroundMusic.isPlaying()) {
            backgroundMusic.play();
            isEnabled = true;
            saveSettings();
            System.out.println("MusicManager: Music started");
        }
    }
    
    public void pause() {
        if (backgroundMusic != null && backgroundMusic.isPlaying()) {
            backgroundMusic.pause();
            isEnabled = false;
            saveSettings();
            System.out.println("MusicManager: Music paused");
        }
    }
    
    public void stop() {
        if (backgroundMusic != null) {
            backgroundMusic.stop();
            isEnabled = false;
            saveSettings();
            System.out.println("MusicManager: Music stopped");
        }
    }
    
    public void toggle() {
        if (isEnabled) {
            pause();
        } else {
            play();
        }
    }
    
    public void setVolume(float newVolume) {
        // Clamp volume to valid range
        volume = Math.max(0f, Math.min(1f, newVolume));
        
        if (backgroundMusic != null) {
            backgroundMusic.setVolume(volume);
        }
        
        saveSettings();
        System.out.println("MusicManager: Volume set to " + volume);
    }
    
    public float getVolume() {
        return volume;
    }
    
    public boolean isEnabled() {
        return isEnabled;
    }
    
    public boolean isPlaying() {
        return backgroundMusic != null && backgroundMusic.isPlaying();
    }
    
    private void saveSettings() {
        prefs.putBoolean(KEY_MUSIC_ENABLED, isEnabled);
        prefs.putFloat(KEY_MUSIC_VOLUME, volume);
        prefs.flush();
    }
    
    public void dispose() {
        if (backgroundMusic != null) {
            backgroundMusic.stop();
            backgroundMusic.dispose();
            System.out.println("MusicManager: Music disposed");
        }
    }
}