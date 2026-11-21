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

    /** Returns -1 when the code is not a valid base-36 seed. */
    public static int codeToSeed(String code) {
        if (code == null) return -1;
        String s = code.trim().toUpperCase();
        if (s.isEmpty() || s.length() > 8) return -1;
        long value = 0;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            int digit;
            if (ch >= '0' && ch <= '9') digit = ch - '0';
            else if (ch >= 'A' && ch <= 'Z') digit = ch - 'A' + 10;
            else return -1;
            value = value * 36 + digit;
            if (value > 0xFFFFFFFFL) return -1;
        }
        return (int) value;
    }

    /** Stable seed for a UTC day number (millis / 86400000). */
    public static int dailySeed(long day) {
        long z = day * 0x9E3779B97F4A7C15L;
        int h = (int) (z ^ (z >>> 32));
        h = (h ^ (h >>> 16)) * 0x45D9F3B;
        return h ^ (h >>> 16);
    }

    public static long todayKey() {
        return System.currentTimeMillis() / 86400000L;
    }
}