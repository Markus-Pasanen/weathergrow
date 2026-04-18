package com.jyu.weathergrow;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;

/** Scene2D UI manager for the game screen. */
public class GameUI {
    private final GameScreen gameScreen;
    private final Stage stage;
    private final Skin skin;
    private final Viewport viewport;

    // UI components
    private Table bottomActionBar;
    private ImageButton waterButton;
    private Window gameOverWindow;
    private Label waterButtonLabel;
    private Table waterButtonContainer;
    
    // Custom drawables
    private TextureRegionDrawable waterButtonUp;
    private TextureRegionDrawable restartButtonUp;

    // Constants for layout
    private static final float VIEWPORT_WIDTH = 720f;
    private static final float VIEWPORT_HEIGHT = 1280f;
    private static final float BUTTON_SIZE = 200f; // Slightly larger than original 160px
    private static final float BUTTON_PADDING = 20f; // Same padding as health icon (20px)
    private static final float THIRST_HEALTHY = 70f;

    public GameUI(GameScreen gameScreen, Skin skin, Viewport viewport) {
        this.gameScreen = gameScreen;
        this.skin = skin;
        this.viewport = viewport;
        this.stage = new Stage(viewport);

        Gdx.app.log("GameUI", "Initializing UI with viewport: " + viewport.getWorldWidth() + "x" + viewport.getWorldHeight());
        initializeUI();
        setupEventHandlers();
        Gdx.app.log("GameUI", "UI initialization complete");
    }

    /** Initialize all UI components and layout */
    private void initializeUI() {
        stage.clear();
        
        // Load custom assets
        loadCustomAssets();

        // Create bottom action bar with buttons
        createBottomActionBar();

        // Create game over window (initially hidden)
        createGameOverWindow();

        // Create empty windows for inventory, shop, settings (buttons are placeholders)
        createEmptyWindows();

        // Layout the UI
        layoutUI();
    }
    
    /** Load custom texture assets for UI */
    private void loadCustomAssets() {
        // Try to load icons with multiple fallback strategies
        waterButtonUp = loadIcon("water");
        restartButtonUp = loadIcon("restart");
    }
    
    /** Load an icon - uses PNG files directly */
    private TextureRegionDrawable loadIcon(String iconName) {
        // Try PNG in main icons directory
        try {
            Texture tex = new Texture(Gdx.files.internal("ui/Icons/" + iconName + ".png"));
            Gdx.app.log("GameUI", "Loaded PNG icon: " + iconName);
            return new TextureRegionDrawable(new TextureRegion(tex));
        } catch (Exception e) {
            Gdx.app.log("GameUI", "PNG failed for " + iconName + ": " + e.getMessage());
        }
        
        // Create colored placeholder as last resort
        Gdx.app.log("GameUI", "Creating colored placeholder for: " + iconName);
        return createColoredIcon(iconName);
    }
    
    /** Create a colored placeholder icon */
    private TextureRegionDrawable createColoredIcon(String iconName) {
        int size = 64;
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        
        // Set color based on icon name
        Color color;
        switch (iconName) {
            case "water":
                color = new Color(0.2f, 0.5f, 0.9f, 1f); // Blue
                break;
            case "restart":
                color = new Color(0.2f, 0.8f, 0.4f, 1f); // Green
                break;
            default:
                color = Color.GRAY;
        }
        
        pixmap.setColor(color);
        pixmap.fillCircle(size/2, size/2, size/2 - 4);
        
        // Add a border
        pixmap.setColor(Color.DARK_GRAY);
        pixmap.drawCircle(size/2, size/2, size/2 - 2);
        
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        
        return new TextureRegionDrawable(new TextureRegion(texture));
    }

    // Removed top status bar - plant health shown only through graphics

    // Removed stats panel - plant health is shown only through graphics and status text

