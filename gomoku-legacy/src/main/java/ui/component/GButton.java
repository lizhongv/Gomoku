package ui.component;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
import ui.Theme;
import ui.ThemeAware;
import ui.design.Tokens;

public class GButton extends JButton implements ThemeAware {
    public enum Variant { PRIMARY, SECONDARY, GHOST }

    private final Variant variant;
    private boolean hover;

    public GButton(String text, Variant variant) {
        super(text);
        this.variant = variant;
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Tokens t = Theme.tokens();
        setFont(t.fontBody);
        setMargin(new Insets(t.space2, t.space4, t.space2, t.space4));
        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
            public void mouseExited(MouseEvent e) { hover = false; repaint(); }
        });
        applyForeground();
    }

    public static GButton primary(String text)   { return new GButton(text, Variant.PRIMARY); }
    public static GButton secondary(String text) { return new GButton(text, Variant.SECONDARY); }
    public static GButton ghost(String text)     { return new GButton(text, Variant.GHOST); }

    private void applyForeground() {
        Tokens t = Theme.tokens();
        switch (variant) {
            case PRIMARY:   setForeground(t.fgOnAccent); break;
            case SECONDARY: setForeground(t.fgDefault);  break;
            case GHOST:     setForeground(t.fgDefault);  break;
        }
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        Tokens t = Theme.tokens();
        return new Dimension(d.width + t.space4, d.height + t.space2);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Tokens t = Theme.tokens();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        int r = t.radiusMd;
        if (variant == Variant.PRIMARY) {
            Color c = hover ? t.accent.darker() : t.accent;
            g2.setColor(c);
            g2.fillRoundRect(0, 0, w, h, r, r);
        } else if (variant == Variant.SECONDARY) {
            Color c = hover ? t.bgCard.darker() : t.bgCard;
            g2.setColor(c);
            g2.fillRoundRect(0, 0, w, h, r, r);
            g2.setColor(t.borderMedium);
            g2.drawRoundRect(0, 0, w - 1, h - 1, r, r);
        } else {
            if (hover) {
                g2.setColor(new Color(0, 0, 0, 24));
                g2.fillRoundRect(0, 0, w, h, r, r);
            }
        }
        g2.dispose();
        super.paintComponent(g);
    }

    @Override
    public void onSkinChanged() {
        Tokens t = Theme.tokens();
        setFont(t.fontBody);
        applyForeground();
        repaint();
    }
}
