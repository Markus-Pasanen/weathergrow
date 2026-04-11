package com.jyu.weathergrow;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

/** Virtual pet plant game with offline time calculation.
 *  Takes care of a plant by watering it. Thirst increases over time.
 *  Save/load game state using Preferences.
 *  Designed for vertical (portrait) phone gameplay with modern UX/UI.
 */
public class Main extends InputAdapter implements ApplicationListener {
    // Constants and Preference keys (thirst, lastTimestamp, waterCount, survivalSeconds, isDead, lastPlayedTime)
    private static final String PREFS_NAME = "MyPlantGame";
    private static final float THIRST_RATE_PER_SECOND = 5.0f; // +5 thirst per second
    private static final float WATER_AMOUNT = 20.0f; // -20 thirst per tap
    private static final float THIRST_MAX = 100.0f;
    private static final float THIRST_HEALTHY = 70.0f; // below this -> healthy
    private static final float THIRST_DRY = 99.0f; // below this -> dry, above -> dead

    // Virtual viewport size (portrait: 720x1280)
    private static final float VIEWPORT_WIDTH = 720f;
    private static final float VIEWPORT_HEIGHT = 1280f;

    // Game state
    private float thirst = 0.0f;
    private long lastTimestamp = System.currentTimeMillis();
    private int waterCount = 0;
    private float survivalSeconds = 0.0f;
    private boolean isDead = false;
    private long lastPlayedTime = 0; // when app was last closed

    // Graphics
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

    // Water effect
    private boolean showWaterEffect = false;
    private float waterEffectTimer = 0.0f;
    private Vector3 tapPosition = new Vector3();
    private float waterEffectScale = 1.0f;

    // UI elements position (in viewport coordinates)
    private float plantCenterX, plantCenterY;
    private float plantWidth, plantHeight;
    private float thirstBarX, thirstBarY, thirstBarWidth, thirstBarHeight;
    private float statsPanelY;
    private float newPlantButtonX, newPlantButtonY, newPlantButtonWidth, newPlantButtonHeight;

    @Override
    public void create() {
        // Load preferences and calculate elapsed time
        loadGameState();
        updateThirstFromElapsedTime();

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

        // Input handling
        Gdx.input.setInputProcessor(this);
    }

    /** Calculate UI element positions for portrait layout */
    private void calculateUIPositions() {
        plantCenterX = VIEWPORT_WIDTH / 2;
        plantCenterY = VIEWPORT_HEIGHT * 0.65f; // Plant in upper portion

        thirstBarWidth = VIEWPORT_WIDTH * 0.8f;
        thirstBarHeight = 40f;
        thirstBarX = (VIEWPORT_WIDTH - thirstBarWidth) / 2;
        thirstBarY = VIEWPORT_HEIGHT * 0.45f; // Below plant

        statsPanelY = VIEWPORT_HEIGHT * 0.25f; // Stats in lower quarter

        newPlantButtonWidth = 250f;
        newPlantButtonHeight = 80f;
        newPlantButtonX = (VIEWPORT_WIDTH - newPlantButtonWidth) / 2;
        newPlantButtonY = VIEWPORT_HEIGHT * 0.35f;
    }

    /** Load saved game state from Preferences */
    private void loadGameState() {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        thirst = prefs.getFloat("thirst", 0.0f);
        lastTimestamp = prefs.getLong("lastTimestamp", System.currentTimeMillis());
        waterCount = prefs.getInteger("waterCount", 0);
        survivalSeconds = prefs.getFloat("survivalSeconds", 0.0f);
        isDead = prefs.getBoolean("isDead", false);
        lastPlayedTime = prefs.getLong("lastPlayedTime", 0);

        // If first launch, initialize with current timestamp
        if (lastTimestamp == 0) {
            lastTimestamp = System.currentTimeMillis();
            saveGameState();
        }
    }

