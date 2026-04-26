package com.jyu.weathergrow;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.TimeUtils;

public class WeatherManager {
    public enum WeatherState {
        SUNNY,
        OVERCAST,
        RAINY
    }
    
    private static final String PREFS_NAME = "WeatherPrefs";
    private static final String KEY_LAST_WEATHER_CHANGE = "lastWeatherChange";
    private static final String KEY_CURRENT_WEATHER = "currentWeather";
    private static final long WEATHER_CHANGE_INTERVAL = 10000; // 10 seconds in milliseconds
    
    // Weighted probabilities: sunny 50%, overcast 30%, rainy 20%
    private static final float[] WEATHER_WEIGHTS = {0.5f, 0.3f, 0.2f};
    
    private WeatherState currentWeather;
    private long lastWeatherChangeTime;
    private Preferences prefs;
    
    // Textures for backgrounds
    private Texture sunnyTexture;
    private Texture overcastTexture;
    private Texture rainyTexture;
    private Texture currentBackground;
    
    // Transition variables
    private boolean isTransitioning = false;
    private float transitionAlpha = 0f;
    private float transitionDuration = 2f; // 2 second transition
    private float transitionTimer = 0f;
    private WeatherState targetWeather;
    private Texture nextBackground;
    
    public WeatherManager() {
        System.out.println("Initializing WeatherManager...");
        prefs = Gdx.app.getPreferences(PREFS_NAME);
        loadWeatherState();
        loadTextures();
        System.out.println("WeatherManager initialized. Current weather: " + currentWeather);
    }
    
    private void loadWeatherState() {
        // Load last weather change time
        lastWeatherChangeTime = prefs.getLong(KEY_LAST_WEATHER_CHANGE, TimeUtils.millis());
        
        // Load current weather state
        String weatherStr = prefs.getString(KEY_CURRENT_WEATHER, WeatherState.SUNNY.toString());
        try {
            currentWeather = WeatherState.valueOf(weatherStr);
        } catch (IllegalArgumentException e) {
            currentWeather = WeatherState.SUNNY;
        }
        
        // Check if enough time has passed for a weather change
        checkForWeatherChange();
    }
    
    private void loadTextures() {
        // Try to load textures, create placeholders if they fail
        sunnyTexture = loadTexture("backgrounds/sunny.png", Color.YELLOW);
        overcastTexture = loadTexture("backgrounds/overcast.png", Color.GRAY);
        rainyTexture = loadTexture("backgrounds/rainy.png", Color.BLUE);
        
        // Set current background based on weather state
        switch (currentWeather) {
            case SUNNY:
                currentBackground = sunnyTexture;
                break;
            case OVERCAST:
                currentBackground = overcastTexture;
                break;
            case RAINY:
                currentBackground = rainyTexture;
                break;
        }
        
        System.out.println("Current background set to: " + currentBackground + " for weather: " + currentWeather);
    }
    
    private Texture loadTexture(String path, Color fallbackColor) {
        try {
            Texture texture = new Texture(Gdx.files.internal(path));
            System.out.println("Successfully loaded texture: " + path + " - " + texture);
            return texture;
        } catch (Exception e) {
            System.err.println("Failed to load texture: " + path + " - " + e.getMessage());
            System.err.println("Creating fallback texture with color: " + fallbackColor);
            
            // Create a simple colored texture as fallback
            return createColorTexture(fallbackColor);
        }
    }
    
    private Texture createColorTexture(Color color) {
        // Create a 1x1 pixel texture with the specified color
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }
    
