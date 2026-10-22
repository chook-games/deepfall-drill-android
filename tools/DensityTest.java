import com.deepfalldrill.game.*;

/** Off-device material distribution check. Not part of the APK. */
public final class DensityTest {
    public static void main(String[] args) {
        int seed = args.length > 0 ? Integer.parseInt(args[0]) : 0x51EED;
        Terrain terrain = new Terrain(new Generator(seed));
        int[] depths = {20, 120, 300, 600, 1200, 2000};
        for (int depth : depths) {
            int[] counts = new int[Materials.ALL.length];
            int total = 0;
            for (int y = depth; y < depth + 240; y++) {
                for (int x = Config.WORLD_LEFT; x < Config.WORLD_RIGHT; x++) {
                    counts[terrain.get(x, y)]++;
                    total++;
                }
            }
            System.out.println("depth " + depth + " cells (" + Config.meters(depth) + " m)");
            for (int i = 1; i < counts.length; i++) {
                float pct = counts[i] * 100f / total;
                if (pct >= 0.15f) {
                    System.out.printf("  %-12s %5.1f%%%n", Materials.get(i).name, pct);
                }
            }
        }
    }
}
