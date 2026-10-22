package com.deepfalldrill.game;

import java.util.ArrayList;

/** Visual only: dust, sparks, smoke and floating text. */
public final class Particles {
    public static final int DUST = 0;
    public static final int SPARK = 1;
    public static final int SMOKE = 2;
    public static final int TEXT = 3;

    public static final class P {
        public int type;
        public float x;
        public float y;
        public float vx;
        public float vy;
        public float life;
        public float maxLife;
        public float size;
        public float gravity;
        public float drag;
        public float spin;
        public int color;
        public String text;
    }

    private static final int MAX = 420;
    public final ArrayList<P> items = new ArrayList<>(MAX);

    public void clear() {
        items.clear();
    }

    public P spawn(int type, float x, float y, float vx, float vy, float life, float size, int color) {
        if (items.size() >= MAX) items.remove(0);
        P p = new P();
        p.type = type;
        p.x = x;
        p.y = y;
        p.vx = vx;
        p.vy = vy;
        p.life = life;
        p.maxLife = life;
        p.size = size;
        p.color = color;
        p.gravity = 14f;
        p.drag = 1.6f;
        items.add(p);
        return p;
    }

    public void burst(float x, float y, int count, int type, int color,
                      float speed, float life, float size, float gravity) {
        for (int i = 0; i < count; i++) {
            double a = Math.random() * Math.PI * 2;
            float sp = speed * (0.35f + (float) Math.random() * 0.9f);
            P p = spawn(type,
                    x + (float) (Math.random() - 0.5) * 0.5f,
                    y + (float) (Math.random() - 0.5) * 0.5f,
                    (float) Math.cos(a) * sp,
                    (float) Math.sin(a) * sp,
                    life * (0.6f + (float) Math.random() * 0.8f),
                    size * (0.5f + (float) Math.random()),
                    color);
            p.gravity = gravity;
            p.spin = (float) (Math.random() - 0.5) * 8f;
        }
    }

    public void text(float x, float y, String text, int color, float size) {
        P p = spawn(TEXT, x, y, 0f, -1.4f, 1.1f, size, color);
        p.gravity = 0f;
        p.drag = 1.2f;
        p.text = text;
    }

    public void update(float dt) {
        for (int i = items.size() - 1; i >= 0; i--) {
            P p = items.get(i);
            p.life -= dt;
            if (p.life <= 0f) {
                items.remove(i);
                continue;
            }
            p.vy += p.gravity * dt;
            float d = (float) Math.exp(-p.drag * dt);
            p.vx *= d;
            p.vy *= d;
            p.x += p.vx * dt;
            p.y += p.vy * dt;
        }
    }
}
