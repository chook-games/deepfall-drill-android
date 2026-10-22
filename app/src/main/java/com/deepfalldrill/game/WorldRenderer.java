package com.deepfalldrill.game;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.Shader;

import java.util.HashMap;
import java.util.Iterator;

/**
 * World renderer. Terrain is baked into one bitmap per chunk (rebuilt when a
 * cell changes) so the fine 48-column grid stays cheap on mobile.
 */
public final class WorldRenderer {
    private final UiKit k;
    private final Paint bgPaint = new Paint();
    private final Paint texPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint bitmapPaint = new Paint();
    private LinearGradient bgGradient;
    private int bgHeight = -1;
    private float ppc = 1f;
    private float lastPpc = -1f;
    private final HashMap<Long, Tex> textures = new HashMap<>();
    private final RectF dst = new RectF();
    private final RectF arcRect = new RectF();

    private static final class Tex {
        Bitmap bmp;
        Canvas canvas;
        long uid;
        int version;
        int px;
        int cx;
        int cy;
    }

    public WorldRenderer(UiKit kit) {
        this.k = kit;
    }

    public void clearTextures() {
        for (Tex t : textures.values()) {
            if (t.bmp != null) t.bmp.recycle();
        }
        textures.clear();
    }

    public void draw(Canvas c, Game g, int w, int h, int safeTop, int safeBottom) {
        ppc = w / (float) Config.VISIBLE_COLS;
        float anchor = h * Config.ANCHOR_Y;
        float camY = g.camY;
        float shx = (float) (Math.random() - 0.5) * g.shake * 12f * k.u;
        float shy = (float) (Math.random() - 0.5) * g.shake * 12f * k.u;
        float time = g.time;

        if (Math.abs(ppc - lastPpc) > 0.01f) {
            clearTextures();
            lastPpc = ppc;
        }

        drawBackground(c, g, w, h, ppc, anchor, camY, time);

        if (g.terrain != null) {
            int top = (int) Math.floor(camY - anchor / ppc) - 1;
            int bottom = (int) Math.ceil(camY + (h - anchor) / ppc) + 1;

            drawTerrain(c, g, w, h, ppc, anchor, camY, shx, shy, top, bottom, time);
            drawLiquids(c, g, w, ppc, anchor, camY, shx, shy, top, bottom, time);
            drawDepthMarks(c, w, h, ppc, anchor, camY, shx, shy, top);
            drawMines(c, g, w, ppc, anchor, camY, shx, shy, time);
            if (g.state == Game.SURFACE) drawPlatform(c, g, w, ppc, anchor, camY, shx, shy);
            drawDrill(c, g, w, ppc, anchor, camY, shx, shy, time);
            drawParticles(c, g, w, ppc, anchor, camY, shx, shy);
            drawSpeedLines(c, g, w, h, ppc, time);
        }

        if (g.state == Game.PLAYING || g.state == Game.PAUSED || g.state == Game.MILESTONE
                || g.state == Game.DYING) {
            drawHud(c, g, w, h, safeTop, safeBottom);
        }

        drawVignette(c, g, w, h);
    }

    // ------------------------------------------------------------- background
    private void drawBackground(Canvas c, Game g, int w, int h, float ppc, float anchor,
                                float camY, float time) {
        if (bgGradient == null || bgHeight != h) {
            bgGradient = new LinearGradient(0, 0, 0, h,
                    0xFF241A12, 0xFF06070A, Shader.TileMode.CLAMP);
            bgHeight = h;
        }
        bgPaint.setShader(bgGradient);
        c.drawRect(0, 0, w, h, bgPaint);
        bgPaint.setShader(null);

        float groundY = anchor + (Config.GROUND_Y - camY) * ppc;
        if (groundY > 0f) {
            float skyBottom = Math.min(h, groundY);
            LinearGradient sky = new LinearGradient(0, 0, 0, Math.max(1f, skyBottom),
                    0xFF6FB7E8, 0xFFD6EEF9, Shader.TileMode.CLAMP);
            bgPaint.setShader(sky);
            c.drawRect(0, 0, w, skyBottom, bgPaint);
            bgPaint.setShader(null);

            // Sun + drifting clouds.
            k.fill.setColor(0xFFFFF3C4);
            k.fill.setAlpha(210);
            c.drawCircle(w * 0.8f, skyBottom * 0.22f, w * 0.055f, k.fill);
            k.fill.setAlpha(255);
            k.fill.setColor(0xE6FFFFFF);
            for (int i = 0; i < 3; i++) {
                float cxp = ((i * 0.31f + time * 0.006f) % 1.2f - 0.1f) * w;
                float cyp = skyBottom * (0.12f + i * 0.09f);
                float cw = w * (0.11f + i * 0.015f);
                float chh = cw * 0.32f;
                c.drawOval(cxp - cw, cyp - chh, cxp + cw, cyp + chh, k.fill);
                c.drawOval(cxp - cw * 0.5f, cyp - chh * 1.6f, cxp + cw * 0.55f, cyp + chh * 0.7f, k.fill);
            }

            // Trees on the surface.
            drawTree(c, w, groundY, ppc, -19.0f);
            drawTree(c, w, groundY, ppc, -8.0f);
            drawTree(c, w, groundY, ppc, 10.0f);
            drawTree(c, w, groundY, ppc, 19.5f);
        }

        bgPaint.setColor(0xFF05070C);
        bgPaint.setAlpha((int) (Config.clamp(camY / 2400f, 0f, 1f) * 120f));
        c.drawRect(0, Math.max(0f, groundY), w, h, bgPaint);
        bgPaint.setAlpha(255);

        if (camY > 2f) {
            float band = h * 0.09f;
            float off = ((-camY * ppc * 0.25f) % band + band) % band;
            bgPaint.setColor(0x0AFFFFFF);
            bgPaint.setAlpha(10);
            for (float y = off; y < h; y += band) {
                c.drawRect(0, y, w, y + h * 0.012f, bgPaint);
            }
            bgPaint.setAlpha(255);
        }
    }

