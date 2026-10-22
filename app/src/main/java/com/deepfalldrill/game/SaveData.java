package com.deepfalldrill.game;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** Meta progression + run checkpoint, persisted as one JSON blob. */
public final class SaveData {
    private static final String PREFS = "deepfall";
    private static final String KEY = "save_v1";

    public int money;
    public int xp;
    public int level = 1;
    public int bestDepth;
    public int bestScore;
    public int totalOre;
    public int runs;
    public int deaths;
    public final HashMap<String, Integer> perm = new HashMap<>();
    public String inputMode = null; // null = ask on first Play
    public boolean vibrate = true;
    public boolean reduceFx = false;
    public boolean sound = true;
    public Checkpoint checkpoint;
    public JSONObject lastRun;

    public static final class Checkpoint {
        public int seed;
        public float x;
        public int depth;
        public float hp;
        public float heat;
        public int moneyEarned;
        public int xpEarned;
        public int oreCount;
        public int milestone;
        public final HashMap<Integer, Integer> ores = new HashMap<>();
        public final HashMap<String, Integer> upgrades = new HashMap<>();

        public JSONObject toJson() throws JSONException {
            JSONObject o = new JSONObject();
            o.put("seed", seed);
            o.put("x", x);
            o.put("depth", depth);
            o.put("hp", hp);
            o.put("heat", heat);
            o.put("moneyEarned", moneyEarned);
            o.put("xpEarned", xpEarned);
            o.put("oreCount", oreCount);
            o.put("milestone", milestone);
            JSONObject or = new JSONObject();
            for (Map.Entry<Integer, Integer> e : ores.entrySet()) or.put(String.valueOf(e.getKey()), e.getValue());
            o.put("ores", or);
            JSONObject up = new JSONObject();
            for (Map.Entry<String, Integer> e : upgrades.entrySet()) up.put(e.getKey(), e.getValue());
            o.put("upgrades", up);
            return o;
        }

        public static Checkpoint fromJson(JSONObject o) {
            if (o == null) return null;
            Checkpoint c = new Checkpoint();
            c.seed = o.optInt("seed", 0);
            c.x = (float) o.optDouble("x", 0);
            c.depth = o.optInt("depth", Config.SPAWN_Y);
            c.hp = (float) o.optDouble("hp", Config.START_HP);
            c.heat = (float) o.optDouble("heat", 0);
            c.moneyEarned = o.optInt("moneyEarned", 0);
            c.xpEarned = o.optInt("xpEarned", 0);
            c.oreCount = o.optInt("oreCount", 0);
            c.milestone = o.optInt("milestone", 0);
            JSONObject or = o.optJSONObject("ores");
            if (or != null) {
                Iterator<String> it = or.keys();
                while (it.hasNext()) {
                    String k = it.next();
                    try {
                        c.ores.put(Integer.parseInt(k), or.optInt(k, 0));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            JSONObject up = o.optJSONObject("upgrades");
            if (up != null) {
                Iterator<String> it = up.keys();
                while (it.hasNext()) {
                    String k = it.next();
                    c.upgrades.put(k, up.optInt(k, 0));
                }
            }
            return c;
        }
    }

    public static SaveData load(Context ctx) {
        try {
            SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String raw = p.getString(KEY, "");
            if (raw == null || raw.isEmpty()) return new SaveData();
            return fromJson(new JSONObject(raw));
        } catch (Exception e) {
            return new SaveData();
        }
    }

    public void save(Context ctx) {
        try {
            SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            p.edit().putString(KEY, toJson().toString()).apply();
        } catch (Exception ignored) {
        }
    }

    public void reset(Context ctx) {
        money = 0;
        xp = 0;
        level = 1;
        bestDepth = 0;
        bestScore = 0;
        totalOre = 0;
        runs = 0;
        deaths = 0;
        perm.clear();
        inputMode = null;
        checkpoint = null;
        lastRun = null;
        save(ctx);
    }

    public String toJsonString() {
        return toJson().toString();
    }

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("version", 1);
            o.put("money", money);
            o.put("xp", xp);
            o.put("level", level);
            o.put("bestDepth", bestDepth);
            o.put("bestScore", bestScore);
            o.put("totalOre", totalOre);
            o.put("runs", runs);
            o.put("deaths", deaths);
            JSONObject p = new JSONObject();
            for (Map.Entry<String, Integer> e : perm.entrySet()) p.put(e.getKey(), e.getValue());
            o.put("perm", p);
            o.put("inputMode", inputMode == null ? JSONObject.NULL : inputMode);
            o.put("vibrate", vibrate);
            o.put("reduceFx", reduceFx);
            o.put("sound", sound);
            o.put("checkpoint", checkpoint == null ? JSONObject.NULL : checkpoint.toJson());
            o.put("lastRun", lastRun == null ? JSONObject.NULL : lastRun);
        } catch (JSONException ignored) {
        }
        return o;
    }

    public static SaveData fromJson(JSONObject o) {
        SaveData d = new SaveData();
        d.money = Math.max(0, o.optInt("money", 0));
        d.xp = Math.max(0, o.optInt("xp", 0));
        d.level = Math.max(1, o.optInt("level", 1));
        d.bestDepth = Math.max(0, o.optInt("bestDepth", 0));
        d.bestScore = Math.max(0, o.optInt("bestScore", 0));
        d.totalOre = Math.max(0, o.optInt("totalOre", 0));
        d.runs = Math.max(0, o.optInt("runs", 0));
        d.deaths = Math.max(0, o.optInt("deaths", 0));
        JSONObject p = o.optJSONObject("perm");
        if (p != null) {
            Iterator<String> it = p.keys();
            while (it.hasNext()) {
                String k = it.next();
                d.perm.put(k, p.optInt(k, 0));
            }
        }
        d.inputMode = o.isNull("inputMode") ? null : o.optString("inputMode", null);
        if (d.inputMode != null && !d.inputMode.equals("zones") && !d.inputMode.equals("drag")
                && !d.inputMode.equals("tilt")) {
            d.inputMode = null;
        }
        d.vibrate = o.optBoolean("vibrate", true);
        d.reduceFx = o.optBoolean("reduceFx", false);
        d.sound = o.optBoolean("sound", true);
        d.checkpoint = Checkpoint.fromJson(o.optJSONObject("checkpoint"));
        d.lastRun = o.optJSONObject("lastRun");
        return d;
    }

    /** Banks a finished run and clears the checkpoint. */
    public List<Integer> applyRunResult(Run.Summary s) {
        runs++;
        deaths++;
        money += s.money;
        totalOre += s.oreCount;
        List<Integer> levels = Upgrades.grantXp(this, s.xp);
        if (s.depthMeters > bestDepth) bestDepth = s.depthMeters;
        if (s.score > bestScore) bestScore = s.score;
        checkpoint = null;
        try {
            JSONObject o = new JSONObject();
            o.put("depth", s.depthMeters);
            o.put("money", s.money);
            o.put("xp", s.xp);
            o.put("oreCount", s.oreCount);
            o.put("score", s.score);
            o.put("seed", s.seed);
            lastRun = o;
        } catch (JSONException ignored) {
        }
        return levels;
    }
}
