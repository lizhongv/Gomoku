// 棋盘绘制规则（背景渐变 / 木纹开关 / 星位样式 / 线宽倍率）
package ui.design;

import java.awt.Color;

public final class BoardStyle {
    public final Color bgTop, bgBottom;
    public final boolean drawWoodGrain;
    public final int starDotSize;
    public final float lineStrokeMul;

    public BoardStyle(Color bgTop, Color bgBottom, boolean drawWoodGrain,
                      int starDotSize, float lineStrokeMul) {
        this.bgTop = bgTop;
        this.bgBottom = bgBottom;
        this.drawWoodGrain = drawWoodGrain;
        this.starDotSize = starDotSize;
        this.lineStrokeMul = lineStrokeMul;
    }
}