    /** Save current game state to Preferences */
    private void saveGameState() {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        prefs.putFloat("thirst", thirst);
        prefs.putLong("lastTimestamp", lastTimestamp);
        prefs.putInteger("waterCount", waterCount);
        prefs.putFloat("survivalSeconds", survivalSeconds);
        prefs.putBoolean("isDead", isDead);
        prefs.putLong("lastPlayedTime", System.currentTimeMillis());
        prefs.flush();
    }

    /** Calculate elapsed time since last save and update thirst.
     *  Implements the critical offline time calculation feature.
     *  Example logic provided by the user (adapted):
     *  private void updateFromElapsedTime() {
     *      Preferences prefs = Gdx.app.getPreferences("MyPlantGame");
     *      long savedTime = prefs.getLong("lastTimestamp", System.currentTimeMillis());
     *      float savedThirst = prefs.getFloat("thirst", 0f);
     *      long currentTime = System.currentTimeMillis();
     *      long secondsPassed = (currentTime - savedTime) / 1000;
     *      if (secondsPassed > 0) {
     *          float thirstIncrease = secondsPassed * 5f;
     *          savedThirst = Math.min(savedThirst + thirstIncrease, 100f);
     *          prefs.putFloat("thirst", savedThirst);
     *          prefs.putLong("lastTimestamp", currentTime);
     *          prefs.flush();
     *      }
     *  }
     */
    private void updateThirstFromElapsedTime() {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        long savedTime = prefs.getLong("lastTimestamp", System.currentTimeMillis());
        float savedThirst = prefs.getFloat("thirst", 0.0f);
        boolean savedIsDead = prefs.getBoolean("isDead", false);

        // If already dead, no further updates
        if (savedIsDead) {
            isDead = true;
            thirst = THIRST_MAX;
            return;
        }

        long currentTime = System.currentTimeMillis();
        long secondsPassed = (currentTime - savedTime) / 1000;

        if (secondsPassed > 0) {
            // Increase thirst based on elapsed time
            float thirstIncrease = secondsPassed * THIRST_RATE_PER_SECOND;
            savedThirst = Math.min(savedThirst + thirstIncrease, THIRST_MAX);

            // Update survival time
            survivalSeconds = prefs.getFloat("survivalSeconds", 0.0f) + secondsPassed;

            // Check for death
            if (savedThirst >= THIRST_MAX) {
                isDead = true;
                savedThirst = THIRST_MAX;
            }

            // Update local state
            thirst = savedThirst;
            lastTimestamp = currentTime;

            // Save updated state
            prefs.putFloat("thirst", savedThirst);
            prefs.putLong("lastTimestamp", currentTime);
            prefs.putFloat("survivalSeconds", survivalSeconds);
            prefs.putBoolean("isDead", isDead);
            prefs.flush();
        }

        updatePlantTexture();
    }

    /** Update plant texture based on thirst and death state */
    private void updatePlantTexture() {
        if (isDead || thirst >= THIRST_MAX) {
            currentPlantTexture = plantDead;
            isDead = true;
        } else if (thirst >= THIRST_HEALTHY) {
            currentPlantTexture = plantDry;
        } else {
            currentPlantTexture = plantHealthy;
        }
    }

    /** Water the plant (called on tap) */
    private void waterPlant() {
        if (isDead) return;

        thirst = Math.max(thirst - WATER_AMOUNT, 0.0f);
        waterCount++;
        showWaterEffect = true;
        waterEffectTimer = 0.8f; // show effect for 0.8 seconds
        waterEffectScale = 1.0f;
        updatePlantTexture();

        // Update timestamp to now (watering resets elapsed time for next calculation)
        lastTimestamp = System.currentTimeMillis();
        saveGameState();
    }

    /** Reset game to start new plant */
    private void resetGame() {
        thirst = 0.0f;
        lastTimestamp = System.currentTimeMillis();
        waterCount = 0;
        survivalSeconds = 0.0f;
        isDead = false;
        showWaterEffect = false;
        updatePlantTexture();
        saveGameState();
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        // Convert screen coordinates to world coordinates
        tapPosition.set(screenX, screenY, 0);
        viewport.unproject(tapPosition);

        // Check if "NEW PLANT" button is tapped (when dead)
        if (isDead) {
            if (tapPosition.x >= newPlantButtonX && tapPosition.x <= newPlantButtonX + newPlantButtonWidth &&
                tapPosition.y >= newPlantButtonY && tapPosition.y <= newPlantButtonY + newPlantButtonHeight) {
                resetGame();
                return true;
            }
        } else {
            // Water the plant on any tap in the plant area
            // Allow tapping anywhere on screen for better UX
            waterPlant();
        }

        return true;
    }

