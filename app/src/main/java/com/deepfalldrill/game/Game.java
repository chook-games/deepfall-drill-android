package com.deepfalldrill.game;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/** State machine, input routing and run lifecycle. Render thread only. */
public final class Game {
    public static final int MENU = 0;
    public static final int CHOOSE_INPUT = 1;
    public static final int PLAYING = 2;
    public static final int PAUSED = 3;
    public static final int MILESTONE = 4;
    public static final int DYING = 5;
    public static final int DEAD = 6;
    public static final int WORKSHOP = 7;
    public static final int SETTINGS = 8;
    public static final int SURFACE = 9;

    public static final class Hit {
        public final String id;
        public final float l;
        public final float t;
        public final float r;
        public final float b;

        Hit(String id, float l, float t, float r, float b) {
            this.id = id;
            this.l = l;
            this.t = t;
            this.r = r;
            this.b = b;
        }
    }

    public volatile int state = MENU;

    final Context context;
    public final SaveData save;
    final UiKit kit = new UiKit();
    final WorldRenderer worldRenderer;
    final UiRenderer uiRenderer;
    final ArrayList<Hit> hits = new ArrayList<>();
    final Random rng = new Random();

    public Terrain terrain;
    public Drill drill;
    public final Mines mines = new Mines();
    public final Particles particles = new Particles();
    public Run run;
    public int seed;
    public float camY;
    public float time;
    public float flash;
    public float shake;
    public float toastTimer;
    public String toastText = "";
    public int width = 1;
    public int height = 1;

    public float damageFlash;
    public float boostFuel;
    public boolean thrustHeld;
    public String deathJoke = "DRILL LOST";

    private int boostPointer = -1;
    private int lastJoke = -1;

    private static final String[] DEATH_JOKES = {
            "BIT THE DUST",
            "A TOTAL BORE",
            "SHAFTED",
            "OUT OF BITS",
            "DEAD BOLT",
            "ROCK BOTTOM",
            "CORE BLIMEY",
            "NO MORE BITS",
            "DRILLED OUT",
            "GROUNDED",
            "UNDER PRESSURE",
            "BLUNT FORCE",
            "BOLT AND GONE",
            "TUNNEL VISION",
            "IT WAS BORED",
            "HARD STOP",
            "SPIN CYCLE",
            "MOTOR? HARDLY.",
            "WE HIT A SNAG",
            "BIT PART OVER",
    };

    public List<Upgrades.RunUpgrade> choices = new ArrayList<>();
    public Run.Summary lastSummary;
    public List<Integer> lastLevelUps = new ArrayList<>();
    public boolean lastNewBest;
    public float resetConfirmTimer;
    public String pendingMode = "zones";

    private String inputMode = "zones";
    private final HashMap<Integer, Integer> zones = new HashMap<>();
    private int dragId = -1;
    private float dragStartX;
    private float dragSteer;
    private float steer;
    private float deathTimer;
    private float saveAcc;
    private float hudAcc;
    private float tiltRaw;
    private boolean tiltSeen;
    private float tiltBase;
    private float lastToastAt = -10f;

    private final Vibrator vibrator;
    private final Sfx sfx;

    public Game(Context context, int width, int height) {
        this.context = context;
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        this.save = SaveData.load(context);
        this.inputMode = save.inputMode != null ? save.inputMode : "zones";
        this.pendingMode = inputMode;
        this.worldRenderer = new WorldRenderer(kit);
        this.uiRenderer = new UiRenderer(kit);
        this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        this.sfx = new Sfx(context);
        this.sfx.setEnabled(save.sound);
        this.mines.onExplode = (x, y, r) -> {
            addShake(0.8f);
            flash = Math.min(1f, flash + 0.7f);
            vibrate(80);
            sfx.play(Sfx.EXPLODE, 0.9f, 1f);
        };
    }

    public void release() {
        sfx.release();
    }

    // ------------------------------------------------------------------ state
    public boolean isMenu() {
        return state == MENU;
    }

    public String inputMode() {
        return inputMode;
    }

    public int seedCodeSeed() {
        return run != null ? run.seed : seed;
    }

    public void onViewport(int w, int h) {
        width = Math.max(1, w);
        height = Math.max(1, h);
    }

    public void setTilt(float raw, boolean seen) {
        tiltRaw = raw;
        tiltSeen = seen;
    }

    public void addShake(float amount) {
        if (save.reduceFx) amount *= 0.35f;
        shake = Math.min(1.6f, Math.max(shake, amount));
    }

