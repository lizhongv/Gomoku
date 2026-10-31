package ui.component;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import ui.Theme;
import ui.ThemeAware;
import ui.design.Tokens;

public class GBadge extends JLabel implements ThemeAware {
    private int count;

    public GBadge() {
        super("", SwingConstants.CENTER);
        setOpaque(false);
        applyStyle();
        setVisible(false);
    }

    public void setCount(int c) {
        this.count = c;
        if (c <= 0) {
            setVisible(false);
        } else {
            setText(c > 99 ? "99+" : String.valueOf(c));
            setVisible(true);
        }
        revalidate();
        repaint();
    }

    public int getCount() { return count; }

    private void applyStyle() {
        Tokens t = Theme.tokens();
        Font base = t.fontCaption;
        setFont(base.deriveFont(Font.BOLD));
        setForeground(t.fgOnAccent);
    }

    @Override
    public Dimension getPreferredSize() {
        Tokens t = Theme.tokens();
        FontMetrics fm = getFontMetrics(getFont());
        int w = Math.max(fm.stringWidth(getText()) + t.space2, fm.getHeight() + 2);
        int h = fm.getHeight() + 2;
        return new Dimension(w, h);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Tokens t = Theme.tokens();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        int r = h;
        Color bg = t.danger;
        g2.setColor(bg);
        g2.fillRoundRect(0, 0, w, h, r, r);
        g2.dispose();
        super.paintComponent(g);
    }

    @Override
    public void onSkinChanged() {
        applyStyle();
        repaint();
    }
}
