package com.jyu.weathergrow;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
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
    
    // Health bar constants
    private static final float HEALTH_BAR_WIDTH = 500f;
    private static final float HEALTH_BAR_HEIGHT = 50f;
    private static final float HEALTH_BAR_Y = VIEWPORT_HEIGHT - 120f;
    private static final float HEALTH_BAR_PADDING = 15f;
    private static final float HEALTH_ICON_SIZE = 160f; // 2x bigger health icon (80 * 2 = 160)
    private static final float HEALTH_BAR_CORNER_RADIUS = 25f;
    private static final float HEALTH_ICON_OVERLAP = 0f; // No overlap - absolute positioning

    // Game state
    private float health = HEALTH_MAX; // Start with 100 health
    private long lastSaveTimestamp = System.currentTimeMillis();
    private int waterCount = 0;
    private float survivalSeconds = 0.0f;
    private boolean isDead = false;
    private long lastPlayedTime = 0;

    // Graphics
    private OrthographicCamera camera;
    private Viewport viewport;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private Skin skin;
    private BitmapFont fontLarge, fontMedium, fontSmall;
    
    // Weather system
    private WeatherManager weatherManager;
    
    // Music system
    private MusicManager musicManager;
    
    // Sound effects system
    private SoundManager soundManager;

    // Plant textures - direct mapping to health levels
    private Texture plantTexture0;   // 0 health (dead)
    private Texture plantTexture10;  // 10 health
    private Texture plantTexture20;  // 20 health
    private Texture plantTexture40;  // 40 health
    private Texture plantTexture60;  // 60 health
    private Texture plantTexture80;  // 80 health
    private Texture plantTexture100; // 100 health (full)
    private Texture currentPlantTexture;
    
    // UI textures
    private Texture healthIconTexture;

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

        // Load plant textures directly
        plantTexture0 = loadTexture("plants/plant_0.png");
        plantTexture10 = loadTexture("plants/plant_10.png");
        plantTexture20 = loadTexture("plants/plant_20.png");
        plantTexture40 = loadTexture("plants/plant_40.png");
        plantTexture60 = loadTexture("plants/plant_60.png");
        plantTexture80 = loadTexture("plants/plant_80.png");
        plantTexture100 = loadTexture("plants/plant_100.png");
        
        // Load health icon texture
        healthIconTexture = loadTexture("ui/Icons/health.png");

        // Store plant dimensions (use 100 health texture as reference)
        plantWidth = plantTexture100.getWidth();
        plantHeight = plantTexture100.getHeight();

        // Calculate UI positions for portrait layout
        calculateUIPositions();

        // Set initial plant texture
        updatePlantTexture();
        
        // Safety check: ensure currentPlantTexture is not null
        if (currentPlantTexture == null) {
            currentPlantTexture = plantTexture100; // Use full health plant as default
        }

        // Load skin and create fonts of different sizes
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        fontLarge = skin.getFont("default");
        fontMedium = skin.getFont("default");
        fontSmall = skin.getFont("default");

        // Scale fonts for better readability
        fontLarge.getData().setScale(1.5f);
        fontMedium.getData().setScale(1.0f);
        fontSmall.getData().setScale(0.8f);
        
        // Initialize weather system
        System.out.println("GameScreen: Initializing WeatherManager...");
        weatherManager = new WeatherManager();
        System.out.println("GameScreen: WeatherManager initialized.");
        
        // Initialize music system
        System.out.println("GameScreen: Initializing MusicManager...");
        musicManager = new MusicManager();
        System.out.println("GameScreen: MusicManager initialized.");
        
        // Initialize sound effects system
        System.out.println("GameScreen: Initializing SoundManager...");
        soundManager = new SoundManager();
        System.out.println("GameScreen: SoundManager initialized.");

        // Initialize GameUI (Scene2D UI)
        gameUI = new GameUI(this, skin, viewport);
        
        // Set GameUI stage as input processor
        Gdx.input.setInputProcessor(gameUI.getStage());
    }
    
    /** Helper method to load texture with error handling */
    private Texture loadTexture(String filename) {
        try {
            Texture texture = new Texture(Gdx.files.internal(filename));
            Gdx.app.log("GameScreen", "Loaded texture: " + filename);
            return texture;
        } catch (Exception e) {
            Gdx.app.error("GameScreen", "Failed to load texture: " + filename, e);
            // Create a placeholder texture
            Pixmap pixmap = new Pixmap(100, 100, Pixmap.Format.RGBA8888);
            pixmap.setColor(0.5f, 0.5f, 0.5f, 1f);
            pixmap.fill();
            Texture placeholder = new Texture(pixmap);
            pixmap.dispose();
            return placeholder;
        }
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
            // Plant is dead - use plant_0.png
            currentPlantTexture = plantTexture0;
            isDead = true;
        } else if (health >= 80) {
            currentPlantTexture = plantTexture100;
        } else if (health >= 60) {
            currentPlantTexture = plantTexture80;
        } else if (health >= 40) {
            currentPlantTexture = plantTexture60;
        } else if (health >= 20) {
            currentPlantTexture = plantTexture40;
        } else if (health >= 10) {
            currentPlantTexture = plantTexture20;
        } else {
            currentPlantTexture = plantTexture10;
        }
    }

    /** Water the plant (called on tap) */
    void waterPlant() {
        if (isDead) return;

        health = Math.min(health + WATER_HEALTH_AMOUNT, HEALTH_MAX);
        waterCount++;
        updatePlantTexture();

        // Play splash sound effect
        if (soundManager != null) {
            soundManager.playSplash();
        }

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
            
            // Update plant texture if health has changed
            // The updatePlantTexture() method checks if we need a new texture
            updatePlantTexture();
            
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
        
        // Update weather system
        weatherManager.update(delta);
        
        // Update music system
        musicManager.update(delta);

        // Clear screen
        Gdx.gl.glClearColor(0f, 0f, 0f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Update camera and set up rendering
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);

        // Draw weather background
        batch.begin();
        weatherManager.render(batch);
        batch.end();

        // Draw health bar at top of screen (using ShapeRenderer)
        drawHealthBar();
        
        // Draw health icon on top of health bar
        batch.begin();
        drawHealthIcon(batch);
        batch.end();
        
        // Draw plant cropped and scaled to fit screen
        batch.begin();
        
        // Calculate available space for plant (screen height minus bottom bar)
        float availableHeight = VIEWPORT_HEIGHT - 180f; // Bottom bar is 180px
        
        // Make plant 50% bigger - target 90% of screen width (was 60%)
        float targetWidth = VIEWPORT_WIDTH * 0.9f;
        float scale = targetWidth / plantWidth;
        
        // Apply the scale
        float scaledWidth = plantWidth * scale;
        float scaledHeight = plantHeight * scale;
        
        // If scaled height is still too tall, scale down further
        if (scaledHeight > availableHeight * 0.9f) {
            scale = (availableHeight * 0.9f) / plantHeight;
            scaledWidth = plantWidth * scale;
            scaledHeight = plantHeight * scale;
        }
        
        // Center plant horizontally, position at very bottom
        float plantX = (VIEWPORT_WIDTH - scaledWidth) / 2;
        float plantY = 0f; // Position at absolute bottom of screen
        
        // No sway animation - plant stays perfectly still
        // Draw the plant with calculated cropping/scaling
        batch.draw(currentPlantTexture, plantX, plantY, scaledWidth, scaledHeight);
        
        // Draw health bar text
        drawHealthBarText(batch);
        
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

    // Music control methods for UI
    public void toggleMusic() {
        if (musicManager != null) {
            musicManager.toggle();
        }
    }
    
    public boolean isMusicPlaying() {
        return musicManager != null && musicManager.isPlaying();
    }
    
    public float getMusicVolume() {
        return musicManager != null ? musicManager.getVolume() : 0.5f;
    }
    
    public void setMusicVolume(float volume) {
        if (musicManager != null) {
            musicManager.setVolume(volume);
        }
    }
    
    // Sound effects control methods for UI
    public void toggleSound() {
        if (soundManager != null) {
            soundManager.toggle();
        }
    }
    
    public boolean isSoundEnabled() {
        return soundManager != null && soundManager.isEnabled();
    }
    
    public float getSoundVolume() {
        return soundManager != null ? soundManager.getVolume() : 0.7f;
    }
    
    public void setSoundVolume(float volume) {
        if (soundManager != null) {
            soundManager.setVolume(volume);
        }
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
        
        // Dispose all plant textures
        if (plantTexture0 != null) plantTexture0.dispose();
        if (plantTexture10 != null) plantTexture10.dispose();
        if (plantTexture20 != null) plantTexture20.dispose();
        if (plantTexture40 != null) plantTexture40.dispose();
        if (plantTexture60 != null) plantTexture60.dispose();
        if (plantTexture80 != null) plantTexture80.dispose();
        if (plantTexture100 != null) plantTexture100.dispose();
        
        // Dispose health icon texture
        if (healthIconTexture != null) healthIconTexture.dispose();
        
        gameUI.dispose();
        
        // Dispose weather system
        if (weatherManager != null) {
            weatherManager.dispose();
        }
        
        // Dispose music system
        if (musicManager != null) {
            musicManager.dispose();
        }
        
        // Dispose sound effects system
        if (soundManager != null) {
            soundManager.dispose();
        }
    }

    /** Draw health bar shapes at top of screen */
    private void drawHealthBar() {
        // Calculate health percentage
        float healthPercent = health / HEALTH_MAX;
        
        // Calculate health bar position (centered)
        float healthBarX = (VIEWPORT_WIDTH - HEALTH_BAR_WIDTH) / 2;
        
        // Draw health bar background (dark gray) with rounded corners
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        // Draw rounded rectangle background
        drawRoundedRect(shapeRenderer, 
                       healthBarX - HEALTH_BAR_PADDING, 
                       HEALTH_BAR_Y - HEALTH_BAR_PADDING, 
                       HEALTH_BAR_WIDTH + HEALTH_BAR_PADDING * 2, 
                       HEALTH_BAR_HEIGHT + HEALTH_BAR_PADDING * 2,
                       HEALTH_BAR_CORNER_RADIUS,
                       new Color(0.1f, 0.1f, 0.1f, 0.8f));
        
        // Health bar fill with gradient colors based on health
        Color barColor;
        if (healthPercent > 0.7f) {
            // Green for healthy (70-100%)
            barColor = new Color(0.2f, 0.8f, 0.2f, 1);
        } else if (healthPercent > 0.3f) {
            // Yellow/orange for dry (30-70%)
            barColor = new Color(1.0f, 0.8f, 0.2f, 1);
        } else if (health > 0) {
            // Red for critical (0-30%)
            barColor = new Color(0.8f, 0.2f, 0.2f, 1);
        } else {
            // Dark red for dead
            barColor = new Color(0.4f, 0.1f, 0.1f, 1);
        }
        
        // Draw health fill with fully rounded corners
        float fillWidth = healthPercent * HEALTH_BAR_WIDTH;
        if (fillWidth > 0) {
            // Ensure minimum width to show rounded corners
            float actualFillWidth = Math.max(fillWidth, HEALTH_BAR_CORNER_RADIUS * 2);
            drawRoundedRect(shapeRenderer, 
                           healthBarX, 
                           HEALTH_BAR_Y, 
                           actualFillWidth, 
                           HEALTH_BAR_HEIGHT,
                           HEALTH_BAR_CORNER_RADIUS,
                           barColor);
        }
        
        shapeRenderer.end();
    }
    
    /** Helper method to draw a rounded rectangle */
    private void drawRoundedRect(ShapeRenderer shapeRenderer, float x, float y, float width, float height, float radius, Color color) {
        shapeRenderer.setColor(color);
        
        // Draw central rectangle
        shapeRenderer.rect(x + radius, y, width - 2 * radius, height);
        
        // Draw top and bottom rectangles
        shapeRenderer.rect(x, y + radius, width, height - 2 * radius);
        
        // Draw four corner circles
        shapeRenderer.circle(x + radius, y + radius, radius);
        shapeRenderer.circle(x + width - radius, y + radius, radius);
        shapeRenderer.circle(x + radius, y + height - radius, radius);
        shapeRenderer.circle(x + width - radius, y + height - radius, radius);
    }
    
    /** Draw health bar text */
    private void drawHealthBarText(SpriteBatch batch) {
        // Calculate health percentage
        float healthPercent = health / HEALTH_MAX;
        
        // Calculate health bar position (centered)
        float healthBarX = (VIEWPORT_WIDTH - HEALTH_BAR_WIDTH) / 2;
        
        // Health percentage text
        String healthText;
        if (isDead) {
            healthText = "DEAD";
            fontMedium.setColor(Color.RED);
        } else {
            healthText = String.format("%.0f%%", health);
            fontMedium.setColor(Color.WHITE);
        }
        
        // Center text in health bar
        GlyphLayout layout = new GlyphLayout(fontMedium, healthText);
        float textX = healthBarX + (HEALTH_BAR_WIDTH - layout.width) / 2;
        float textY = HEALTH_BAR_Y + HEALTH_BAR_HEIGHT / 2 + layout.height / 2;
        
        fontMedium.draw(batch, healthText, textX, textY);
        fontMedium.setColor(Color.WHITE);
    }
    
    /** Draw health icon on top of health bar start */
    private void drawHealthIcon(SpriteBatch batch) {
        // Absolute positioning with vertical alignment to health bar
        float iconX = 20f; // 20px from left edge
        // Align icon vertically with health bar (center of icon aligns with center of health bar)
        float iconY = HEALTH_BAR_Y + (HEALTH_BAR_HEIGHT - HEALTH_ICON_SIZE) / 2;
        
        // Draw health icon (2x bigger, vertically aligned with health bar)
        batch.draw(healthIconTexture, iconX, iconY, HEALTH_ICON_SIZE, HEALTH_ICON_SIZE);
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