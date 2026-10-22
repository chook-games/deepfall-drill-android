import com.deepfalldrill.game.*;

/** Headless drilling speed/ore check. Not part of the APK. */
public final class DrillTest {
    private static float tdebug;

    public static void main(String[] args) {
        int seed = args.length > 0 ? Integer.parseInt(args[0]) : 12345;
        int seconds = args.length > 1 ? Integer.parseInt(args[1]) : 60;
        Terrain terrain = new Terrain(new Generator(seed));
        Drill drill = new Drill(terrain);
        Stats stats = new Stats();
        stats.maxHp = 100f;
        stats.hp = 100f;
        stats.digPower = 1f;
        stats.steerMul = 1f;
        drill.spawn(0f, 10f, stats);

        final int[] digs = {0};
        final int[] oreCells = {0};
        final int[] money = {0};
        drill.onDig = (m, x, y) -> {
            digs[0]++;
            Materials.M mat = Materials.get(m);
            if (mat.value > 0) {
                oreCells[0]++;
                money[0] += mat.value;
            }
        };
        drill.onDamage = (amount, source) -> {
            if (tdebug < 7f) {
                System.out.printf("  DAMAGE %.1f from %s at y=%.1f%n", amount, source, drill.y);
            }
        };

        float dt = 1f / 120f;
        float last = 0f;
        for (float t = 0f; t < seconds; t += dt) {
            tdebug = t;
            terrain.ensureAround(drill.x, drill.y, 3);
            terrain.cull(drill.y);
            drill.update(dt, 0f);
            if (t - last >= 10f) {
                last = t;
                System.out.printf(
                        "t=%4.0fs depth=%4dm vy=%5.1f digs=%5d hp=%5.1f oreCells=%3d oreValue=%5d%n",
                        t, Config.meters(drill.y), drill.vy, digs[0], drill.hp, oreCells[0], money[0]);
            }
        }
    }
}
