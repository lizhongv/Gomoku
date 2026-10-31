// 皮肤包：完整视觉规则集（Tokens + BoardStyle + PieceStyle + 元信息）
package ui.design;

public final class Skin {
    public final String id;
    public final String displayName;
    public final boolean premium;
    public final int priceCoins;          // free skin = 0
    public final Tokens tokens;
    public final BoardStyle board;
    public final PieceStyle pieces;

    public Skin(String id, String displayName, boolean premium, int priceCoins,
                Tokens tokens, BoardStyle board, PieceStyle pieces) {
        this.id = id;
        this.displayName = displayName;
        this.premium = premium;
        this.priceCoins = priceCoins;
        this.tokens = tokens;
        this.board = board;
        this.pieces = pieces;
    }
}
