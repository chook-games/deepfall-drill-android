# DEEPFALL DRILL — native Android

A heavy drill, endless gravity, and only left/right steering. This is the native
version: plain Java + Canvas, no engine, no WebView, no external libraries.
Single Activity, one SurfaceView, one render thread.

> **Download APK:** https://github.com/chook-games/deepfall-drill-android/releases/latest
>
> **Play in browser:** https://chook-games.github.io/deepfall-drill/

The HTML5 prototype in `..\i Dig it` remains as a reference; this folder is the
Android build with the requested changes:

- **No horizontal camera movement.** The world is exactly the screen: 48 columns
  wide, with bedrock walls. The camera only follows depth.
- **Fine destructible grid.** 48 columns across (3× finer than the first native
  build) and a 1.15-cell drill radius. Terrain chunks are baked into bitmaps so
  the fine grid still runs at 60 fps.
- **100% solid underground.** No caves, pockets, shafts or internal voids. Every
  cell below the surface is solid rock, ore, lava or miner's tunnel. The only
  empty space is what the drill cuts (plus mine craters).
- **Surface start.** Sky, trees, grass and a launch platform above the ground.
  The drill waits on the platform until the player presses START.
- **Deeper drill on screen.** The drill sits at 59% of the screen height.
- **No heat.** Heat is gone from the HUD and from gameplay. The hull drops only
  from lava and mines; falling does not hurt in the MVP.
- **Procedural sound.** Dig, impact, ore, gems, lava, mines, upgrades, UI and
  death are synthesized at startup; no audio assets in the APK. Toggle in
  Settings.

## Open in Android Studio

This is a standard single-module Gradle project (`:app`) with a wrapper.

1. Android Studio → **File → Open** → select `i Dig it android` (the folder with
   `settings.gradle`, not the `app` subfolder).
2. **Gradle JDK — read this if you see “Incompatible Gradle JVM version”.**
   Gradle 8.13 supports Java 8–23, not the JDK 25 bundled with Android Studio.
   This project already pins the Gradle JVM to `ms-21` in `.idea/gradle.xml` and
   `org.gradle.java.home` in `gradle.properties`. If the IDE still complains:
   - click **Apply compatible Gradle JDK configuration and sync**, or
   - **Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK**
     → select **`ms-21`** (`C:\Users\George\.jdks\ms-21.0.12.1`) or any JDK 17/21.
   Then **File → Sync Project with Gradle Files**. Do not choose the JBR 25 for
   this project.
3. Pick the emulator or your phone in the device dropdown and press **Run ▶**.
   The launcher activity is `com.deepfalldrill.game.MainActivity`.

`local.properties` already contains `sdk.dir=C:\Users\George\AppData\Local\Android\Sdk`.
Android Studio may rewrite it, which is fine.

Command line (same project, without the IDE):

