import com.deepfalldrill.game.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/** Off-device checks for clustered hazards and lava damage. Not in the APK. */
public final class GameLogicTest {
    public static void main(String[] args) {
        clusterTest();
        lavaTest();
    }

    private static void clusterTest() {
        int seed = 12345;
        Terrain terrain = new Terrain(new Generator(seed));
        int x0 = -23;
        int x1 = 22;
        int y0 = 420;
        int y1 = 1100;
        HashSet<Long> seen = new HashSet<>();
        List<Integer> lavaSizes = new ArrayList<>();
        List<Integer> oreSizes = new ArrayList<>();

        for (int y = y0; y < y1; y++) {
            for (int x = x0; x <= x1; x++) {
                int id = terrain.get(x, y);
                boolean lava = id == Materials.LAVA;
                boolean ore = Materials.isOre(id);
                if (!lava && !ore) continue;
                long key = ((long) x << 32) ^ (y & 0xFFFFFFFFL);
                if (seen.contains(key)) continue;

                int size = 0;
                ArrayDeque<int[]> queue = new ArrayDeque<>();
                queue.add(new int[]{x, y});
                seen.add(key);
                while (!queue.isEmpty()) {
                    int[] p = queue.poll();
                    size++;
                    int[][] n = {{p[0] + 1, p[1]}, {p[0] - 1, p[1]}, {p[0], p[1] + 1}, {p[0], p[1] - 1}};
                    for (int[] q : n) {
                        int id2 = terrain.get(q[0], q[1]);
                        boolean same = lava ? id2 == Materials.LAVA : (Materials.isOre(id2) && id2 == id);
                        if (!same) continue;
                        long k2 = ((long) q[0] << 32) ^ (q[1] & 0xFFFFFFFFL);
                        if (seen.contains(k2)) continue;
                        seen.add(k2);
                        queue.add(q);
                    }
                }
                if (lava) lavaSizes.add(size);
                else oreSizes.add(size);
            }
        }

        System.out.println("lava blobs=" + lavaSizes.size() + " sizes=" + lavaSizes);
        System.out.println("ore veins=" + oreSizes.size() + " sizes=" + oreSizes);
        int minLava = lavaSizes.stream().mapToInt(Integer::intValue).min().orElse(0);
        int minOre = oreSizes.stream().mapToInt(Integer::intValue).min().orElse(0);
        System.out.println("min lava=" + minLava + " min ore=" + minOre);
    }

    private static void lavaTest() {
        Terrain terrain = new Terrain(new Generator(777));
        for (int y = 18; y <= 24; y++) {
            for (int x = -3; x <= 3; x++) terrain.set(x, y, Materials.EMPTY);
        }
        for (int y = 21; y <= 23; y++) {
            for (int x = -2; x <= 2; x++) terrain.set(x, y, Materials.LAVA);
        }

        Drill drill = new Drill(terrain);
        Stats stats = new Stats();
        stats.maxHp = 100f;
        stats.hp = 100f;
        drill.spawn(0f, 18f, stats);

        float before = drill.hp;
        float maxSlowVy = -999f;
        for (int i = 0; i < 240; i++) { // 2 seconds
            terrain.ensureAround(drill.x, drill.y, 3);
            drill.update(1f / 120f, 0f);
            if (drill.inLava) maxSlowVy = Math.max(maxSlowVy, drill.vy);
        }
        System.out.printf("lava test: hp %.1f -> %.1f, inLava=%s, vyWhileLava=%.1f, slowCap=%.1f%n",
                before, drill.hp, drill.inLava, maxSlowVy, Config.LAVA_SLOW);
    }
}
