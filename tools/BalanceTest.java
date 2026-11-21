import com.deepfalldrill.game.*;

/** Headless first-run difficulty sampler; no upgrades or steering are applied. */
public final class BalanceTest {
    private static final int RUNS = 24;
    private static final int TARGET_METERS = 900;
    private static final float DT = 1f / 60f;
    private static final float MAX_SECONDS = 240f;

    private static final class Result {
        int depth;
        int ore;
        int payout;
        float seconds;
        float hp;
        float[] milestoneSeconds = new float[4];
    }

    public static void main(String[] args) {
        int[] reached = new int[4];
        float[] times = new float[4];
        int deaths = 0;
        for (int i = 0; i < RUNS; i++) {
            Result result = simulate(1000 + i);
            if (result.hp <= 0f) deaths++;
            int[] milestones = {100, 250, 500, 900};
            for (int j = 0; j < milestones.length; j++) {
                if (result.depth >= milestones[j]) {
                    reached[j]++;
                    times[j] += result.milestoneSeconds[j];
                }
            }
            System.out.printf("seed=%d depth=%dm time=%.1fs hull=%.0f ore=%d payout=%d%n",
                    1000 + i, result.depth, result.seconds, result.hp, result.ore, result.payout);
        }
        System.out.printf("first-run sample: survived=%d/%d, reached 100m=%d, 250m=%d, 500m=%d, 900m=%d%n",
                RUNS - deaths, RUNS, reached[0], reached[1], reached[2], reached[3]);
        for (int i = 0; i < reached.length; i++) {
            System.out.printf("reached %dm: %.1fs average among survivors%n",
                    new int[]{100, 250, 500, 900}[i],
                    reached[i] == 0 ? 0f : times[i] / reached[i]);
        }
    }

    private static Result simulate(int seed) {
        SaveData meta = new SaveData();
        Run run = new Run(seed, meta, null, false);
        Terrain terrain = new Terrain(new Generator(seed));
        Mines mines = new Mines();
        terrain.onChunk = mines::addChunk;
        Drill drill = new Drill(terrain);
        drill.spawn(run.startX, run.startDepth, run.stats);
        terrain.preload((int) drill.y);
        drill.onDig = (material, x, y) -> run.addOre(material);
        float elapsed = 0f;
        int[] milestones = {100, 250, 500, 900};
        boolean[] crossed = new boolean[milestones.length];
        float[] milestoneSeconds = new float[milestones.length];
        while (!drill.dead && elapsed < MAX_SECONDS
                && Config.meters(drill.y) < TARGET_METERS) {
            terrain.ensureAround(drill.x, drill.y, Config.CHUNKS_PER_FRAME);
            drill.update(DT, 0f);
            mines.update(DT, drill, terrain, null);
            run.update(DT, drill.y);
            elapsed += DT;
            int depth = Config.meters(drill.y);
            for (int i = 0; i < milestones.length; i++) {
                if (!crossed[i] && depth >= milestones[i]) {
                    crossed[i] = true;
                    milestoneSeconds[i] = elapsed;
                }
            }
        }
        Result result = new Result();
        result.depth = Config.meters(drill.y);
        result.ore = run.oreCount;
        result.payout = run.summary().money;
        result.seconds = elapsed;
        result.hp = drill.hp;
        result.milestoneSeconds = milestoneSeconds;
        return result;
    }
}
