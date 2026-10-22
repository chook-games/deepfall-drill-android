package com.deepfalldrill.game;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.SystemClock;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;

/**
 * Tiny procedural sound bank. All clips are synthesized to WAV at startup and
 * played through SoundPool, so the APK carries no audio assets.
 */
public final class Sfx {
    public static final int CLICK = 0;
    public static final int DIG_SOFT = 1;
    public static final int DIG_HARD = 2;
    public static final int ORE = 3;
    public static final int GEM = 4;
    public static final int IMPACT = 5;
    public static final int EXPLODE = 6;
    public static final int LAVA = 7;
    public static final int WATER = 8;
    public static final int UPGRADE = 9;
    public static final int DEATH = 10;
    public static final int LEVEL = 11;
    public static final int COIN = 12;
    public static final int HEAT = 13;
    public static final int THRUST = 14;

    private static final int SR = 22050;
    private static final int COUNT = 15;
    private static final String[] NAMES = {
            "click", "dig_soft", "dig_hard", "ore", "gem", "impact", "explode",
            "lava", "water", "upgrade", "death", "level", "coin", "heat", "thrust",
    };
    private static final long[] MIN_GAP = {
            30, 55, 55, 35, 35, 90, 200, 350, 250, 200, 500, 300, 40, 2500, 300,
    };

    private final SoundPool pool;
    private final int[] ids = new int[COUNT];
    private final boolean[] ready = new boolean[COUNT];
    private final long[] lastPlayed = new long[COUNT];
    private final Random noiseRng = new Random(0x51EED);
    private volatile boolean enabled = true;

    public Sfx(Context context) {
        pool = new SoundPool.Builder()
                .setMaxStreams(8)
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                .build();
        pool.setOnLoadCompleteListener((soundPool, sampleId, status) -> {
            for (int i = 0; i < COUNT; i++) {
                if (ids[i] == sampleId && status == 0) ready[i] = true;
            }
        });

        try {
            File dir = new File(context.getCacheDir(), "sfx");
            if (!dir.exists()) dir.mkdirs();
            for (int i = 0; i < COUNT; i++) {
                File f = new File(dir, NAMES[i] + ".wav");
                if (!f.exists()) writeWav(f, make(i));
                ids[i] = pool.load(f.getAbsolutePath(), 1);
            }
            Log.i("Deepfall", "Sfx ready: " + COUNT + " clips in " + dir);
        } catch (Exception e) {
            // Audio is optional; gameplay must never depend on it.
            Log.w("Deepfall", "Sfx init failed", e);
        }
    }

