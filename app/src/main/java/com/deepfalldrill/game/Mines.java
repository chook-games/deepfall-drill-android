package com.deepfalldrill.game;

import java.util.ArrayList;
import java.util.Random;

/** Pressure mines. They blink before they can hurt you. */
public final class Mines {
    public interface ExplodeListener {
        void onExplode(float x, float y, float radius);
    }

    public static final class Mine {
        public float x;
        public float y;
        public float phase;
        public float fuse = -1f;
        public boolean armed;
        public boolean dead;
    }

    private static final float TRIGGER = 2.4f;
    private static final float ARM = 22f;

    public final ArrayList<Mine> list = new ArrayList<>();
    public ExplodeListener onExplode;

    public void reset() {
        list.clear();
    }

    public void addChunk(Terrain.Chunk c) {
        for (int[] m : c.mines) {
            Mine mine = new Mine();
            mine.x = m[0] + 0.5f;
            mine.y = m[1] + 0.5f;
            mine.phase = (float) (Math.random() * Math.PI * 2);
            list.add(mine);
        }
    }

    public void clearNear(float px, float py, float radius) {
        for (int i = list.size() - 1; i >= 0; i--) {
            Mine m = list.get(i);
            if (Math.hypot(m.x - px, m.y - py) < radius) list.remove(i);
        }
    }

    public void update(float dt, Drill drill, Terrain terrain, Particles particles) {
        for (int i = list.size() - 1; i >= 0; i--) {
            Mine m = list.get(i);
            if (m.dead || m.y < drill.y - 180f) {
                list.remove(i);
                continue;
            }
            if (m.fuse >= 0f) {
                m.fuse -= dt;
                if (m.fuse <= 0f) explode(m, drill, terrain, particles);
                continue;
            }
            float dx = m.x - drill.x;
            float dy = m.y - drill.y;
            float dist = (float) Math.hypot(dx, dy);
            m.armed = dist < ARM;
            if (dist < TRIGGER) m.fuse = 0.12f;
        }
    }

    private void explode(Mine mine, Drill drill, Terrain terrain, Particles particles) {
        if (mine.dead) return;
        mine.dead = true;
        float x = mine.x;
        float y = mine.y;
        float r = Config.MINE_RADIUS;
        float r2 = r * r;

        for (int cy = (int) Math.floor(y - r); cy <= (int) Math.floor(y + r); cy++) {
            for (int cx = (int) Math.floor(x - r); cx <= (int) Math.floor(x + r); cx++) {
                float dx = cx + 0.5f - x;
                float dy = cy + 0.5f - y;
                if (dx * dx + dy * dy > r2) continue;
                int id = terrain.get(cx, cy);
                if (id == Materials.LAVA) terrain.set(cx, cy, Materials.STONE);
                else if (Materials.isDiggable(id)) terrain.set(cx, cy, Materials.EMPTY);
            }
        }

        if (particles != null) {
            particles.burst(x, y, 24, Particles.SPARK, 0xFFFFB347, 24f, 0.45f, 0.6f, 18f);
            particles.burst(x, y, 16, Particles.SMOKE, 0xFF5A5050, 12f, 0.9f, 0.9f, -12f);
            particles.text(x, y - 1.2f, "BOOM", 0xFFFF8A3D, 1.4f);
        }

        float dx = drill.x - x;
        float dy = drill.y - y;
        float dist = (float) Math.hypot(dx, dy);
        if (dist < r + 0.4f) {
            float falloff = Math.max(0.25f, 1f - dist / (r + 0.4f));
            Stats st = drill.stats();
            float armor = st != null ? st.armor : 0f;
            float resist = st != null ? st.mineResist : 0f;
            drill.applyDamage(Config.MINE_DAMAGE * falloff * (1f - armor) * (1f - resist), "mine");
            float push = 18f * falloff;
            float len = dist > 0.001f ? dist : 1f;
            drill.vx += (dx / len) * push;
            drill.vy += (dy / len) * push - 9f;
        }

        if (onExplode != null) onExplode.onExplode(x, y, r);

        for (int i = 0; i < list.size(); i++) {
            Mine other = list.get(i);
            if (other == mine || other.dead || other.fuse >= 0f) continue;
            if (Math.hypot(other.x - x, other.y - y) < r + 1.2f) {
                other.fuse = 0.1f + new Random().nextFloat() * 0.12f;
            }
        }
    }
}
