# Larger deposits and longer progression

- Mines are single 2x2-cell objects, including placement validation, centered
  triggering, a larger visual plate and a size-adjusted explosion radius.
- Mineral deposits have a dense 3x3 or rounded 4x4 core, with an irregular
  two-cell pixel fringe. They spawn less frequently than the former small veins.
- Five buried power-ups: Boost Tank (refill), Repair Kit (+25 hull), Overdrive
  (+45% dig power for 8 seconds), Ore Pulse (collect ore within 10 cells), and
  Shield (45% damage reduction for 8 seconds).
- New milestone choices: Fuel Sipper, Blast Suit, Shock Plates.
- Workshop has eight upgrade tracks: Ore Magnet and Fuel Cells are new.
- First-level prices are 150-200 ore; later prices scale by 1.60-1.68 per level.
  Upgrade caps are now 8-12 levels. Existing purchases remain owned; there is no
  save reset or retroactive currency deduction.

The new costs are initial balance values, not a claim of completed player testing.
Power-up effects are temporary and do not persist through checkpoint reloads.

## Build

Open this folder in Android Studio, sync using JDK 21 and run the `app` module.
Signed publishing build: `tools/build-release.ps1`.
Play artifact: `app/build/outputs/bundle/release/app-release.aab`.

## Verification

`tools/ContentTest.java` exercises generated mine footprints, dense mineral cores,
power-up collection and the permanent upgrade cost curve. It runs on the desktop
JVM with Android's platform JAR on the classpath (Android APIs are not invoked).
