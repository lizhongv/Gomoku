package feature.rank;

public final class RankCalculator {
    public enum GameType { ONLINE, AI_EASY, AI_NORMAL, AI_HARD }

    private RankCalculator() {}

    public static int calcDelta(int myElo, GameType type, boolean won) {
        int oppElo;
        switch (type) {
            case AI_EASY:   oppElo = 800;  break;
            case AI_NORMAL: oppElo = 1200; break;
            case AI_HARD:   oppElo = 1600; break;
            default:        oppElo = 1200; break;
        }
        int K = (type == GameType.ONLINE) ? 32 : 16;
        double expected = 1.0 / (1.0 + Math.pow(10, (oppElo - myElo) / 400.0));
        double actual = won ? 1.0 : 0.0;
        return (int) Math.round(K * (actual - expected));
    }
}
