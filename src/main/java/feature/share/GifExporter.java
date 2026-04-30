package feature.share;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.BiConsumer;
import com.madgag.gif.fmsware.AnimatedGifEncoder;
import config.AppConfig;
import feature.skin.SkinPack;
import game.Manual;
import ui.design.BoardStyle;
import ui.design.PieceStyle;
import ui.design.Skin;
import ui.design.Tokens;

public final class GifExporter {
    private static final int N = 15;

    private GifExporter() {}

    public static File export(Manual manual, ExportOpts opts, BiConsumer<Integer, Integer> progress)
            throws IOException {
        if (manual == null) throw new IllegalArgumentException("manual is null");
        if (opts == null) opts = new ExportOpts();

        Skin skin = SkinPack.byId(opts.skinId);
        int size = Math.max(120, opts.scale);

        File dir = new File(AppConfig.get().userDataDir(), "exports");
        if (!dir.exists()) dir.mkdirs();
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String black = safeUser(manual.getbUser());
        String white = safeUser(manual.getwUer());
        String fname = String.format("manual_%s_vs_%s_%s.gif", black, white, fmt.format(new Date()));
        File out = new File(dir, fname);

        AnimatedGifEncoder enc = new AnimatedGifEncoder();
        if (!enc.start(out.getAbsolutePath())) {
            throw new IOException("无法启动 GIF 编码: " + out.getAbsolutePath());
        }
        enc.setRepeat(0);
        enc.setDelay(Math.max(50, opts.frameDelayMs));
        enc.setSize(size, size);
        enc.setQuality(10);

        int total = manual.getTotalStep();
        int[][] cl = manual.getChessList();
        int frames = Math.min(total, opts.maxFrames);
        if (cl != null) frames = Math.min(frames, cl.length);

        String winnerLabel = winnerLabel(manual);
        try {
            for (int i = 0; i < frames; i++) {
                BufferedImage img = drawFrame(skin, cl, i, size, opts.showLastMoveRing,
                        manual.getbUser(), manual.getwUer(), null);
                enc.addFrame(img);
                if (progress != null) progress.accept(i + 1, frames);
            }
            int holdMs = Math.max(opts.frameDelayMs * 4, 2000);
            enc.setDelay(holdMs);
            BufferedImage finalImg = drawFrame(skin, cl, frames - 1, size, opts.showLastMoveRing,
                    manual.getbUser(), manual.getwUer(), winnerLabel);
            enc.addFrame(finalImg);
        } finally {
            enc.finish();
        }
        return out;
    }

    private static String winnerLabel(Manual m) {
        String w = m.getWinner();
        if (w == null || w.isEmpty()) return "终局";
        return w + " 胜";
    }

    private static String safeUser(String s) {
        if (s == null || s.isEmpty()) return "anon";
        return s.replaceAll("[^A-Za-z0-9_\\u4e00-\\u9fa5]", "_");
    }

    private static BufferedImage drawFrame(Skin skin, int[][] cl, int upto, int size, boolean ring,
                                            String black, String white, String banner) {
        int headerH = 36;
        BufferedImage img = new BufferedImage(size, size + headerH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Tokens tk = skin.tokens;
        BoardStyle bs = skin.board;
        PieceStyle ps = skin.pieces;

        g2.setColor(tk.bgToolbar);
        g2.fillRect(0, 0, size, headerH);
        g2.setFont(ui.design.Fonts.sans(14, java.awt.Font.BOLD));
        g2.setColor(tk.fgDefault);
        String title = "● " + safeText(black, "黑方") + "  vs  ○ " + safeText(white, "白方");
        java.awt.FontMetrics fm = g2.getFontMetrics();
        g2.drawString(title, (size - fm.stringWidth(title)) / 2, headerH - 12);

        g2.translate(0, headerH);

        g2.setPaint(new GradientPaint(0, 0, bs.bgTop, 0, size, bs.bgBottom));
        g2.fillRect(0, 0, size, size);

        if (bs.drawWoodGrain) {
            Color grain = new Color(0x3E, 0x27, 0x23, 18);
            g2.setColor(grain);
            for (int y = 0; y < size; y += 7) {
                int dy = (int) (Math.sin(y * 0.04) * 1.5);
                g2.drawLine(0, y + dy, size, y + dy);
            }
        }

        int margin = Math.max(20, size / 18);
        int cellSize = (size - 2 * margin) / (N - 1);
        int boardSize = cellSize * (N - 1) + 2 * margin;
        int originX = (size - boardSize) / 2 + margin;
        int originY = (size - boardSize) / 2 + margin;

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

        int diameter = (int) (cellSize * 0.92);
        if (cl != null) {
            int last = Math.min(upto, cl.length - 1);
            int lastCol = -1, lastRow = -1;
            boolean lastBlack = true;
            for (int i = 0; i <= last; i++) {
                int[] mv = cl[i];
                if (mv == null || mv.length < 2) continue;
                int col = mv[0];
                int row = mv[1];
                if (col < 0 || col >= N || row < 0 || row >= N) continue;
                boolean isBlack = (i % 2 == 0);
                drawStone(g2, ps, originX, originY, cellSize, diameter, col, row, isBlack);
                lastCol = col; lastRow = row; lastBlack = isBlack;
            }
            if (ring && lastCol >= 0) {
                int cx = originX + lastCol * cellSize;
                int cy = originY + lastRow * cellSize;
                Color base = tk.lastMoveRing;
                g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 220));
                g2.setStroke(new BasicStroke(2.0f));
                double r = diameter / 2.0 + 4;
                g2.drawOval((int) (cx - r), (int) (cy - r), (int) (r * 2), (int) (r * 2));
                if (!lastBlack) {
                    // suppress warning for unused var
                }
            }
        }

        if (banner != null) {
            int boxH = 64;
            int boxY = (size - boxH) / 2;
            g2.setColor(new Color(0, 0, 0, 170));
            g2.fillRect(0, boxY, size, boxH);
            g2.setColor(java.awt.Color.WHITE);
            g2.setFont(ui.design.Fonts.sans(28, java.awt.Font.BOLD));
            java.awt.FontMetrics fmB = g2.getFontMetrics();
            g2.drawString(banner, (size - fmB.stringWidth(banner)) / 2, boxY + 42);
        }

        g2.dispose();
        return img;
    }

    private static String safeText(String s, String fallback) {
        return (s == null || s.isEmpty()) ? fallback : s;
    }

    private static void drawStone(Graphics2D g2, PieceStyle ps, int originX, int originY,
                                  int cellSize, int diameter, int col, int row, boolean black) {
        int cx = originX + col * cellSize;
        int cy = originY + row * cellSize;
        int x = cx - diameter / 2;
        int y = cy - diameter / 2;
        if (ps.dropShadow) {
            g2.setColor(new Color(0, 0, 0, 70));
            g2.fillOval(x + 2, y + 3, diameter, diameter);
        }
        Color hi = black ? ps.blackHighlight : ps.whiteHighlight;
        Color base = black ? ps.blackBase : ps.whiteBase;
        float fx = x + diameter * 0.32f;
        float fy = y + diameter * 0.32f;
        float radius = diameter * 0.7f;
        g2.setPaint(new RadialGradientPaint(fx, fy, radius,
                new float[]{0f, 1f}, new Color[]{hi, base}));
        g2.fillOval(x, y, diameter, diameter);
        g2.setColor(new Color(0, 0, 0, 90));
        g2.drawOval(x, y, diameter - 1, diameter - 1);
    }
}
