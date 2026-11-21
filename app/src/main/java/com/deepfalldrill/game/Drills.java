package com.deepfalldrill.game;

/** Pickable drill hulls. They change how a run feels, not just numbers. */
public final class Drills {
    private Drills() {
    }

    public static final class D {
        public final String id;
        public final String name;
        public final String desc;
        public final int cost;
        public final int colors;

        D(String id, String name, String desc, int cost, int colors) {
            this.id = id;
            this.name = name;
            this.desc = desc;
            this.cost = cost;
            this.colors = colors;
        }
    }

    public static final D[] ALL = new D[]{
            new D("standard", "Standard", "Balanced hull, dig and steering", 0, 0xFF7D8694),
            new D("brick", "The Brick", "+50 hull, -18% steering, -8% dig", 2500, 0xFF9A6B45),
            new D("needle", "The Needle", "+18% dig, +8% steering, -20 hull", 3500, 0xFF9FB6C9),
    };

    public static D byId(String id) {
        for (D d : ALL) if (d.id.equals(id)) return d;
        return ALL[0];
    }

    public static boolean isUnlocked(SaveData meta, String id) {
        return "standard".equals(id) || meta.unlockedDrills.contains(id);
    }

    /** Returns true when selected or successfully purchased. */
    public static boolean selectOrBuy(SaveData meta, String id) {
        D def = byId(id);
        if (isUnlocked(meta, def.id)) {
            meta.drillId = def.id;
            return true;
        }
        if (meta.money < def.cost) return false;
        meta.money -= def.cost;
        meta.unlockedDrills.add(def.id);
        meta.drillId = def.id;
        return true;
    }

    public static void applyStats(Stats s, String id) {
        switch (byId(id).id) {
            case "brick":
                s.maxHp += 50f;
                s.steerMul *= 0.82f;
                s.digPower *= 0.92f;
                break;
            case "needle":
                s.maxHp -= 20f;
                s.digPower *= 1.18f;
                s.steerMul *= 1.08f;
                break;
            default:
                break;
        }
    }
}
