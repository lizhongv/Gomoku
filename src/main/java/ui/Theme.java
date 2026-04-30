// 当前皮肤的 Tokens / BoardStyle / PieceStyle 快捷访问
package ui;
import ui.design.Skin;
import ui.design.Tokens;
import ui.design.BoardStyle;
import ui.design.PieceStyle;

public final class Theme {
    private static volatile Skin current;
    private Theme() {}
    public static void set(Skin s) { current = s; }
    public static Skin currentSkin() {
        if (current == null) throw new IllegalStateException("Theme.set 必须先调用");
        return current;
    }
    public static Tokens tokens() { return currentSkin().tokens; }
    public static BoardStyle board() { return currentSkin().board; }
    public static PieceStyle pieces() { return currentSkin().pieces; }
}
