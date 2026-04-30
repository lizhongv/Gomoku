package ui.component;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JComponent;
import ui.Theme;
import ui.ThemeAware;
import ui.design.Tokens;

public class ScoreChart extends JComponent implements ThemeAware {
    private int[] blackScores;
    private int[] whiteScores;
    private String progressText;

    public ScoreChart() {
        setPreferredSize(new Dimension(280, 180));
        setOpaque(false);
    }

    public void setData(int[] black, int[] white) {
        this.blackScores = black;
        this.whiteScores = white;
        this.progressText = null;
        repaint();
    }

    public void setProgress(String text) {
        this.progressText = text;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Tokens t = Theme.tokens();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();

        g2.setColor(t.bgCard);
        g2.fillRoundRect(0, 0, w, h, t.radiusMd, t.radiusMd);
        g2.setColor(t.borderLight);
        g2.drawRoundRect(0, 0, w - 1, h - 1, t.radiusMd, t.radiusMd);

        int padTop = t.space4 + 4;
        int padLeft = t.space4;
        int padRight = t.space3;
        int padBottom = t.space3;

        g2.setFont(t.fontHeading);
        g2.setColor(t.fgDefault);
        FontMetrics tm = g2.getFontMetrics();
        g2.drawString("评分曲线", padLeft, tm.getAscent() + 4);

        boolean empty = blackScores == null || whiteScores == null
                || blackScores.length == 0 || whiteScores.length == 0;
        if (empty) {
            String msg = progressText == null ? "暂无数据" : progressText;
            g2.setFont(t.fontBody);
            g2.setColor(t.fgMuted);
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(msg);
            g2.drawString(msg, (w - tw) / 2, h / 2 + fm.getAscent() / 2);
            g2.dispose();
            return;
        }

        int n = Math.max(blackScores.length, whiteScores.length);
        int chartTop = padTop + tm.getHeight() + t.space2;
        int chartLeft = padLeft + 4;
        int chartRight = w - padRight;
        int chartBottom = h - padBottom;
        int chartW = chartRight - chartLeft;
        int chartH = chartBottom - chartTop;
        if (chartW <= 0 || chartH <= 0) { g2.dispose(); return; }

        int minV = Integer.MAX_VALUE, maxV = Integer.MIN_VALUE;
        for (int v : blackScores) { if (v < minV) minV = v; if (v > maxV) maxV = v; }
        for (int v : whiteScores) { if (v < minV) minV = v; if (v > maxV) maxV = v; }
        if (minV == maxV) { minV -= 1; maxV += 1; }

        g2.setColor(t.borderMedium);
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(chartLeft, chartBottom, chartRight, chartBottom);
        g2.drawLine(chartLeft, chartTop, chartLeft, chartBottom);

        if (minV < 0 && maxV > 0) {
            int zeroY = chartBottom - (int) ((0 - minV) * (long) chartH / (maxV - minV));
            g2.setColor(new Color(t.borderMedium.getRed(), t.borderMedium.getGreen(), t.borderMedium.getBlue(), 90));
            g2.drawLine(chartLeft, zeroY, chartRight, zeroY);
        }

        drawSeries(g2, blackScores, n, minV, maxV, chartLeft, chartTop, chartW, chartH, t.fgDefault);
        drawSeries(g2, whiteScores, n, minV, maxV, chartLeft, chartTop, chartW, chartH, t.danger);

        g2.setFont(t.fontCaption);
        g2.setColor(t.fgMuted);
        FontMetrics cm = g2.getFontMetrics();
        int legendY = chartTop - 4;
        int legendX = chartRight - 80;
        g2.setColor(t.fgDefault);
        g2.fillRect(legendX, legendY - cm.getAscent() / 2 - 2, 10, 3);
        g2.setColor(t.fgMuted);
        g2.drawString("黑", legendX + 14, legendY);
        g2.setColor(t.danger);
        g2.fillRect(legendX + 36, legendY - cm.getAscent() / 2 - 2, 10, 3);
        g2.setColor(t.fgMuted);
        g2.drawString("白", legendX + 50, legendY);

        g2.dispose();
    }

    private void drawSeries(Graphics2D g2, int[] data, int n, int minV, int maxV,
                            int chartLeft, int chartTop, int chartW, int chartH, Color color) {
        if (data.length < 1) return;
        g2.setColor(color);
        g2.setStroke(new BasicStroke(1.8f));
        int prevX = -1, prevY = -1;
        int span = (n > 1) ? (n - 1) : 1;
        int range = maxV - minV;
        for (int i = 0; i < data.length; i++) {
            int x = chartLeft + (int) ((long) i * chartW / span);
            int y = chartTop + chartH - (int) ((long) (data[i] - minV) * chartH / range);
            if (prevX >= 0) g2.drawLine(prevX, prevY, x, y);
            g2.fillOval(x - 2, y - 2, 4, 4);
            prevX = x;
            prevY = y;
        }
    }

    @Override
    public void onSkinChanged() {
        repaint();
    }
}
