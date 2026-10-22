package com.deepfalldrill.game;

/**
 * The player. Always pulled down; steering only. Collision is a circle against
 * cell rectangles in small substeps, so the drill can never ghost through
 * terrain: velocity into a solid is cancelled, and the drill only advances when
 * cells are actually destroyed.
 */
public final class Drill {
    public interface DigListener {
        void onDig(int material, int x, int y);
    }

    public interface ImpactListener {
        void onImpact(float speed, int material, float x, float y);
    }

    public interface DamageListener {
        void onDamage(float amount, String source);
    }

    public interface LiquidListener {
        void onLiquid(String kind, float x, float y);
    }

    public final Terrain terrain;
    public final float radius = Config.DRILL_RADIUS;

    public float x = 0f;
    public float y = 0f;
    public float vx = 0f;
    public float vy = 0f;
    public float angle = 0f;
    public float hp;
    public float maxHp;
    public float steer;
    public boolean dead;

    public boolean inLava;
    public boolean inWater;
    public boolean floorContact;
    public boolean wallContact;
    public boolean thrusting;
    public float thrustPower = Config.BOOST_POWER;
    public float stunTimer;
    public float contactTimer;
    public float impactTimer;
    public int digCount;
    public int digMaterial = Materials.EMPTY;

    private Stats stats;
    private float digEnergy;
    private int dugThisFrame;
    private final int[] hitX = new int[64];
    private final int[] hitY = new int[64];
    private final int[] hitId = new int[64];
    private int hitCount;
    private final int[] order = new int[64];

    public DigListener onDig;
    public ImpactListener onImpact;
    public DamageListener onDamage;
    public LiquidListener onLiquid;

    public Drill(Terrain terrain) {
        this.terrain = terrain;
    }

    public void spawn(float sx, float sy, Stats stats) {
        this.x = sx;
        this.y = sy;
        this.vx = 0f;
        this.vy = 0f;
        this.angle = 0f;
        this.stats = stats;
        this.maxHp = stats.maxHp;
        this.hp = stats.hp > 0 ? Math.min(stats.hp, stats.maxHp) : stats.maxHp;
        this.dead = false;
        this.contactTimer = 0f;
        this.digEnergy = 0f;
        this.impactTimer = 0f;
        this.inLava = false;
        this.inWater = false;
        this.digCount = 0;
        this.floorContact = false;
        this.wallContact = false;
        this.thrusting = false;
        this.stunTimer = 0f;
    }

    public void setStats(Stats s) {
        this.stats = s;
        this.maxHp = s.maxHp;
        this.hp = Math.min(this.hp, this.maxHp);
    }

    public Stats stats() {
        return stats;
    }

    public void applyDamage(float amount, String source) {
        if (dead || amount <= 0f) return;
        hp = Math.max(0f, hp - amount);
        if (onDamage != null) onDamage.onDamage(amount, source);
        if (hp <= 0f) dead = true;
    }

    public float digRateFor(int id) {
        Materials.M m = Materials.get(id);
        if (!m.solid || !m.diggable) return 0f;
        float depthPenalty = 1f + Math.max(0f, y) / Config.DEPTH_HARDNESS;
        float hardness = Math.max(0.05f, m.hardness * depthPenalty);
        float power = stats != null ? stats.digPower : 1f;
        return Math.max(Config.DIG_MIN, (Config.DIG_BASE / hardness) * power);
    }

    public void update(float dt, float steerInput) {
        if (dead) return;
        if (stunTimer > 0f) {
            stunTimer = Math.max(0f, stunTimer - dt);
            steerInput = 0f;
            thrusting = false;
        }
        steer = Config.clamp(steerInput, -1f, 1f);
        impactTimer = Math.max(0f, impactTimer - dt);
        contactTimer = Math.max(0f, contactTimer - dt);
        dugThisFrame = 0;

        float maxFall = (inWater ? Config.WATER_MAX_FALL : Config.MAX_FALL)
                * (stats != null ? stats.fallMul : 1f);

        vy += Config.GRAVITY * dt;
        if (inWater) vy *= Math.exp(-Config.WATER_DRAG * dt);
        if (vy > maxFall) vy = maxFall;
        if (thrusting && stunTimer <= 0f) vy = -thrustPower;

        boolean grounded = contactTimer > 0f;
        float steerMax = Config.STEER_MAX * (stats != null ? stats.steerMul : 1f);
        float response = grounded ? Config.STEER_GROUND : Config.STEER_AIR;
        float target = steer * steerMax;
        vx += (target - vx) * (1f - Math.exp(-response * dt));
        if (Math.abs(steer) < 0.01f) {
            vx *= Math.exp(-(grounded ? 2.5f : Config.AIR_DRIFT) * dt);
        }

        float lean = Config.clamp(vx / Math.max(1f, steerMax), -1f, 1f) * 0.26f;
        angle += (lean - angle) * Math.min(1f, 9f * dt);

        float speed = (float) Math.hypot(vx, vy);
        int steps = Config.clampInt((int) Math.ceil((speed * dt) / 0.2f), 1, 14);
        float sdt = dt / steps;
        for (int i = 0; i < steps; i++) {
            x += vx * sdt;
            y += vy * sdt;
            float minX = Config.WORLD_LEFT + 1 + radius;
            float maxX = Config.WORLD_RIGHT - 1 - radius;
            if (x < minX) {
                x = minX;
                if (vx < 0) vx = 0;
            } else if (x > maxX) {
                x = maxX;
                if (vx > 0) vx = 0;
            }
            resolveStep(sdt);
        }

        updateLiquids(dt);
    }

