package ui.component;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import ui.Theme;
import ui.ThemeAware;
import ui.design.Tokens;

public class GChip extends JLabel implements ThemeAware {
    private Color bg;

    public GChip(String text, Color bg, Color fg) {
        super(text);
        this.bg = bg;
        setForeground(fg);
        setOpaque(false);
        Tokens t = Theme.tokens();
        setFont(t.fontCaption);
        setBorder(BorderFactory.createEmptyBorder(t.space1, t.space2, t.space1, t.space2));
    }

    public void setBackgroundColor(Color c) {
        this.bg = c;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        int r = h;
        g2.setColor(bg);
        g2.fillRoundRect(0, 0, w, h, r, r);
        g2.dispose();
        super.paintComponent(g);
    }

    @Override
    public void onSkinChanged() {
        Tokens t = Theme.tokens();
        setFont(t.fontCaption);
        setBorder(BorderFactory.createEmptyBorder(t.space1, t.space2, t.space1, t.space2));
        repaint();
    }
}
