package feature.rank;

public class RankCalculatorTest {
    static int passed = 0, failed = 0;

    public static void main(String[] args) {
        testEqualEloDelta();
        testEqualEloLossDelta();
        testAiEasyWinDelta();
        testAiHardWinDelta();
        testTierOf();
        System.out.println("结果: passed=" + passed + " failed=" + failed);
        System.exit(failed == 0 ? 0 : 1);
    }

    static void testEqualEloDelta() {
        int delta = RankCalculator.calcDelta(1200, RankCalculator.GameType.ONLINE, true);
        ok("ELO=1200 ONLINE 胜 1200 → delta == 16 (实际=" + delta + ")", delta == 16);
    }

    static void testEqualEloLossDelta() {
        int delta = RankCalculator.calcDelta(1200, RankCalculator.GameType.ONLINE, false);
        ok("ELO=1200 ONLINE 负 1200 → delta == -16 (实际=" + delta + ")", delta == -16);
    }

    static void testAiEasyWinDelta() {
        int delta = RankCalculator.calcDelta(1200, RankCalculator.GameType.AI_EASY, true);
        ok("ELO=1200 AI_EASY 胜 → delta in [0,2] (实际=" + delta + ")",
                delta >= 0 && delta <= 2);
    }

    static void testAiHardWinDelta() {
        int delta = RankCalculator.calcDelta(1200, RankCalculator.GameType.AI_HARD, true);
        ok("ELO=1200 AI_HARD 胜 → delta in [13,15] (实际=" + delta + ")",
                delta >= 13 && delta <= 15);
    }

    static void testTierOf() {
        boolean a = Tier.of(0)    == Tier.BRONZE;
        boolean b = Tier.of(1300) == Tier.GOLD;
        boolean c = Tier.of(2500) == Tier.MASTER;
        ok("Tier.of: 0=BRONZE 1300=GOLD 2500=MASTER", a && b && c);
    }

    static void ok(String name, boolean cond) {
        if (cond) { passed++; System.out.println("  + " + name); }
        else      { failed++; System.out.println("  x " + name); }
    }
}
