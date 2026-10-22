package com.deepfalldrill.game;

import android.graphics.Canvas;
import android.graphics.Paint;

/** Immediate-mode menus drawn on the same canvas as the world. */
public final class UiRenderer {
    private final UiKit k;

    private static final int AMBER = 0xFFFFB03A;
    private static final int AMBER_DARK = 0xFF9C550D;
    private static final int TEXT = 0xFFE9EEF6;
    private static final int MUTED = 0xFF8B95A6;
    private static final int DANGER = 0xFFFF5747;
    private static final int GREEN = 0xFF4FD18B;

    public UiRenderer(UiKit kit) {
        this.k = kit;
    }

    public void draw(Canvas c, Game g, int w, int h, int safeTop, int safeBottom) {
        switch (g.state) {
            case Game.MENU:
                drawMenu(c, g, w, h, safeTop, safeBottom);
                break;
            case Game.CHOOSE_INPUT:
                drawInputSelect(c, g, w, h, safeTop, safeBottom);
                break;
            case Game.PAUSED:
                drawPause(c, g, w, h, safeTop, safeBottom);
                break;
            case Game.MILESTONE:
                drawUpgrade(c, g, w, h, safeTop, safeBottom);
                break;
            case Game.DEAD:
                drawDeath(c, g, w, h, safeTop, safeBottom);
                break;
            case Game.WORKSHOP:
                drawWorkshop(c, g, w, h, safeTop, safeBottom);
                break;
            case Game.SETTINGS:
                drawSettings(c, g, w, h, safeTop, safeBottom);
                break;
            case Game.SURFACE:
                drawSurface(c, g, w, h, safeTop, safeBottom);
                break;
            default:
                break;
        }
        if (g.state != Game.PLAYING && g.state != Game.DYING && g.state != Game.PAUSED
                && g.state != Game.MILESTONE) {
            drawToast(c, g, w, h);
        }
    }

    // ------------------------------------------------------------------ kit
    private void btn(Canvas c, Game g, String id, String label, float l, float t, float w, float h, int style) {
        float r = k.px(13f);
        if (style == 0) {
            k.rect(c, l, t + k.px(4f), l + w, t + h + k.px(4f), AMBER_DARK, 13f);
            k.rect(c, l, t, l + w, t + h, AMBER, 13f);
            k.txt(c, label, l + w / 2f, t + h / 2f + k.px(5f), 14f, 0xFF1B1004, Paint.Align.CENTER, true);
        } else if (style == 1) {
            k.rect(c, l, t, l + w, t + h, 0x14FFFFFF, 13f);
            k.outline(c, l, t, l + w, t + h, 0x1FFFFFFF, 1f, 13f);
            k.txt(c, label, l + w / 2f, t + h / 2f + k.px(5f), 14f, 0xFFCFD8E6, Paint.Align.CENTER, true);
        } else if (style == 2) {
            k.rect(c, l, t, l + w, t + h, 0x1AFF5747, 13f);
            k.outline(c, l, t, l + w, t + h, 0x66FF5747, 1f, 13f);
            k.txt(c, label, l + w / 2f, t + h / 2f + k.px(5f), 13f, DANGER, Paint.Align.CENTER, true);
        } else {
            k.rect(c, l, t, l + w, t + h, 0x1AFFB03A, 13f);
            k.outline(c, l, t, l + w, t + h, 0x66FFB03A, 1f, 13f);
            k.txt(c, label, l + w / 2f, t + h / 2f + k.px(5f), 13f, AMBER, Paint.Align.CENTER, true);
        }
        g.addHit(id, l - k.px(4f), t - k.px(4f), l + w + k.px(4f), t + h + k.px(4f));
        if (r <= 0f) {
            // never (keeps lint quiet about unused)
        }
    }

    private void kicker(Canvas c, String s, float x, float y, Paint.Align align) {
        k.spaced(c, s, x, y, 9f, AMBER, align, true, 0.26f);
    }

    private void title(Canvas c, String s, float x, float y) {
        k.txt(c, s, x, y, 24f, TEXT, Paint.Align.LEFT, true);
    }