    private void drawTree(Canvas c, int w, float groundY, float ppc, float worldX) {
        float sx = w / 2f + worldX * ppc;
        float trunkW = ppc * 0.9f;
        float trunkH = ppc * 5.5f;
        k.fill.setColor(0xFF6B4A2F);
        c.drawRect(sx - trunkW / 2f, groundY - trunkH, sx + trunkW / 2f, groundY, k.fill);
        k.fill.setColor(0xFF2E7D4F);
        c.drawCircle(sx, groundY - trunkH - ppc * 1.2f, ppc * 3.2f, k.fill);
        k.fill.setColor(0xFF3E9B63);
        c.drawCircle(sx - ppc * 1.8f, groundY - trunkH + ppc * 0.2f, ppc * 2.3f, k.fill);
        c.drawCircle(sx + ppc * 1.9f, groundY - trunkH - ppc * 0.1f, ppc * 2.0f, k.fill);
        k.fill.setColor(0xFF4CAF6F);
        c.drawCircle(sx + ppc * 0.3f, groundY - trunkH - ppc * 2.4f, ppc * 1.9f, k.fill);
    }

    // ---------------------------------------------------------------- terrain
    private void drawTerrain(Canvas c, Game g, int w, int h, float ppc, float anchor, float camY,
                             float shx, float shy, int top, int bottom, float time) {
        int cx0 = Math.floorDiv(Config.WORLD_LEFT, Config.CHUNK);
        int cx1 = Math.floorDiv(Config.WORLD_RIGHT - 1, Config.CHUNK);
        int cy0 = Math.floorDiv(top, Config.CHUNK) - 1;
        int cy1 = Math.floorDiv(bottom, Config.CHUNK) + 1;

        int builds = 0;
        for (int cy = cy0; cy <= cy1; cy++) {
            for (int cx = cx0; cx <= cx1; cx++) {
                Terrain.Chunk chunk = g.terrain.getChunk(cx, cy);
                long key = ((long) cx << 32) ^ (cy & 0xFFFFFFFFL);
                Tex tex = textures.get(key);
                int px = Math.max(8, Math.round(ppc * Config.CHUNK));

                boolean valid = tex != null && tex.uid == chunk.uid
                        && tex.version == chunk.version && tex.px == px && tex.bmp != null;
                if (!valid && builds < 3) {
                    tex = buildTexture(g, chunk, tex, px, cx, cy);
                    textures.put(key, tex);
                    builds++;
                    valid = true;
                }
                if (!valid) {
                    drawChunkDirect(c, g, chunk, w, ppc, anchor, camY, shx, shy, time);
                    continue;
                }

                dst.set(w / 2f + cx * Config.CHUNK * ppc + shx,
                        anchor + (cy * Config.CHUNK - camY) * ppc + shy,
                        w / 2f + (cx + 1) * Config.CHUNK * ppc + shx,
                        anchor + ((cy + 1) * Config.CHUNK - camY) * ppc + shy);
                c.drawBitmap(tex.bmp, null, dst, texPaint);
            }
        }

        // Drop textures for chunks that no longer exist.
        Iterator<HashMap.Entry<Long, Tex>> it = textures.entrySet().iterator();
        while (it.hasNext()) {
            HashMap.Entry<Long, Tex> e = it.next();
            Tex t = e.getValue();
            if (!g.terrain.hasChunk(t.cx, t.cy)) {
                if (t.bmp != null) t.bmp.recycle();
                it.remove();
            }
        }
    }

