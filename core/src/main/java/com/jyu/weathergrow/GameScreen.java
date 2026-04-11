package com.jyu.weathergrow;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.Preferences;

/** Main game screen containing plant simulation logic and UI. */
public class GameScreen implements Screen {
    // Constants
    private static final String PREFS_NAME = "MyPlantGame";
    private static final float HEALTH_DECAY_PER_SECOND = 1.0f; // 1 health point per second
    private static final float WATER_HEALTH_AMOUNT = 20.0f; // Water restores 20 health
    private static final float HEALTH_MAX = 100.0f;
    private static final float HEALTH_DRY = 30.0f; // Below this, plant shows dry texture
    private static final float HEALTH_DEAD = 0.0f;

    // Virtual viewport size (portrait: 720x1280)
    private static final float VIEWPORT_WIDTH = 720f;
    private static final float VIEWPORT_HEIGHT = 1280f;

    // Game state
    private float health = HEALTH_MAX; // Start with 100 health
    private long lastSaveTimestamp = System.currentTimeMillis();
    private int waterCount = 0;
    private float survivalSeconds = 0.0f;
    private boolean isDead = false;
    private long lastPlayedTime = 0;

    // Graphics (temporary - will be moved to GameUI gradually)
    private OrthographicCamera camera;
    private Viewport viewport;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont fontLarge, fontMedium, fontSmall;
    private Skin skin;

    // Textures
    private Texture plantHealthy;
    private Texture plantDry;
    private Texture plantDead;
    private Texture currentPlantTexture;

    // Water effect is now handled by GameUI
    private Vector3 tapPosition = new Vector3();
    
    // Game timer
    private float saveTimer = 0f;

    // UI element positions (temporary)
    private float plantCenterX, plantCenterY;
    private float plantWidth, plantHeight;
    private float thirstBarX, thirstBarY, thirstBarWidth, thirstBarHeight;
    private float statsPanelY;
    private float newPlantButtonX, newPlantButtonY, newPlantButtonWidth, newPlantButtonHeight;

    // GameUI instance (will handle Scene2D UI)
    private GameUI gameUI;

    @Override
    public void show() {
        // Load preferences and calculate elapsed time
        loadGameState();
        updateHealthFromElapsedTime();

        // Initialize graphics with portrait viewport
        camera = new OrthographicCamera();
        viewport = new FitViewport(VIEWPORT_WIDTH, VIEWPORT_HEIGHT, camera);
        viewport.apply();

        camera.position.set(VIEWPORT_WIDTH / 2, VIEWPORT_HEIGHT / 2, 0);
        camera.update();

        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        // Load textures
        plantHealthy = new Texture(Gdx.files.internal("plants/plant_healthy.png"));
        plantDry = new Texture(Gdx.files.internal("plants/plant_dry.png"));
        plantDead = new Texture(Gdx.files.internal("plants/plant_dead.png"));

        // Store plant dimensions
        plantWidth = plantHealthy.getWidth();
        plantHeight = plantHealthy.getHeight();

        // Calculate UI positions for portrait layout
        calculateUIPositions();

        // Set initial plant texture
        updatePlantTexture();

        // Load skin and create fonts of different sizes
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        fontLarge = skin.getFont("default");
        fontMedium = skin.getFont("default");
        fontSmall = skin.getFont("default");

        // Scale fonts for better readability
        fontLarge.getData().setScale(1.5f);
        fontMedium.getData().setScale(1.0f);
        fontSmall.getData().setScale(0.8f);

        // Initialize GameUI (Scene2D UI)
        gameUI = new GameUI(this, skin, viewport);
        
        // Set GameUI stage as input processor
        Gdx.input.setInputProcessor(gameUI.getStage());
    }

    /** Calculate UI element positions for portrait layout */
    private void calculateUIPositions() {
        // Plant position is calculated dynamically in render() based on scaling
        plantCenterX = VIEWPORT_WIDTH / 2;
        plantCenterY = VIEWPORT_HEIGHT / 2;

        // Old UI elements removed - keeping for compatibility
        thirstBarWidth = VIEWPORT_WIDTH * 0.8f;
        thirstBarHeight = 40f;
        thirstBarX = (VIEWPORT_WIDTH - thirstBarWidth) / 2;
        thirstBarY = VIEWPORT_HEIGHT * 0.45f;

        statsPanelY = VIEWPORT_HEIGHT * 0.25f;

        newPlantButtonWidth = 250f;
        newPlantButtonHeight = 80f;
        newPlantButtonX = (VIEWPORT_WIDTH - newPlantButtonWidth) / 2;
        newPlantButtonY = VIEWPORT_HEIGHT * 0.35f;
    }

