package com.deepfalldrill.game;

import java.util.Random;

/** Deterministic noise and hashing. Same seed always builds the same world. */
public final class Rng {
    private Rng() {
    }

    public static int hash2u(int x, int y, int seed) {
        int h = seed ^ (x * 374761393) ^ (y * 668265263);
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return h;
    }

    public static float hash2f(int x, int y, int seed) {
        return (hash2u(x, y, seed) & 0xFFFFFFFFL) / 4294967296f;
    }

    private static float smooth(float t) {
        return t * t * (3f - 2f * t);
    }

    public static float valueNoise(int seed, float x, float y) {
        int xi = (int) Math.floor(x);
        int yi = (int) Math.floor(y);
        float xf = x - xi;
        float yf = y - yi;
        float a = hash2f(xi, yi, seed);
        float b = hash2f(xi + 1, yi, seed);
        float c = hash2f(xi, yi + 1, seed);
        float d = hash2f(xi + 1, yi + 1, seed);
        float u = smooth(xf);
        float v = smooth(yf);
        return (a * (1 - u) + b * u) * (1 - v) + (c * (1 - u) + d * u) * v;
    }

    public static float fbm(int seed, float x, float y, int octaves) {
        float amp = 1f;
        float freq = 1f;
        float sum = 0f;
        float norm = 0f;
        for (int i = 0; i < octaves; i++) {
            sum += valueNoise(seed + i * 7919, x * freq, y * freq) * amp;
            norm += amp;
            amp *= 0.5f;
            freq *= 2.03f;
        }
        return sum / norm;
    }

    public static float ridged(int seed, float x, float y, int octaves) {
        float amp = 1f;
        float freq = 1f;
        float sum = 0f;
        float norm = 0f;
        for (int i = 0; i < octaves; i++) {
            float n = valueNoise(seed + i * 4231, x * freq, y * freq);
            n = 1f - Math.abs(n * 2f - 1f);
            sum += n * n * amp;
            norm += amp;
            amp *= 0.5f;
            freq *= 2.01f;
        }
        return sum / norm;
    }

    public static int makeSeed() {
        return new Random().nextInt();
    }

    public static String toCode(int seed) {
        long v = seed & 0xFFFFFFFFL;
        StringBuilder sb = new StringBuilder();
        do {
            int d = (int) (v % 36);
            sb.append((char) (d < 10 ? ('0' + d) : ('A' + d - 10)));
            v /= 36;
        } while (v > 0);
        while (sb.length() < 6) sb.append('0');
        return sb.reverse().toString();
    }
}