    private void statBox(Canvas c, float l, float t, float w, float h, String value, String label) {
        k.rect(c, l, t, l + w, t + h, 0x0DFFFFFF, 12f);
        k.outline(c, l, t, l + w, t + h, 0x16FFFFFF, 1f, 12f);
        k.txt(c, value, l + w / 2f, t + h * 0.52f, 20f, TEXT, Paint.Align.CENTER, true);
        k.spaced(c, label, l + w / 2f, t + h - k.px(10f), 8f, MUTED, Paint.Align.CENTER, true, 0.16f);
    }

    private void chips(Canvas c, Game g, String idPrefix, String selected,
                       float l, float t, float w, float h) {
        k.rect(c, l, t, l + w, t + h, 0x59000000, 11f);
        k.outline(c, l, t, l + w, t + h, 0x16FFFFFF, 1f, 11f);
        String[] modes = {"zones", "drag", "tilt"};
        String[] labels = {"ZONES", "DRAG", "TILT"};
        float gap = k.px(4f);
        float cw = (w - gap * 2f - gap * 2f) / 3f;
        for (int i = 0; i < 3; i++) {
            float x = l + gap + i * (cw + gap);
            boolean active = modes[i].equals(selected);
            if (active) {
                k.rect(c, x, t + gap, x + cw, t + h - gap, AMBER, 8f);
                k.txt(c, labels[i], x + cw / 2f, t + h / 2f + k.px(4f), 10f, 0xFF1B1004, Paint.Align.CENTER, true);
            } else {
                k.txt(c, labels[i], x + cw / 2f, t + h / 2f + k.px(4f), 10f, MUTED, Paint.Align.CENTER, true);
            }
            g.addHit(idPrefix + modes[i], x, t, x + cw, t + h);
        }
    }

    private void toggle(Canvas c, Game g, String id, String label, boolean on, float l, float t, float w) {
        float h = k.px(44f);
        k.rect(c, l, t, l + w, t + h - k.px(1f), 0x08FFFFFF, 0f);
        k.txt(c, label, l, t + h * 0.6f, 11f, 0xFFCFD8E6, Paint.Align.LEFT, true);
        float sw = k.px(42f);
        float sh = k.px(24f);
        float sx = l + w - sw;
        k.rect(c, sx, t + (h - sh) / 2f, sx + sw, t + (h + sh) / 2f, on ? 0xFFFFB03A : 0x33FFFFFF, 12f);
        k.fill.setColor(on ? 0xFF1B1004 : 0xFF9AA4B4);
        c.drawCircle(sx + (on ? sw - sh / 2f : sh / 2f), t + h / 2f, sh * 0.38f, k.fill);
        g.addHit(id, l, t, l + w, t + h);
    }

    private void toastWide(Canvas c, Game g, int w, int h) {
        if (g.toastTimer <= 0f || g.toastText == null || g.toastText.isEmpty()) return;
        float alpha = Config.clamp(g.toastTimer / 0.35f, 0f, 1f);
        float tw = Math.min(k.px(12f) * (g.toastText.length() * 0.62f + 1.6f), w - k.px(32f));
        float cx = w / 2f;
        float cy = h * 0.86f;
        k.fill.setColor(0xEB0C0E13);
        k.fill.setAlpha((int) (235 * alpha));
        c.drawRoundRect(cx - tw / 2f, cy - k.px(16f), cx + tw / 2f, cy + k.px(16f),
                k.px(16f), k.px(16f), k.fill);
        k.fill.setAlpha(255);
        k.txt(c, g.toastText, cx, cy + k.px(4f), 10.5f, AMBER, Paint.Align.CENTER, true);
    }

    private void drawToast(Canvas c, Game g, int w, int h) {
        toastWide(c, g, w, h);
    }