    /** Load saved game state from Preferences */
    void loadGameState() {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        health = prefs.getFloat("health", HEALTH_MAX); // Default to max health on first launch
        lastSaveTimestamp = prefs.getLong("lastSaveTimestamp", System.currentTimeMillis());
        waterCount = prefs.getInteger("waterCount", 0);
        survivalSeconds = prefs.getFloat("survivalSeconds", 0.0f);
        isDead = prefs.getBoolean("isDead", false);
        lastPlayedTime = prefs.getLong("lastPlayedTime", 0);

        // If first launch, initialize with current timestamp and max health
        if (lastSaveTimestamp == 0) {
            lastSaveTimestamp = System.currentTimeMillis();
            health = HEALTH_MAX;
            saveGameState();
        }
    }

    /** Save current game state to Preferences */
    void saveGameState() {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        prefs.putFloat("health", health);
        prefs.putLong("lastSaveTimestamp", lastSaveTimestamp);
        prefs.putInteger("waterCount", waterCount);
        prefs.putFloat("survivalSeconds", survivalSeconds);
        prefs.putBoolean("isDead", isDead);
        prefs.putLong("lastPlayedTime", System.currentTimeMillis());
        prefs.flush();
    }

    /** Calculate elapsed time since last save and update health (1 point per second). */
    void updateHealthFromElapsedTime() {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        long savedTime = prefs.getLong("lastSaveTimestamp", System.currentTimeMillis());
        float savedHealth = prefs.getFloat("health", HEALTH_MAX);
        boolean savedIsDead = prefs.getBoolean("isDead", false);

        // If already dead, no further updates
        if (savedIsDead) {
            isDead = true;
            health = HEALTH_DEAD;
            updatePlantTexture();
            return;
        }

        long currentTime = System.currentTimeMillis();
        long secondsPassed = (currentTime - savedTime) / 1000;

        if (secondsPassed > 0) {
            // Decrease health based on elapsed time (1 point per second)
            float healthDecrease = secondsPassed * HEALTH_DECAY_PER_SECOND;
            savedHealth = Math.max(savedHealth - healthDecrease, HEALTH_DEAD);

            // Update survival time
            survivalSeconds = prefs.getFloat("survivalSeconds", 0.0f) + secondsPassed;

            // Check for death
            if (savedHealth <= HEALTH_DEAD) {
                isDead = true;
                savedHealth = HEALTH_DEAD;
            }

            // Update local state
            health = savedHealth;
            lastSaveTimestamp = currentTime;

            // Save updated state
            prefs.putFloat("health", savedHealth);
            prefs.putLong("lastSaveTimestamp", currentTime);
            prefs.putFloat("survivalSeconds", survivalSeconds);
            prefs.putBoolean("isDead", isDead);
            prefs.flush();
        }

        updatePlantTexture();
    }

    /** Update plant texture based on health and death state */
    void updatePlantTexture() {
        if (isDead || health <= HEALTH_DEAD) {
            currentPlantTexture = plantDead;
            isDead = true;
        } else if (health <= HEALTH_DRY) {
            currentPlantTexture = plantDry;
        } else {
            currentPlantTexture = plantHealthy;
        }
    }

    /** Water the plant (called on tap) */
    void waterPlant() {
        if (isDead) return;

        health = Math.min(health + WATER_HEALTH_AMOUNT, HEALTH_MAX);
        waterCount++;
        updatePlantTexture();

        // Update timestamp to now
        lastSaveTimestamp = System.currentTimeMillis();
        saveGameState();
    }

    /** Reset game to start new plant */
    void resetGame() {
        health = HEALTH_MAX;
        lastSaveTimestamp = System.currentTimeMillis();
        waterCount = 0;
        survivalSeconds = 0.0f;
        isDead = false;
        saveTimer = 0f;
        updatePlantTexture();
        saveGameState();
    }