    /** Create bottom action bar with only water button in bottom right corner */
    private void createBottomActionBar() {
        bottomActionBar = new Table();
        bottomActionBar.setFillParent(true); // Fill entire stage
        bottomActionBar.bottom().right(); // Align to bottom right

        // Water button (functional)
        ImageButton.ImageButtonStyle waterStyle = new ImageButton.ImageButtonStyle();
        waterStyle.up = waterButtonUp;
        waterStyle.down = waterButtonUp;
        waterStyle.over = waterButtonUp;
        
        waterButton = new ImageButton(waterStyle);
        
        // Add press animation - change color when pressed
        waterButton.addListener(new ClickListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                // Add color tint for press feedback
                waterButton.setColor(0.7f, 0.7f, 1.0f, 1.0f); // Brighter blue tint
                return true;
            }
            
            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                // Restore color
                waterButton.setColor(Color.WHITE);
            }
            
            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                // If finger exits button without releasing, restore normal state
                waterButton.setColor(Color.WHITE);
            }
        });
        
        // Create simple button container with label
        waterButtonContainer = createSimpleButton(waterButton, "WATER");
        waterButtonLabel = getLabelFromContainer(waterButtonContainer);

        // Add button to bottom right corner with padding
        bottomActionBar.add(waterButtonContainer).pad(BUTTON_PADDING).bottom().right();
    }
    
    /** Create a simple button without label (no frame) */
    private Table createSimpleButton(ImageButton button, String labelText) {
        Table container = new Table();
        
        // Add button with fixed size
        container.add(button).size(BUTTON_SIZE);
        
        return container;
    }
    
    /** Get the label from a button container created by createSimpleButton */
    private Label getLabelFromContainer(Table container) {
        // No label in container anymore
        return null;
    }
    
    /** Create a standard popup window with title and close button */
    private Window createPopupWindow(String title, float width, float height) {
        Window window = new Window("", skin);
        window.setModal(true);
        window.setVisible(false);
        window.setMovable(false);
        window.setSize(width, height);
        window.pad(20);
        
        // Create title label
        Label titleLabel = new Label(title, skin);
        titleLabel.setColor(new Color(0.2f, 0.2f, 0.2f, 1f));
        titleLabel.getStyle().font.getData().setScale(2.0f);
        
        // Create close button (X icon) - larger and more prominent
        ImageButton.ImageButtonStyle closeStyle = new ImageButton.ImageButtonStyle();
        // Create colored placeholder for close button
        Pixmap closePixmap = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        closePixmap.setColor(new Color(0.9f, 0.3f, 0.3f, 1f)); // Red
        closePixmap.fillCircle(32, 32, 30);
        closePixmap.setColor(Color.DARK_GRAY);
        closePixmap.drawCircle(32, 32, 31);
        Texture closeTexture = new Texture(closePixmap);
        closePixmap.dispose();
        closeStyle.up = new TextureRegionDrawable(new TextureRegion(closeTexture));
        closeStyle.down = closeStyle.up;
        closeStyle.over = closeStyle.up;
        ImageButton closeButton = new ImageButton(closeStyle);
        closeButton.setSize(50, 50);
        
        // Add close button listener
        closeButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                window.setVisible(false);
            }
        });
        
        // Create header table with title and close button
        Table header = new Table();
        header.add(titleLabel).expandX().left().padLeft(20);
        header.add(closeButton).size(50).right().padRight(20);
        
        window.add(header).expandX().fillX().padBottom(30).row();
        
        return window;
    }

    /** Create plant status label - already created in top status bar */

    /** Create game over window with restart button */
    private void createGameOverWindow() {
        // No game over window - water button will change to restart button when plant dies
        gameOverWindow = new Window("", skin);
        gameOverWindow.setVisible(false); // Always hidden
    }

    // Removed placeholder dialog - using proper windows instead

    /** Layout UI components on stage */
    private void layoutUI() {
        // Clear stage
        stage.clear();

        // Add bottom action bar to stage (already positioned in bottom right)
        stage.addActor(bottomActionBar);
    }
    
    /** Center a window on screen */
    private void centerWindow(Window window) {
        window.setPosition((VIEWPORT_WIDTH - window.getWidth()) / 2,
                          (VIEWPORT_HEIGHT - window.getHeight()) / 2);
    }

    /** Set up event handlers for buttons */
    private void setupEventHandlers() {
        // Water button - waters the plant with visual feedback
        waterButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (gameScreen.isPlantDead()) {
                    // Plant is dead - restart the game
                    gameScreen.resetGame();
                    
                    // Visual feedback - scale animation
                    waterButton.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence(
                        com.badlogic.gdx.scenes.scene2d.actions.Actions.scaleTo(0.9f, 0.9f, 0.05f),
                        com.badlogic.gdx.scenes.scene2d.actions.Actions.scaleTo(1.0f, 1.0f, 0.1f)
                    ));
                } else {
                    // Plant is alive - water it
                    gameScreen.waterPlant();
                    
                    // Visual feedback - scale animation
                    waterButton.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence(
                        com.badlogic.gdx.scenes.scene2d.actions.Actions.scaleTo(0.9f, 0.9f, 0.05f),
                        com.badlogic.gdx.scenes.scene2d.actions.Actions.scaleTo(1.0f, 1.0f, 0.1f)
                    ));
                    
                    // Add water splash effect
                    showWaterEffect();
                }
            }
        });

        // Restart functionality is handled by the restartButton in the game over window
    }
    
    /** Show water splash effect animation */
    private void showWaterEffect() {
        // Create water droplet image using water button icon
        Image waterDroplet = new Image(waterButtonUp);
        
        // Start from center of screen
        float centerX = VIEWPORT_WIDTH / 2;
        float centerY = VIEWPORT_HEIGHT / 2;
        
        // Start small and centered
        waterDroplet.setSize(BUTTON_SIZE * 0.5f, BUTTON_SIZE * 0.5f);
        waterDroplet.setPosition(
            centerX - waterDroplet.getWidth() / 2,
            centerY - waterDroplet.getHeight() / 2
        );
        
        // No color tint - use the icon as-is
        waterDroplet.setColor(1f, 1f, 1f, 1f);
        
        stage.addActor(waterDroplet);
        
        // Animate droplet - expand from center and fade out
        waterDroplet.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence(
            com.badlogic.gdx.scenes.scene2d.actions.Actions.parallel(
                // Expand to 3x size
                com.badlogic.gdx.scenes.scene2d.actions.Actions.scaleTo(3.0f, 3.0f, 0.4f),
                // Fade out gradually
                com.badlogic.gdx.scenes.scene2d.actions.Actions.alpha(0f, 0.4f)
            ),
            com.badlogic.gdx.scenes.scene2d.actions.Actions.removeActor()
        ));
    }

    // Removed placeholder dialog method - using proper windows instead

    /** Update UI components with current game state */
    public void update(float delta) {
        // Update water/restart button based on plant state
        boolean plantDead = gameScreen.isPlantDead();
        
        if (plantDead) {
            // Plant is dead - change water button to restart button
            if (waterButton.getStyle().up != restartButtonUp) {
                // Change icon to restart
                waterButton.getStyle().up = restartButtonUp;
                waterButton.getStyle().down = restartButtonUp;
                waterButton.getStyle().over = restartButtonUp;
                
                // No label to update
                
                // Reset scale
                waterButton.setScale(1.0f);
            }
        } else {
            // Plant is alive - ensure water button shows water icon
            if (waterButton.getStyle().up != waterButtonUp) {
                // Change icon back to water
                waterButton.getStyle().up = waterButtonUp;
                waterButton.getStyle().down = waterButtonUp;
                waterButton.getStyle().over = waterButtonUp;
                
                // No label to update
            }
            
            // Add subtle pulse animation to water button when plant health is low
            if (gameScreen.getHealth() < 30) {
                float pulse = (float) (Math.sin(System.currentTimeMillis() * 0.003) * 0.1f + 1.0f);
                waterButton.setScale(pulse);
            } else {
                waterButton.setScale(1.0f);
            }
        }
    }

    // Plant status is shown only through graphics, not text
    
    /** Create empty windows (buttons are placeholders) */
    private void createEmptyWindows() {
        // No windows needed since we only have water button
    }

    /** Render the UI stage */
    public void render(float delta) {
        update(delta);
        stage.act(delta);
        stage.draw();
    }

    /** Handle screen resize */
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        layoutUI();
    }

    /** Dispose resources */
    public void dispose() {
        stage.dispose();
    }

    /** Get the stage for input processing */
    public Stage getStage() {
        return stage;
    }
}