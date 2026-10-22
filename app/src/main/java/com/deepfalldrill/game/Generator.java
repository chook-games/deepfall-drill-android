package com.deepfalldrill.game;

import java.util.Random;

/**
 * Deterministic generator for the fully solid lane.
 *
 * Every cell below the surface is rock, dirt, basalt, clustered ore or a
 * clustered lava pocket. No caves, pockets, shafts or rooms: the only empty
 * space is what the drill cuts (and mine craters).
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

    // ---------------------------------------------------------------- ore veins
    private void placeOreVeins(Terrain.Chunk c) {
        int bx = c.cx * Config.CHUNK;
        int by = c.cy * Config.CHUNK;
        int depthMid = by + Config.CHUNK / 2;
        if (depthMid < 24) return;

        Random rng = new Random(Rng.hash2u(c.cx, c.cy, seed ^ 0x0EA1));
        int veins = 2;

        int minLX = Math.max(1, Config.WORLD_LEFT + 1 - bx);
        int maxLX = Math.min(Config.CHUNK - 2, Config.WORLD_RIGHT - 2 - bx);
        if (maxLX <= minLX) return;

        for (int v = 0; v < veins; v++) {
            int centerY = by + 2 + rng.nextInt(Config.CHUNK - 4);
            if (centerY < 24) continue;
            int type = pickOre(centerY, rng);
            int x = minLX + rng.nextInt(maxLX - minLX + 1);
            int y = 2 + rng.nextInt(Config.CHUNK - 4);
            int count = 4 + rng.nextInt(4); // 4..7 contiguous cells
            int placed = 0;

            for (int i = 0; i < count * 6 && placed < count; i++) {
                int idx = y * Config.CHUNK + x;
                int here = c.cells[idx] & 0xFF;
                if (here == Materials.DIRT || here == Materials.STONE || here == Materials.BASALT) {
                    c.cells[idx] = (byte) type;
                    placed++;
                }
                int dir = rng.nextInt(4);
                if (dir == 0) x++;
                else if (dir == 1) x--;
                else if (dir == 2) y++;
                else y--;
                x = Config.clampInt(x, minLX, maxLX);
                y = Config.clampInt(y, 1, Config.CHUNK - 2);
            }
        }
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
        int depthMid = by + Config.CHUNK / 2;
        if (depthMid < 420) return;

        Random rng = new Random(Rng.hash2u(c.cx, c.cy, seed ^ 0x1A7A));
        float t = Math.min(1f, (depthMid - 420) / 3000f);
        if (rng.nextFloat() > 0.25f + t * 0.15f) return;

        int bw = 2 + rng.nextInt(2); // 2..3 wide
        int bh = 3 + rng.nextInt(2); // 3..4 tall -> 6..12 cells
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
                int here = c.cells[idx] & 0xFF;
                if (here == Materials.DIRT || here == Materials.STONE || here == Materials.BASALT) {
                    c.cells[idx] = (byte) Materials.LAVA;
                }
            }
        }
    }

    // ------------------------------------------------------------------- mines
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
            for (int attempt = 0; attempt < 24; attempt++) {
                int lx = 1 + rng.nextInt(Config.CHUNK - 2);
                int ly = 1 + rng.nextInt(Config.CHUNK - 2);
                int x = bx + lx;
                int y = by + ly;
                if (Math.abs(x) < 8 && y < 180) continue;
                int here = c.cells[ly * Config.CHUNK + lx] & 0xFF;
                if (!Materials.isDiggable(here)) continue;
                if (!Materials.isSolid(c.cells[(ly + 1) * Config.CHUNK + lx] & 0xFF)) continue;

                boolean close = false;
                for (int[] m : c.mines) {
                    if (Math.abs(m[0] - x) < 8 && Math.abs(m[1] - y) < 8) close = true;
                }
                if (close) continue;

                c.mines.add(new int[]{x, y});
                break;
            }
        }
    }
}