    public void setEnabled(boolean on) {
        enabled = on;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void play(int id) {
        play(id, 0.8f, 1f);
    }

    public void play(int id, float volume, float rate) {
        if (!enabled || id < 0 || id >= COUNT || !ready[id]) return;
        long now = SystemClock.uptimeMillis();
        if (now - lastPlayed[id] < MIN_GAP[id]) return;
        lastPlayed[id] = now;
        try {
            pool.play(ids[id], volume, volume, 1, 0, rate);
        } catch (Exception ignored) {
        }
    }

    public void release() {
        try {
            pool.release();
        } catch (Exception ignored) {
        }
    }

    // ------------------------------------------------------------- synthesis
    private float[] make(int id) {
        switch (id) {
            case CLICK:
                return click();
            case DIG_SOFT:
                return digSoft();
            case DIG_HARD:
                return digHard();
            case ORE:
                return ore();
            case GEM:
                return gem();
            case IMPACT:
                return impact();
            case EXPLODE:
                return explode();
            case LAVA:
                return lava();
            case WATER:
                return water();
            case UPGRADE:
                return arpeggio(new float[]{523f, 659f, 784f}, 0.10f, 0.42f, 0.32f);
            case DEATH:
                return death();
            case LEVEL:
                return arpeggio(new float[]{523f, 659f, 784f, 1046f}, 0.09f, 0.55f, 0.3f);
            case COIN:
                return coin();
            case HEAT:
                return beep();
            case THRUST:
                return thrust();
            default:
                return new float[128];
        }
    }

    private float[] click() {
        float[] b = buf(0.05f);
        tone(b, 0f, 0.035f, 1500f, 1250f, 0.3f, 55f, 1);
        noise(b, 0f, 0.02f, 0.22f, 90f, 0.35f);
        return b;
    }

    private float[] digSoft() {
        float[] b = buf(0.13f);
        tone(b, 0f, 0.11f, 95f, 55f, 0.45f, 18f, 1);
        noise(b, 0f, 0.1f, 0.34f, 16f, 0.12f);
        return b;
    }

    private float[] digHard() {
        float[] b = buf(0.1f);
        tone(b, 0f, 0.07f, 420f, 300f, 0.28f, 40f, 3);
        tone(b, 0f, 0.05f, 1250f, 900f, 0.2f, 55f, 1);
        noise(b, 0f, 0.07f, 0.28f, 28f, 0.5f);
        return b;
    }

    private float[] ore() {
        float[] b = buf(0.26f);
        tone(b, 0f, 0.14f, 880f, 880f, 0.28f, 18f, 0);
        tone(b, 0.06f, 0.18f, 1320f, 1320f, 0.22f, 14f, 0);
        return b;
    }

    private float[] gem() {
        float[] b = buf(0.36f);
        tone(b, 0f, 0.1f, 1046f, 1046f, 0.24f, 20f, 0);
        tone(b, 0.07f, 0.12f, 1318f, 1318f, 0.22f, 18f, 0);
        tone(b, 0.15f, 0.18f, 1568f, 1568f, 0.2f, 14f, 0);
        return b;
    }

    private float[] impact() {
        float[] b = buf(0.32f);
        tone(b, 0f, 0.28f, 140f, 45f, 0.55f, 9f, 1);
        noise(b, 0f, 0.16f, 0.4f, 14f, 0.2f);
        return b;
    }

    private float[] explode() {
        float[] b = buf(0.6f);
        noise(b, 0f, 0.55f, 0.6f, 7f, 0.08f);
        tone(b, 0f, 0.45f, 70f, 35f, 0.5f, 6f, 1);
        return b;
    }

    private float[] lava() {
        float[] b = buf(0.45f);
        noise(b, 0f, 0.42f, 0.34f, 5f, 0.7f);
        tone(b, 0f, 0.4f, 180f, 70f, 0.22f, 5f, 2);
        return b;
    }

    private float[] water() {
        float[] b = buf(0.32f);
        noise(b, 0f, 0.28f, 0.2f, 8f, 0.35f);
        tone(b, 0.02f, 0.16f, 500f, 900f, 0.16f, 16f, 0);
        return b;
    }

    private float[] death() {
        float[] b = buf(0.95f);
        tone(b, 0f, 0.9f, 300f, 55f, 0.4f, 4f, 3);
        noise(b, 0f, 0.6f, 0.32f, 5f, 0.15f);
        return b;
    }

    private float[] coin() {
        float[] b = buf(0.2f);
        tone(b, 0f, 0.06f, 1046f, 1046f, 0.22f, 30f, 1);
        tone(b, 0.05f, 0.12f, 1568f, 1568f, 0.2f, 22f, 1);
        return b;
    }

    private float[] thrust() {
        float[] b = buf(0.5f);
        noise(b, 0f, 0.46f, 0.33f, 3.5f, 0.3f);
        tone(b, 0f, 0.35f, 120f, 70f, 0.22f, 6f, 2);
        return b;
    }

    private float[] beep() {
        float[] b = buf(0.16f);
        tone(b, 0f, 0.07f, 1150f, 1150f, 0.2f, 30f, 1);
        tone(b, 0.09f, 0.07f, 1150f, 1150f, 0.2f, 30f, 1);
        return b;
    }

    private float[] arpeggio(float[] notes, float step, float length, float amp) {
        float[] b = buf(length);
        for (int i = 0; i < notes.length; i++) {
            tone(b, i * step, length - i * step, notes[i], notes[i], amp, 9f, 0);
        }
        return b;
    }

    private float[] buf(float seconds) {
        return new float[Math.max(32, (int) (SR * seconds))];
    }

    private void tone(float[] b, float start, float dur, float f0, float f1,
                      float amp, float decay, int harmonics) {
        int s0 = (int) (start * SR);
        int n = (int) (dur * SR);
        float sweep = f1 - f0;
        for (int i = 0; i < n && s0 + i < b.length; i++) {
            float t = i / (float) SR;
            float p = i / (float) n;
            float phase = (float) (Math.PI * 2 * (f0 * t + 0.5 * sweep * t * t / dur));
            float env = (float) Math.exp(-decay * p);
            float v = (float) Math.sin(phase);
            if (harmonics > 0) v += 0.3f * (float) Math.sin(phase * (harmonics + 2));
            b[s0 + i] += v * amp * env;
        }
    }

    private void noise(float[] b, float start, float dur, float amp, float decay, float cutoff) {
        int s0 = (int) (start * SR);
        int n = (int) (dur * SR);
        float prev = 0f;
        for (int i = 0; i < n && s0 + i < b.length; i++) {
            float w = noiseRng.nextFloat() * 2f - 1f;
            prev += (w - prev) * cutoff;
            float p = i / (float) n;
            b[s0 + i] += prev * amp * (float) Math.exp(-decay * p);
        }
    }

    private void writeWav(File f, float[] samples) throws Exception {
        int n = samples.length;
        ByteBuffer bb = ByteBuffer.allocate(44 + n * 2).order(ByteOrder.LITTLE_ENDIAN);
        bb.put("RIFF".getBytes("US-ASCII"));
        bb.putInt(36 + n * 2);
        bb.put("WAVE".getBytes("US-ASCII"));
        bb.put("fmt ".getBytes("US-ASCII"));
        bb.putInt(16);
        bb.putShort((short) 1);
        bb.putShort((short) 1);
        bb.putInt(SR);
        bb.putInt(SR * 2);
        bb.putShort((short) 2);
        bb.putShort((short) 16);
        bb.put("data".getBytes("US-ASCII"));
        bb.putInt(n * 2);
        for (float s : samples) {
            float v = s;
            if (v > 1f) v = 1f;
            if (v < -1f) v = -1f;
            bb.putShort((short) (v * 32000f));
        }
        try (FileOutputStream out = new FileOutputStream(f)) {
            out.write(bb.array());
        }
    }
}