    // --------------------------------------------------------------- surface
    private void drawSurface(Canvas c, Game g, int w, int h, int safeTop, int safeBottom) {
        float u = k.u;

        // Small top bar over the sky.
        k.spaced(c, "MENU", u * 16f, safeTop + u * 26f, 9f, 0xCC14313F, Paint.Align.LEFT, true, 0.22f);
        g.addHit("menu", 0, safeTop, u * 88f, safeTop + u * 40f);
        k.spaced(c, "BEST " + g.save.bestDepth + " m", w - u * 16f, safeTop + u * 26f, 9f,
                0xCC14313F, Paint.Align.RIGHT, true, 0.16f);

        k.shadowText(c, "DEEPFALL DRILL", w / 2f, h * 0.34f, 25f, 0xFF10344A,
                Paint.Align.CENTER, true, 2f, 0x33FFFFFF);
        k.spaced(c, "READY TO DRILL", w / 2f, h * 0.385f, 10f, 0xB01B3A4B,
                Paint.Align.CENTER, true, 0.3f);

        // Bottom scrim keeps the button readable over grass and dirt.
        android.graphics.LinearGradient scrim = new android.graphics.LinearGradient(
                0, h * 0.6f, 0, h, 0x0006070A, 0xF206070A,
                android.graphics.Shader.TileMode.CLAMP);
        k.fill.setShader(scrim);
        c.drawRect(0, h * 0.6f, w, h, k.fill);
        k.fill.setShader(null);

        float bw = Math.min(w - u * 60f, u * 300f);
        float bx = (w - bw) / 2f;
        float by = h - safeBottom - u * 150f;
        btn(c, g, "start_drill", "START DRILL", bx, by, bw, u * 60f, 0);
        k.spaced(c, "STEER: " + g.inputMode().toUpperCase(), w / 2f, by + u * 84f, 9f,
                0xFFE9EEF6, Paint.Align.CENTER, true, 0.18f);
        k.spaced(c, "MINE DEEP. STAY HEAVY.", w / 2f, by + u * 102f, 8f,
                0x99FFFFFF, Paint.Align.CENTER, true, 0.2f);
    }

    // ----------------------------------------------------------------- menu
    private void drawMenu(Canvas c, Game g, int w, int h, int safeTop, int safeBottom) {
        k.dim(c);
        float u = k.u;
        float cw = Math.min(w - u * 40f, u * 400f);
        float x = (w - cw) / 2f;
        float cy = h * 0.21f;

        k.shadowText(c, "DEEP", w / 2f, cy, 52f, 0xFFD7E2F0, Paint.Align.CENTER, true, 5f, 0x99000000);
        k.shadowText(c, "FALL", w / 2f, cy + u * 48f, 52f, AMBER, Paint.Align.CENTER, true, 8f, 0xAA8A4A08);
        k.spaced(c, "DRILL", w / 2f, cy + u * 78f, 17f, MUTED, Paint.Align.CENTER, true, 0.5f);
        k.spaced(c, "GRAVITY PULLS. YOU STEER.", w / 2f, cy + u * 104f, 9f, MUTED,
                Paint.Align.CENTER, true, 0.2f);

        float statsY = cy + u * 122f;
        float gap = u * 8f;
        float sw = (cw - gap * 2f) / 3f;
        statBox(c, x, statsY, sw, u * 62f, String.valueOf(g.save.bestDepth), "BEST m");
        statBox(c, x + sw + gap, statsY, sw, u * 62f, String.valueOf(g.save.money), "ORE");
        statBox(c, x + (sw + gap) * 2f, statsY, sw, u * 62f, String.valueOf(g.save.level), "LEVEL");

        float y = statsY + u * 78f;
        btn(c, g, "play", "PLAY", x, y, cw, u * 54f, 0);
        y += u * 66f;

        if (g.save.checkpoint != null) {
            int depth = Math.round(g.save.checkpoint.depth * Config.METERS_PER_CELL);
            btn(c, g, "resume", "RESUME  ·  " + depth + " m", x, y, cw, u * 52f, 3);
            y += u * 64f;
        }

        float half = (cw - u * 8f) / 2f;
        btn(c, g, "hub", "WORKSHOP", x, y, half, u * 50f, 1);
        btn(c, g, "settings", "SETTINGS", x + half + u * 8f, y, half, u * 50f, 1);

        k.spaced(c, "v0.1 · NATIVE ENDLESS", w / 2f, h - safeBottom - u * 22f, 8f,
                0x44FFFFFF, Paint.Align.CENTER, true, 0.22f);
    }

