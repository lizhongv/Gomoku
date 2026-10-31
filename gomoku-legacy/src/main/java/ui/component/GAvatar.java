package ui.component;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;
import ui.Theme;
import ui.ThemeAware;
import ui.design.PieceStyle;

public class GAvatar extends JComponent implements ThemeAware {
    private final int size;
    private boolean isBlack;
    private BufferedImage image;

    public GAvatar(int size, boolean isBlack) {
        this.size = size;
        this.isBlack = isBlack;
        Dimension d = new Dimension(size, size);
        setPreferredSize(d);
        setMinimumSize(d);
        setMaximumSize(d);
    }

    public void setImage(BufferedImage img) {
        this.image = img;
        repaint();
    }

    public void setImagePath(String p) {
        try {
            if (p != null && !p.isEmpty()) {
                java.io.File f = new java.io.File(p);
                if (f.isFile()) setImage(javax.imageio.ImageIO.read(f));
            }
        } catch (Exception ignored) {}
    }

    public void setBlack(boolean v) {
        this.isBlack = v;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        int d = Math.min(w, h);
        int x = (w - d) / 2, y = (h - d) / 2;
        Shape circle = new Ellipse2D.Double(x, y, d, d);
        Shape oldClip = g2.getClip();
        g2.setClip(circle);
        if (image != null) {
            g2.drawImage(image, x, y, d, d, null);
        } else {
            PieceStyle p = Theme.pieces();
            float fx = x + d * 0.35f;
            float fy = y + d * 0.35f;
            float radius = d * 0.6f;
            Color hi = isBlack ? p.blackHighlight : p.whiteHighlight;
            Color base = isBlack ? p.blackBase : p.whiteBase;
            RadialGradientPaint paint = new RadialGradientPaint(
                fx, fy, radius, new float[]{0f, 1f}, new Color[]{hi, base});
            g2.setPaint(paint);
            g2.fill(circle);
        }
        g2.setClip(oldClip);
        g2.setColor(new Color(0, 0, 0, 90));
        g2.drawOval(x, y, d - 1, d - 1);
        g2.dispose();
    }

    @Override
    public void onSkinChanged() { repaint(); }
}
