package com.jyu.weathergrow;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class WeatherTest extends ApplicationAdapter {
    private SpriteBatch batch;
    private WeatherManager weatherManager;
    
    @Override
    public void create() {
        batch = new SpriteBatch();
        weatherManager = new WeatherManager();
        
        // Test: Print initial weather state
        System.out.println("Initial weather: " + weatherManager.getCurrentWeather());
        System.out.println("Is transitioning: " + weatherManager.isTransitioning());
    }
    
    @Override
    public void render() {
        // Update weather
        weatherManager.update(Gdx.graphics.getDeltaTime());
        
        // Clear screen
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        // Render weather
        batch.begin();
        weatherManager.render(batch);
        batch.end();
        
        // Occasionally print weather state (every 60 frames)
        if (Gdx.graphics.getFrameId() % 60 == 0) {
            System.out.println("Current weather: " + weatherManager.getCurrentWeather() + 
                             ", Transitioning: " + weatherManager.isTransitioning());
        }
    }
    
    @Override
    public void dispose() {
        batch.dispose();
        weatherManager.dispose();
    }
}