    private void checkForWeatherChange() {
        long currentTime = TimeUtils.millis();
        long timeSinceLastChange = currentTime - lastWeatherChangeTime;
        
        if (timeSinceLastChange >= WEATHER_CHANGE_INTERVAL) {
            // Calculate how many weather changes should have occurred
            int changes = (int)(timeSinceLastChange / WEATHER_CHANGE_INTERVAL);
            
            // Apply all weather changes that should have happened
            for (int i = 0; i < changes; i++) {
                WeatherState newWeather = getRandomWeather();
                
                // Only start transition if this is the last change
                if (i == changes - 1) {
                    startWeatherTransition(newWeather);
                } else {
                    currentWeather = newWeather;
                }
                
                lastWeatherChangeTime += WEATHER_CHANGE_INTERVAL;
            }
            
            saveWeatherState();
        }
    }
    
    private WeatherState getRandomWeather() {
        float random = MathUtils.random();
        float cumulative = 0f;
        
        for (int i = 0; i < WeatherState.values().length; i++) {
            cumulative += WEATHER_WEIGHTS[i];
            if (random <= cumulative) {
                return WeatherState.values()[i];
            }
        }
        
        return WeatherState.SUNNY; // Fallback
    }
    
    private void startWeatherTransition(WeatherState newWeather) {
        if (newWeather == currentWeather) {
            return; // No transition needed if weather stays the same
        }
        
        targetWeather = newWeather;
        isTransitioning = true;
        transitionTimer = 0f;
        transitionAlpha = 0f;
        
        // Set next background texture
        switch (targetWeather) {
            case SUNNY:
                nextBackground = sunnyTexture;
                break;
            case OVERCAST:
                nextBackground = overcastTexture;
                break;
            case RAINY:
                nextBackground = rainyTexture;
                break;
        }
    }
    
    public void update(float deltaTime) {
        checkForWeatherChange();
        
        if (isTransitioning) {
            transitionTimer += deltaTime;
            transitionAlpha = Math.min(transitionTimer / transitionDuration, 1f);
            
            if (transitionTimer >= transitionDuration) {
                // Transition complete
                isTransitioning = false;
                currentWeather = targetWeather;
                currentBackground = nextBackground;
                saveWeatherState();
            }
        }
    }
    
    public void render(SpriteBatch batch) {
        if (currentBackground != null) {
            batch.setColor(1, 1, 1, 1);
            // Draw the texture stretched to fill the screen
            batch.draw(currentBackground, 0, 0, 720, 1280);
            
            // Debug: log when rendering (less frequent to avoid spam)
            if (Gdx.graphics.getFrameId() % 300 == 0) { // Every 5 seconds at 60fps
                System.out.println("Rendering weather background: " + currentWeather + 
                                 ", texture size: " + currentBackground.getWidth() + "x" + currentBackground.getHeight());
            }
        } else {
            // Debug: log when no background
            if (Gdx.graphics.getFrameId() % 300 == 0) {
                System.err.println("No current background texture! Weather: " + currentWeather);
            }
        }
        
        // Draw transition overlay if transitioning
        if (isTransitioning && nextBackground != null) {
            batch.setColor(1, 1, 1, transitionAlpha);
            batch.draw(nextBackground, 0, 0, 720, 1280);
            batch.setColor(1, 1, 1, 1);
        }
    }
    
    private void saveWeatherState() {
        prefs.putLong(KEY_LAST_WEATHER_CHANGE, lastWeatherChangeTime);
        prefs.putString(KEY_CURRENT_WEATHER, currentWeather.toString());
        prefs.flush();
    }
    
    public WeatherState getCurrentWeather() {
        return currentWeather;
    }

    public float getHealthDrainMultiplier() {
        switch (currentWeather) {
            case SUNNY:
                return 2.0f;
            case OVERCAST:
                return 1.0f;
            case RAINY:
                return -0.5f;
            default:
                return 1.0f;
        }
    }
    
    public boolean isTransitioning() {
        return isTransitioning;
    }
    
    public void dispose() {
        if (sunnyTexture != null) sunnyTexture.dispose();
        if (overcastTexture != null) overcastTexture.dispose();
        if (rainyTexture != null) rainyTexture.dispose();
    }
}