```powershell
$env:JAVA_HOME = "C:\Users\George\.jdks\ms-21.0.12.1"
.\gradlew.bat assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## Play Store release

The release build is configured for Play: R8 + resource shrinking, version
1.0.0, targetSdk 36, adaptive icon + monochrome layer, and signing through an
upload key that is **not** in the project.

1. **Create the upload key once** (already done on this machine):

```powershell
.\tools\make-keystore.ps1
```

It creates `upload-keystore.jks` and `keystore.properties` (both gitignored).
**Back both up.** If the key is lost you must request an upload-key reset in Play
Console.

2. Bump `versionCode` (and `versionName`) in `app/build.gradle` before every
   upload. Play rejects a duplicate version code.

3. Build the signed artifacts:

```powershell
.\tools\build-release.ps1
```

- **Upload:** `app/build/outputs/bundle/release/app-release.aab`
- **Sideload test:** `app/build/outputs/apk/release/app-release.apk`
- Keep `app/build/outputs/mapping/release/mapping.txt` for crash reports.

4. Play Console → **Internal testing** → upload the AAB. Test on a real phone
   (portrait, touch, audio, tilt), then promote.

5. Store kit is in `store/`:

- `store/listing.md` — descriptions, category, data-safety answers, checklist
- `store/icon-512.png` — 512×512 listing icon
- `store/feature-graphic-1024x500.png` — feature graphic
- `store/screenshots/phone-01..04.png` — 1080×1920 (9:16) phone screenshots

6. Privacy policy: `PRIVACY.md`. Host it publicly (GitHub Pages is fine) and
   paste the URL into Play Console. The game collects no data, has no internet
   permission, no ads and no in-app purchases.

## Fast build script (aapt2, no Gradle)

There is also a dependency-free pipeline that builds the APK without Gradle:

```powershell
powershell -ExecutionPolicy Bypass -File .\build.ps1
```

Output: `dist\deepfall-drill.apk` (signed with the standard debug keystore).
This is useful when Gradle or a sync is unavailable; both paths compile the same
sources.

Build and install to a connected device / emulator:

```powershell
.\build.ps1 -Install -Run
```

If several devices are connected:

```powershell
adb -s emulator-5554 install -r dist\deepfall-drill.apk
adb -s emulator-5554 shell am start -n com.deepfalldrill.game/.MainActivity
```

Screenshots:

```powershell
.\tools\shot.ps1 -Out shot.png -Device emulator-5554
```

Package: `com.deepfalldrill.game`, minSdk 24, targetSdk 36, portrait, immersive.

## Controls

- Chosen on first Play: **Two touch zones**, **Drag**, or **Tilt**. Changeable
  from Pause and Settings.
- In-game: steer only. No up/down.
- Pause button top right. Android back pauses, then leaves the run.
- Tilt uses the accelerometer with a dead zone; it is calibrated when selected.

## Gameplay

- The run starts on the surface. The drill sits on a launch platform and waits
  for the START button; only then does gravity take over.
- Below the surface the world is 100% solid. No caves, pockets, shafts or
  internal voids: every metre is cut by the drill.
- Gravity always pulls. Contact with terrain is the only traction.
- Soft dirt is ploughed through; stone grinds; basalt is nearly a wall.
- Falling does not damage the hull in the MVP. The only damage sources are
  embedded lava pockets and buried mines.
- There is no heat mechanic: the HUD is depth, best, ore and hull only.
- Lava is embedded in the rock and burns on contact. Mines blink before they
  trigger and explode with chain reactions.
- Ore: iron, gold, gem clusters, diamond. Money + XP banked when the run ends.
- Every 250 m: free 3-choice upgrade + autosave checkpoint.
- Death: summary, level-ups, permanent ore. Workshop buys permanent upgrades.
- Sound effects: dig (soft/hard), impact, ore, gem, explosion, lava, upgrade,
  death, level-up, UI click and purchase. Toggle in Settings.

## Tuning

All values live in `app/src/main/java/com/deepfalldrill/game/Config.java`.

| Value | Native |
| --- | --- |
| Cells across | 48 (46 playable + 2 bedrock walls) |
| Cell size | 1/6 m |
| Drill radius | 1.15 cells (≈2.3 cells wide on screen) |
| Screen anchor | 59% height |
| Gravity | 78 cells/s² |
| Max fall | 51 cells/s |
| Steering | 25.5 cells/s, response 54 ground / 13.2 air |
| Dig base | 54, min 3 cells/s |
| Impact | threshold 42 cells/s; FX only, no hull damage in MVP |
| Lava | 7 entry, 13/s |
| Mine | 24 damage, radius 7 cells, starts at 270 cells (45 m) |
| Milestone | 250 m |
| Heat | none (removed from HUD and gameplay) |

One cell = 0.5 m. The HUD shows vertical metres drilled.

## Project map

```
settings.gradle           includes :app
build.gradle              AGP 8.13.2
gradlew.bat / gradle/     Gradle 8.13 wrapper
gradle.properties         pins the JDK 21 used for Gradle
local.properties          sdk.dir
PRIVACY.md                Play privacy policy template (host it publicly)
keystore.properties.example
build.ps1                 aapt2 + javac + d8 + zipalign + apksigner pipeline
app/build.gradle          namespace, versions, signing, R8
app/src/main/
  AndroidManifest.xml
  res/mipmap-*            adaptive + legacy launcher icons
  res/drawable/           vector drill foreground + monochrome icon
  java/com/deepfalldrill/game/
    MainActivity.java     immersive fullscreen, back handling, keeps screen on
    GameView.java         SurfaceView render thread, touch queue, accelerometer
    Game.java             state machine, input routing, run lifecycle, save
    Config.java           ALL tuning
    Rng.java              deterministic hashes + noise
    Materials.java        material database
    Terrain.java          chunk grid, destruction, liquids, streaming
    Generator.java        solid-lane procedural generation
    Drill.java            circle-vs-cell physics, multi-cell digging, lava
    Mines.java            mines, fuses, chain explosions
    Particles.java        dust/sparks/smoke/text
    Run.java              per-run state, ore, score, milestones, checkpoint
    Upgrades.java         run + permanent upgrades, XP curve
    SaveData.java         SharedPreferences JSON save + checkpoint
    Sfx.java              procedural WAV sound bank + SoundPool
    WorldRenderer.java    world, drill, HUD
    UiRenderer.java       menus, workshop, upgrade cards
    UiKit.java            small immediate-mode drawing kit
