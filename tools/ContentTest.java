import com.deepfalldrill.game.*;

/** Headless regression checks for content footprints, pickups and progression. */
public final class ContentTest {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        int[] seeds = {0, 1, 35, 123456789, -1, Integer.MIN_VALUE};
        for (int seed : seeds) {
            check(Rng.codeToSeed(Rng.toCode(seed)) == seed, "Seed codes round-trip");
        }
        check(Rng.dailySeed(20000) == Rng.dailySeed(20000), "Daily seed is stable");
        check(Rng.dailySeed(20000) != Rng.dailySeed(20001), "Adjacent daily seeds differ");

        int mines = 0, cores = 0, powers = 0;
        for (int seed = 1; seed <= 8; seed++) {
            Terrain terrain = new Terrain(new Generator(seed));
            for (int cy = 9; cy < 30; cy++) {
                for (int cx = -1; cx <= 0; cx++) {
                    Terrain.Chunk chunk = terrain.getChunk(cx, cy);
                    for (int[] mine : chunk.mines) {
                        check(mine.length == 3 && mine[2] == 2, "Mine must have a 2x2 footprint");
                        for (int dy = 0; dy < 2; dy++) {
                            for (int dx = 0; dx < 2; dx++) {
                                check(Materials.isDiggable(terrain.get(mine[0] + dx, mine[1] + dy)),
                                        "Mine must be fully inside diggable ground");
                            }
                        }
                        Mines field = new Mines();
                        field.addChunk(chunk);
                        check(field.list.get(0).size == 2f, "Runtime mine size must match generation");
                        mines++;
                    }
                    for (int y = 1; y < Config.CHUNK - 3; y++) {
                        for (int x = 1; x < Config.CHUNK - 3; x++) {
                            int material = chunk.cells[y * Config.CHUNK + x] & 255;
                            if (!Materials.isOre(material)) continue;
                            boolean full = true;
                            for (int dy = 0; dy < 3; dy++) {
                                for (int dx = 0; dx < 3; dx++) {
                                    if ((chunk.cells[(y + dy) * Config.CHUNK + x + dx] & 255) != material)
                                        full = false;
                                }
                            }
                            if (full) cores++;
                        }
                    }
                    powers += chunk.pickups.size();
                }
            }
        }
        check(mines > 50 && cores > 30 && powers > 10, "All content categories must spawn");
        System.out.println("PASS content: " + mines + " large mines, " + cores
                + " dense mineral cores, " + powers + " pickups");

        Terrain terrain = new Terrain(new Generator(91));
        Stats stats = new Stats();
        stats.maxHp = stats.hp = 100f;
        Drill drill = new Drill(terrain);
        drill.spawn(0, 100, stats);
        terrain.getChunk(0, 3).pickups.add(new int[]{0, 100, Pickups.REPAIR});
        Pickups field = new Pickups();
        field.addChunk(terrain.getChunk(0, 3));
        int[] collections = {0};
        field.onCollect = (kind, x, y) -> collections[0]++;
        field.update(0.016f, drill);
        int collected = collections[0];
        field.update(0.016f, drill);
        check(collected >= 1 && collections[0] == collected, "Pickups cannot be collected twice");
        System.out.println("PASS one-shot pickup collection");

        long totalCost = 0;
        check(Upgrades.PERM.length == 8 && Upgrades.RUN.length >= 13, "Expanded upgrade catalogue");
        for (Upgrades.PermUpgrade upgrade : Upgrades.PERM) {
            int previous = 0;
            for (int level = 0; level < upgrade.max; level++) {
                int cost = upgrade.cost(level);
                check(cost > previous, "Upgrade costs must increase: " + upgrade.id);
                totalCost += cost;
                previous = cost;
            }
        }
        check(totalCost > 150000, "Permanent progression must have a meaningful long-term sink");
        SaveData meta = new SaveData();
        meta.perm.put("pshield", 1);
        Run run = new Run(1, meta, null, false);
        check(run.stats.mineResist > 0f, "Mine resistance survives stat recomputation");
        meta.unlockedDrills.add("brick");
        meta.drillId = "brick";
        SaveData.Checkpoint checkpoint = new SaveData.Checkpoint();
        checkpoint.seed = 7;
        checkpoint.drillId = "needle";
        checkpoint.contractTarget = 150;
        Run resumed = new Run(7, meta, checkpoint, false);
        check("needle".equals(resumed.drillId), "Checkpoint retains the run's selected drill");
        check(resumed.contractReward > 0, "Resumed contracts retain their reward");
        System.out.println("PASS progression: " + totalCost + " ore to buy all permanent levels");
    }
}
