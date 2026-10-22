package com.deepfalldrill.game;

/** Material database. Values are tuned for the narrow 16-cell playfield. */
public final class Materials {
    private Materials() {
    }

    public static final int EMPTY = 0;
    public static final int DIRT = 1;
    public static final int STONE = 2;
    public static final int BASALT = 3;
    public static final int IRON = 4;
    public static final int GOLD = 5;
    public static final int DIAMOND = 6;
    public static final int GEM = 7;
    public static final int LAVA = 8;
    public static final int WATER = 9;
    public static final int BEDROCK = 10;

    public static final class M {
        public final int id;
        public final String name;
        public final boolean solid;
        public final boolean liquid;
        public final boolean diggable;
        public final boolean ore;
        public final float hardness;
        public final float heat;
        public final float impact;
        public final float friction;
        public final int value;
        public final int xp;
        public final int[] colors;
        public final int accent;
        public final int dust;
        public final float glow;

        M(int id, String name, boolean solid, boolean liquid, boolean diggable, boolean ore,
          float hardness, float heat, float impact, float friction, int value, int xp,
          int[] colors, int accent, int dust, float glow) {
            this.id = id;
            this.name = name;
            this.solid = solid;
            this.liquid = liquid;
            this.diggable = diggable;
            this.ore = ore;
            this.hardness = hardness;
            this.heat = heat;
            this.impact = impact;
            this.friction = friction;
            this.value = value;
            this.xp = xp;
            this.colors = colors;
            this.accent = accent;
            this.dust = dust;
            this.glow = glow;
        }
    }

    public static final M[] ALL = new M[11];

    static {
        ALL[EMPTY] = new M(EMPTY, "Air", false, false, false, false,
                0f, 0f, 0f, 0f, 0, 0,
                new int[]{0xFF000000}, 0xFF000000, 0xFF000000, 0f);

        ALL[DIRT] = new M(DIRT, "Soft Dirt", true, false, true, false,
                0.22f, 0.15f, 0f, 0f, 0, 0,
                new int[]{0xFF6D452B, 0xFF7A4F33, 0xFF5E3A23}, 0xFF9A6A45, 0xFF8A5B3A, 0f);

        ALL[STONE] = new M(STONE, "Stone", true, false, true, false,
                0.95f, 1.2f, 0.55f, 2.2f, 0, 0,
                new int[]{0xFF4B515C, 0xFF464C56, 0xFF535965}, 0xFF6A7280, 0xFF7B8290, 0f);

        ALL[BASALT] = new M(BASALT, "Basalt", true, false, true, false,
                2.6f, 3.0f, 0.95f, 4.6f, 0, 0,
                new int[]{0xFF2B2B38, 0xFF262632, 0xFF313140}, 0xFF4A4560, 0xFF3A3A4C, 0f);

        ALL[IRON] = new M(IRON, "Iron Ore", true, false, true, true,
                1.1f, 1.4f, 0.65f, 2.4f, 5, 3,
                new int[]{0xFF5E5148, 0xFF584B43, 0xFF66584E}, 0xFFA9C2D8, 0xFF8EA6BB, 0f);

        ALL[GOLD] = new M(GOLD, "Gold Vein", true, false, true, true,
                1.0f, 1.2f, 0.6f, 2.4f, 15, 8,
                new int[]{0xFF5A4F3A, 0xFF554A36, 0xFF615540}, 0xFFFFCC55, 0xFFD8A53A, 0.25f);

        ALL[DIAMOND] = new M(DIAMOND, "Diamond", true, false, true, true,
                2.2f, 4.0f, 1.0f, 3.4f, 40, 22,
                new int[]{0xFF3F5560, 0xFF3A4F5A, 0xFF455C68}, 0xFF7FE8FF, 0xFFA8F0FF, 0.5f);

        ALL[GEM] = new M(GEM, "Gem Cluster", true, false, true, true,
                0.7f, 0.8f, 0.3f, 1.4f, 26, 14,
                new int[]{0xFF4B3357, 0xFF452E50, 0xFF523961}, 0xFFE07BFF, 0xFFD79AFF, 0.45f);

        ALL[LAVA] = new M(LAVA, "Lava", false, true, false, false,
                0f, 0f, 0f, 0f, 0, 0,
                new int[]{0xFFFF7A1F, 0xFFFF5A0A, 0xFFFFA63A}, 0xFFFFD27A, 0xFFFF7A1F, 1f);

        ALL[WATER] = new M(WATER, "Water", false, true, false, false,
                0f, 0f, 0f, 0f, 0, 0,
                new int[]{0xFF2F6FD0, 0xFF2A63BB, 0xFF3B7FE0}, 0xFF9FD0FF, 0xFF7FB8FF, 0f);

        ALL[BEDROCK] = new M(BEDROCK, "Bedrock", true, false, false, false,
                99f, 0f, 1.1f, 5f, 0, 0,
                new int[]{0xFF17181F, 0xFF1C1D26, 0xFF121319}, 0xFF3A3D4A, 0xFF2A2C36, 0f);
    }

    public static M get(int id) {
        if (id < 0 || id >= ALL.length || ALL[id] == null) return ALL[EMPTY];
        return ALL[id];
    }

    public static boolean isSolid(int id) {
        return get(id).solid;
    }

    public static boolean isLiquid(int id) {
        return get(id).liquid;
    }

    public static boolean isDiggable(int id) {
        return get(id).diggable;
    }

    public static boolean isOre(int id) {
        return get(id).ore;
    }
}
