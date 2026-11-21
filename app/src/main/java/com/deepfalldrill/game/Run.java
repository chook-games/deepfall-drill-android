package com.deepfalldrill.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/** Everything that lives for exactly one descent. */
public final class Run {
    public static final int CONTRACT_DEPTH = 0;
    public static final int CONTRACT_ORE = 1;
    public static final int CONTRACT_GOLD = 2;
    public static final int CONTRACT_GEM = 3;

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
        public boolean daily;
        public int salvage;
    }

    public final int seed;
    public final SaveData meta;
    public final Stats stats;
    public final HashMap<Integer, Integer> ores = new HashMap<>();
    public final HashMap<String, Integer> upgrades = new HashMap<>();
    public final boolean daily;
    public final String drillId;

    public int startDepth;
    public float startX;
    public int moneyEarned;
    public int xpEarned;
    public int oreCount;
    public int milestone;
    public float maxDepth;
    public float elapsed;
    public float hp;

    public int contractType;
    public int contractTarget;
    public int contractProgress;
    public boolean contractDone;
    public int contractReward;
    public int contractXp;

    private Stats current;

    public Run(int seed, SaveData meta, SaveData.Checkpoint cp, boolean daily) {
        this.seed = seed;
        this.meta = meta;
        this.daily = cp != null ? cp.daily : daily;
        this.drillId = cp != null && cp.drillId != null
                ? cp.drillId : (meta.drillId != null ? meta.drillId : "standard");
        this.stats = Upgrades.baseStats(meta, drillId);
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
            contractType = cp.contractType;
            contractTarget = cp.contractTarget;
            contractProgress = cp.contractProgress;
            contractDone = cp.contractDone;
        } else {
            startDepth = Config.SPAWN_Y;
            startX = Config.SPAWN_X;
            milestone = Config.meters(Math.max(0, startDepth)) / Config.MILESTONE_METERS;
        }

        if (contractTarget <= 0) pickContract();
        else setContractReward();

        recompute();
        hp = snapshotHp > 0f ? Math.min(snapshotHp, stats.maxHp) : stats.maxHp;
        stats.hp = hp;
        maxDepth = startDepth;
    }

    private void pickContract() {
        Random rng = new Random(seed ^ 0x00C0FFEE);
        contractType = rng.nextInt(4);
        switch (contractType) {
            case CONTRACT_DEPTH:
                contractTarget = 150;
                break;
            case CONTRACT_ORE:
                contractTarget = 8;
                break;
            case CONTRACT_GOLD:
                contractTarget = 3;
                break;
            default:
                contractTarget = 2;
                break;
        }
        setContractReward();
        contractProgress = 0;
        contractDone = false;
    }

    private void setContractReward() {
        contractReward = 120 + contractType * 30;
        contractXp = 30 + contractType * 10;
    }

    public void recompute() {
        Stats s = Upgrades.baseStats(meta, drillId);
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
        dst.mineResist = src.mineResist;
        dst.lavaResist = src.lavaResist;
        dst.oreMul = src.oreMul;
        dst.xpMul = src.xpMul;
        dst.magnet = src.magnet;
        dst.fallMul = src.fallMul;
        dst.boostCapacity = src.boostCapacity;
        dst.boostPower = src.boostPower;
        dst.boostRechargeMul = src.boostRechargeMul;
        dst.stunResist = src.stunResist;
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

        if (!contractDone) {
            if (contractType == CONTRACT_ORE) contractProgress++;
            else if (contractType == CONTRACT_GOLD && material == Materials.GOLD) contractProgress++;
            else if (contractType == CONTRACT_GEM
                    && (material == Materials.GEM || material == Materials.DIAMOND)) contractProgress++;
            checkContract();
        }
        return new OreGain(money, xp, m.name, m.accent);
    }

    private void checkContract() {
        if (contractDone || contractProgress < contractTarget) return;
        contractDone = true;
        moneyEarned += contractReward;
        xpEarned += contractXp;
    }

    public void update(float dt, float depthCells) {
        elapsed += dt;
        if (depthCells > maxDepth) maxDepth = depthCells;
        if (!contractDone && contractType == CONTRACT_DEPTH) {
            contractProgress = currentDepthMeters();
            checkContract();
        }
    }

    public String contractText() {
        switch (contractType) {
            case CONTRACT_DEPTH:
                return "REACH " + contractTarget + " m  " + Math.min(contractProgress, contractTarget) + "/" + contractTarget;
            case CONTRACT_ORE:
                return "MINE " + contractTarget + " ORE  " + contractProgress + "/" + contractTarget;
            case CONTRACT_GOLD:
                return "MINE " + contractTarget + " GOLD  " + contractProgress + "/" + contractTarget;
            default:
                return "FIND " + contractTarget + " GEMS  " + contractProgress + "/" + contractTarget;
        }
    }

    public String contractHudText() {
        if (contractDone) return "CONTRACT DONE  +" + contractReward;
        return "CONTRACT  " + contractText();
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
        s.salvage = 20 + s.depthMeters / 6;
        s.money = moneyEarned + s.salvage;
        s.xp = xpEarned;
        s.oreCount = oreCount;
        s.score = score();
        s.seed = seed;
        s.daily = daily;
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
        c.drillId = drillId;
        c.daily = daily;
        c.contractType = contractType;
        c.contractTarget = contractTarget;
        c.contractProgress = contractProgress;
        c.contractDone = contractDone;
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
