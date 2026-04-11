package com.jyu.weathergrow;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.Batch;
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

/** Scene2D UI manager for the game screen. */
public class GameUI {
    private final GameScreen gameScreen;
    private final Stage stage;
    private final Skin skin;
    private final Viewport viewport;

    // UI components
    private Table bottomActionBar;
    private ImageButton waterButton;
    private ImageButton inventoryButton;
    private ImageButton shopButton;
    private ImageButton settingsButton;
    private Window gameOverWindow;
    private Window inventoryWindow;
    private Window shopWindow;
    private Window settingsWindow;
    private Label waterButtonLabel;
    private Table waterButtonContainer;
    
    // Custom drawables
    private TextureRegionDrawable waterButtonUp;
    private TextureRegionDrawable inventoryButtonUp;
    private TextureRegionDrawable shopButtonUp;
    private TextureRegionDrawable settingsButtonUp;
    private TextureRegionDrawable restartButtonUp;
    private TextureRegionDrawable xButtonUp;

    // Constants for layout
    private static final float VIEWPORT_WIDTH = 720f;
    private static final float VIEWPORT_HEIGHT = 1280f;
    private static final float BOTTOM_BAR_HEIGHT = 180f;
    private static final float BUTTON_SIZE = 80f;
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
        inventoryButtonUp = loadIcon("inventory");
        shopButtonUp = loadIcon("store");
        settingsButtonUp = loadIcon("settings");
        restartButtonUp = loadIcon("restart");
        xButtonUp = loadIcon("x");
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
            case "inventory":
                color = new Color(0.9f, 0.6f, 0.2f, 1f); // Orange
                break;
            case "store":
                color = new Color(0.9f, 0.8f, 0.2f, 1f); // Yellow
                break;
            case "settings":
                color = new Color(0.6f, 0.2f, 0.8f, 1f); // Purple
                break;
            case "restart":
                color = new Color(0.2f, 0.8f, 0.4f, 1f); // Green
                break;
            case "x":
                color = new Color(0.9f, 0.3f, 0.3f, 1f); // Red
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

    /** Create bottom action bar with water, inventory, shop, settings buttons */
    private void createBottomActionBar() {
        bottomActionBar = new Table();
        bottomActionBar.pad(20);

        // Water button (functional)
        ImageButton.ImageButtonStyle waterStyle = new ImageButton.ImageButtonStyle();
        waterStyle.up = waterButtonUp;
        waterStyle.down = waterButtonUp;
        waterStyle.over = waterButtonUp;
        
        waterButton = new ImageButton(waterStyle);
        
        // Inventory button icon
        ImageButton.ImageButtonStyle invStyle = new ImageButton.ImageButtonStyle();
        invStyle.up = inventoryButtonUp;
        invStyle.down = inventoryButtonUp;
        invStyle.over = inventoryButtonUp;
        inventoryButton = new ImageButton(invStyle);

        // Shop button icon  
        ImageButton.ImageButtonStyle shopStyle = new ImageButton.ImageButtonStyle();
        shopStyle.up = shopButtonUp;
        shopStyle.down = shopButtonUp;
        shopStyle.over = shopButtonUp;
        shopButton = new ImageButton(shopStyle);

        // Settings button icon
        ImageButton.ImageButtonStyle settingsStyle = new ImageButton.ImageButtonStyle();
        settingsStyle.up = settingsButtonUp;
        settingsStyle.down = settingsButtonUp;
        settingsStyle.over = settingsButtonUp;
        settingsButton = new ImageButton(settingsStyle);

        // Create simple button containers with labels (no frames)
        waterButtonContainer = createSimpleButton(waterButton, "WATER");
        waterButtonLabel = getLabelFromContainer(waterButtonContainer);
        Table invContainer = createSimpleButton(inventoryButton, "INVENTORY");
        Table shopContainer = createSimpleButton(shopButton, "STORE");
        Table settingsContainer = createSimpleButton(settingsButton, "SETTINGS");

        bottomActionBar.add(waterButtonContainer).pad(15);
        bottomActionBar.add(invContainer).pad(15);
        bottomActionBar.add(shopContainer).pad(15);
        bottomActionBar.add(settingsContainer).pad(15);
    }
    
    /** Create a simple button with label (no frame) */
    private Table createSimpleButton(ImageButton button, String labelText) {
        Table container = new Table();
        
        button.setSize(BUTTON_SIZE, BUTTON_SIZE);
        container.add(button).size(BUTTON_SIZE).row();
        
        Label label = new Label(labelText, skin);
        label.setColor(new Color(0.2f, 0.2f, 0.2f, 1f)); // Dark gray
        label.getStyle().font.getData().setScale(1.0f);
        container.add(label).padTop(6);
        
        return container;
    }
    
    /** Get the label from a button container created by createSimpleButton */
    private Label getLabelFromContainer(Table container) {
        // The label is the second child (index 1) in the container
        if (container.getChildren().size > 1) {
            return (Label) container.getChildren().get(1);
        }
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
        closeStyle.up = xButtonUp;
        closeStyle.down = xButtonUp;
        closeStyle.over = xButtonUp;
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

        // Bottom action bar at the bottom
        bottomActionBar.setPosition(0, 0);
        bottomActionBar.setSize(VIEWPORT_WIDTH, BOTTOM_BAR_HEIGHT);

        // Add only bottom action bar to stage
        // Other windows are empty and always hidden
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

        // Inventory button - placeholder (no popup)
        inventoryButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // Just scale animation, no window
                inventoryButton.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence(
                    com.badlogic.gdx.scenes.scene2d.actions.Actions.scaleTo(0.9f, 0.9f, 0.05f),
                    com.badlogic.gdx.scenes.scene2d.actions.Actions.scaleTo(1.0f, 1.0f, 0.1f)
                ));
            }
        });

        // Shop button - placeholder (no popup)
        shopButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // Just scale animation, no window
                shopButton.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence(
                    com.badlogic.gdx.scenes.scene2d.actions.Actions.scaleTo(0.9f, 0.9f, 0.05f),
                    com.badlogic.gdx.scenes.scene2d.actions.Actions.scaleTo(1.0f, 1.0f, 0.1f)
                ));
            }
        });

        // Settings button - placeholder (no popup)
        settingsButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // Just scale animation, no window
                settingsButton.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence(
                    com.badlogic.gdx.scenes.scene2d.actions.Actions.scaleTo(0.9f, 0.9f, 0.05f),
                    com.badlogic.gdx.scenes.scene2d.actions.Actions.scaleTo(1.0f, 1.0f, 0.1f)
                ));
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
                
                // Change label text to "NEW PLANT"
                if (waterButtonLabel != null) {
                    waterButtonLabel.setText("NEW PLANT");
                    waterButtonLabel.setColor(new Color(0.8f, 0.2f, 0.2f, 1f)); // Red color for restart
                }
                
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
                
                // Change label text back to "WATER"
                if (waterButtonLabel != null) {
                    waterButtonLabel.setText("WATER");
                    waterButtonLabel.setColor(new Color(0.2f, 0.2f, 0.2f, 1f)); // Dark gray color
                }
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
    
    /** Create empty windows for inventory, shop, settings (buttons are placeholders) */
    private void createEmptyWindows() {
        // Create empty windows that are always hidden
        inventoryWindow = new Window("", skin);
        inventoryWindow.setVisible(false);
        
        shopWindow = new Window("", skin);
        shopWindow.setVisible(false);
        
        settingsWindow = new Window("", skin);
        settingsWindow.setVisible(false);
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