    // Getters for GameUI to access game state
    public float getHealth() { return health; }
    public float getHealthMax() { return HEALTH_MAX; }
    public int getWaterCount() { return waterCount; }
    public float getSurvivalSeconds() { return survivalSeconds; }
    public boolean isPlantDead() { return isDead; }
    public long getLastPlayedTime() { return lastPlayedTime; }
    public float getViewportWidth() { return VIEWPORT_WIDTH; }
    public float getViewportHeight() { return VIEWPORT_HEIGHT; }

    @Override
    public void render(float delta) {
        // Water effect is now handled by GameUI
        // No need to update water effect timer here
        
        // Update health in real-time (1 point per second)
        if (!isDead) {
            health -= HEALTH_DECAY_PER_SECOND * delta;
            
            // Check for death
            if (health <= HEALTH_DEAD) {
                health = HEALTH_DEAD;
                isDead = true;
                updatePlantTexture();
                saveGameState();
            }
            
            // Update plant texture if health crosses threshold
            if ((health <= HEALTH_DRY && currentPlantTexture != plantDry) || 
                (health > HEALTH_DRY && currentPlantTexture != plantHealthy && !isDead)) {
                updatePlantTexture();
            }
            
            // Update survival time
            survivalSeconds += delta;
            
            // Save game state periodically (every 10 seconds)
            saveTimer += delta;
            if (saveTimer >= 10f) {
                lastSaveTimestamp = System.currentTimeMillis();
                saveGameState();
                saveTimer = 0f;
            }
        }

        // Clear screen with white background
        Gdx.gl.glClearColor(1f, 1f, 1f, 1); // White background
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Update camera and set up rendering
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);

        // Draw white background for plant area
        drawPlantBackground();

        // Draw plant cropped and scaled to fit screen
        batch.begin();
        
        // Calculate available space for plant (screen height minus bottom bar)
        float availableHeight = VIEWPORT_HEIGHT - 180f; // Bottom bar is 180px
        
        // Make plant much smaller - target 60% of available width
        float targetWidth = VIEWPORT_WIDTH * 0.6f;
        float scale = targetWidth / plantWidth;
        
        // Apply the scale
        float scaledWidth = plantWidth * scale;
        float scaledHeight = plantHeight * scale;
        
        // If scaled height is still too tall, scale down further
        if (scaledHeight > availableHeight * 0.8f) {
            scale = (availableHeight * 0.8f) / plantHeight;
            scaledWidth = plantWidth * scale;
            scaledHeight = plantHeight * scale;
        }
        
        // Center plant horizontally and vertically (above bottom bar)
        float plantX = (VIEWPORT_WIDTH - scaledWidth) / 2;
        float plantY = (VIEWPORT_HEIGHT - scaledHeight - 180f) / 2 + 180f; // Center in space above bottom bar
        
        // No sway animation - plant stays perfectly still
        // Draw the plant with calculated cropping/scaling
        batch.draw(currentPlantTexture, plantX, plantY, scaledWidth, scaledHeight);

        // Water effect is now handled by GameUI with proper water droplet icon
        // No need to draw plant texture with blue tint
        batch.end();

        // Render Scene2D UI (handles all UI elements)
        gameUI.render(delta);
    }

    @Override
    public void resize(int width, int height) {
        if (width <= 0 || height <= 0) return;
        viewport.update(width, height);
        camera.position.set(VIEWPORT_WIDTH / 2, VIEWPORT_HEIGHT / 2, 0);
        camera.update();
        gameUI.resize(width, height);
    }

    @Override
    public void pause() {
        // Save game state when app is paused (minimized, etc.)
        saveGameState();
    }

    @Override
    public void resume() {
        // When app resumes, load saved state and calculate elapsed time
        loadGameState();
        updateHealthFromElapsedTime();
    }

    @Override
    public void hide() {
        // Screen is hidden (e.g., another screen is shown)
    }

    @Override
    public void dispose() {
        // Clean up resources
        batch.dispose();
        shapeRenderer.dispose();
        fontLarge.dispose();
        fontMedium.dispose();
        fontSmall.dispose();
        skin.dispose();
        plantHealthy.dispose();
        plantDry.dispose();
        plantDead.dispose();
        gameUI.dispose();
    }

    /** Draw white background for plant area */
    private void drawPlantBackground() {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        // White background for entire screen
        shapeRenderer.setColor(1f, 1f, 1f, 1);
        shapeRenderer.rect(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT);
        
        shapeRenderer.end();
    }
}