package feature.review;

public final class ReviewMove {
    public enum Tag { BRILLIANT, GOOD, OK, INACCURATE, MISTAKE, BLUNDER }
    public final int col, row;
    public final int evalBest, evalActual, delta;
    public final Tag tag;
    public final int[] suggestedMove;

    public ReviewMove(int col, int row, int evalBest, int evalActual, int delta, Tag tag, int[] suggestedMove) {
        this.col = col;
        this.row = row;
        this.evalBest = evalBest;
        this.evalActual = evalActual;
        this.delta = delta;
        this.tag = tag;
        this.suggestedMove = suggestedMove;
    }
}
