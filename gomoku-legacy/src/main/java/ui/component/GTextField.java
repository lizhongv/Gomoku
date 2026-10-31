package ui.component;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JTextField;
import ui.Theme;
import ui.ThemeAware;
import ui.design.Tokens;

public class GTextField extends JTextField implements ThemeAware {
    private String placeholder = "";

    public GTextField() {
        super();
        setOpaque(false);
        applyStyle();
        installFocusRepaint();
    }

    public GTextField(int columns) {
        super(columns);
        setOpaque(false);
        applyStyle();
        installFocusRepaint();
    }

    private void installFocusRepaint() {
        addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) { repaint(); }
            @Override public void focusLost(java.awt.event.FocusEvent e) { repaint(); }
        });
    }

    public void setPlaceholder(String p) {
        this.placeholder = (p == null) ? "" : p;
        repaint();
    }

    private void applyStyle() {
        Tokens t = Theme.tokens();
        setFont(t.fontBody);
        setForeground(t.fgDefault);
        setCaretColor(t.fgDefault);
        setBorder(BorderFactory.createEmptyBorder(t.space2, t.space3, t.space2, t.space3));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Tokens t = Theme.tokens();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        int r = t.radiusMd;
        g2.setColor(t.bgCard);
        g2.fillRoundRect(0, 0, w, h, r, r);
        g2.setColor(t.borderMedium);
        g2.drawRoundRect(0, 0, w - 1, h - 1, r, r);
        g2.dispose();
        super.paintComponent(g);
        if (!placeholder.isEmpty() && getText().isEmpty() && !isFocusOwner()) {
            Graphics2D gp = (Graphics2D) g.create();
            gp.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            gp.setColor(t.fgMuted);
            gp.setFont(t.fontBody);
            java.awt.Insets ins = getInsets();
            java.awt.FontMetrics fm = gp.getFontMetrics();
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            gp.drawString(placeholder, ins.left, y);
            gp.dispose();
        }
    }

    @Override
    public void onSkinChanged() {
        applyStyle();
        repaint();
    }
}