    // ---------------------------------------------------------- choose input
    private void drawInputSelect(Canvas c, Game g, int w, int h, int safeTop, int safeBottom) {
        k.dim(c);
        float u = k.u;
        float pw = Math.min(w - u * 28f, u * 400f);
        float contentW = pw - u * 36f;
        float cardH = u * 78f;
        float ph = u * 22f + u * 16f + u * 34f + u * 22f + (cardH + u * 10f) * 3f + u * 70f;
        float pl = (w - pw) / 2f;
        float pt = Math.max(safeTop + u * 12f, (h - ph) / 2f);
        k.panel(c, pl, pt, pl + pw, pt + ph);

        float x = pl + u * 18f;
        float y = pt + u * 26f;
        kicker(c, "ONE-TIME SETUP", x, y, Paint.Align.LEFT);
        title(c, "CHOOSE STEERING", x, y + u * 28f);
        k.txt(c, "You can change this any time from the pause menu.", x, y + u * 50f, 11f,
                MUTED, Paint.Align.LEFT, false);

        float cy = y + u * 62f;
        modeCard(c, g, "chip:zones", "◧  ◨", "Two Touch Zones",
                "Hold the left or right half of the screen.", "zones".equals(g.pendingMode),
                x, cy, contentW, cardH);
        cy += cardH + u * 10f;
        modeCard(c, g, "chip:drag", "↔", "Drag To Steer",
                "Touch anywhere and drag left or right.", "drag".equals(g.pendingMode),
                x, cy, contentW, cardH);
        cy += cardH + u * 10f;
        modeCard(c, g, "chip:tilt", "⟲", "Tilt",
                "Tilt the phone left or right. Keep it steady.", "tilt".equals(g.pendingMode),
                x, cy, contentW, cardH);

        btn(c, g, "input_start", "START DRILL", x, pt + ph - u * 80f, contentW, u * 54f, 0);
    }

    private void modeCard(Canvas c, Game g, String id, String icon, String name, String desc,
                          boolean selected, float l, float t, float w, float h) {
        k.rect(c, l, t, l + w, t + h, selected ? 0x1AFFB03A : 0x08FFFFFF, 14f);
        k.outline(c, l, t, l + w, t + h, selected ? AMBER : 0x16FFFFFF, selected ? 1.4f : 1f, 14f);
        float is = k.px(44f);
        k.rect(c, l + k.px(12f), t + (h - is) / 2f, l + k.px(12f) + is, t + (h + is) / 2f,
                0x59000000, 11f);
        k.txt(c, icon, l + k.px(12f) + is / 2f, t + h / 2f + k.px(6f), 17f, AMBER, Paint.Align.CENTER, true);
        k.txt(c, name, l + k.px(66f), t + h * 0.44f, 13.5f, TEXT, Paint.Align.LEFT, true);
        k.txt(c, desc, l + k.px(66f), t + h * 0.66f, 10f, MUTED, Paint.Align.LEFT, false);
        g.addHit(id, l, t, l + w, t + h);
    }

