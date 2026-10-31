package feature.review;

import java.sql.Date;
import game.Manual;

public class ReviewEngineTest {
    static int passed = 0, failed = 0;

    public static void main(String[] args) {
        testWinningMoveTagged();
        testReportCounts();
        System.out.println("结果: passed=" + passed + " failed=" + failed);
        System.exit(failed == 0 ? 0 : 1);
    }

    static Manual buildSampleManual() {
        // 黑(5,5) 白(6,5) 黑(5,6) 白(7,5) 黑(5,7) 白(8,5) 黑(5,8) 白(9,5) 黑(5,9) — 黑垂直五连
        int[][] moves = {
            {5,5}, {6,5}, {5,6}, {7,5}, {5,7}, {8,5}, {5,8}, {9,5}, {5,9}
        };
        return new Manual("black", "white",
                new Date(System.currentTimeMillis()),
                "black", moves.length, moves);
    }

    static void testWinningMoveTagged() {
        Manual m = buildSampleManual();
        ReviewReport report = ReviewEngine.analyze(m, 1, 5, 15, null);
        ReviewMove last = report.moves[8];
        boolean okTag = last.tag == ReviewMove.Tag.BRILLIANT
                     || last.tag == ReviewMove.Tag.GOOD
                     || last.tag == ReviewMove.Tag.OK;
        ok("最后一手赢棋走法 tag in {BRILLIANT,GOOD,OK} (实际=" + last.tag + ")", okTag);
    }

    static void testReportCounts() {
        Manual m = buildSampleManual();
        ReviewReport report = ReviewEngine.analyze(m, 1, 5, 15, null);
        boolean structOk = report.moves.length == 9
                && report.blackMistakes >= 0
                && report.whiteMistakes >= 0
                && report.blackBrilliant >= 0
                && report.whiteBrilliant >= 0;
        ok("report 结构正确：moves.length=9 且失误/妙手计数 >=0", structOk);
    }

    static void ok(String name, boolean cond) {
        if (cond) { passed++; System.out.println("  + " + name); }
        else      { failed++; System.out.println("  x " + name); }
    }
}