tools/shot.ps1            adb screenshot + downscale
tools/density.ps1         material distribution check (desktop JVM)
tools/DensityTest.java    generator sampler used by density.ps1
tools/DrillTest.java      headless drilling / collision speed check
tools/make-assets.ps1     regenerates launcher/store icons + feature graphic
tools/make-keystore.ps1   creates the upload key and keystore.properties
tools/build-release.ps1   signed AAB + release APK for Play
store/                    Play listing kit (icon, graphic, screenshots, copy)
```

Every file is under 900 lines. The render thread owns all game state; touch and
sensor input are queued so the loop never locks.

## Save model

`SharedPreferences("deepfall")` → key `save_v1`, one JSON string:

- money, xp, level, bestDepth, bestScore, totalOre, runs, deaths
- perm levels, steering mode, vibration, reduce-effects
- checkpoint: seed, x, depth, hp, run money/xp/ores, upgrades, milestone
- lastRun summary

Checkpoints are written every 15 s, on pause, on quit and at every milestone.
Death banks money/XP and clears the checkpoint. Terrain is deterministic, so a
resume rebuilds the same world from the seed; the pocket around the drill is
cleared and nearby mines are removed.

## Tests performed

On the 1080×2400 emulator (API 36), with screenshots in `screenshots/`:

- cold boot → menu → first-play steering selection → surface scene
- surface: sky, trees, grass, launch platform, drill waiting, START button
- START releases the drill; it falls onto the ground and starts cutting
- fixed camera, 48 columns; zero horizontal scrolling
- 100% solid underground: no caves or black voids (screenshots `surface-start`,
  `solid-run`, `deep-run`)
- milestone at 250 m → 3 upgrade cards → pick resumes the run
- pause shows seed and depth; resume and checkpoint reload work
- zones, drag and tilt steering (tilt driven through the emulator sensor)
- Home/pause lifecycle keeps the run and the checkpoint
- workshop: buy a permanent upgrade, level pip, price bump, next run starts with
  the upgraded hull
- death summary with new-best line, banked ore/XP, level-up hook
- `dumpsys gfxinfo`: 0.1–0.2% janky frames, 16 ms median frame time
- headless drilling test (`tools/DrillTest.java`) verifies collision, no
  ghosting, no self-damage while digging and a stable descent rate
- material density verified off-device with `tools/density.ps1`
- signed release AAB + APK built with the upload key, R8 and resource shrinking
- release APK tested on the emulator (no R8 regressions, sounds load)
- predictive back verified: run -> pause -> menu -> exit
- store kit generated in `store/` (icon, feature graphic, 4 screenshots, listing copy)

Install with `build.ps1 -Install -Run`, or open the project in Android Studio and press Run.

## Material density

`tools/density.ps1` compiles the generator on the desktop JVM and samples
240 rows per depth (percentages of all cells; air omitted):

| Depth | Air | Dirt | Stone | Basalt | Ore | Lava |
| --- | --- | --- | --- | --- | --- | --- |
| 3 m | 0% | 58.6% | 36.9% | 0% | 0.3% | 0% |
| 20 m | 0% | 42.9% | 52.4% | 0% | 0.5% | 0% |
| 50 m | 0% | 40.4% | 53.9% | 0.8% | 0.5% | 0% |
| 100 m | 0% | 17.6% | 65.5% | 11.6% | 0.7% | 0.2% |
| 200 m | 0% | 19.3% | 58.2% | 17.0% | 1.0% | 0.3% |
| 333 m | 0% | 0% | 43.9% | 50.5% | 1.1% | 0.3% |

Bedrock walls are 4.2% of every sample (2 of 48 columns). Air is 0%: the map is
fully solid apart from the player's tunnel and explosion craters.

## Known limitations / next

- Checkpoint restores the world but not old tunnels (same trade-off as the web
  prototype).
- No music yet; all effects are procedural.
- Falling does not damage the hull in this MVP. Only lava and mines do.
- Campaign modes, more hazards (falling rocks, gas), sprite art and a release
  signing config are next.
- Tilt can be inverted on some devices only if the accelerometer axes are
  remapped by the OEM; the mode is easy to flip in `GameView.onSensorChanged`.
