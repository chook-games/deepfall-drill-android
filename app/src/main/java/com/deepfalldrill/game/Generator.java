package com.deepfalldrill.game;

import java.util.Random;

/**
 * Deterministic generator for the fully solid lane.
 *
 * Below the surface everything is rock, dirt, basalt, big clustered ore veins,
 * 6-12 cell lava blobs, 2x2 mines and the occasional power-up. No caves.
 */
public final class Generator {
    private final int seed;

    public Generator(int seed) {
        this.seed = seed;
    }

    public void fill(Terrain.Chunk c) {
        int bx = c.cx * Config.CHUNK;
        int by = c.cy * Config.CHUNK;
        for (int ly = 0; ly < Config.CHUNK; ly++) {
            for (int lx = 0; lx < Config.CHUNK; lx++) {
                c.cells[ly * Config.CHUNK + lx] = (byte) baseCell(bx + lx, by + ly);
            }
        }
        placeLavaBlobs(c);
        placeOreVeins(c);
        placeMines(c);
        placePickups(c);
    }

    private int baseCell(int x, int y) {
        if (x == Config.WORLD_LEFT || x == Config.WORLD_RIGHT - 1) return Materials.BEDROCK;
        if (y < Config.GROUND_Y) return Materials.EMPTY;

        float d = y;
        float strata = Rng.fbm(seed + 55, x * 0.35f, y * 0.14f, 2);
        int mat;
        if (d < 6) {
            mat = Materials.DIRT;
        } else if (d < 120) {
            mat = strata > 0.66f ? Materials.STONE : Materials.DIRT;
        } else if (d < 510) {
            mat = strata > 0.47f ? Materials.STONE : Materials.DIRT;
        } else if (d < 1440) {
            mat = strata > 0.40f ? Materials.STONE
                    : (strata < 0.28f ? Materials.BASALT : Materials.DIRT);
        } else if (d < 3300) {
            mat = strata < 0.5f ? Materials.BASALT : Materials.STONE;
        } else {
            mat = strata < 0.68f ? Materials.BASALT : Materials.STONE;
        }

        if (d > 720 && mat == Materials.STONE
                && Rng.ridged(seed + 66, x * 0.25f, y * 0.1f, 2) > 0.75f) {
            mat = Materials.BASALT;
        }
        return mat;
    }

    private static boolean plainRock(int id) {
        return id == Materials.DIRT || id == Materials.STONE || id == Materials.BASALT;
    }

    private static boolean inPlayableX(int x) {
        return x >= Config.WORLD_LEFT + 1 && x <= Config.WORLD_RIGHT - 2;
    }

    // --------------------------------------------------------------- ore veins
    // A solid 3x3 / 4x4 core plus scattered ore around it, like pixel art.
    private void placeOreVeins(Terrain.Chunk c) {
        int bx = c.cx * Config.CHUNK;
        int by = c.cy * Config.CHUNK;
        if (by + 16 < 24) return;

        Random rng = new Random(Rng.hash2u(c.cx, c.cy, seed ^ 0x0EA1));
        if (rng.nextFloat() > 0.55f) return;

        int type = pickOre(by + 8 + rng.nextInt(16), rng);
        boolean four = rng.nextFloat() < 0.45f;
        int w = four ? 4 : 3;

        int minX0 = Math.max(1, Config.WORLD_LEFT + 1 - bx + 2);
        int maxX0 = Math.min(Config.CHUNK - w - 3, Config.WORLD_RIGHT - 2 - bx - w - 1);
        if (maxX0 < minX0) return;

        int x0 = minX0 + rng.nextInt(maxX0 - minX0 + 1);
        int y0 = 3 + rng.nextInt(Config.CHUNK - w - 6);

        for (int yy = 0; yy < w; yy++) {
            for (int xx = 0; xx < w; xx++) {
                if (four && (xx == 0 || xx == w - 1) && (yy == 0 || yy == w - 1)) continue;
                paintOre(c, bx + x0 + xx, by + y0 + yy, type);
            }
        }

        float cxWorld = x0 + w / 2f;
        float cyWorld = y0 + w / 2f;
        for (int yy = y0 - 2; yy < y0 + w + 2; yy++) {
            for (int xx = x0 - 2; xx < x0 + w + 2; xx++) {
                boolean core = xx >= x0 && xx < x0 + w && yy >= y0 && yy < y0 + w;
                if (core) continue;
                float dist = Math.max(Math.abs(xx - cxWorld), Math.abs(yy - cyWorld));
                float p = dist <= w / 2f + 1f ? 0.45f : 0.18f;
                if (rng.nextFloat() < p) paintOre(c, bx + xx, by + yy, type);
            }
        }
    }

    private void paintOre(Terrain.Chunk c, int wx, int wy, int type) {
        if (!inPlayableX(wx) || wy < 24) return;
        int bx = c.cx * Config.CHUNK;
        int by = c.cy * Config.CHUNK;
        int lx = wx - bx;
        int ly = wy - by;
        if (lx < 0 || lx >= Config.CHUNK || ly < 0 || ly >= Config.CHUNK) return;
        int idx = ly * Config.CHUNK + lx;
        if (plainRock(c.cells[idx] & 0xFF)) c.cells[idx] = (byte) type;
    }

