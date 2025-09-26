# Robot IDE Parkour

Robot IDE Parkour is a 2D side-scrolling platformer built in Java for Android. It features a cartoon robot sprinting across an IDE-inspired landscape where code blocks, terminals, and debug panels form the level geometry.

## Build & Run
1. Open the project in Android Studio Iguana/Koala or newer.
2. Allow Gradle to sync; no additional plugins or assets are required.
3. Connect an Android device (API 24+) or start an emulator (tablet or phone, landscape preferred).
4. Press **Run** to install and launch the game.

## Controls
- **Touch**: Left / Right arrow buttons for movement, Up arrow launches a jump.
- **Keyboard / Emulator**: `←` and `→` to move, `Space` to jump, `Back` to return to the previous menu.

## Gameplay Flow
1. **Menu Scene**: Tap *Play* to start, *Scoreboard* to view best times, *Settings* to toggle sound/music.
2. **Game Scene**: Collect brace-coins, avoid syntax-error spikes, and reach the green Run flag.
3. **Game Over Scene**: Retry, return to the menu, or jump to the scoreboard.
4. **Scoreboard Scene**: Stores the Top-10 fastest completion times in `SharedPreferences` on the device.

The HUD shows collected coins, elapsed time, remaining lives, and measured FPS. Sound effects use `SoundPool`, while the looping background track plays through `MediaPlayer`. The SurfaceView is driven by a fixed-step game loop targeting 60 FPS.

## Adding New Levels
Level layouts live inside `app/src/main/java/com/example/robotparkour/scene/GameScene.java` as string arrays. Each character represents a 32×32 tile:

- `G` – Code editor block (solid)
- `B` – Terminal block (solid)
- `Q` – Debug button block (solid)
- `C` – Coin `{}` collectible
- `S` – Syntax error spike (hazard)
- `F` – Goal flag (finish)
- `R` – Robot spawn point
- `.` – Empty space

To create a new level:
1. Replace or extend the `LEVEL_DATA` array with your tile rows.
2. Ensure each string has the same length and uses only the symbols above.
3. Adjust `INITIAL_LIVES`, camera, or physics constants as desired.

## Project Structure Highlights
- `MainActivity` hosts the `GameView` SurfaceView.
- `GameView` runs the `GameThread`, manages scenes, audio, and FPS tracking.
- `scene` package implements the Menu, Game, Game Over, Scoreboard, and Settings screens.
- `entity` package contains the robot, tiles, coins, spikes, and flag rendering/logic.
- `storage/ScoreboardManager` persists best times (Top-10) using `SharedPreferences`.
- `audio/GameAudioManager` loads sound effects and the background chiptune loop.

Enjoy sprinting through the IDE!
