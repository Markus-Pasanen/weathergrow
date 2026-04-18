package com.jyu.weathergrow;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.audio.Sound;

public class SoundManager {
    private static final String PREFS_NAME = "SoundPrefs";
    private static final String KEY_SOUND_ENABLED = "soundEnabled";
    private static final String KEY_SOUND_VOLUME = "soundVolume";
    
    private Sound splashSound;
    private Preferences prefs;
    
    private boolean isEnabled = true;
    private float volume = 0.7f; // Default volume 70% for sound effects
    
    public SoundManager() {
        prefs = Gdx.app.getPreferences(PREFS_NAME);
        loadSettings();
        loadSounds();
    }
    
    private void loadSettings() {
        isEnabled = prefs.getBoolean(KEY_SOUND_ENABLED, true);
        volume = prefs.getFloat(KEY_SOUND_VOLUME, 0.7f);
        
        // Clamp volume to valid range
        volume = Math.max(0f, Math.min(1f, volume));
        
        System.out.println("SoundManager: Loaded settings - enabled: " + isEnabled + ", volume: " + volume);
    }
    
    private void loadSounds() {
        try {
            splashSound = Gdx.audio.newSound(Gdx.files.internal("music/splash.mp3"));
            System.out.println("SoundManager: Splash sound loaded successfully");
        } catch (Exception e) {
            System.err.println("SoundManager: Failed to load splash sound: " + e.getMessage());
        }
    }
    
    public void playSplash() {
        if (isEnabled && splashSound != null) {
            long soundId = splashSound.play(volume);
            System.out.println("SoundManager: Playing splash sound (id: " + soundId + ")");
        } else if (!isEnabled) {
            System.out.println("SoundManager: Sound effects disabled, not playing splash");
        } else {
            System.err.println("SoundManager: Cannot play splash - sound not loaded");
        }
    }
    
    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
        saveSettings();
        System.out.println("SoundManager: Sound effects " + (enabled ? "enabled" : "disabled"));
    }
    
    public void toggle() {
        setEnabled(!isEnabled);
    }
    
    public void setVolume(float newVolume) {
        // Clamp volume to valid range
        volume = Math.max(0f, Math.min(1f, newVolume));
        saveSettings();
        System.out.println("SoundManager: Volume set to " + volume);
    }
    
    public float getVolume() {
        return volume;
    }
    
    public boolean isEnabled() {
        return isEnabled;
    }
    
    private void saveSettings() {
        prefs.putBoolean(KEY_SOUND_ENABLED, isEnabled);
        prefs.putFloat(KEY_SOUND_VOLUME, volume);
        prefs.flush();
    }
    
    public void dispose() {
        if (splashSound != null) {
            splashSound.dispose();
            System.out.println("SoundManager: Sounds disposed");
        }
    }
}