    private Tex buildTexture(Game g, Terrain.Chunk chunk, Tex tex, int px, int cx, int cy) {
        if (tex == null) tex = new Tex();
        if (tex.bmp == null || tex.bmp.getWidth() != px) {
            if (tex.bmp != null) tex.bmp.recycle();
            tex.bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888);
            tex.canvas = new Canvas(tex.bmp);
            tex.px = px;
        }
        tex.canvas.drawColor(0, PorterDuff.Mode.CLEAR);
        tex.cx = cx;
        tex.cy = cy;
        tex.uid = chunk.uid;
        tex.version = chunk.version;

        for (int ly = 0; ly < Config.CHUNK; ly++) {
            for (int lx = 0; lx < Config.CHUNK; lx++) {
                int id = chunk.cells[ly * Config.CHUNK + lx] & 0xFF;
                if (!Materials.isSolid(id)) continue;
                paintCell(tex.canvas, g, cx * Config.CHUNK + lx, cy * Config.CHUNK + ly,
                        id, lx * ppc, ly * ppc);
            }
        }
        return tex;
    }

    private void drawChunkDirect(Canvas c, Game g, Terrain.Chunk chunk, int w, float ppc,
                                 float anchor, float camY, float shx, float shy, float time) {
        float baseX = w / 2f + chunk.cx * Config.CHUNK * ppc + shx;
        float baseY = anchor + (chunk.cy * Config.CHUNK - camY) * ppc + shy;
        for (int ly = 0; ly < Config.CHUNK; ly++) {
            for (int lx = 0; lx < Config.CHUNK; lx++) {
                int id = chunk.cells[ly * Config.CHUNK + lx] & 0xFF;
                if (!Materials.isSolid(id)) continue;
                paintCell(c, g, chunk.cx * Config.CHUNK + lx, chunk.cy * Config.CHUNK + ly,
                        id, baseX + lx * ppc, baseY + ly * ppc);
            }
        }
    }

    private void paintCell(Canvas c, Game g, int x, int y, int id, float left, float top) {
        Materials.M m = Materials.get(id);
        float size = ppc + 1f;
        float hash = Rng.hash2f(x, y, 1337);
        int color = m.colors[(int) (hash * m.colors.length) % m.colors.length];
        k.rect(c, left, top, left + size, top + size, color, 0f);

        boolean aboveSolid = Materials.isSolid(g.terrain.get(x, y - 1));
        if (!aboveSolid) {
            if (y == Config.GROUND_Y) {
                // Grass on the surface row.
                k.rect(c, left, top, left + size, top + Math.max(2f, ppc * 0.3f), 0xFF4FBF63, 0f);
                k.rect(c, left, top + ppc * 0.28f, left + size, top + ppc * 0.38f, 0xFF2E7D4F, 0f);
                float tuft = Rng.hash2f(x, y, 17);
                k.rect(c, left + tuft * ppc * 0.7f, top - ppc * 0.14f,
                        left + tuft * ppc * 0.7f + Math.max(1f, ppc * 0.08f), top + 1f, 0xFF3E9B63, 0f);
            } else {
                k.rect(c, left, top, left + size, top + Math.max(1.5f, ppc * 0.12f), 0x1CFFFFFF, 0f);
                k.rect(c, left, top + ppc * 0.12f, left + size, top + Math.max(2f, ppc * 0.16f), 0x2E000000, 0f);
            }
        }

        int grain = id == Materials.DIRT ? 2 : 1;
        for (int i = 0; i < grain; i++) {
            float a = Rng.hash2f(x * 7 + i, y, 5);
            float b = Rng.hash2f(x, y * 7 + i, 6);
            float s = Math.max(1f, ppc * 0.06f);
            int col = i == 0 ? 0x1E000000 : 0x0AFFFFFF;
            k.rect(c, left + a * (ppc - s), top + b * (ppc - s),
                    left + a * (ppc - s) + s, top + b * (ppc - s) + s, col, 0f);
        }

        if (id == Materials.BASALT) {
            k.line.setColor(0x59000000);
            k.line.setStrokeWidth(Math.max(1f, ppc * 0.03f));
            float a = Rng.hash2f(x, y, 21);
            float b = Rng.hash2f(x, y, 22);
            c.drawLine(left + a * ppc, top, left + b * ppc, top + ppc, k.line);
        }

        if (id == Materials.BEDROCK) {
            k.line.setColor(0x332F3342);
            k.line.setStrokeWidth(Math.max(1f, ppc * 0.045f));
            c.drawLine(left - ppc * 0.2f, top + ppc, left + ppc, top - ppc * 0.2f, k.line);
        }

        if (m.ore) {
            float fleck = Math.max(1.5f, ppc * 0.14f);
            for (int i = 0; i < 3; i++) {
                float a = Rng.hash2f(x * 3 + i, y, 77);
                float b = Rng.hash2f(x, y * 3 + i, 91);
                k.rect(c, left + a * (ppc - fleck), top + b * (ppc - fleck),
                        left + a * (ppc - fleck) + fleck, top + b * (ppc - fleck) + fleck, m.accent, 0f);
            }
        }
    }

    private void drawLiquids(Canvas c, Game g, int w, float ppc, float anchor, float camY,
                             float shx, float shy, int top, int bottom, float time) {
        for (int y = Math.max(0, top); y <= bottom; y++) {
            for (int x = Config.WORLD_LEFT; x < Config.WORLD_RIGHT; x++) {
                int id = g.terrain.get(x, y);
                if (!Materials.isLiquid(id)) continue;
                Materials.M m = Materials.get(id);
                float left = w / 2f + x * ppc + shx;
                float tp = anchor + (y - camY) * ppc + shy;
                float size = ppc + 1f;
                float surf = id == Materials.WATER ? 0.22f : 0.08f;
                boolean liquidAbove = Materials.isLiquid(g.terrain.get(x, y - 1));
                float wob = (float) Math.sin(time * 2.4f + x * 0.9f) * ppc * 0.05f;

                k.fill.setColor(m.colors[0]);
                k.fill.setAlpha(id == Materials.WATER ? 215 : 245);
                if (!liquidAbove) {
                    float y0 = tp + ppc * surf + wob;
                    c.drawRect(left, y0, left + size, tp + size, k.fill);
                    k.fill.setColor(m.accent);
                    k.fill.setAlpha(130);
                    c.drawRect(left, y0, left + size, y0 + Math.max(1f, ppc * 0.05f), k.fill);
                } else {
                    c.drawRect(left, tp, left + size, tp + size, k.fill);
                }
                k.fill.setAlpha(255);

                if (id == Materials.LAVA) {
                    float b = Rng.hash2f(x, y + ((int) (time * 2f)), 5);
                    k.fill.setColor(b > 0.7f ? 0xFFFFD27A : 0xFFFF9A3D);
                    k.fill.setAlpha(200);
                    float s = ppc * 0.2f;
                    c.drawRect(left + b * (ppc - s), tp + ((b * 7f) % 1f) * (ppc - s),
                            left + b * (ppc - s) + s, tp + ((b * 7f) % 1f) * (ppc - s) + s, k.fill);
                    k.fill.setAlpha(255);
                }
            }
        }
    }

    private void drawDepthMarks(Canvas c, int w, int h, float ppc, float anchor,
                                float camY, float shx, float shy, int top) {
        int endY = (int) Math.ceil(camY + (h - anchor) / ppc);
        k.text.setTypeface(k.regular);
        k.text.setTextSize(k.px(9f));
        k.text.setTextAlign(Paint.Align.RIGHT);
        int step = (int) (Config.MILESTONE_METERS / Config.METERS_PER_CELL); // 600 cells = 100 m
        int first = Math.max(step, (int) (Math.floor(Math.max(0, top) / (float) step) * step));
        for (int y = first; y <= endY; y += step) {
            float sy = anchor + (y - camY) * ppc + shy;
            k.rect(c, 0, sy, w, sy + Math.max(1f, k.u * 0.5f), 0x0FFFFFFF, 0f);
            k.text.setColor(0x66FFDCA0);
            c.drawText(Config.meters(y) + " m", w - k.px(10f), sy - k.px(4f), k.text);
        }
    }

    private void drawMines(Canvas c, Game g, int w, float ppc, float anchor, float camY,
                           float shx, float shy, float time) {
        for (Mines.Mine m : g.mines.list) {
            if (m.dead) continue;
            float px = w / 2f + m.x * ppc + shx;
            float py = anchor + (m.y - camY) * ppc + shy;
            if (px < -80 || px > w + 80 || py < -80 || py > g.height + 80) continue;
            boolean blink = m.armed
                    && (float) Math.sin(time * (m.fuse >= 0f ? 40f : 11f) + m.phase) > 0f;

            c.save();
            c.translate(px, py);
            k.line.setColor(0xFF23262C);
            k.line.setStrokeWidth(Math.max(2f, ppc * 0.12f));
            for (int i = 0; i < 6; i++) {
                double a = (i / 6.0) * Math.PI * 2 + 0.3;
                c.drawLine((float) Math.cos(a) * ppc * 0.26f, (float) Math.sin(a) * ppc * 0.26f,
                        (float) Math.cos(a) * ppc * 0.55f, (float) Math.sin(a) * ppc * 0.55f, k.line);
            }
            k.fill.setColor(0xFF3C3F46);
            c.drawCircle(0, 0, ppc * 0.34f, k.fill);
            k.fill.setColor(blink ? 0xFFFF3B2F : 0xFF6B2A26);
            c.drawCircle(0, 0, ppc * 0.15f, k.fill);
            if (blink) {
                k.fill.setColor(0xFFFF3B2F);
                k.fill.setAlpha(55);
                c.drawCircle(0, 0, ppc * 0.7f, k.fill);
                k.fill.setAlpha(255);
            }
            c.restore();
        }
    }

    private void drawPlatform(Canvas c, Game g, int w, float ppc, float anchor, float camY,
                              float shx, float shy) {
        if (g.drill == null) return;
        float cx = w / 2f + g.drill.x * ppc + shx;
        float top = anchor + (g.drill.y + g.drill.radius - 0.1f - camY) * ppc + shy;
        float half = ppc * 4.4f;
        float thick = ppc * 0.55f;
        float ground = anchor + (Config.GROUND_Y - camY) * ppc + shy;

        k.fill.setColor(0xFF2B2F36);
        c.drawRect(cx - half, top, cx + half, top + thick, k.fill);
        k.fill.setColor(0xFFE0A03A);
        float stripe = ppc * 0.5f;
        for (float x = cx - half; x < cx + half; x += stripe * 2f) {
            c.drawRect(x, top, Math.min(x + stripe, cx + half), top + thick * 0.35f, k.fill);
        }
        k.fill.setColor(0xFF3B414B);
        c.drawRect(cx - half + ppc * 0.4f, top + thick, cx - half + ppc * 0.9f, ground, k.fill);
        c.drawRect(cx + half - ppc * 0.9f, top + thick, cx + half - ppc * 0.4f, ground, k.fill);
        k.outline(c, cx - half, top, cx + half, top + thick, 0x66000000, 1.2f, 0f);
    }

    private void drawDrill(Canvas c, Game g, int w, float ppc, float anchor, float camY,
                           float shx, float shy, float time) {
        Drill d = g.drill;
        if (d == null) return;
        float px = w / 2f + d.x * ppc + shx;
        float py = anchor + (d.y - camY) * ppc + shy;

        c.save();
        c.translate(px, py);
        c.rotate((float) Math.toDegrees(d.angle));

        float bw = ppc * 2.3f;
        float bh = ppc * 2.6f;

        if (d.thrusting) {
            k.fill.setColor(0x55A8E8FF);
            c.drawCircle(0, 0, ppc * 1.7f, k.fill);
            k.fill.setColor(0xCCFFB03A);
            android.graphics.Path flame = new android.graphics.Path();
            float fb = bh * 0.96f;
            flame.moveTo(-bw * 0.24f, fb);
            flame.lineTo(bw * 0.24f, fb);
            flame.lineTo(0, fb + ppc * 1.0f);
            flame.close();
            c.drawPath(flame, k.fill);
            k.fill.setColor(0xE6BFE8FF);
            flame.reset();
            flame.moveTo(-bw * 0.13f, fb);
            flame.lineTo(bw * 0.13f, fb);
            flame.lineTo(0, fb + ppc * 0.55f);
            flame.close();
            c.drawPath(flame, k.fill);
            flame.reset();
            flame.close();
            flame.reset();
        }

        k.fill.setColor(0xFF3B414B);
        c.drawRect(-bw * 0.72f, -bh * 0.16f, -bw * 0.5f, bh * 0.42f, k.fill);
        c.drawRect(bw * 0.5f, -bh * 0.16f, bw * 0.72f, bh * 0.42f, k.fill);

        k.fill.setColor(0xFF7D8694);
        k.line.setColor(0xFF20242B);
        k.line.setStrokeWidth(Math.max(1.4f, ppc * 0.09f));
        float bodyL = -bw * 0.5f;
        float bodyT = -bh * 0.6f;
        c.drawRoundRect(bodyL, bodyT, bodyL + bw, bodyT + bh * 0.9f, ppc * 0.26f, ppc * 0.26f, k.fill);

        k.fill.setColor(0xFFE0A03A);
        c.drawRect(bodyL, -bh * 0.14f, bodyL + bw, -bh * 0.02f, k.fill);
        k.fill.setColor(0x59000000);
        for (int i = -2; i <= 2; i++) {
            c.drawRect(bodyL + bw * 0.5f + i * bw * 0.2f, -bh * 0.14f,
                    bodyL + bw * 0.5f + i * bw * 0.2f + bw * 0.06f, -bh * 0.02f, k.fill);
        }

        c.drawRoundRect(bodyL, bodyT, bodyL + bw, bodyT + bh * 0.9f, ppc * 0.26f, ppc * 0.26f, k.line);

        k.fill.setColor(d.inWater ? 0xFF7FD0FF : 0xFF9AD7FF);
        c.drawCircle(0, -bh * 0.34f, ppc * 0.19f, k.fill);

        float bitTop = bh * 0.27f;
        float bitBottom = bh * 1.02f;
        k.fill.setColor(0xFFC9D1DC);
        android.graphics.Path bit = new android.graphics.Path();
        bit.moveTo(-bw * 0.42f, bitTop);
        bit.lineTo(bw * 0.42f, bitTop);
        bit.lineTo(0, bitBottom);
        bit.close();
        c.drawPath(bit, k.fill);
        c.drawPath(bit, k.line);

        k.line.setColor(0x8C1E2228);
        k.line.setStrokeWidth(Math.max(1f, ppc * 0.05f));
        float spin = (time * (2.2f + Math.abs(d.vy) * 0.05f)) % 1f;
        for (int i = 0; i < 3; i++) {
            float t = (spin + i / 3f) % 1f;
            float yy = bitTop + (bitBottom - bitTop) * t;
            float ww = bw * (0.42f - 0.42f * t);
            c.drawLine(-ww, yy, ww, yy + ppc * 0.06f, k.line);
        }

        k.fill.setColor(d.hp < d.maxHp * 0.3f && (float) Math.sin(time * 10f) > 0f
                ? 0xFFFF4A2A : 0xFF7A2A20);
        c.drawCircle(0, -bh * 0.52f, ppc * 0.085f, k.fill);

        c.restore();
    }

    private void drawParticles(Canvas c, Game g, int w, float ppc, float anchor,
                               float camY, float shx, float shy) {
        for (Particles.P p : g.particles.items) {
            if (g.save.reduceFx && p.type == Particles.SMOKE) continue;
            float px = w / 2f + p.x * ppc + shx;
            float py = anchor + (p.y - camY) * ppc + shy;
            if (px < -60 || px > w + 60 || py < -60 || py > g.height + 60) continue;
            float a = Config.clamp(p.life / p.maxLife, 0f, 1f);

            if (p.type == Particles.TEXT) {
                k.text.setTypeface(k.bold);
                k.text.setTextSize(ppc * p.size);
                k.text.setColor(p.color);
                k.text.setAlpha((int) (255 * a));
                k.text.setTextAlign(Paint.Align.CENTER);
                c.drawText(p.text, px, py, k.text);
                k.text.setAlpha(255);
                continue;
            }

            k.fill.setColor(p.color);
            k.fill.setAlpha((int) (255 * a));
            if (p.type == Particles.SPARK) {
                k.line.setColor(p.color);
                k.line.setAlpha((int) (255 * a));
                k.line.setStrokeWidth(Math.max(1.5f, ppc * 0.09f));
                c.drawLine(px, py, px - p.vx * 0.03f * ppc, py - p.vy * 0.03f * ppc, k.line);
                k.line.setAlpha(255);
            } else if (p.type == Particles.SMOKE) {
                k.fill.setAlpha((int) (255 * a * 0.45f));
                c.drawCircle(px, py, ppc * p.size * (1.8f - a), k.fill);
            } else {
                float s = Math.max(1.8f, ppc * p.size);
                c.drawRect(px - s / 2f, py - s / 2f, px + s / 2f, py + s / 2f, k.fill);
            }
            k.fill.setAlpha(255);
        }
    }

    private void drawSpeedLines(Canvas c, Game g, int w, int h, float ppc, float time) {
        if (g.save.reduceFx || g.drill == null || g.drill.vy < 42f) return;
        float strength = Config.clamp((g.drill.vy - 42f) / 12f, 0f, 1f);
        k.line.setColor(0x14FFFFFF);
        k.line.setStrokeWidth(2f * k.u);
        for (int i = 0; i < 9; i++) {
            float x = (i * 97 + (int) (time * 400f)) % w;
            float y = (i * 173 + (int) (time * 900f)) % h;
            c.drawLine(x, y, x, y - ppc * 4.5f * strength, k.line);
        }
    }

    private void drawHud(Canvas c, Game g, int w, int h, int safeTop, int safeBottom) {
        if (g.drill == null || g.run == null) return;
        float u = k.u;
        float top = safeTop + u * 8f;

        // Soft top scrim keeps the HUD readable over the sky and the surface.
        android.graphics.LinearGradient scrim = new android.graphics.LinearGradient(
                0, 0, 0, safeTop + u * 96f, 0xB3000000, 0x00000000,
                android.graphics.Shader.TileMode.CLAMP);
        bgPaint.setShader(scrim);
        c.drawRect(0, 0, w, safeTop + u * 96f, bgPaint);
        bgPaint.setShader(null);

        k.spaced(c, "DEPTH", u * 14f, top + u * 10f, 9f, 0xFF8B95A6, Paint.Align.LEFT, true, 0.2f);
        String depthStr = String.valueOf(Config.meters(g.drill.y));
        k.shadowText(c, depthStr, u * 14f, top + u * 40f,
                34f, 0xFFE9EEF6, Paint.Align.LEFT, true, 3f, 0xAA000000);
        k.text.setTypeface(k.bold);
        k.text.setTextSize(k.px(34f));
        float depthW = k.text.measureText(depthStr);
        k.txt(c, "m", u * 14f + depthW + u * 2f, top + u * 40f,
                14f, 0xFFFFB03A, Paint.Align.LEFT, true);
        int best = Math.max(g.save.bestDepth, g.run.currentDepthMeters());
        k.spaced(c, "BEST " + best + " m", u * 14f, top + u * 54f, 8f, 0xFF8B95A6,
                Paint.Align.LEFT, true, 0.14f);

        String money = String.valueOf(g.run.moneyEarned);
        float pillW = k.px(22f) + k.px(8f) * money.length() + u * 30f;
        float pillR = w - u * 68f;
        float pillL = pillR - pillW;
        k.rect(c, pillL, top + u * 2f, pillR, top + u * 26f, 0x8C080A0E, 13f);
        k.outline(c, pillL, top + u * 2f, pillR, top + u * 26f, 0x18FFFFFF, 1f, 13f);
        k.fill.setColor(0xFFFFB03A);
        c.drawCircle(pillL + u * 14f, top + u * 14f, u * 4.5f, k.fill);
        k.txt(c, money, pillL + u * 24f, top + u * 19f, 13f, 0xFFE9EEF6, Paint.Align.LEFT, true);

        float bw = u * 40f;
        float bl = w - u * 54f;
        k.rect(c, bl, top + u * 4f, bl + bw, top + u * 44f, 0x99080A0E, 12f);
        k.outline(c, bl, top + u * 4f, bl + bw, top + u * 44f, 0x18FFFFFF, 1f, 12f);
        k.fill.setColor(0xFFE9EEF6);
        c.drawRect(bl + u * 15f, top + u * 16f, bl + u * 18f, top + u * 32f, k.fill);
        c.drawRect(bl + u * 23f, top + u * 16f, bl + u * 26f, top + u * 32f, k.fill);
        g.addHit("pause", bl, top, bl + bw, top + u * 46f);

        float barTop = top + u * 62f;
        float barL = u * 50f;
        float barW = Math.min(u * 250f, w - u * 120f);
        k.spaced(c, "HULL", u * 14f, barTop + u * 7f, 8f, 0xFF8B95A6, Paint.Align.LEFT, true, 0.12f);
        float hullFrac = Config.clamp(g.drill.hp / g.drill.maxHp, 0f, 1f);
        int hullColor = hullFrac > 0.55f ? 0xFF4FD18B : (hullFrac > 0.28f ? 0xFFFFC558 : 0xFFE3392C);
        k.bar(c, barL, barTop, barL + barW, barTop + u * 7f, hullFrac, hullColor);
        k.txt(c, String.valueOf((int) Math.ceil(g.drill.hp)), barL + barW + u * 8f, barTop + u * 7f,
                8f, 0xFF8B95A6, Paint.Align.LEFT, true);

        // ------------------------------------------------ reverse thruster button
        float cap = g.run.stats.boostCapacity;
        float frac = cap > 0.001f ? Config.clamp(g.boostFuel / cap, 0f, 1f) : 0f;
        boolean stunned = g.drill.stunTimer > 0f;
        float br = u * 38f;
        float bcx = w - u * 56f;
        float bcy = h - safeBottom - u * 66f;
        k.fill.setColor(0xB3080A0E);
        c.drawCircle(bcx, bcy, br + u * 5f, k.fill);

        k.line.setStyle(Paint.Style.STROKE);
        k.line.setStrokeWidth(u * 3f);
        k.line.setColor(0x33FFFFFF);
        c.drawCircle(bcx, bcy, br, k.line);

        arcRect.set(bcx - br, bcy - br, bcx + br, bcy + br);
        int fuelColor = stunned ? 0x66FF5747 : (frac > 0.15f
                ? (g.thrustHeld ? 0xFFFFD166 : 0xFFFFB03A) : 0x66FF5747);
        k.line.setColor(fuelColor);
        k.line.setStrokeWidth(u * 5f);
        c.drawArc(arcRect, -90f, 360f * frac, false, k.line);
        k.line.setStyle(Paint.Style.FILL);

        k.fill.setColor(stunned ? 0xAAFF5747 : 0xFFE9EEF6);
        android.graphics.Path arrow = new android.graphics.Path();
        arrow.moveTo(bcx, bcy - u * 20f);
        arrow.lineTo(bcx + u * 9f, bcy - u * 7f);
        arrow.lineTo(bcx - u * 9f, bcy - u * 7f);
        arrow.close();
        c.drawPath(arrow, k.fill);
        k.spaced(c, stunned ? "STUN" : "BOOST", bcx, bcy + u * 20f, 7.5f,
                stunned ? 0xFFFF5747 : 0xFF9FB4C7, Paint.Align.CENTER, true, 0.12f);
        g.addHit("boost", bcx - br - u * 8f, bcy - br - u * 8f,
                bcx + br + u * 8f, bcy + br + u * 8f);

        if (stunned) {
            float alpha = Config.clamp(g.drill.stunTimer, 0f, 1f);
            k.fill.setColor(0xCC140A0A);
            c.drawRoundRect(w / 2f - u * 64f, h * 0.36f - u * 18f,
                    w / 2f + u * 64f, h * 0.36f + u * 18f, u * 12f, u * 12f, k.fill);
            k.txt(c, "STUNNED  " + String.format("%.1f", g.drill.stunTimer) + "s",
                    w / 2f, h * 0.36f + u * 6f, 12f, 0xFFFF6A4D, Paint.Align.CENTER, true);
        }

        if (g.toastTimer > 0f && g.toastText != null && !g.toastText.isEmpty()) {
            float alpha = Config.clamp(g.toastTimer / 0.35f, 0f, 1f);
            float tw = Math.min(k.px(12f) * (g.toastText.length() * 0.62f + 1.6f), w - u * 32f);
            float cx = w / 2f;
            float cy = h * 0.8f;
            k.fill.setColor(0xEB0C0E13);
            k.fill.setAlpha((int) (235 * alpha));
            c.drawRoundRect(cx - tw / 2f, cy - u * 16f, cx + tw / 2f, cy + u * 16f,
                    u * 16f, u * 16f, k.fill);
            k.fill.setAlpha(255);
            k.text.setTypeface(k.bold);
            k.text.setTextSize(k.px(10.5f));
            k.text.setTextAlign(Paint.Align.CENTER);
            k.text.setColor(0xFFFFB03A);
            k.text.setAlpha((int) (255 * alpha));
            c.drawText(g.toastText, cx, cy + k.px(4f), k.text);
            k.text.setAlpha(255);
        }
    }

    private void drawVignette(Canvas c, Game g, int w, int h) {
        if (g.drill != null && g.state != Game.MENU && g.state != Game.SURFACE) {
            float hpFrac = Config.clamp(g.drill.hp / g.drill.maxHp, 0f, 1f);
            if (hpFrac < 0.35f) {
                float pulse = 0.5f + 0.5f * (float) Math.sin(g.time * 6f);
                k.fill.setColor(0xFFC81414);
                k.fill.setAlpha((int) (255 * (0.35f - hpFrac) * 0.9f * pulse));
                c.drawRect(0, 0, w, h, k.fill);
                k.fill.setAlpha(255);
            }
        }
        if (g.damageFlash > 0.001f) {
            k.fill.setColor(0xFFFF2020);
            k.fill.setAlpha((int) (255 * 0.38f * Config.clamp(g.damageFlash, 0f, 1f)));
            c.drawRect(0, 0, w, h, k.fill);
            k.fill.setAlpha(255);
        }
        if (g.flash > 0.01f) {
            k.fill.setColor(0xFFFF7828);
            k.fill.setAlpha((int) (255 * Math.min(0.45f, g.flash * 0.45f)));
            c.drawRect(0, 0, w, h, k.fill);
            k.fill.setAlpha(255);
        }
    }
}
