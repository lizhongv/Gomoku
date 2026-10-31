package ui.component;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import ui.Theme;
import ui.ThemeAware;
import ui.design.Tokens;

public class StatusBar extends JPanel implements ThemeAware {
    private final Dot dot;
    private final JLabel statusText;
    private final JLabel rightText;

    public StatusBar() {
        super(new BorderLayout());
        Tokens t = Theme.tokens();
        setBackground(t.bgToolbar);
        setBorder(BorderFactory.createEmptyBorder(t.space2, t.space3, t.space2, t.space3));

        dot = new Dot();
        statusText = new JLabel("未连接");
        statusText.setFont(t.fontCaption);
        statusText.setForeground(t.fgMuted);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, t.space2, 0));
        left.setOpaque(false);
        left.add(dot);
        left.add(statusText);

        rightText = new JLabel("");
        rightText.setFont(t.fontCaption);
        rightText.setForeground(t.fgMuted);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(rightText);

        add(left, BorderLayout.WEST);
        add(right, BorderLayout.EAST);

        setOffline("未连接");
    }

    public void setOnline(String text)  { applyState(Theme.tokens().success,  text); }
    public void setOffline(String text) { applyState(Theme.tokens().fgMuted,  text); }
    public void setError(String text)   { applyState(Theme.tokens().danger,   text); }
    public void setRight(String text)   { rightText.setText(text == null ? "" : text); }

    private void applyState(Color c, String text) {
        dot.setColor(c);
        statusText.setText(text == null ? "" : text);
        statusText.setForeground(Theme.tokens().fgDefault);
    }

    @Override
    public void onSkinChanged() {
        Tokens t = Theme.tokens();
        setBackground(t.bgToolbar);
        setBorder(BorderFactory.createEmptyBorder(t.space2, t.space3, t.space2, t.space3));
        statusText.setFont(t.fontCaption);
        rightText.setFont(t.fontCaption);
        rightText.setForeground(t.fgMuted);
        repaint();
    }

    private static class Dot extends JComponent {
        private Color color = Theme.tokens().fgMuted;
        Dot() {
            int s = 10;
            setPreferredSize(new java.awt.Dimension(s, s));
        }
        void setColor(Color c) { this.color = c; repaint(); }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            int d = Math.min(w, h);
            int x = (w - d) / 2, y = (h - d) / 2;
            g2.setColor(color);
            g2.fillOval(x, y, d, d);
            g2.dispose();
        }
    }
}
