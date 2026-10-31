package feature.review;

public final class ReviewReport {
    public final ReviewMove[] moves;
    public final int blackMistakes, whiteMistakes;
    public final int blackBrilliant, whiteBrilliant;

    public ReviewReport(ReviewMove[] moves, int bm, int wm, int bb, int wb) {
        this.moves = moves;
        this.blackMistakes = bm;
        this.whiteMistakes = wm;
        this.blackBrilliant = bb;
        this.whiteBrilliant = wb;
    }
}