    public void addHit(String id, float l, float t, float r, float b) {
        hits.add(new Hit(id, l, t, r, b));
    }

    private String hitTest(float x, float y) {
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (x >= h.l && x <= h.r && y >= h.t && y <= h.b) return h.id;
        }
        return null;
    }

    // ------------------------------------------------------------------ input
    public void onInput(int type, int id, float x, float y) {
        if (type == GameView.EV_BACK) {
            onBack();
            return;
        }
        if (type == GameView.EV_APP_PAUSE) {
            if (state == PLAYING) pauseGame();
            else if (state == MILESTONE) saveCheckpoint();
            return;
        }

        if (state == PLAYING) {
            if (type == GameView.EV_DOWN) {
                String hit = hitTest(x, y);
                if ("boost".equals(hit)) {
                    boostPointer = id;
                    thrustHeld = true;
                    return;
                }
                if (hit != null) {
                    onAction(hit);
                    return;
                }
                steerDown(id, x);
            } else if (type == GameView.EV_MOVE) {
                if (id == boostPointer) return;
                steerMove(id, x);
            } else if (type == GameView.EV_UP) {
                if (id == boostPointer) {
                    boostPointer = -1;
                    thrustHeld = false;
                    return;
                }
                steerUp(id);
            }
            return;
        }

        if (type == GameView.EV_DOWN) {
            String hit = hitTest(x, y);
            if (hit != null) onAction(hit);
        }
    }

    private void steerDown(int id, float x) {
        if ("zones".equals(inputMode)) {
            zones.put(id, x < width / 2f ? -1 : 1);
        } else if ("drag".equals(inputMode) && dragId < 0) {
            dragId = id;
            dragStartX = x;
            dragSteer = 0f;
        }
    }

    private void steerMove(int id, float x) {
        if ("zones".equals(inputMode) && zones.containsKey(id)) {
            zones.put(id, x < width / 2f ? -1 : 1);
        } else if ("drag".equals(inputMode) && id == dragId) {
            float dead = Config.UI_UNIT_REFERENCE * 0.012f * (width / Config.UI_UNIT_REFERENCE);
            float delta = x - dragStartX;
            float threshold = width * 0.14f;
            if (Math.abs(delta) <= dead) dragSteer = 0f;
            else dragSteer = Config.clamp((delta - Math.signum(delta) * dead) / threshold, -1f, 1f);
        }
    }

    private void steerUp(int id) {
        zones.remove(id);
        if (id == dragId) {
            dragId = -1;
            dragSteer = 0f;
        }
    }

    private float computeSteer() {
        if ("tilt".equals(inputMode)) {
            float delta = tiltRaw - tiltBase;
            if (Math.abs(delta) < 0.45f) return 0f;
            return Config.clamp((delta - Math.signum(delta) * 0.45f) / 3.2f, -1f, 1f);
        }
        if ("drag".equals(inputMode)) return dragSteer;
        int sum = 0;
        for (int v : zones.values()) sum += v;
        return Config.clamp(sum, -1f, 1f);
    }

    private void clearTouches() {
        zones.clear();
        dragId = -1;
        dragSteer = 0f;
        boostPointer = -1;
        thrustHeld = false;
    }

    private void onBack() {
        if (state == PLAYING) pauseGame();
        else if (state == PAUSED) quitToMenu();
        else if (state == MILESTONE) {
            // Do not skip a milestone by accident; ignore back here.
        } else if (state == DEAD) {
            state = MENU;
        } else if (state == SURFACE) {
            state = MENU;
        } else if (state == WORKSHOP || state == SETTINGS || state == CHOOSE_INPUT) {
            state = MENU;
        }
    }

    // ----------------------------------------------------------------- action
    private void onAction(String id) {
        sfx.play(Sfx.CLICK, 0.5f, 1f);
        if (id.startsWith("buy:")) {
            buyPerm(id.substring(4));
            return;
        }
        if (id.startsWith("upgrade:")) {
            pickUpgrade(id.substring(8));
            return;
        }
        if (id.startsWith("chip:")) {
            pendingMode = id.substring(5);
            return;
        }
        if (id.startsWith("mode:")) {
            changeMode(id.substring(5));
            return;
        }
        switch (id) {
            case "play":
                if (save.inputMode == null) {
                    pendingMode = "zones";
                    state = CHOOSE_INPUT;
                } else {
                    inputMode = save.inputMode;
                    startRun(Rng.makeSeed(), null);
                }
                break;
            case "resume":
                if (save.checkpoint != null) startRun(save.checkpoint.seed, save.checkpoint);
                break;
            case "start_drill":
                if (state == SURFACE) {
                    state = PLAYING;
                    saveAcc = 0f;
                    sfx.play(Sfx.COIN, 0.7f, 0.8f);
                }
                break;
            case "hub":
                state = WORKSHOP;
                break;
            case "settings":
                state = SETTINGS;
                break;
            case "pause":
                pauseGame();
                break;
            case "resume_game":
                if (state == PAUSED) {
                    state = PLAYING;
                    saveAcc = 0f;
                }
                break;
            case "restart":
                if (run != null) startRun(run.seed, null);
                break;
            case "quit":
                quitToMenu();
                break;
            case "retry":
                startRun(Rng.makeSeed(), null);
                break;
            case "menu":
                state = MENU;
                break;
            case "back":
                state = MENU;
                break;
            case "input_start":
                save.inputMode = pendingMode;
                save.save(context);
                inputMode = pendingMode;
                calibrateTilt();
                startRun(Rng.makeSeed(), null);
                break;
            case "toggle_vibrate":
                save.vibrate = !save.vibrate;
                save.save(context);
                break;
            case "toggle_fx":
                save.reduceFx = !save.reduceFx;
                save.save(context);
                break;
            case "toggle_sound":
                save.sound = !save.sound;
                sfx.setEnabled(save.sound);
                save.save(context);
                if (save.sound) sfx.play(Sfx.COIN, 0.7f, 1f);
                break;
            case "reset":
                if (resetConfirmTimer > 0f) {
                    resetConfirmTimer = 0f;
                    save.reset(context);
                    pendingMode = "zones";
                    inputMode = "zones";
                    toast("PROGRESS RESET");
                } else {
                    resetConfirmTimer = 3.5f;
                    toast("TAP AGAIN TO CONFIRM");
                }
                break;
            default:
                break;
        }
    }

    private void calibrateTilt() {
        tiltBase = tiltSeen ? tiltRaw : 0f;
    }

    private void changeMode(String mode) {
        inputMode = mode;
        save.inputMode = mode;
        save.save(context);
        pendingMode = mode;
        clearTouches();
        calibrateTilt();
        toast("STEERING: " + mode.toUpperCase());
    }

    private void buyPerm(String id) {
        if (Upgrades.buyPerm(save, id)) {
            save.save(context);
            toast("PURCHASED");
            sfx.play(Sfx.COIN, 0.85f, 1f);
        } else {
            toast("NEED MORE ORE");
        }
    }

    // ------------------------------------------------------------------ runs
    private void startRun(int newSeed, SaveData.Checkpoint cp) {
        seed = newSeed;
        particles.clear();
        mines.reset();
        terrain = new Terrain(new Generator(seed));
        terrain.onChunk = c -> mines.addChunk(c);
        terrain.onReaction = (x, y, kind) -> {
            if ("steam".equals(kind)) {
                particles.burst(x + 0.5f, y + 0.5f, 5, Particles.SMOKE, 0xFFCFD6DF, 7.5f, 0.8f, 0.55f, -9f);
            }
        };
        run = new Run(seed, save, cp);
        drill = new Drill(terrain);
        drill.spawn(run.startX, run.startDepth, run.stats);
        hookDrill();
        terrain.preload((int) drill.y);
        if (cp != null) prepareResume();
        camY = drill.y;
        clearTouches();
        state = cp != null ? PLAYING : SURFACE;
        flash = 0f;
        shake = 0f;
        damageFlash = 0f;
        boostFuel = run.stats.boostCapacity;
        deathJoke = "DRILL LOST";
        saveAcc = 0f;
        hudAcc = 0f;
        toastTimer = 0f;
        if (cp != null) toast("RESUMED AT " + run.currentDepthMeters() + " m");
    }

    private void hookDrill() {
        drill.onDig = (mat, x, y) -> {
            Materials.M m = Materials.get(mat);
            sfx.play(m.hardness > 1.2f ? Sfx.DIG_HARD : Sfx.DIG_SOFT, 0.62f,
                    0.9f + rng.nextFloat() * 0.2f);
            particles.burst(x + 0.5f, y + 0.5f, m.hardness > 1.2f ? 6 : 3,
                    Particles.DUST, m.dust, 7.8f + m.hardness * 5.4f, 0.45f, 0.5f, 42f);
            Run.OreGain ore = run.addOre(mat);
            if (ore == null) return;
            sfx.play(mat == Materials.DIAMOND || mat == Materials.GEM ? Sfx.GEM : Sfx.ORE, 0.75f, 1f);
            particles.text(x + 0.5f, y + 0.45f, "+" + ore.money, ore.color, 1.3f);
            particles.burst(x + 0.5f, y + 0.5f, 5, Particles.SPARK, ore.color, 12f, 0.4f, 0.36f, 12f);
            vibrate(7);
            if ((mat == Materials.DIAMOND || mat == Materials.GEM) && run.elapsed - lastToastAt > 3f) {
                lastToastAt = run.elapsed;
                toast(m.name.toUpperCase() + "  +" + ore.money + " ORE");
            }
            if (run.stats.magnet > 0f) magnetCollect(x, y);
        };
        drill.onImpact = (speed, mat, x, y) -> {
            Materials.M m = Materials.get(mat);
            particles.burst(x, y, 7, Particles.SPARK, m.accent, 15f, 0.4f, 0.4f, 24f);
            sfx.play(Sfx.IMPACT, 0.7f, 0.92f + rng.nextFloat() * 0.16f);
            vibrate(18);
        };
        drill.onDamage = (amount, source) -> {
            if (amount <= 0f) return;
            damageFlash = 1f;
            if ("mine".equals(source)) {
                drill.stunTimer = Config.MINE_STUN;
                addShake(0.5f);
            }
            if ("lava".equals(source)) vibrate(30);
        };
        drill.onLiquid = (kind, x, y) -> {
            if ("water".equals(kind)) {
                particles.burst(x, y, 6, Particles.SMOKE, 0xFFBFE3FF, 9f, 0.6f, 0.42f, -18f);
                sfx.play(Sfx.WATER, 0.7f, 1f);
            } else if ("lava".equals(kind)) {
                addShake(0.55f);
                sfx.play(Sfx.LAVA, 0.85f, 1f);
            }
        };
    }

    private void prepareResume() {
        int px = (int) Math.floor(drill.x);
        int py = (int) Math.floor(drill.y);
        terrain.clearPocket(px, py, 7f);
        mines.clearNear(px, py, 18f);
        drill.vx = 0f;
        drill.vy = 2f;
        camY = drill.y;
    }

    private void magnetCollect(int x, int y) {
        int r = (int) Math.ceil(run.stats.magnet);
        int picked = 0;
        for (int dy = -r; dy <= r && picked < 6; dy++) {
            for (int dx = -r; dx <= r && picked < 6; dx++) {
                if (dx == 0 && dy == 0) continue;
                if (dx * dx + dy * dy > run.stats.magnet * run.stats.magnet + 9f) continue;
                int id = terrain.get(x + dx, y + dy);
                if (!Materials.isOre(id)) continue;
                terrain.set(x + dx, y + dy, Materials.EMPTY);
                Run.OreGain ore = run.addOre(id);
                if (ore != null) {
                    particles.text(x + dx + 0.5f, y + dy, "+" + ore.money, ore.color, 1.1f);
                    picked++;
                }
            }
        }
    }

    private void updatePlaying(float dt) {
        steer = computeSteer();

        boolean stunned = drill.stunTimer > 0f;
        boolean canBoost = thrustHeld && !stunned && boostFuel > 0.001f;
        if (canBoost) {
            boostFuel = Math.max(0f, boostFuel - dt);
            drill.thrusting = true;
            drill.thrustPower = run.stats.boostPower;
            if (rng.nextFloat() < 0.8f) {
                float bx = drill.x + (rng.nextFloat() - 0.5f) * 1.4f;
                float by = drill.y + drill.radius * 0.7f;
                particles.burst(bx, by, 1, Particles.SPARK,
                        rng.nextBoolean() ? 0xFFBFE8FF : 0xFFFFC46B, 12f, 0.28f, 0.55f, 30f);
            }
            sfx.play(Sfx.THRUST, 0.55f, 0.95f + rng.nextFloat() * 0.1f);
        } else {
            drill.thrusting = false;
            boostFuel = Math.min(run.stats.boostCapacity,
                    boostFuel + dt * Config.BOOST_RECHARGE);
        }

        terrain.ensureAround(drill.x, drill.y, Config.CHUNKS_PER_FRAME);
        drill.update(dt, steer);
        terrain.cull(drill.y);
        terrain.stepFluids(dt, (int) drill.x, (int) drill.y);
        mines.update(dt, drill, terrain, particles);
        run.update(dt, drill.y);
        particles.update(dt);

        float target = drill.y + Math.min(Config.CAMERA_LOOKAHEAD,
                Math.max(0f, drill.vy) * 0.08f);
        camY += (target - camY) * (1f - (float) Math.exp(-Config.CAMERA_SMOOTH * dt));

        hudAcc += dt;
        saveAcc += dt;
        if (saveAcc >= 15f) {
            saveAcc = 0f;
            saveCheckpoint();
        }
        if (run.checkMilestone(drill.y)) {
            triggerMilestone();
            return;
        }
        if (drill.dead) die();
    }

    private void updateDying(float dt) {
        deathTimer -= dt;
        particles.update(dt);
        terrain.stepFluids(dt, (int) drill.x, (int) drill.y);
        camY += (drill.y - camY) * (1f - (float) Math.exp(-6f * dt));
        if (deathTimer <= 0f) finishRun();
    }

    private void triggerMilestone() {
        run.advanceMilestone();
        choices = run.rollChoices(new Random(run.seed ^ (run.milestone * 0x9E3779B1)));
        saveCheckpoint();
        if (choices.isEmpty()) {
            state = PLAYING;
            return;
        }
        state = MILESTONE;
        clearTouches();
    }

    private void pickUpgrade(String id) {
        Upgrades.RunUpgrade def = null;
        for (Upgrades.RunUpgrade u : choices) if (u.id.equals(id)) def = u;
        if (def != null) run.applyUpgrade(def, drill);
        choices = new ArrayList<>();
        state = PLAYING;
        saveCheckpoint();
        sfx.play(Sfx.UPGRADE, 0.8f, 1f);
        if (def != null) toast(def.name.toUpperCase());
    }

    private void die() {
        state = DYING;
        deathTimer = 1.0f;
        clearTouches();
        addShake(1.6f);
        flash = 1f;
        vibrate(140);
        sfx.play(Sfx.DEATH, 0.9f, 1f);
        particles.burst(drill.x, drill.y, 28, Particles.SPARK, 0xFFFFB347, 30f, 0.6f, 0.6f, 36f);
        particles.burst(drill.x, drill.y, 20, Particles.SMOKE, 0xFF4A4440, 15f, 1.1f, 1.05f, -15f);
        deathJoke = pickDeathJoke();
    }

    private String pickDeathJoke() {
        if (DEATH_JOKES.length == 1) return DEATH_JOKES[0];
        int idx;
        do {
            idx = rng.nextInt(DEATH_JOKES.length);
        } while (idx == lastJoke);
        lastJoke = idx;
        return DEATH_JOKES[idx];
    }

    private void finishRun() {
        lastSummary = run.summary();
        lastNewBest = lastSummary.depthMeters > save.bestDepth;
        lastLevelUps = save.applyRunResult(lastSummary);
        save.save(context);
        state = DEAD;
        clearTouches();
        if (!lastLevelUps.isEmpty()) sfx.play(Sfx.LEVEL, 0.85f, 1f);
    }

    private void pauseGame() {
        if (state != PLAYING) return;
        state = PAUSED;
        clearTouches();
        saveCheckpoint();
    }

    private void quitToMenu() {
        saveCheckpoint();
        state = MENU;
        clearTouches();
    }

    public void saveCheckpoint() {
        if (run == null || drill == null) return;
        if (run.currentDepthMeters() < 10) return;
        save.checkpoint = run.snapshot(drill);
        save.save(context);
    }

    public void toast(String text) {
        toastText = text;
        toastTimer = 1.9f;
    }

    private void vibrate(int ms) {
        if (!save.vibrate || vibrator == null || !vibrator.hasVibrator()) return;
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(ms);
            }
        } catch (Exception ignored) {
        }
    }

    // ------------------------------------------------------------------ frame
    public void update(float dt) {
        time += dt;
        if (toastTimer > 0f) toastTimer = Math.max(0f, toastTimer - dt);
        if (resetConfirmTimer > 0f) resetConfirmTimer = Math.max(0f, resetConfirmTimer - dt);
        flash = Math.max(0f, flash - dt * 3f);
        damageFlash = Math.max(0f, damageFlash - dt);
        shake *= (float) Math.exp(-10f * dt);

        if (state == PLAYING) updatePlaying(dt);
        else if (state == DYING) updateDying(dt);
    }

    public void draw(android.graphics.Canvas canvas, int w, int h, int safeTop, int safeBottom) {
        this.width = w;
        this.height = h;
        kit.setViewport(w, h);
        hits.clear();
        worldRenderer.draw(canvas, this, w, h, safeTop, safeBottom);
        uiRenderer.draw(canvas, this, w, h, safeTop, safeBottom);
    }
}

