package com.jyu.weathergrow;

import com.badlogic.gdx.Game;

/** Entry point for the Weathergrow game. Manages screen transitions. */
public class WeathergrowGame extends Game {

    @Override
    public void create() {
        // Set the initial screen to the main game screen
        setScreen(new GameScreen());
    }
}