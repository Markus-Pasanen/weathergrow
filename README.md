# WeatherGrow - Plant Care Game

A minimalist Android plant care game built with LibGDX where you nurture a virtual plant that loses health over time, both while playing and when the app is closed.

## Game Overview

WeatherGrow is a simple, elegant plant care game with a clean UI and intuitive gameplay. Your plant starts with 100 health and loses 1 point per second in real-time. The game saves your progress and calculates health loss based on elapsed time when you reopen the app.

## Features

### Core Gameplay
- **Health System**: Plant starts at 100 health, loses 1 point per second
- **Offline Calculation**: Game saves timestamp and calculates health loss when reopened
- **Visual Health Indicators**: Plant shows different textures based on health:
  - **Healthy** (>30 health): Green, vibrant plant
  - **Dry** (≤30 health): Brown, wilted plant  
  - **Dead** (≤0 health): Gray, dead plant
- **Watering**: Tap water button to restore 20 health points

### Clean UI Design
- **Minimalist Interface**: No health bars, counters, or status text
- **Bottom Action Bar**: 4 intuitive buttons with icons and labels
- **Dynamic Water/Restart Button**: Changes from water to restart icon when plant dies

### Technical Features
- **LibGDX Framework**: Cross-platform game development
- **Android Native**: Optimized for mobile with proper screen scaling
- **State Persistence**: Saves game state between sessions

## Game Controls

### Bottom Action Bar (Left to Right):
1. **Water/Restart Button**:
   - When plant is alive: Water droplet icon with "WATER" label
   - When plant is dead: Restart icon with "NEW PLANT" label (red text)
   - Tapping waters plant or restarts game based on state

2. **Inventory Button** (Placeholder): Backpack icon with "INVENTORY" label

3. **Store Button** (Placeholder): Store icon with "STORE" label

4. **Settings Button** (Placeholder): Gear icon with "SETTINGS" label

## Visual Effects

- **Watering Animation**: Water droplet icon expands from center of screen and fades out
- **Button Feedback**: All buttons have subtle scale animations when tapped
- **Low Health Pulse**: Water button gently pulses when plant health is below 30
- **Static Plant**: Plant remains perfectly still (no dizzying animations)

## Project Structure

```
weathergrow/
├── android/          # Android platform implementation
├── core/            # Core game logic (platform-independent)
├── assets/          # Game assets
│   ├── plants/      # Plant textures (healthy, dry, dead)
│   └── ui/Icons/    # UI icons (PNG format)
└── README.md        # This file
```

## Building and Running

### Prerequisites
- Java JDK 8 or higher
- Android SDK
- Gradle

### Build Commands
```bash
# Build the project
./gradlew build

# Run on Android device/emulator
./gradlew android:run

# Build Android APK
./gradlew android:assembleRelease
```

## Development Notes

### Key Implementation Details
- Game state is saved using `Preferences` for simple persistence
- Health loss is calculated based on elapsed time using system timestamps
- UI uses Scene2D for clean, scalable interface components
- All game logic is in `GameScreen.java`, UI in `GameUI.java`

### Assets
- Plant textures: `plant_healthy.png`, `plant_dry.png`, `plant_dead.png`
- UI icons: `water.png`, `inventory.png`, `store.png`, `settings.png`, `restart.png`, `x.png`
- Font: Uses default LibGDX skin font with scaling

## Future Enhancements (Placeholder Features)
The bottom action bar has three placeholder buttons ready for future implementation:
- **Inventory System**: Track collected items and upgrades
- **Store**: Purchase plant upgrades and decorative items  
- **Settings**: Game options and preferences

## License
School project - for educational purposes

---
**Built with LibGDX | Android Mobile Game**
