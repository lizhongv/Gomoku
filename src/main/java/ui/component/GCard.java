package ui.component;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import ui.Theme;
import ui.ThemeAware;
import ui.design.Tokens;

public class GCard extends JPanel implements ThemeAware {
    private final boolean elevated;

    public GCard() { this(false); }

    public GCard(boolean elevated) {
        super(new BorderLayout());
        this.elevated = elevated;
        setOpaque(false);
        applyPadding();
    }

    private void applyPadding() {
        Tokens t = Theme.tokens();
        int p = t.space4;
        int extra = elevated ? 3 : 0;
        setBorder(BorderFactory.createEmptyBorder(p, p, p + extra, p));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Tokens t = Theme.tokens();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        int r = t.radiusLg;
        int bottomReserve = elevated ? 3 : 0;
        int rectH = h - bottomReserve;
        if (elevated) {
            Color shadow = t.boardShadow;
            int sa = shadow.getAlpha();
            for (int i = 1; i <= 3; i++) {
                int alpha = Math.max(0, sa / (i + 2));
                g2.setColor(new Color(shadow.getRed(), shadow.getGreen(), shadow.getBlue(), alpha));
                g2.fillRoundRect(0, i, w, rectH, r, r);
            }
        }
        g2.setColor(t.bgCard);
        g2.fillRoundRect(0, 0, w, rectH, r, r);
        g2.setColor(t.borderLight);
        g2.drawRoundRect(0, 0, w - 1, rectH - 1, r, r);
        g2.dispose();
        super.paintComponent(g);
    }

    @Override
    public void onSkinChanged() {
        applyPadding();
        repaint();
    }
}
