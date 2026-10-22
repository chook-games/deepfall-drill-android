package com.deepfalldrill.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public final class Upgrades {
    private Upgrades() {
    }

    public interface Effect {
        void apply(Stats s, Run r);
    }

    public interface PermEffect {
        void apply(Stats s, int level);
    }

    public static final class RunUpgrade {
        public final String id;
        public final String name;
        public final String desc;
        public final String rarity;
        public final int max;
        public final float heal;
        public final Effect effect;

        RunUpgrade(String id, String name, String desc, String rarity, int max, float heal, Effect effect) {
            this.id = id;
            this.name = name;
            this.desc = desc;
            this.rarity = rarity;
            this.max = max;
            this.heal = heal;
            this.effect = effect;
        }
    }

    public static final class PermUpgrade {
        public final String id;
        public final String name;
        public final String desc;
        public final int max;
        public final PermEffect effect;

        PermUpgrade(String id, String name, String desc, int max, PermEffect effect) {
            this.id = id;
            this.name = name;
            this.desc = desc;
            this.max = max;
            this.effect = effect;
        }

        public int cost(int level) {
            float base;
            float growth;
            switch (id) {
                case "phull": base = 80; growth = 1.50f; break;
                case "pbit": base = 110; growth = 1.52f; break;
                case "pshield": base = 95; growth = 1.50f; break;
                case "prefine": base = 100; growth = 1.55f; break;
                case "pgyro": base = 90; growth = 1.48f; break;
                case "pboost": base = 100; growth = 1.50f; break;
                default: base = 85; growth = 1.45f; break;
            }
            return Math.round((base * (float) Math.pow(growth, level)) / 5f) * 5;
        }
    }

    public static final RunUpgrade[] RUN = new RunUpgrade[]{
            new RunUpgrade("gyro", "Gyro Assist", "+20% steering authority", "common", 4, 0f,
                    (s, r) -> s.steerMul *= 1.2f),
            new RunUpgrade("plating", "Reinforced Plating", "-22% impact damage", "uncommon", 4, 0f,
                    (s, r) -> s.armor = Math.min(0.78f, s.armor + 0.22f)),
            new RunUpgrade("blast", "Blast Shield", "-30% mine damage", "uncommon", 3, 0f,
                    (s, r) -> s.mineResist = Math.min(0.8f, s.mineResist + 0.3f)),
            new RunUpgrade("bit", "Diamond Bit", "+18% dig power", "common", 5, 0f,
                    (s, r) -> s.digPower *= 1.18f),
            new RunUpgrade("hull", "Hull Patch", "+18 max hull, repaired", "common", 5, 18f,
                    (s, r) -> s.maxHp += 18f),
            new RunUpgrade("magnet", "Greed Magnet", "+35% ore value, pulls nearby ore", "uncommon", 3, 0f,
                    (s, r) -> {
                        s.oreMul *= 1.35f;
                        s.magnet += 4.8f;
                    }),
            new RunUpgrade("brakes", "Aero Brakes", "-12% max fall speed, easier control", "rare", 2, 0f,
                    (s, r) -> s.fallMul *= 0.88f),
            new RunUpgrade("lens", "Prospector Lens", "+25% XP from ore", "common", 3, 0f,
                    (s, r) -> s.xpMul *= 1.25f),
            new RunUpgrade("thrusters", "Reverse Thrusters", "+0.6s boost tank, +12% thrust", "uncommon", 3, 0f,
                    (s, r) -> {
                        s.boostCapacity += 0.6f;
                        s.boostPower *= 1.12f;
                    }),
            new RunUpgrade("repair", "Emergency Repair", "Repair 45 hull immediately", "rare", 99, 45f,
                    (s, r) -> {
                    }),
    };

    public static final PermUpgrade[] PERM = new PermUpgrade[]{
            new PermUpgrade("phull", "Reinforced Hull", "+10 starting hull per level", 10,
                    (s, l) -> s.maxHp += 10f * l),
            new PermUpgrade("pbit", "Tungsten Bit", "+7% dig power per level", 8,
                    (s, l) -> s.digPower *= 1f + 0.07f * l),
            new PermUpgrade("pshield", "Blast Shield", "-6% mine damage per level", 8,
                    (s, l) -> s.mineResist = Math.min(0.7f, s.mineResist + 0.06f * l)),
            new PermUpgrade("prefine", "Ore Refinery", "+9% ore value per level", 8,
                    (s, l) -> s.oreMul *= 1f + 0.09f * l),
            new PermUpgrade("pgyro", "Stabilizers", "+6% steering authority per level", 8,
                    (s, l) -> s.steerMul *= 1f + 0.06f * l),
            new PermUpgrade("pboost", "Thruster Tuning", "+0.15s tank and +8% thrust per level", 8,
                    (s, l) -> {
                        s.boostCapacity += 0.15f * l;
                        s.boostPower *= 1f + 0.08f * l;
                    }),
    };

    public static RunUpgrade runById(String id) {
        for (RunUpgrade u : RUN) if (u.id.equals(id)) return u;
        return null;
    }

    public static PermUpgrade permById(String id) {
        for (PermUpgrade u : PERM) if (u.id.equals(id)) return u;
        return null;
    }

    public static int rarityColor(String rarity) {
        if ("uncommon".equals(rarity)) return 0xFF63D2A0;
        if ("rare".equals(rarity)) return 0xFFC98BFF;
        return 0xFF9FB4C7;
    }

    public static List<RunUpgrade> roll(Random rng, HashMap<String, Integer> owned, int count) {
        List<RunUpgrade> pool = new ArrayList<>();
        for (RunUpgrade u : RUN) {
            int have = owned.containsKey(u.id) ? owned.get(u.id) : 0;
            if (have < u.max) pool.add(u);
        }
        List<RunUpgrade> picks = new ArrayList<>();
        while (picks.size() < count && !pool.isEmpty()) {
            int total = 0;
            for (RunUpgrade u : pool) total += weight(u.rarity);
            int roll = rng.nextInt(Math.max(1, total));
            int acc = 0;
            int index = 0;
            for (int i = 0; i < pool.size(); i++) {
                acc += weight(pool.get(i).rarity);
                if (roll < acc) {
                    index = i;
                    break;
                }
            }
            picks.add(pool.remove(index));
        }
        return picks;
    }

    private static int weight(String rarity) {
        if ("uncommon".equals(rarity)) return 30;
        if ("rare".equals(rarity)) return 12;
        return 58;
    }

    public static Stats baseStats(SaveData meta) {
        Stats s = new Stats();
        s.maxHp = Config.START_HP + (Math.max(1, meta.level) - 1) * 3f;
        for (PermUpgrade u : PERM) {
            int lvl = Math.min(u.max, meta.perm.getOrDefault(u.id, 0));
            if (lvl > 0) u.effect.apply(s, lvl);
        }
        return s;
    }

    public static int xpForLevel(int level) {
        return Math.round(70f * (float) Math.pow(1.42, level - 1));
    }

    public static boolean buyPerm(SaveData meta, String id) {
        PermUpgrade u = permById(id);
        if (u == null) return false;
        int lvl = meta.perm.getOrDefault(id, 0);
        if (lvl >= u.max) return false;
        int cost = u.cost(lvl);
        if (meta.money < cost) return false;
        meta.money -= cost;
        meta.perm.put(id, lvl + 1);
        return true;
    }

    /** Returns the list of levels gained. */
    public static List<Integer> grantXp(SaveData meta, int amount) {
        List<Integer> levels = new ArrayList<>();
        meta.xp += Math.max(0, amount);
        while (meta.xp >= xpForLevel(meta.level)) {
            meta.xp -= xpForLevel(meta.level);
            meta.level++;
            levels.add(meta.level);
        }
        return levels;
    }
}
