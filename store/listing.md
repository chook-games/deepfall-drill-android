# Deepfall Drill — Play Store listing kit

## App name
Deepfall Drill

## Short description (max 80 characters)
Steer, boost and dig deep. Daily challenges, contracts, and new drills await.

## Full description
DEEPFALL DRILL is a heavy arcade mining roguelite built around one idea:
gravity is the pilot, you are the steering.

Your drill is always falling. You cannot brake or fly away. Steering, and a
limited reverse thruster, are all you have.

SOLID WORLD
No caves, no shortcuts, no free-fall corridor. Every metre is drilled by you.
The first metres are slow; the drill ramps up as you descend.

HEAVY MACHINE
Soft dirt is ploughed through. Stone grinds. Basalt nearly stops you.

BIG DEPOSITS
Mineral veins come as solid 3x3 / 4x4 cores with a pixel-art fringe, not as
scattered dots. Find richer ore as you push deeper.

HAZARDS
Lava burns and slows the drill. Mines blast and stun it for two seconds.
The screen flashes red whenever the hull takes damage.

REVERSE THRUSTERS
Hold the booster to climb back up your own tunnel. It recharges over time;
upgrades add tank size, thrust and recharge speed.

POWER-UPS
Buried pickups: boost refill, repair kit, overdrive, ore pulse and shield.

ORE
Iron, gold, gem clusters and diamond. Every run also pays depth-based salvage,
so each descent moves your workshop forward.

FRESH GOALS
Take on a new daily seed, complete a run contract, or share a seed code and
challenge a friend to beat your depth.

MILESTONES
Every 250 m choose one of three temporary upgrades.

WORKSHOP
Eight permanent upgrade tracks and three drill hulls to unlock. Brick and Needle
change the feel of every run.

THREE CONTROLS
Two touch zones, drag-to-steer, or tilt.

PORTRAIT · OFFLINE · NO ADS · NO IN-APP PURCHASES

No personal data is collected. The game works offline; sharing a run is optional
and uses Android's system share sheet.

## Category
Games → Arcade

## Content rating questionnaire (suggested, answer honestly)
- Violence: cartoon/fantasy drilling and small explosions, no blood.
  → likely Everyone 10+ / PEGI 7.
- No in-game chat or user-generated content. Players may export generated run
  details to another app through Android's system share sheet.
- Personal data collection: no.
- Ads: no.
- In-app purchases: no.
- Gambling: no.
- Controlled substances: no.
- Sexual content / language: no.

## Data safety
- Personal data collected by the game: **None**
- Optional sharing: a run's depth, generated seed and death line are passed to
  Android's share sheet only when the player taps Share and chooses an app.
- Save data: local only; the game has no backend or account system.
- Deletion: uninstall removes local data.
- Ads/analytics/crash SDKs: none.

## App access
All functionality is available without login or special access.

## Privacy policy
Host `PRIVACY.md` publicly and paste the URL in Play Console.

## Release notes — 1.2.0
Daily runs, seed sharing, run contracts, three selectable drill hulls, depth
salvage, and a smoother endless difficulty curve with richer deep ore and
increasing mine pressure.

## Store assets in this folder
- `icon-512.png` — 512×512 app icon for the listing
- `feature-graphic-1024x500.png` — feature graphic
- `screenshots/phone-01-surface.png` — surface start / START button
- `screenshots/phone-02-run.png` — drilling at depth, boost button
- `screenshots/phone-03-death.png` — run summary with a drill joke
- `screenshots/phone-04-workshop.png` — permanent upgrades

All phone screenshots are 1080×1920 (9:16), within Play limits.

## Release build
```powershell
# one time: create and back up the upload key
.\tools\make-keystore.ps1

# every release: bump versionCode/versionName in app/build.gradle first
.\tools\build-release.ps1
```
Upload `app/build/outputs/bundle/release/app-release.aab` to **Internal testing**
first, then promote to closed/open/production.

Keep `app/build/outputs/mapping/release/mapping.txt` for every release.

## Submission checklist
- [ ] Upload key backed up (keystore + password)
- [ ] Privacy policy URL live (replace YOUR_EMAIL_HERE first)
- [ ] Data safety form: no personal data collected; accurately review the
  user-initiated Share sheet flow
- [ ] Content rating completed
- [ ] 2–8 phone screenshots uploaded
- [ ] 512 icon + 1024×500 feature graphic uploaded
- [ ] Screenshots captured from the 1.2.0 build
- [ ] App access: no restrictions
- [ ] Target audience + ads declaration
- [ ] Internal test on a physical phone
- [ ] versionCode increased for every upload
