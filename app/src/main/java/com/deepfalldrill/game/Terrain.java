package com.deepfalldrill.game;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Chunked destructible cell grid. The visible world is only 16 cells wide, so
 * two chunk columns (-1 and 0) cover it; streaming is vertical only.
 */
public final class Terrain {
    public interface ChunkListener {
        void onChunk(Chunk c);
    }

    public interface ReactionListener {
        void onReaction(int x, int y, String kind);
    }

    public static final class Chunk {
        public final int cx;
        public final int cy;
        public final byte[] cells = new byte[Config.CHUNK * Config.CHUNK];
        public final ArrayList<int[]> mines = new ArrayList<>();
        public boolean room;
        public int version;
        public long uid;

        Chunk(int cx, int cy) {
            this.cx = cx;
            this.cy = cy;
            this.uid = NEXT_UID++;
        }
    }

    private static long NEXT_UID = 1;

    private final HashMap<Long, Chunk> chunks = new HashMap<>();
    private final Generator generator;
    private long lastKey = Long.MIN_VALUE;
    private Chunk lastChunk;
    private float fluidAcc;
    private int fluidFrame;
    private int[] scratch = new int[3 * 512];
    private int scratchLen;
    private final ArrayList<long[]> moved = new ArrayList<>(64);

    public ChunkListener onChunk;
    public ReactionListener onReaction;
    public int generatedCount;

    public Terrain(Generator generator) {
        this.generator = generator;
    }

    private static long key(int cx, int cy) {
        return ((long) cx << 32) ^ (cy & 0xFFFFFFFFL);
    }

    public Chunk getChunk(int cx, int cy) {
        long k = key(cx, cy);
        if (k == lastKey && lastChunk != null) return lastChunk;
        Chunk c = chunks.get(k);
        if (c == null) {
            c = new Chunk(cx, cy);
            generator.fill(c);
            chunks.put(k, c);
            generatedCount++;
            if (onChunk != null) onChunk.onChunk(c);
        }
        lastKey = k;
        lastChunk = c;
        return c;
    }

    public int get(int x, int y) {
        if (x < Config.WORLD_LEFT || x >= Config.WORLD_RIGHT) return Materials.BEDROCK;
        if (y < 0) return Materials.EMPTY;
        int cx = Math.floorDiv(x, Config.CHUNK);
        int cy = Math.floorDiv(y, Config.CHUNK);
        Chunk c = getChunk(cx, cy);
        int lx = Math.floorMod(x, Config.CHUNK);
        int ly = Math.floorMod(y, Config.CHUNK);
        return c.cells[ly * Config.CHUNK + lx] & 0xFF;
    }

    public void set(int x, int y, int id) {
        if (x < Config.WORLD_LEFT || x >= Config.WORLD_RIGHT || y < 0) return;
        int cx = Math.floorDiv(x, Config.CHUNK);
        int cy = Math.floorDiv(y, Config.CHUNK);
        Chunk c = getChunk(cx, cy);
        int lx = Math.floorMod(x, Config.CHUNK);
        int ly = Math.floorMod(y, Config.CHUNK);
        c.cells[ly * Config.CHUNK + lx] = (byte) id;
        c.version++;
    }

    public int dig(int x, int y) {
        int id = get(x, y);
        if (!Materials.isDiggable(id)) return Materials.EMPTY;
        set(x, y, Materials.EMPTY);
        return id;
    }

    public boolean isSolid(int x, int y) {
        return Materials.isSolid(get(x, y));
    }

