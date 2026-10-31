// 棋子绘制规则（黑/白渐变 + 落地阴影开关）
package ui.design;

import java.awt.Color;

public final class PieceStyle {
    public final Color blackHighlight;
    public final Color blackBase;
    public final Color whiteHighlight;
    public final Color whiteBase;
    public final boolean dropShadow;

    public PieceStyle(Color blackHighlight, Color blackBase,
                      Color whiteHighlight, Color whiteBase,
                      boolean dropShadow) {
        this.blackHighlight = blackHighlight;
        this.blackBase = blackBase;
        this.whiteHighlight = whiteHighlight;
        this.whiteBase = whiteBase;
        this.dropShadow = dropShadow;
    }
}
