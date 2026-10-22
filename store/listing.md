# Deepfall Drill — Play Store listing kit

## App name
Deepfall Drill

## Short description (max 80 characters)
Heavy drill. Endless descent. Steer through rock, lava and mines.

## Full description
DEEPFALL DRILL is a heavy arcade mining roguelite built around one idea:
gravity is the pilot, you are the steering.

Your drill is always falling. You cannot climb, brake or fly. You steer left
and right, cut through the ground and decide which rock is worth eating.

SOLID WORLD
No caves, no shortcuts, no free-fall corridor. Every metre is drilled by you.

HEAVY MACHINE
Soft dirt is ploughed through. Stone grinds. Basalt nearly stops you.

HAZARDS
Embedded lava pockets and buried mines. The hull is all you have.

ORE
Iron, gold, gem clusters and diamond. Bank them and upgrade the drill.

MILESTONES
Every 250 m choose one of three temporary upgrades.

WORKSHOP
Permanent upgrades that survive every run.

THREE CONTROLS
Two touch zones, drag-to-steer, or tilt.

PORTRAIT · OFFLINE · NO ADS · NO IN-APP PURCHASES

Deepfall Drill collects no data and needs no internet connection.

## Category
Games → Arcade

## Tags
arcade, mining, drill, roguelite, endless, pixel, offline

## Content rating questionnaire (suggested answers)
- Violence: cartoon drilling and small explosions, no blood. → Everyone / PEGI 3–7.
- User-generated content: no.
- Data collection: no.
- Ads: no.
- In-app purchases: no.

## Data safety
- Data collected: **None**
- Data shared: **None**
- Security practices: data is not transmitted; local save only.
- Deletion: uninstall removes local data.
- Ads/analytics SDKs: none.

## App access
All functionality is available without login or special access.

## Privacy policy
Host `PRIVACY.md` publicly and paste the URL in Play Console.

## Release notes — 1.0.0
First release. Solid endless drilling, three steering modes, lava, mines,
ore economy, 250 m upgrades and a permanent workshop.

## Store assets in this folder
- `icon-512.png` — 512×512 app icon for the listing
- `feature-graphic-1024x500.png` — feature graphic
- `screenshots/phone-01-surface.png` — surface start / START button
- `screenshots/phone-02-run.png` — drilling at depth
- `screenshots/phone-03-death.png` — run summary
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
- [ ] Privacy policy URL live
- [ ] Data safety form: no data collected
- [ ] Content rating completed
- [ ] 2–8 phone screenshots uploaded
- [ ] 512 icon + 1024×500 feature graphic uploaded
- [ ] App access: no restrictions
- [ ] Target audience + ads declaration
- [ ] Internal test on a physical phone
- [ ] versionCode increased for every upload