    private void updateLiquids(float dt) {
        int cx = (int) Math.floor(x);
        int cy = (int) Math.floor(y);
        int scan = (int) Math.ceil(radius) + 1;
        boolean lava = false;
        boolean water = false;
        for (int dy = -scan; dy <= scan; dy++) {
            for (int dx = -scan; dx <= scan; dx++) {
                int id = terrain.get(cx + dx, cy + dy);
                if (id == Materials.LAVA) lava = true;
                else if (id == Materials.WATER) water = true;
            }
        }
        float resist = stats != null ? stats.lavaResist : 0f;

        if (lava) {
            if (!inLava) {
                applyDamage(Config.LAVA_ENTRY * (1f - resist), "lava");
                if (onLiquid != null) onLiquid.onLiquid("lava", x, y);
            }
            applyDamage(Config.LAVA_DPS * (1f - resist) * dt, "lava");
            // Lava is thick: it slows the descent hard.
            if (vy > Config.LAVA_SLOW) vy = Config.LAVA_SLOW;
            vx *= Math.exp(-2.5f * dt);
        }
        if (water) {
            if (!inWater && onLiquid != null) onLiquid.onLiquid("water", x, y);
        }
        inLava = lava;
        inWater = water;
    }

    /** Returns true if at least one cell was destroyed this substep. */
    private boolean resolveStep(float dt) {
        hitCount = 0;
        float r = radius;
        int x0 = (int) Math.floor(x - r);
        int x1 = (int) Math.floor(x + r);
        int y0 = (int) Math.floor(y - r);
        int y1 = (int) Math.floor(y + r);

        boolean floor = false;
        boolean wall = false;
        float aheadX = x + vx * 0.05f + steer * 1.0f;
        float aheadY = y + vy * 0.04f + 1.3f;

        for (int cy = y0; cy <= y1; cy++) {
            for (int cx = x0; cx <= x1; cx++) {
                int id = terrain.get(cx, cy);
                if (!Materials.isSolid(id)) continue;

                float px = Config.clamp(x, cx, cx + 1);
                float py = Config.clamp(y, cy, cy + 1);
                float dx = x - px;
                float dy = y - py;
                float d2 = dx * dx + dy * dy;
                if (d2 >= r * r) continue;

                float nx;
                float ny;
                if (d2 > 1e-6f) {
                    float inv = 1f / (float) Math.sqrt(d2);
                    nx = dx * inv;
                    ny = dy * inv;
                } else {
                    float ex = x - (cx + 0.5f);
                    float ey = y - (cy + 0.5f);
                    float len = (float) Math.hypot(ex, ey);
                    if (len < 1e-4f) len = 1f;
                    nx = ex / len;
                    ny = ey / len;
                }

                Materials.M m = Materials.get(id);
                float vn = vx * nx + vy * ny;
                if (vn < 0f) {
                    float impactSpeed = -vn;
                    if (impactSpeed > Config.IMPACT_THRESHOLD && impactTimer <= 0f && m.impact > 0f) {
                        float armor = stats != null ? stats.armor : 0f;
                        float dmg = (impactSpeed - Config.IMPACT_THRESHOLD)
                                * Config.IMPACT_DAMAGE * m.impact * (1f - armor);
                        impactTimer = Config.IMPACT_COOLDOWN;
                        applyDamage(dmg, "impact");
                        if (onImpact != null) onImpact.onImpact(impactSpeed, id, x, y);
                    }
                    vx -= nx * vn;
                    vy -= ny * vn;

                    if (Math.abs(nx) > 0.7f && Math.abs(vx) > 12f && vy > 33f && m.friction > 0f) {
                        vy *= Math.exp(-m.friction * dt);
                    }
                }

                if (ny < -0.35f) floor = true;
                if (Math.abs(nx) > 0.4f) wall = true;

                if (hitCount < hitX.length) {
                    hitX[hitCount] = cx;
                    hitY[hitCount] = cy;
                    hitId[hitCount] = id;
                    hitCount++;
                }
            }
        }

        floorContact = floor;
        wallContact = wall;
        if (hitCount == 0) return false;

        // Hitting a corner must not launch the drill sideways at fall speed.
        float steerCap = Config.STEER_MAX * (stats != null ? stats.steerMul : 1f);
        if (vx > steerCap) vx = steerCap;
        else if (vx < -steerCap) vx = -steerCap;
        if (floor) vx *= 0.55f;

        // Score the overlapping cells by how close they are to the drill's
        // intended direction, then cut them as long as there is dig energy.
        for (int i = 0; i < hitCount; i++) order[i] = i;
        for (int i = 1; i < hitCount; i++) {
            int key = order[i];
            float kx = hitX[key] + 0.5f - aheadX;
            float ky = hitY[key] + 0.5f - aheadY;
            float kScore = kx * kx + ky * ky + Materials.get(hitId[key]).hardness * 0.12f;
            int j = i - 1;
            while (j >= 0) {
                int p = order[j];
                float px = hitX[p] + 0.5f - aheadX;
                float py = hitY[p] + 0.5f - aheadY;
                float pScore = px * px + py * py + Materials.get(hitId[p]).hardness * 0.12f;
                if (pScore <= kScore) break;
                order[j + 1] = p;
                j--;
            }
            order[j + 1] = key;
        }

        int targetIndex = -1;
        for (int i = 0; i < hitCount; i++) {
            int idx = order[i];
            if (Materials.isDiggable(hitId[idx]) && hitY[idx] + 0.5f >= y - 0.15f) {
                targetIndex = idx;
                break;
            }
        }
        if (targetIndex < 0) return false; // bedrock only

        contactTimer = 0.12f;
        int bestId = hitId[targetIndex];
        float rate = digRateFor(bestId);

        // The drill feeds into the material it cuts. Hard rock slows descent.
        boolean cuttingDown = (hitY[targetIndex] + 0.5f) > y;
        if (!thrusting && (floor || cuttingDown)) {
            float terminal = (inWater ? Config.WATER_MAX_FALL : Config.MAX_FALL)
                    * (stats != null ? stats.fallMul : 1f);
            float ramp = Config.START_SPEED + (Config.MAX_DIG_SPEED - Config.START_SPEED)
                    * Config.clamp(y / Config.START_RAMP_CELLS, 0f, 1f);
            float feed = Math.min(rate / 2.0f + 2f, Math.min(terminal * 1.03f, ramp));
            vy = feed;
        }

        float power = stats != null ? stats.digPower : 1f;
        digEnergy = Math.min(3f, digEnergy + dt * Config.DIG_BASE * power);
        boolean dug = false;
        int guard = 0;
        while (digEnergy > 0f && guard++ < 10 && dugThisFrame < 14) {
            // Re-pick the best remaining cell: destroyed cells may reveal better
            // targets inside the same substep.
            int best = -1;
            float bestScore = Float.MAX_VALUE;
            for (int i = 0; i < hitCount; i++) {
                int idx = order[i];
                int id = hitId[idx];
                if (!Materials.isDiggable(id)) continue;
                if (hitY[idx] + 0.5f < y - 0.15f) continue;
                if (terrain.get(hitX[idx], hitY[idx]) != id) continue;
                float ex = hitX[idx] + 0.5f - aheadX;
                float ey = hitY[idx] + 0.5f - aheadY;
                float score = ex * ex + ey * ey + Materials.get(id).hardness * 0.12f;
                if (score < bestScore) {
                    bestScore = score;
                    best = idx;
                }
            }
            if (best < 0) break;

            int id = hitId[best];
            float cost = Materials.get(id).hardness * 0.55f;
            if (digEnergy < cost) break;

            int dugId = terrain.dig(hitX[best], hitY[best]);
            if (dugId == Materials.EMPTY) {
                digEnergy = 0f;
                break;
            }
            digEnergy -= cost;
            dugThisFrame++;
            dug = true;
            digCount++;
            digMaterial = dugId;
            vy *= 0.995f;
            if (onDig != null) onDig.onDig(dugId, hitX[best], hitY[best]);
        }
        return dug;
    }
}
