package feature.rank;

public final class Rank {
    public final int elo;
    public final Tier tier;
    public final int divisionPoints;

    public Rank(int elo) {
        this.elo = elo;
        this.tier = Tier.of(elo);
        if (tier.max == Integer.MAX_VALUE) divisionPoints = 100;
        else divisionPoints = (elo - tier.min) * 100 / (tier.max - tier.min);
    }
}