    @Override
    public void render() {
        // Update water effect timer with scaling animation
        if (showWaterEffect) {
            waterEffectTimer -= Gdx.graphics.getDeltaTime();
            waterEffectScale = 1.0f + (0.5f * (0.8f - waterEffectTimer) / 0.8f); // Scale up effect
            if (waterEffectTimer <= 0) {
                showWaterEffect = false;
            }
        }

        // Clear screen with pleasant gradient-like background
        Gdx.gl.glClearColor(0.15f, 0.25f, 0.15f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Update camera and set up rendering
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);

        // Draw subtle background pattern (optional)
        drawBackground();

        // Draw plant centered
        batch.begin();
        float plantX = plantCenterX - plantWidth / 2;
        float plantY = plantCenterY - plantHeight / 2;
        batch.draw(currentPlantTexture, plantX, plantY, plantWidth, plantHeight);

        // Draw water effect with animation
        if (showWaterEffect) {
            fontLarge.setColor(0.2f, 0.6f, 1.0f, waterEffectTimer * 1.5f);
            String waterText = "+ Water!";
            GlyphLayout layout = new GlyphLayout(fontLarge, waterText);
            float textWidth = layout.width;
            fontLarge.draw(batch, waterText,
                tapPosition.x - textWidth / 2,
                tapPosition.y + 80 * waterEffectScale);
            fontLarge.setColor(Color.WHITE);
        }

        batch.end();

        // Draw thirst bar with modern design
        drawThirstBar();

        // Draw game stats panel
        drawStatsPanel();

        // Draw plant status
        drawPlantStatus();

        // Draw game over screen if dead
        if (isDead) {
            drawGameOverScreen();
        } else {
            // Draw instructions for alive plant
            drawInstructions();
        }
    }

    /** Draw subtle background pattern */
    private void drawBackground() {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.12f, 0.22f, 0.12f, 1);
        shapeRenderer.rect(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT);

