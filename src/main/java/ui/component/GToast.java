package ui.component;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import ui.Theme;
import ui.design.Tokens;

public class GToast extends JWindow {
    public enum Level { INFO, SUCCESS, ERROR }

    private final String message;
    private final Level level;
    private float alpha = 1.0f;

    private GToast(Component parent, String message, Level level) {
        super(parent == null ? null : SwingUtilities.getWindowAncestor(parent));
        this.message = message;
        this.level = level;
        setBackground(new Color(0, 0, 0, 0));
        Tokens t = Theme.tokens();
        JPanel content = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                Color bg = backgroundFor(level, t);
                g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, alpha));
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, w, h, t.radiusMd, t.radiusMd);
                g2.setColor(t.fgOnAccent);
                g2.setFont(t.fontBody);
                FontMetrics fm = g2.getFontMetrics();
                int tx = (w - fm.stringWidth(message)) / 2;
                int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(message, tx, ty);
                g2.dispose();
            }
        };
        content.setOpaque(false);
        FontMetrics fm = content.getFontMetrics(t.fontBody);
        int textW = fm.stringWidth(message);
        Dimension d = new Dimension(textW + t.space5 * 2, fm.getHeight() + t.space3 * 2);
        content.setPreferredSize(d);
        setContentPane(content);
        pack();
        if (parent != null && parent.isShowing()) {
            Point pp = parent.getLocationOnScreen();
            int x = pp.x + (parent.getWidth() - getWidth()) / 2;
            int y = pp.y + 64;
            setLocation(x, y);
        }
    }

    private static Color backgroundFor(Level lv, Tokens t) {
        switch (lv) {
            case SUCCESS: return t.success;
            case ERROR:   return t.danger;
            default:      return new Color(0, 0, 0, 220);
        }
    }

    public static void info(Component parent, String msg)    { show(parent, msg, Level.INFO); }
    public static void success(Component parent, String msg) { show(parent, msg, Level.SUCCESS); }
    public static void error(Component parent, String msg)   { show(parent, msg, Level.ERROR); }

    private static void show(Component parent, String msg, Level lv) {
        GToast t = new GToast(parent, msg, lv);
        t.setVisible(true);
        Timer hold = new Timer(2500, e -> t.startFadeOut());
        hold.setRepeats(false);
        hold.start();
    }

    private void startFadeOut() {
        Timer fade = new Timer(40, null);
        fade.addActionListener(e -> {
            alpha -= 0.08f;
            if (alpha <= 0f) {
                fade.stop();
                dispose();
            } else {
                repaint();
            }
        });
        fade.start();
    }
}