    // ---------------------------------------------------------------- pause
    private void drawPause(Canvas c, Game g, int w, int h, int safeTop, int safeBottom) {
        k.dim(c);
        float u = k.u;
        float pw = Math.min(w - u * 28f, u * 390f);
        float ph = u * 22f + u * 30f + u * 86f + u * 26f + u * 18f + u * 44f + u * 18f + u * 54f * 3f + u * 28f;
        float pl = (w - pw) / 2f;
        float pt = Math.max(safeTop + u * 10f, (h - ph) / 2f);
        k.panel(c, pl, pt, pl + pw, pt + ph);

        float x = pl + u * 18f;
        float cw = pw - u * 36f;
        float y = pt + u * 26f;
        title(c, "PAUSED", x, y + u * 18f);

        float seedY = y + u * 32f;
        k.rect(c, x, seedY, x + cw, seedY + u * 68f, 0xFF050609, 12f);
        k.outline(c, x, seedY, x + cw, seedY + u * 68f, 0x16FFFFFF, 1f, 12f);
        k.spaced(c, "SEED", x + cw / 2f, seedY + u * 16f, 8f, MUTED, Paint.Align.CENTER, true, 0.28f);
        k.text.setTypeface(k.mono);
        k.text.setTextSize(k.px(21f));
        k.text.setColor(AMBER);
        k.text.setTextAlign(Paint.Align.CENTER);
        c.drawText(Rng.toCode(g.seedCodeSeed()), x + cw / 2f, seedY + u * 42f, k.text);
        k.txt(c, "Endless mode. Same seed, same world.", x + cw / 2f, seedY + u * 58f, 9f,
                MUTED, Paint.Align.CENTER, false);

        float dy = seedY + u * 86f;
        k.spaced(c, "DEPTH", x + u * 2f, dy, 9f, MUTED, Paint.Align.LEFT, true, 0.2f);
        k.txt(c, String.valueOf(Config.meters(g.drill != null ? g.drill.y : 0)) + " m",
                x + cw - u * 2f, dy, 15f, TEXT, Paint.Align.RIGHT, true);

        k.spaced(c, "STEERING", x, dy + u * 22f, 8f, MUTED, Paint.Align.LEFT, true, 0.2f);
        chips(c, g, "mode:", g.inputMode(), x, dy + u * 28f, cw, u * 40f);

        float by = dy + u * 82f;
        btn(c, g, "resume_game", "RESUME", x, by, cw, u * 54f, 0);
        btn(c, g, "restart", "RESTART RUN", x, by + u * 62f, cw, u * 50f, 1);
        btn(c, g, "quit", "QUIT TO MENU", x, by + u * 122f, cw, u * 50f, 1);
    }

    // -------------------------------------------------------------- upgrades
    private void drawUpgrade(Canvas c, Game g, int w, int h, int safeTop, int safeBottom) {
        k.dim(c);
        float u = k.u;
        float pw = Math.min(w - u * 24f, u * 440f);
        float cardH = u * 86f;
        float ph = u * 22f + u * 16f + u * 32f + u * 24f + cardH * 3f + u * 20f + u * 20f;
        float pl = (w - pw) / 2f;
        float pt = Math.max(safeTop + u * 10f, (h - ph) / 2f);
        k.panel(c, pl, pt, pl + pw, pt + ph);

        float x = pl + u * 18f;
        float cw = pw - u * 36f;
        float y = pt + u * 26f;
        kicker(c, "MILESTONE REACHED", x, y, Paint.Align.LEFT);
        title(c, "CHOOSE AN UPGRADE", x, y + u * 28f);
        int depth = g.drill != null ? Config.meters(g.drill.y) : 0;
        k.txt(c, depth + " m", x, y + u * 48f, 11f, MUTED, Paint.Align.LEFT, false);

        float cy = y + u * 62f;
        for (Upgrades.RunUpgrade up : g.choices) {
            upgradeCard(c, g, up, x, cy, cw, cardH);
            cy += cardH + u * 8f;
        }
    }

    private void upgradeCard(Canvas c, Game g, Upgrades.RunUpgrade up,
                             float l, float t, float w, float h) {
        int rarity = Upgrades.rarityColor(up.rarity);
        k.rect(c, l, t, l + w, t + h, 0x0DFFFFFF, 14f);
        k.outline(c, l, t, l + w, t + h, 0x1AFFFFFF, 1f, 14f);
        k.rect(c, l, t + k.px(3f), l + k.px(3f), t + h - k.px(3f), rarity, 2f);
        k.txt(c, up.name, l + k.px(16f), t + h * 0.38f, 14.5f, TEXT, Paint.Align.LEFT, true);
        k.spaced(c, up.rarity.toUpperCase(), l + w - k.px(14f), t + h * 0.36f, 8f, rarity,
                Paint.Align.RIGHT, true, 0.18f);
        k.txt(c, up.desc, l + k.px(16f), t + h * 0.68f, 11f, MUTED, Paint.Align.LEFT, false);
        g.addHit("upgrade:" + up.id, l, t, l + w, t + h);
    }

