package ui.component;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import javax.swing.JComponent;
import javax.swing.Timer;
import ui.Theme;
import ui.ThemeAware;
import ui.design.BoardStyle;
import ui.design.PieceStyle;
import ui.design.Tokens;

public class GBoard extends JComponent implements ThemeAware {
    public static final int N = 15;

    public static class Stone {
        public final int col, row;
        public final boolean black;
        public Stone(int col, int row, boolean black) {
            this.col = col; this.row = row; this.black = black;
        }
    }

    private final List<Stone> stones = new ArrayList<>();
    private Stone lastMove;
    private int hoverCol = -1, hoverRow = -1;
    private boolean hoverIsBlack = true;
    private boolean interactive = true;
    private BiConsumer<Integer, Integer> clickHandler;

    private Timer pulseTimer;
    private double pulsePhase;

    private int span, margin, originX, originY, cellSize;

    public GBoard() {
        setPreferredSize(new Dimension(560, 560));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int[] cr = pointToGrid(e.getX(), e.getY());
                int nc = cr[0], nr = cr[1];
                if (nc != hoverCol || nr != hoverRow) {
                    hoverCol = nc; hoverRow = nr;
                    repaint();
                }
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                if (hoverCol != -1 || hoverRow != -1) {
                    hoverCol = -1; hoverRow = -1;
                    repaint();
                }
            }
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!interactive || clickHandler == null) return;
                int[] cr = pointToGrid(e.getX(), e.getY());
                int c = cr[0], r = cr[1];
                if (c < 0 || r < 0) return;
                for (Stone s : stones) if (s.col == c && s.row == r) return;
                clickHandler.accept(c, r);
            }
        });
    }

    public void onClick(BiConsumer<Integer, Integer> handler) { this.clickHandler = handler; }

    public void placeChess(int col, int row, boolean isBlack) {
        if (col < 0 || col >= N || row < 0 || row >= N) return;
        for (Stone s : stones) {
            if (s.col == col && s.row == row) return;
        }
        Stone s = new Stone(col, row, isBlack);
        stones.add(s);
        lastMove = s;
        repaint();
    }

    public void clear() {
        stones.clear();
        lastMove = null;
        repaint();
    }

    public void setStones(List<Stone> ss) {
        stones.clear();
        stones.addAll(ss);
        lastMove = null;
        repaint();
    }

    public void setInteractive(boolean v) { this.interactive = v; }
    public void setHoverIsBlack(boolean v) { this.hoverIsBlack = v; }

    private void computeLayout() {
        int w = getWidth(), h = getHeight();
        span = Math.min(w, h);
        margin = Math.max(20, span / 18);
        cellSize = (span - 2 * margin) / (N - 1);
        int boardSize = cellSize * (N - 1) + 2 * margin;
        originX = (w - boardSize) / 2 + margin;
        originY = (h - boardSize) / 2 + margin;
    }

    private int[] pointToGrid(int px, int py) {
        computeLayout();
        if (cellSize <= 0) return new int[]{-1, -1};
        int c = Math.round((px - originX) / (float) cellSize);
        int r = Math.round((py - originY) / (float) cellSize);
        if (c < 0 || c >= N || r < 0 || r >= N) return new int[]{-1, -1};
        int dx = px - (originX + c * cellSize);
        int dy = py - (originY + r * cellSize);
        if (Math.sqrt(dx * dx + dy * dy) > cellSize * 0.5) return new int[]{-1, -1};
        return new int[]{c, r};
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (pulseTimer == null) {
            pulseTimer = new Timer(33, e -> {
                pulsePhase += 0.15;
                if (lastMove != null) repaint();
            });
            pulseTimer.start();
        }
    }

    @Override
    public void removeNotify() {
        if (pulseTimer != null) {
            pulseTimer.stop();
            pulseTimer = null;
        }
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics g) {
        computeLayout();
        Tokens tk = Theme.tokens();
        BoardStyle bs = Theme.board();
        PieceStyle ps = Theme.pieces();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        g2.setPaint(new GradientPaint(0, 0, bs.bgTop, 0, h, bs.bgBottom));
        g2.fillRect(0, 0, w, h);
        if (bs.drawWoodGrain) drawWoodGrain(g2, w, h);
        g2.setColor(tk.boardLine);
        g2.setStroke(new BasicStroke(1.0f * bs.lineStrokeMul));
        for (int i = 0; i < N; i++) {
            int x = originX + i * cellSize;
            int y = originY + i * cellSize;
            g2.drawLine(originX, y, originX + cellSize * (N - 1), y);
            g2.drawLine(x, originY, x, originY + cellSize * (N - 1));
        }
        g2.setColor(tk.boardStar);
        int[] starIdx = {3, 7, 11};
        int sd = bs.starDotSize;
        for (int sx : starIdx) for (int sy : starIdx) {
            int x = originX + sx * cellSize - sd / 2;
            int y = originY + sy * cellSize - sd / 2;
            g2.fillOval(x, y, sd, sd);
        }
        for (Stone s : stones) drawStone(g2, ps, s.col, s.row, s.black, 255);
        if (lastMove != null) drawLastMoveRing(g2, tk);
        if (interactive && hoverCol >= 0 && hoverRow >= 0) {
            boolean occupied = false;
            for (Stone s : stones) if (s.col == hoverCol && s.row == hoverRow) { occupied = true; break; }
            if (!occupied) {
                int alpha = hoverIsBlack ? 90 : 130;
                drawStone(g2, ps, hoverCol, hoverRow, hoverIsBlack, alpha);
            }
        }
        g2.dispose();
    }

    private void drawWoodGrain(Graphics2D g2, int w, int h) {
        Color grain = new Color(0x3E, 0x27, 0x23, 18);
        g2.setColor(grain);
        for (int y = 0; y < h; y += 7) {
            int dy = (int) (Math.sin(y * 0.04) * 1.5);
            g2.drawLine(0, y + dy, w, y + dy);
        }
    }

    private void drawStone(Graphics2D g2, PieceStyle ps, int col, int row, boolean black, int alpha) {
        int diameter = (int) (cellSize * 0.92);
        int cx = originX + col * cellSize;
        int cy = originY + row * cellSize;
        int x = cx - diameter / 2;
        int y = cy - diameter / 2;
        if (ps.dropShadow && alpha == 255) {
            g2.setColor(new Color(0, 0, 0, 70));
            g2.fillOval(x + 2, y + 3, diameter, diameter);
        }
        Color hi = black ? ps.blackHighlight : ps.whiteHighlight;
        Color base = black ? ps.blackBase : ps.whiteBase;
        if (alpha < 255) {
            hi = new Color(hi.getRed(), hi.getGreen(), hi.getBlue(), alpha);
            base = new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
        }
        float fx = x + diameter * 0.32f;
        float fy = y + diameter * 0.32f;
        float radius = diameter * 0.7f;
        g2.setPaint(new RadialGradientPaint(fx, fy, radius, new float[]{0f, 1f}, new Color[]{hi, base}));
        g2.fillOval(x, y, diameter, diameter);
        g2.setColor(new Color(0, 0, 0, Math.min(alpha, 90)));
        g2.drawOval(x, y, diameter - 1, diameter - 1);
    }

    private void drawLastMoveRing(Graphics2D g2, Tokens tk) {
        int diameter = (int) (cellSize * 0.92);
        int cx = originX + lastMove.col * cellSize;
        int cy = originY + lastMove.row * cellSize;
        double sin = Math.sin(pulsePhase);
        int alpha = (int) (210 + 30 * sin);
        if (alpha < 180) alpha = 180;
        if (alpha > 240) alpha = 240;
        double r = diameter / 2.0 + 3 + 3 * sin;
        Color base = tk.lastMoveRing;
        g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha));
        g2.setStroke(new BasicStroke(2.0f));
        g2.drawOval((int)(cx - r), (int)(cy - r), (int)(r * 2), (int)(r * 2));
    }

    @Override
    public void onSkinChanged() { repaint(); }
}
