package com.deepfalldrill.game;

import java.util.ArrayList;

/** Embedded power-up pickups. Touch one to collect it. */
public final class Pickups {
    public static final int BOOST = 0;
    public static final int REPAIR = 1;
    public static final int OVERDRIVE = 2;
    public static final int MAGNET = 3;
    public static final int ARMOR = 4;

    public interface CollectListener {
        void onCollect(int kind, float x, float y);
    }

    public static final class Pickup {
        public float x;
        public float y;
        public int kind;
        public float phase;
        public boolean dead;
    }

    public final ArrayList<Pickup> list = new ArrayList<>();
    public CollectListener onCollect;

    public void reset() {
        list.clear();
    }

    public void addChunk(Terrain.Chunk c) {
        for (int[] p : c.pickups) {
            Pickup u = new Pickup();
            u.x = p[0] + 0.5f;
            u.y = p[1] + 0.5f;
            u.kind = p[2];
            u.phase = (float) (Math.random() * Math.PI * 2);
            list.add(u);
        }
    }

    public void clearNear(float px, float py, float radius) {
        for (int i = list.size() - 1; i >= 0; i--) {
            Pickup p = list.get(i);
            if (Math.hypot(p.x - px, p.y - py) < radius) list.remove(i);
        }
    }

    public void update(float dt, Drill drill) {
        for (int i = list.size() - 1; i >= 0; i--) {
            Pickup p = list.get(i);
            if (p.dead || p.y < drill.y - 180f) {
                list.remove(i);
                continue;
            }
            float dx = p.x - drill.x;
            float dy = p.y - drill.y;
            if (dx * dx + dy * dy < (drill.radius + 1.2f) * (drill.radius + 1.2f)) {
                p.dead = true;
                list.remove(i);
                if (onCollect != null) onCollect.onCollect(p.kind, p.x, p.y);
            }
        }
    }

    public static String name(int kind) {
        switch (kind) {
            case BOOST: return "BOOST TANK";
            case REPAIR: return "REPAIR KIT";
            case OVERDRIVE: return "OVERDRIVE";
            case MAGNET: return "ORE PULSE";
            default: return "SHIELD";
        }
    }

    public static int color(int kind) {
        switch (kind) {
            case BOOST: return 0xFF7FE8FF;
            case REPAIR: return 0xFF4FD18B;
            case OVERDRIVE: return 0xFFFFA03A;
            case MAGNET: return 0xFFE07BFF;
            default: return 0xFF6FA8FF;
        }
    }
}