    // ---------------------------------------------------------------- death
    private void drawDeath(Canvas c, Game g, int w, int h, int safeTop, int safeBottom) {
        k.dim(c);
        float u = k.u;
        Run.Summary s = g.lastSummary;
        if (s == null) return;
        float pw = Math.min(w - u * 28f, u * 400f);
        float ph = u * 430f;
        float pl = (w - pw) / 2f;
        float pt = Math.max(safeTop + u * 10f, (h - ph) / 2f);
        k.panel(c, pl, pt, pl + pw, pt + ph);

        float x = pl + u * 18f;
        float cw = pw - u * 36f;
        float y = pt + u * 26f;
        kicker(c, "HULL FAILURE", x, y, Paint.Align.LEFT);
        String joke = (g.deathJoke == null || g.deathJoke.isEmpty()) ? "DRILL LOST" : g.deathJoke;
        k.txt(c, joke, x, y + u * 28f, joke.length() > 18 ? 19f : 24f, TEXT,
                Paint.Align.LEFT, true);

        k.shadowText(c, String.valueOf(s.depthMeters), w / 2f, y + u * 84f, 56f, AMBER,
                Paint.Align.CENTER, true, 8f, 0x553A1E04);
        k.txt(c, "m", w / 2f + k.px(46f), y + u * 84f, 16f, MUTED, Paint.Align.LEFT, true);

        float gy = y + u * 96f;
        float gap = u * 8f;
        float sw = (cw - gap) / 2f;
        statBox(c, x, gy, sw, u * 54f, String.valueOf(s.money), "ORE");
        statBox(c, x + sw + gap, gy, sw, u * 54f, String.valueOf(s.xp), "XP");
        statBox(c, x, gy + u * 62f, sw, u * 54f, String.valueOf(s.oreCount), "ORE FOUND");
        statBox(c, x + sw + gap, gy + u * 62f, sw, u * 54f, String.valueOf(s.score), "SCORE");

        float ny = gy + u * 128f;
        if (g.lastNewBest) {
            k.spaced(c, "NEW BEST DEPTH", w / 2f, ny, 9.5f, AMBER, Paint.Align.CENTER, true, 0.22f);
        }
        if (g.lastLevelUps != null && !g.lastLevelUps.isEmpty()) {
            k.spaced(c, "LEVEL UP  " + g.lastLevelUps.get(g.lastLevelUps.size() - 1), w / 2f,
                    ny + u * 18f, 10f, GREEN, Paint.Align.CENTER, true, 0.18f);
        }

        float by = pt + ph - u * 138f;
        btn(c, g, "retry", "DIVE AGAIN", x, by, cw, u * 54f, 0);
        btn(c, g, "menu", "MENU", x, by + u * 62f, cw, u * 48f, 1);
    }

    // ------------------------------------------------------------- workshop
    private void drawWorkshop(Canvas c, Game g, int w, int h, int safeTop, int safeBottom) {
        k.dim(c);
        float u = k.u;
        float pw = Math.min(w - u * 24f, u * 440f);
        float pl = (w - pw) / 2f;
        float pt = safeTop + u * 16f;
        float pb = h - safeBottom - u * 16f;
        k.panel(c, pl, pt, pl + pw, pb);

        float x = pl + u * 18f;
        float cw = pw - u * 36f;
        float y = pt + u * 22f;
        title(c, "WORKSHOP", x, y + u * 20f);
        k.fill.setColor(AMBER);
        c.drawCircle(pl + pw - u * 46f, y + u * 12f, u * 4f, k.fill);
        k.txt(c, String.valueOf(g.save.money), pl + pw - u * 38f, y + u * 17f, 16f, AMBER,
                Paint.Align.LEFT, true);
        k.txt(c, "Permanent upgrades. They survive every drill.", x, y + u * 40f, 10.5f,
                MUTED, Paint.Align.LEFT, false);

        float listTop = y + u * 52f;
        float listBottom = pb - u * 76f;
        float rowH = (listBottom - listTop) / 6f;
        float ry = listTop;
        for (Upgrades.PermUpgrade up : Upgrades.PERM) {
            permRow(c, g, up, x, ry, cw, rowH - u * 6f);
            ry += rowH;
        }

        btn(c, g, "back", "BACK", x, pb - u * 64f, cw, u * 52f, 0);
    }

