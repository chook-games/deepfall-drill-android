package com.deepfalldrill.game;

/** All gameplay tuning. One cell = 1/6 m. */
public final class Config {
    private Config() {
    }

    // Fixed camera. 48 columns fit across the screen; bedrock walls frame it.
    // Visible columns -24..23, leaving 46 playable columns.
    public static final int VISIBLE_COLS = 48;
    public static final int WORLD_LEFT = -24;
    public static final int WORLD_RIGHT = 24;
    public static final int CHUNK = 32;
    public static final float METERS_PER_CELL = 1f / 6f;

    // Surface start
    public static final int GROUND_Y = 0;
    public static final int SPAWN_X = 0;
    public static final int SPAWN_Y = -3;

    // Physics is scaled to the finer grid so screen speed stays the same.
    public static final float GRAVITY = 78f;
    public static final float MAX_FALL = 51f;
    public static final float STEER_MAX = 25.5f;
    public static final float STEER_GROUND = 54f;
    public static final float STEER_AIR = 13.2f;
    public static final float AIR_DRIFT = 2.7f;

    public static final float DIG_BASE = 54f;
    public static final float DIG_MIN = 3f;
    public static final float DEPTH_HARDNESS = 21000f;
    public static final float IMPACT_THRESHOLD = 42f;
    public static final float IMPACT_DAMAGE = 0f; // MVP: falling does not damage the hull
    public static final float IMPACT_COOLDOWN = 0.35f;

    public static final float DRILL_RADIUS = 1.15f;
    public static final int START_HP = 100;
    public static final float ANCHOR_Y = 0.59f;
    public static final float CAMERA_SMOOTH = 9f;
    public static final float CAMERA_LOOKAHEAD = 4.8f;

    public static final int MILESTONE_METERS = 250;
    public static final int CHUNKS_PER_FRAME = 3;
    public static final int CHUNK_CULL_ABOVE = 10;
    public static final int CHUNK_CULL_BELOW = 10;

    public static final float FLUID_TICK = 1f / 7f;
    public static final int FLUID_RADIUS = 30;
    public static final int FLUID_MAX_MOVES = 180;

    public static final int MINE_DAMAGE = 24;
    public static final float MINE_RADIUS = 7f;
    public static final int MINE_START_DEPTH = 270; // cells = 45 m

    public static final float LAVA_ENTRY = 10f;
    public static final float LAVA_DPS = 18f;
    public static final float LAVA_SLOW = 8f;
    public static final float WATER_DRAG = 3.4f;
    public static final float WATER_MAX_FALL = 24f;

    // Booster
    public static final float BOOST_CAPACITY = 2.0f;   // seconds of full thrust
    public static final float BOOST_POWER = 30f;      // cells/s upward
    public static final float BOOST_RECHARGE = 0.33f;  // fuel seconds per second
    public static final float START_RAMP_CELLS = 480f; // 0-70 m speed ramp
    public static final float START_SPEED = 14f;
    public static final float MAX_DIG_SPEED = 40f;
    public static final float MINE_STUN = 2f;

    public static final float UI_UNIT_REFERENCE = 390f;

    public static int meters(float cells) {
        int m = (int) (cells * METERS_PER_CELL);
        return Math.max(0, m);
    }

    public static float clamp(float v, float a, float b) {
        return v < a ? a : (v > b ? b : v);
    }

    public static int clampInt(int v, int a, int b) {
        return v < a ? a : (v > b ? b : v);
    }
}