    public void clearPocket(int cx, int cy, float radius) {
        int r = (int) Math.ceil(radius);
        for (int dy = -r; dy <= r; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                if (dx * dx + dy * dy > radius * radius) continue;
                set(cx + dx, cy + dy, Materials.EMPTY);
            }
        }
    }

    public void ensureAround(float px, float py, int budget) {
        int pcy = Math.floorDiv((int) py, Config.CHUNK);
        int made = 0;
        for (int dy = 0; dy <= 4; dy++) {
            for (int cx = -1; cx <= 0; cx++) {
                if (made >= budget) return;
                if (!chunks.containsKey(key(cx, pcy + dy))) {
                    getChunk(cx, pcy + dy);
                    made++;
                }
            }
        }
        for (int dy = 1; dy <= 2; dy++) {
            for (int cx = -1; cx <= 0; cx++) {
                if (made >= budget) return;
                if (!chunks.containsKey(key(cx, pcy - dy))) {
                    getChunk(cx, pcy - dy);
                    made++;
                }
            }
        }
    }

    public boolean hasChunk(int cx, int cy) {
        return chunks.containsKey(key(cx, cy));
    }

    public void preload(int py) {
        ensureAround(0f, py, 999);
    }

    public void cull(float py) {
        int pcy = Math.floorDiv((int) py, Config.CHUNK);
        ArrayList<Long> remove = null;
        for (Long k : chunks.keySet()) {
            Chunk c = chunks.get(k);
            if (c == null) continue;
            if (c.cy > pcy + Config.CHUNK_CULL_BELOW || c.cy < pcy - Config.CHUNK_CULL_ABOVE
                    || c.cx < -2 || c.cx > 1) {
                if (remove == null) remove = new ArrayList<>();
                remove.add(k);
            }
        }
        if (remove != null) {
            for (Long k : remove) {
                chunks.remove(k);
                if (k == lastKey) {
                    lastKey = Long.MIN_VALUE;
                    lastChunk = null;
                }
            }
        }
    }

    // ------------------------------------------------------------------ fluids
    public void stepFluids(float dt, int centerX, int centerY) {
        fluidAcc = Math.min(fluidAcc + dt, Config.FLUID_TICK * 3);
        int guard = 0;
        while (fluidAcc >= Config.FLUID_TICK && guard < 3) {
            fluidAcc -= Config.FLUID_TICK;
            fluidFrame++;
            fluidTick(centerX, centerY);
            guard++;
        }
    }

    private void fluidTick(int cx, int cy) {
        int r = Config.FLUID_RADIUS;
        scratchLen = 0;
        boolean flip = (fluidFrame & 1) == 0;
        for (int y = cy + r; y >= cy - r; y--) {
            boolean rowDir = (Rng.hash2f(y, fluidFrame, 7) < 0.5f) == flip;
            for (int i = 0; i <= r * 2; i++) {
                int x = rowDir ? cx - r + i : cx + r - i;
                int id = get(x, y);
                if (id == Materials.LAVA || id == Materials.WATER) {
                    if ((scratchLen + 3) > scratch.length) {
                        int[] bigger = new int[scratch.length * 2];
                        System.arraycopy(scratch, 0, bigger, 0, scratchLen);
                        scratch = bigger;
                    }
                    scratch[scratchLen++] = x;
                    scratch[scratchLen++] = y;
                    scratch[scratchLen++] = id;
                }
            }
        }

        moved.clear();
        int moves = 0;
        for (int i = 0; i < scratchLen && moves < Config.FLUID_MAX_MOVES; i += 3) {
            int x = scratch[i];
            int y = scratch[i + 1];
            int id = scratch[i + 2];
            if (wasMoved(x, y)) continue;
            if (get(x, y) != id) continue;

            if (id == Materials.LAVA) {
                if (reactLava(x, y)) continue;
                if (Rng.hash2f(x, y + fluidFrame, 19) < 0.45f) continue;
            }
            if (moveLiquid(x, y, id)) moves++;
        }
    }

    private boolean wasMoved(int x, int y) {
        for (int i = 0; i < moved.size(); i++) {
            long[] p = moved.get(i);
            if (p[0] == x && p[1] == y) return true;
        }
        return false;
    }

    private boolean reactLava(int x, int y) {
        int[][] around = {{x - 1, y}, {x + 1, y}, {x, y - 1}, {x, y + 1}};
        for (int[] n : around) {
            if (get(n[0], n[1]) != Materials.WATER) continue;
            if (Rng.hash2f(x, y, fluidFrame + 91) < 0.22f) continue;
            if (Rng.hash2f(n[0], n[1], 5) < 0.5f) {
                set(x, y, Materials.STONE);
                set(n[0], n[1], Materials.EMPTY);
            } else {
                set(n[0], n[1], Materials.STONE);
                set(x, y, Materials.EMPTY);
            }
            if (onReaction != null) onReaction.onReaction(n[0], n[1], "steam");
            return true;
        }
        return false;
    }

    private boolean moveLiquid(int x, int y, int id) {
        if (get(x, y + 1) == Materials.EMPTY) return moveTo(x, y, x, y + 1);
        int dir = Rng.hash2f(x, y, fluidFrame + 3) < 0.5f ? -1 : 1;
        if (get(x + dir, y + 1) == Materials.EMPTY) return moveTo(x, y, x + dir, y + 1);
        if (get(x - dir, y + 1) == Materials.EMPTY) return moveTo(x, y, x - dir, y + 1);

        float spread = id == Materials.LAVA ? 0.14f : 0.5f;
        if (Rng.hash2f(x, y, fluidFrame + 11) > spread) return false;

        if (get(x + dir, y) == Materials.EMPTY && Materials.isSolid(get(x + dir, y + 1))) {
            return moveTo(x, y, x + dir, y);
        }
        if (get(x - dir, y) == Materials.EMPTY && Materials.isSolid(get(x - dir, y + 1))) {
            return moveTo(x, y, x - dir, y);
        }
        return false;
    }

    private boolean moveTo(int x, int y, int nx, int ny) {
        int id = get(x, y);
        if (id == Materials.EMPTY) return false;
        set(x, y, Materials.EMPTY);
        set(nx, ny, id);
        moved.add(new long[]{x, y});
        moved.add(new long[]{nx, ny});
        return true;
    }
}
