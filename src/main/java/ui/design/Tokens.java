// 设计变量集合：所有颜色 / 字体 / 间距 / 圆角集中在此
// 不可变；由 Skin 提供具体值
package ui.design;

import java.awt.Color;
import java.awt.Font;

public final class Tokens {
    public final Color bgPrimary, bgSecondary, bgCard, bgBoard, bgToolbar;
    public final Color fgDefault, fgMuted, fgOnAccent;
    public final Color accent, accentDark, danger, success, warning;
    public final Color boardLine, boardStar, boardShadow, lastMoveRing;
    public final Color borderLight, borderMedium;

    public final int space1 = 4, space2 = 8, space3 = 12, space4 = 16,
                     space5 = 24, space6 = 32, space7 = 48;

    public final Font fontDisplay, fontTitle, fontHeading, fontBody, fontCaption, fontMono;

    public final int radiusSm = 4, radiusMd = 8, radiusLg = 12, radiusFull = 999;

    private Tokens(Builder b) {
        bgPrimary = b.bgPrimary; bgSecondary = b.bgSecondary; bgCard = b.bgCard;
        bgBoard = b.bgBoard; bgToolbar = b.bgToolbar;
        fgDefault = b.fgDefault; fgMuted = b.fgMuted; fgOnAccent = b.fgOnAccent;
        accent = b.accent; accentDark = b.accentDark; danger = b.danger;
        success = b.success; warning = b.warning;
        boardLine = b.boardLine; boardStar = b.boardStar;
        boardShadow = b.boardShadow; lastMoveRing = b.lastMoveRing;
        borderLight = b.borderLight; borderMedium = b.borderMedium;
        fontDisplay = b.fontDisplay; fontTitle = b.fontTitle; fontHeading = b.fontHeading;
        fontBody = b.fontBody; fontCaption = b.fontCaption; fontMono = b.fontMono;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        public Color bgPrimary, bgSecondary, bgCard, bgBoard, bgToolbar;
        public Color fgDefault, fgMuted, fgOnAccent;
        public Color accent, accentDark, danger, success, warning;
        public Color boardLine, boardStar, boardShadow, lastMoveRing;
        public Color borderLight, borderMedium;
        public Font fontDisplay, fontTitle, fontHeading, fontBody, fontCaption, fontMono;
        public Tokens build() { return new Tokens(this); }
    }
}