    private void permRow(Canvas c, Game g, Upgrades.PermUpgrade up,
                         float l, float t, float w, float h) {
        int lvl = g.save.perm.getOrDefault(up.id, 0);
        boolean maxed = lvl >= up.max;
        int cost = up.cost(lvl);
        boolean afford = g.save.money >= cost;

        k.rect(c, l, t, l + w, t + h, 0x08FFFFFF, 12f);
        k.outline(c, l, t, l + w, t + h, 0x14FFFFFF, 1f, 12f);

        k.txt(c, up.name, l + k.px(12f), t + k.px(20f), 12.5f, TEXT, Paint.Align.LEFT, true);
        float pipY = t + k.px(28f);
        for (int i = 0; i < up.max; i++) {
            float pipW = k.px(8f);
            k.rect(c, l + k.px(12f) + i * (pipW + k.px(2f)), pipY,
                    l + k.px(12f) + i * (pipW + k.px(2f)) + pipW, pipY + k.px(4f),
                    i < lvl ? AMBER : 0x22FFFFFF, 2f);
        }
        k.txt(c, up.desc, l + k.px(12f), t + h - k.px(8f), 9.5f, MUTED, Paint.Align.LEFT, false);

        float bw = k.px(70f);
        float bh = k.px(32f);
        float bx = l + w - bw - k.px(10f);
        float by = t + (h - bh) / 2f;
        if (maxed) {
            k.rect(c, bx, by, bx + bw, by + bh, 0x14FFFFFF, 9f);
            k.txt(c, "MAX", bx + bw / 2f, by + bh / 2f + k.px(4f), 10f, MUTED, Paint.Align.CENTER, true);
        } else {
            k.rect(c, bx, by, bx + bw, by + bh, afford ? 0x1AFFB03A : 0x0DFFFFFF, 9f);
            k.outline(c, bx, by, bx + bw, by + bh, afford ? 0x66FFB03A : 0x18FFFFFF, 1f, 9f);
            k.txt(c, cost + " ORE", bx + bw / 2f, by + bh / 2f + k.px(4f), 9.5f,
                    afford ? AMBER : 0x66FFFFFF, Paint.Align.CENTER, true);
            g.addHit("buy:" + up.id, bx, by, bx + bw, by + bh);
        }
    }

    // -------------------------------------------------------------- settings
    private void drawSettings(Canvas c, Game g, int w, int h, int safeTop, int safeBottom) {
        k.dim(c);
        float u = k.u;
        float pw = Math.min(w - u * 28f, u * 390f);
        float ph = u * 462f;
        float pl = (w - pw) / 2f;
        float pt = Math.max(safeTop + u * 10f, (h - ph) / 2f);
        k.panel(c, pl, pt, pl + pw, pt + ph);

        float x = pl + u * 18f;
        float cw = pw - u * 36f;
        float y = pt + u * 26f;
        title(c, "SETTINGS", x, y + u * 18f);

        k.spaced(c, "STEERING", x, y + u * 44f, 8f, MUTED, Paint.Align.LEFT, true, 0.2f);
        chips(c, g, "mode:", g.inputMode(), x, y + u * 50f, cw, u * 40f);

        float ty = y + u * 100f;
        toggle(c, g, "toggle_sound", "SOUND", g.save.sound, x, ty, cw);
        toggle(c, g, "toggle_vibrate", "VIBRATION", g.save.vibrate, x, ty + u * 46f, cw);
        toggle(c, g, "toggle_fx", "REDUCE EFFECTS", g.save.reduceFx, x, ty + u * 92f, cw);

        k.spaced(c, "DATA", x, ty + u * 150f, 8f, MUTED, Paint.Align.LEFT, true, 0.2f);
        String resetLabel = g.resetConfirmTimer > 0f ? "CONFIRM RESET" : "RESET PROGRESS";
        btn(c, g, "reset", resetLabel, x, ty + u * 158f, cw, u * 46f, 2);

        btn(c, g, "back", "BACK", x, pt + ph - u * 72f, cw, u * 52f, 0);
    }
}
