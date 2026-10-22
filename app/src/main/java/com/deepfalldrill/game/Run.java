package com.deepfalldrill.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/** Everything that lives for exactly one descent. */
public final class Run {
    public static final class OreGain {
        public final int money;
        public final int xp;
        public final String name;
        public final int color;

        OreGain(int money, int xp, String name, int color) {
            this.money = money;
            this.xp = xp;
            this.name = name;
            this.color = color;
        }
    }

    public static final class Summary {
        public int depthMeters;
        public int money;
        public int xp;
        public int oreCount;
        public int score;
        public int seed;
    }

    public final int seed;
    public final SaveData meta;
    public final Stats stats;
    public final HashMap<Integer, Integer> ores = new HashMap<>();
    public final HashMap<String, Integer> upgrades = new HashMap<>();

    public int startDepth;
    public float startX;
    public int moneyEarned;
    public int xpEarned;
    public int oreCount;
    public int milestone;
    public float maxDepth;
    public float elapsed;
    public float hp;

    private Stats current;

    public Run(int seed, SaveData meta, SaveData.Checkpoint cp) {
        this.seed = seed;
        this.meta = meta;
        this.stats = Upgrades.baseStats(meta);
        this.current = stats;

        float snapshotHp = -1f;
        if (cp != null) {
            startDepth = cp.depth;
            startX = cp.x;
            hp = cp.hp;
            snapshotHp = cp.hp;
            moneyEarned = cp.moneyEarned;
            xpEarned = cp.xpEarned;
            oreCount = cp.oreCount;
            milestone = cp.milestone;
            ores.putAll(cp.ores);
            upgrades.putAll(cp.upgrades);
        } else {
            startDepth = Config.SPAWN_Y;
            startX = Config.SPAWN_X;
            milestone = Config.meters(Math.max(0, startDepth)) / Config.MILESTONE_METERS;
        }

        recompute();
        hp = snapshotHp > 0f ? Math.min(snapshotHp, stats.maxHp) : stats.maxHp;
        stats.hp = hp;
        maxDepth = startDepth;
    }

    public void recompute() {
        Stats s = Upgrades.baseStats(meta);
        for (HashMap.Entry<String, Integer> e : upgrades.entrySet()) {
            Upgrades.RunUpgrade def = Upgrades.runById(e.getKey());
            if (def == null) continue;
            for (int i = 0; i < e.getValue(); i++) def.effect.apply(s, this);
        }
        s.hp = hp;
        current = s;
        copyInto(stats, s);
    }

    private static void copyInto(Stats dst, Stats src) {
        dst.maxHp = src.maxHp;
        dst.hp = src.hp;
        dst.digPower = src.digPower;
        dst.steerMul = src.steerMul;
        dst.armor = src.armor;
        dst.lavaResist = src.lavaResist;
        dst.oreMul = src.oreMul;
        dst.xpMul = src.xpMul;
        dst.magnet = src.magnet;
        dst.fallMul = src.fallMul;
        dst.boostCapacity = src.boostCapacity;
        dst.boostPower = src.boostPower;
    }

    public OreGain addOre(int material) {
        Materials.M m = Materials.get(material);
        if (!m.ore || m.value <= 0) return null;
        int money = Math.max(1, Math.round(m.value * stats.oreMul));
        int xp = Math.max(1, Math.round(m.xp * stats.xpMul));
        moneyEarned += money;
        xpEarned += xp;
        oreCount++;
        ores.put(material, ores.getOrDefault(material, 0) + 1);
        return new OreGain(money, xp, m.name, m.accent);
    }

    public void update(float dt, float depthCells) {
        elapsed += dt;
        if (depthCells > maxDepth) maxDepth = depthCells;
    }

    public int currentDepthMeters() {
        return Config.meters(maxDepth);
    }

    public int nextMilestoneMeters() {
        return (milestone + 1) * Config.MILESTONE_METERS;
    }

    public boolean checkMilestone(float depthCells) {
        return Config.meters(depthCells) >= nextMilestoneMeters();
    }

    public void advanceMilestone() {
        milestone++;
    }

    public List<Upgrades.RunUpgrade> rollChoices(Random rng) {
        return Upgrades.roll(rng, upgrades, 3);
    }

    public boolean applyUpgrade(Upgrades.RunUpgrade def, Drill drill) {
        int have = upgrades.getOrDefault(def.id, 0);
        if (have >= def.max) return false;
        upgrades.put(def.id, have + 1);

        float before = stats.maxHp;
        recompute();
        float delta = stats.maxHp - before;
        if (drill != null) {
            drill.setStats(stats);
            if (delta > 0f) drill.hp = Math.min(drill.maxHp, drill.hp + delta);
            if (def.heal > 0f) drill.hp = Math.min(drill.maxHp, drill.hp + def.heal);
        }
        return true;
    }

    public int score() {
        return currentDepthMeters() * 10 + moneyEarned + oreCount * 8;
    }

    public Summary summary() {
        Summary s = new Summary();
        s.depthMeters = currentDepthMeters();
        s.money = moneyEarned;
        s.xp = xpEarned;
        s.oreCount = oreCount;
        s.score = score();
        s.seed = seed;
        return s;
    }

    public SaveData.Checkpoint snapshot(Drill drill) {
        SaveData.Checkpoint c = new SaveData.Checkpoint();
        c.seed = seed;
        c.x = drill.x;
        c.depth = (int) Math.floor(drill.y);
        c.hp = Math.max(1f, drill.hp);
        c.heat = 0f;
        c.moneyEarned = moneyEarned;
        c.xpEarned = xpEarned;
        c.oreCount = oreCount;
        c.milestone = milestone;
        for (HashMap.Entry<Integer, Integer> e : ores.entrySet()) c.ores.put(e.getKey(), e.getValue());
        for (HashMap.Entry<String, Integer> e : upgrades.entrySet()) c.upgrades.put(e.getKey(), e.getValue());
        return c;
    }

    public List<String> upgradeNames() {
        List<String> names = new ArrayList<>();
        for (HashMap.Entry<String, Integer> e : upgrades.entrySet()) {
            Upgrades.RunUpgrade def = Upgrades.runById(e.getKey());
            if (def != null) names.add(def.name + " x" + e.getValue());
        }
        return names;
    }
}