    private int pickOre(int depth, Random rng) {
        float r = rng.nextFloat();
        if (depth > 1500) {
            if (r < 0.15f) return Materials.DIAMOND;
            if (r < 0.37f) return Materials.GEM;
            if (r < 0.65f) return Materials.GOLD;
            return Materials.IRON;
        }
        if (depth > 900) {
            if (r < 0.15f) return Materials.GEM;
            if (r < 0.45f) return Materials.GOLD;
            return Materials.IRON;
        }
        if (depth > 420) {
            if (r < 0.08f) return Materials.GEM;
            if (r < 0.36f) return Materials.GOLD;
            return Materials.IRON;
        }
        return Materials.IRON;
    }

    // --------------------------------------------------------------- lava blobs
    private void placeLavaBlobs(Terrain.Chunk c) {
        int bx = c.cx * Config.CHUNK;
        int by = c.cy * Config.CHUNK;
        if (by + Config.CHUNK / 2 < 420) return;

        Random rng = new Random(Rng.hash2u(c.cx, c.cy, seed ^ 0x1A7A));
        float t = Math.min(1f, (by + 16 - 420) / 3000f);
        if (rng.nextFloat() > 0.25f + t * 0.15f) return;

        int bw = 2 + rng.nextInt(2); // 2..3
        int bh = 3 + rng.nextInt(2); // 3..4 -> 6..12 cells
        int minLX = Math.max(1, Config.WORLD_LEFT + 1 - bx);
        int maxLX = Math.min(Config.CHUNK - bw - 1, Config.WORLD_RIGHT - 2 - bx - bw + 1);
        if (maxLX < minLX) return;
        int sx = minLX + rng.nextInt(maxLX - minLX + 1);
        int sy = 1 + rng.nextInt(Config.CHUNK - bh - 2);

        for (int y = sy; y < sy + bh; y++) {
            for (int x = sx; x < sx + bw; x++) {
                int worldY = by + y;
                if (worldY < 420) continue;
                int idx = y * Config.CHUNK + x;
                if (plainRock(c.cells[idx] & 0xFF)) c.cells[idx] = (byte) Materials.LAVA;
            }
        }
    }

    // ------------------------------------------------------------------- mines
    // A mine occupies a 2x2 block of rock as one object.
    private void placeMines(Terrain.Chunk c) {
        int bx = c.cx * Config.CHUNK;
        int by = c.cy * Config.CHUNK;
        int depth = by + Config.CHUNK / 2;
        if (depth < Config.MINE_START_DEPTH) return;

        Random rng = new Random(Rng.hash2u(c.cx, c.cy, seed ^ 0x5151));
        float t = Math.min(1f, (depth - Config.MINE_START_DEPTH) / 2700f);
        int count = 0;
        if (rng.nextFloat() < 0.35f + t * 0.35f) count = 1;
        if (rng.nextFloat() < t * 0.25f) count++;

        for (int i = 0; i < count; i++) {
            for (int attempt = 0; attempt < 28; attempt++) {
                int lx = 1 + rng.nextInt(Config.CHUNK - 4);
                int ly = 1 + rng.nextInt(Config.CHUNK - 4);
                int x = bx + lx;
                int y = by + ly;
                if (x < Config.WORLD_LEFT + 1 || x + 1 > Config.WORLD_RIGHT - 2) continue;
                if (Math.abs(x) < 8 && y < 180) continue;

                boolean ok = true;
                for (int dy = 0; dy < 2 && ok; dy++) {
                    for (int dx = 0; dx < 2; dx++) {
                        if (!plainRock(c.cells[(ly + dy) * Config.CHUNK + lx + dx] & 0xFF)) {
                            ok = false;
                            break;
                        }
                    }
                }
                if (!ok) continue;
                if (!Materials.isSolid(c.cells[(ly + 2) * Config.CHUNK + lx] & 0xFF)) continue;
                if (!Materials.isSolid(c.cells[(ly + 2) * Config.CHUNK + lx + 1] & 0xFF)) continue;

                boolean close = false;
                for (int[] m : c.mines) {
                    if (Math.abs(m[0] - x) < 10 && Math.abs(m[1] - y) < 10) close = true;
                }
                if (close) continue;

                c.mines.add(new int[]{x, y, 2});
                break;
            }
        }
    }

    // ----------------------------------------------------------------- pickups
    private void placePickups(Terrain.Chunk c) {
        int bx = c.cx * Config.CHUNK;
        int by = c.cy * Config.CHUNK;
        int depth = by + Config.CHUNK / 2;
        if (depth < 40) return;

        Random rng = new Random(Rng.hash2u(c.cx, c.cy, seed ^ 0x91C7));
        if (rng.nextFloat() > 0.22f) return;
        int kind = rng.nextInt(5);

        for (int attempt = 0; attempt < 24; attempt++) {
            int lx = 2 + rng.nextInt(Config.CHUNK - 4);
            int ly = 2 + rng.nextInt(Config.CHUNK - 4);
            int x = bx + lx;
            int y = by + ly;
            if (!inPlayableX(x)) continue;
            if (!plainRock(c.cells[ly * Config.CHUNK + lx] & 0xFF)) continue;

            boolean close = false;
            for (int[] m : c.mines) {
                if (Math.abs(m[0] - x) < 6 && Math.abs(m[1] - y) < 6) close = true;
            }
            if (close) continue;

            c.pickups.add(new int[]{x, y, kind});
            break;
        }
    }
}