        // Add some subtle grid lines
        shapeRenderer.setColor(0.18f, 0.28f, 0.18f, 0.3f);
        for (int i = 0; i < VIEWPORT_WIDTH; i += 100) {
            shapeRenderer.rect(i, 0, 1, VIEWPORT_HEIGHT);
        }
        for (int i = 0; i < VIEWPORT_HEIGHT; i += 100) {
            shapeRenderer.rect(0, i, VIEWPORT_WIDTH, 1);
        }
        shapeRenderer.end();
    }

    /** Draw modern thirst bar with color coding and text */
    private void drawThirstBar() {
        float fillWidth = (thirst / THIRST_MAX) * thirstBarWidth;

        // Bar background with rounded corners effect
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.1f, 0.1f, 0.1f, 0.8f);
        shapeRenderer.rect(thirstBarX - 5, thirstBarY - 5, thirstBarWidth + 10, thirstBarHeight + 10);

        // Bar fill with gradient colors
        Color barColor;
        if (thirst < 50) barColor = new Color(0.2f, 0.8f, 0.2f, 1); // Green
        else if (thirst < 80) barColor = new Color(1.0f, 0.8f, 0.2f, 1); // Yellow
        else barColor = new Color(0.8f, 0.2f, 0.2f, 1); // Red

        shapeRenderer.setColor(barColor);
        shapeRenderer.rect(thirstBarX, thirstBarY, fillWidth, thirstBarHeight);

        // Bar border
        shapeRenderer.setColor(0.8f, 0.8f, 0.8f, 0.5f);
        shapeRenderer.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.rect(thirstBarX, thirstBarY, thirstBarWidth, thirstBarHeight);
        shapeRenderer.end();

        // Thirst text above bar
        batch.begin();
        fontMedium.setColor(Color.WHITE);
        String thirstText = String.format("Thirst: %.0f/100", thirst);
        GlyphLayout layout = new GlyphLayout(fontMedium, thirstText);
        float textWidth = layout.width;
        fontMedium.draw(batch, thirstText, thirstBarX + thirstBarWidth / 2 - textWidth / 2, thirstBarY + thirstBarHeight + 30);

        // Add percentage indicator inside bar
        if (fillWidth > 50) { // Draw text on right if bar is filled enough
            fontSmall.setColor(Color.WHITE);
            String percentText = String.format("%.0f%%", thirst);
            fontSmall.draw(batch, percentText, thirstBarX + fillWidth - 20, thirstBarY + thirstBarHeight / 2 + 5);
        } else { // Draw text on left if bar is not filled much
            fontSmall.setColor(Color.BLACK);
            String percentText = String.format("%.0f%%", thirst);
            fontSmall.draw(batch, percentText, thirstBarX + fillWidth + 5, thirstBarY + thirstBarHeight / 2 + 5);
        }
        batch.end();
    }

    /** Draw game statistics panel */
    private void drawStatsPanel() {
        float panelX = VIEWPORT_WIDTH * 0.1f;
        float panelWidth = VIEWPORT_WIDTH * 0.8f;
        float panelHeight = VIEWPORT_HEIGHT * 0.2f;
        float statY = statsPanelY + panelHeight;
        float statSpacing = 35f;

        // Panel background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.1f, 0.1f, 0.1f, 0.6f);
        shapeRenderer.rect(panelX, statsPanelY, panelWidth, panelHeight);
        shapeRenderer.setColor(0.3f, 0.5f, 0.3f, 0.8f);
        shapeRenderer.rect(panelX, statsPanelY, panelWidth, 5); // Top accent line
        shapeRenderer.end();

        // Draw stats text
        batch.begin();
        fontMedium.setColor(Color.LIGHT_GRAY);

        // Water count
        String waterText = "Watered: " + waterCount + " times";
        fontMedium.draw(batch, waterText, panelX + 20, statY);

        // Survival time (if alive)
        if (!isDead) {
            String survivalText = String.format("Alive for: %.0f seconds", survivalSeconds);
            fontMedium.draw(batch, survivalText, panelX + 20, statY - statSpacing);
        }

        // Last played time
        if (lastPlayedTime > 0) {
            long secondsAgo = (System.currentTimeMillis() - lastPlayedTime) / 1000;
            String timeText;
            if (secondsAgo < 60) timeText = "Last played: " + secondsAgo + " seconds ago";
            else if (secondsAgo < 3600) timeText = "Last played: " + (secondsAgo / 60) + " minutes ago";
            else timeText = "Last played: " + (secondsAgo / 3600) + " hours ago";

            fontMedium.draw(batch, timeText, panelX + 20, statY - statSpacing * 2);
        }

        batch.end();
    }

    /** Draw plant status indicator */
    private void drawPlantStatus() {
        batch.begin();

        String stateText;
        Color stateColor;
        if (isDead) {
            stateText = "PLANT DIED - GAME OVER";
            stateColor = Color.RED;
        } else if (thirst >= THIRST_HEALTHY) {
            stateText = "DRY - NEEDS WATER!";
            stateColor = Color.ORANGE;
        } else {
            stateText = "HEALTHY & HAPPY";
            stateColor = Color.GREEN;
        }

        fontLarge.setColor(stateColor);
        GlyphLayout layout = new GlyphLayout(fontLarge, stateText);
        float textWidth = layout.width;
        fontLarge.draw(batch, stateText, VIEWPORT_WIDTH / 2 - textWidth / 2, thirstBarY - 40);
        fontLarge.setColor(Color.WHITE);

        batch.end();
    }

    /** Draw game over screen with restart button */
    private void drawGameOverScreen() {
        // Semi-transparent overlay
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.7f);
        shapeRenderer.rect(0, 0, VIEWPORT_WIDTH, VIEWPORT_HEIGHT);
        shapeRenderer.end();

        // Game over text
        batch.begin();
        fontLarge.setColor(Color.RED);
        String gameOverText = "GAME OVER";
        GlyphLayout goLayout = new GlyphLayout(fontLarge, gameOverText);
        float goTextWidth = goLayout.width;
        fontLarge.draw(batch, gameOverText, VIEWPORT_WIDTH / 2 - goTextWidth / 2, VIEWPORT_HEIGHT * 0.6f);

        // Game over message
        fontMedium.setColor(Color.LIGHT_GRAY);
        String message = "Your plant died of thirst!";
        GlyphLayout msgLayout = new GlyphLayout(fontMedium, message);
        float msgWidth = msgLayout.width;
        fontMedium.draw(batch, message, VIEWPORT_WIDTH / 2 - msgWidth / 2, VIEWPORT_HEIGHT * 0.55f);

        // Final stats
        String finalStats = String.format("Survived: %.0f seconds | Watered: %d times", survivalSeconds, waterCount);
        GlyphLayout statsLayout = new GlyphLayout(fontMedium, finalStats);
        float statsWidth = statsLayout.width;
        fontMedium.draw(batch, finalStats, VIEWPORT_WIDTH / 2 - statsWidth / 2, VIEWPORT_HEIGHT * 0.5f);
        batch.end();

        // New plant button with hover effect
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Check if mouse is over button for hover effect
        Vector3 mousePos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(mousePos);
        boolean hover = mousePos.x >= newPlantButtonX && mousePos.x <= newPlantButtonX + newPlantButtonWidth &&
                       mousePos.y >= newPlantButtonY && mousePos.y <= newPlantButtonY + newPlantButtonHeight;

        if (hover) {
            shapeRenderer.setColor(0.4f, 0.8f, 0.4f, 1); // Brighter green on hover
        } else {
            shapeRenderer.setColor(0.3f, 0.7f, 0.3f, 1); // Normal green
        }

        // Draw button with rounded corners (simulated)
        shapeRenderer.rect(newPlantButtonX, newPlantButtonY, newPlantButtonWidth, newPlantButtonHeight);

        // Button border
        shapeRenderer.setColor(0.2f, 0.5f, 0.2f, 1);
        shapeRenderer.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.rect(newPlantButtonX, newPlantButtonY, newPlantButtonWidth, newPlantButtonHeight);
        shapeRenderer.end();

        // Button text
        batch.begin();
        fontLarge.setColor(Color.WHITE);
        String buttonText = "NEW PLANT";
        GlyphLayout btnLayout = new GlyphLayout(fontLarge, buttonText);
        float btnTextWidth = btnLayout.width;
        fontLarge.draw(batch, buttonText,
            newPlantButtonX + newPlantButtonWidth / 2 - btnTextWidth / 2,
            newPlantButtonY + newPlantButtonHeight / 2 + 10);
        batch.end();
    }

    /** Draw instructions for alive plant */
    private void drawInstructions() {
        batch.begin();
        fontSmall.setColor(new Color(0.8f, 0.9f, 0.8f, 0.8f));
        String instruction = "Tap anywhere to water your plant!";
        GlyphLayout instrLayout = new GlyphLayout(fontSmall, instruction);
        float instrWidth = instrLayout.width;
        fontSmall.draw(batch, instruction, VIEWPORT_WIDTH / 2 - instrWidth / 2, 100);

        // Add a subtle hint about thirst rate
        String hint = "Thirst increases by 5 per second when away";
        GlyphLayout hintLayout = new GlyphLayout(fontSmall, hint);
        float hintWidth = hintLayout.width;
        fontSmall.draw(batch, hint, VIEWPORT_WIDTH / 2 - hintWidth / 2, 70);
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        if (width <= 0 || height <= 0) return;
        viewport.update(width, height);
        camera.position.set(VIEWPORT_WIDTH / 2, VIEWPORT_HEIGHT / 2, 0);
        camera.update();
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
        updateThirstFromElapsedTime();
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
    }
}