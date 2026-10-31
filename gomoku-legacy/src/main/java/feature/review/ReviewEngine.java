package feature.review;

import ai.Evaluate;
import game.Manual;

public final class ReviewEngine {
    public interface ProgressCallback { void onProgress(int current, int total); }

    private ReviewEngine() {}

    public static ReviewReport analyze(Manual m, int aiDepth, int aiSamples,
                                       int boardSize, ProgressCallback cb) {
        int total = m.getTotalStep();
        ReviewMove[] moves = new ReviewMove[total];
        int[][] board = new int[boardSize][boardSize];

        int blackMistakes = 0, whiteMistakes = 0;
        int blackBrilliant = 0, whiteBrilliant = 0;

        for (int i = 0; i < total; i++) {
            if (cb != null) cb.onProgress(i, total);
            int col = m.getChessList()[i][0];
            int row = m.getChessList()[i][1];
            int color = (i % 2 == 0) ? 1 : 2;

            int[] bestPos = new Evaluate(board, color, aiDepth, aiSamples).getTheBestPosition();

            board[col][row] = color;
            int evalAfter = quickScore(board, color, boardSize);

            int[][] sim = copy(board);
            sim[col][row] = 0;
            if (bestPos != null && bestPos.length >= 2
                    && bestPos[0] >= 0 && bestPos[0] < boardSize
                    && bestPos[1] >= 0 && bestPos[1] < boardSize
                    && sim[bestPos[0]][bestPos[1]] == 0) {
                sim[bestPos[0]][bestPos[1]] = color;
            }
            int evalBest = quickScore(sim, color, boardSize);

            int delta = Math.abs(evalBest - evalAfter);
            ReviewMove.Tag tag;
            boolean isBest = bestPos != null && col == bestPos[0] && row == bestPos[1];
            if (isBest && (evalAfter > 5000)) tag = ReviewMove.Tag.BRILLIANT;
            else if (isBest)                  tag = ReviewMove.Tag.GOOD;
            else if (delta <= 300)            tag = ReviewMove.Tag.OK;
            else if (delta <= 1000)           tag = ReviewMove.Tag.INACCURATE;
            else if (delta <= 5000)           tag = ReviewMove.Tag.MISTAKE;
            else                              tag = ReviewMove.Tag.BLUNDER;

            if (color == 1 && (tag == ReviewMove.Tag.MISTAKE || tag == ReviewMove.Tag.BLUNDER)) blackMistakes++;
            if (color == 2 && (tag == ReviewMove.Tag.MISTAKE || tag == ReviewMove.Tag.BLUNDER)) whiteMistakes++;
            if (color == 1 && tag == ReviewMove.Tag.BRILLIANT) blackBrilliant++;
            if (color == 2 && tag == ReviewMove.Tag.BRILLIANT) whiteBrilliant++;

            moves[i] = new ReviewMove(col, row, evalBest, evalAfter, delta, tag, bestPos);
        }
        if (cb != null) cb.onProgress(total, total);
        return new ReviewReport(moves, blackMistakes, whiteMistakes, blackBrilliant, whiteBrilliant);
    }

    private static int quickScore(int[][] b, int color, int n) {
        return new Evaluate(b, color, 0, 0).snapshotScore();
    }

    private static int[][] copy(int[][] b) {
        int[][] c = new int[b.length][];
        for (int i = 0; i < b.length; i++) c[i] = b[i].clone();
        return c;
    }
}
