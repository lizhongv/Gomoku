package feature.rank;

public enum Tier {
    BRONZE("青铜",   0,    1000),
    SILVER("白银",   1000, 1300),
    GOLD("黄金",     1300, 1600),
    PLATINUM("铂金", 1600, 1900),
    DIAMOND("钻石",  1900, 2200),
    MASTER("大师",   2200, Integer.MAX_VALUE);

    public final String displayName;
    public final int min, max;

    Tier(String n, int min, int max) {
        this.displayName = n;
        this.min = min;
        this.max = max;
    }

    public static Tier of(int elo) {
        for (Tier t : values()) if (elo >= t.min && elo < t.max) return t;
        return BRONZE;
    }
}
