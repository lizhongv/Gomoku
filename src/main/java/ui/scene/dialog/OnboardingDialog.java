package ui.scene.dialog;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import profile.Profile;
import ui.Theme;
import ui.component.GAvatar;
import ui.component.GButton;
import ui.component.GDialog;
import ui.design.Tokens;

public class OnboardingDialog extends GDialog {
    private static final int STEPS = 3;
    private int step = 0;
    private final CardLayout cards;
    private final JPanel cardHolder;
    private final GButton nextBtn;

    public OnboardingDialog(Window owner) {
        super(owner, "新手引导", true);
        Tokens t = Theme.tokens();

        cards = new CardLayout();
        cardHolder = new JPanel(cards);
        cardHolder.setOpaque(false);
        cardHolder.add(buildStep1(), "0");
        cardHolder.add(buildStep2(), "1");
        cardHolder.add(buildStep3(), "2");

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, t.space2, 0));
        south.setOpaque(false);

        GButton skipBtn = GButton.ghost("跳过");
        skipBtn.addActionListener(e -> finish());

        nextBtn = GButton.primary("下一步");
        nextBtn.addActionListener(e -> {
            if (step < STEPS - 1) {
                step++;
                cards.show(cardHolder, String.valueOf(step));
                refreshNextLabel();
            } else {
                finish();
            }
        });

        south.add(skipBtn);
        south.add(nextBtn);

        content.add(cardHolder, BorderLayout.CENTER);
        content.add(south, BorderLayout.SOUTH);

        setSize(480, 420);
        setLocationRelativeTo(owner);
    }

    private void refreshNextLabel() {
        nextBtn.setText(step == STEPS - 1 ? "开始游戏" : "下一步");
    }

    private void finish() {
        Profile p = Profile.get();
        p.onboardingDone = true;
        p.save();
        dispose();
    }

    private JPanel buildStep1() {
        Tokens t = Theme.tokens();
        JPanel panel = vertical();
        GAvatar logo = new GAvatar(96, true);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel title = centeredLabel("欢迎来到 Gomoku", t.fontDisplay, t.fgDefault);
        JLabel sub = centeredLabel("经典五子棋，简单上手，复盘进阶", t.fontBody, t.fgMuted);
        panel.add(Box.createVerticalGlue());
        panel.add(logo);
        panel.add(Box.createVerticalStrut(t.space4));
        panel.add(title);
        panel.add(Box.createVerticalStrut(t.space2));
        panel.add(sub);
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel buildStep2() {
        Tokens t = Theme.tokens();
        JPanel panel = vertical();
        MiniBoard board = new MiniBoard();
        board.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel title = centeredLabel("落子规则", t.fontTitle, t.fgDefault);
        JLabel sub = centeredLabel("黑棋先手，纵横斜任一方向五子连珠者胜",
                t.fontBody, t.fgMuted);
        panel.add(Box.createVerticalGlue());
        panel.add(board);
        panel.add(Box.createVerticalStrut(t.space3));
        panel.add(title);
        panel.add(Box.createVerticalStrut(t.space2));
        panel.add(sub);
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel buildStep3() {
        Tokens t = Theme.tokens();
        JPanel panel = vertical();
        ShopPlaceholder shop = new ShopPlaceholder();
        shop.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel title = centeredLabel("商店与金币", t.fontTitle, t.fgDefault);
        JLabel sub = centeredLabel("完成对局赚金币，购买皮肤改变棋盘风格",
                t.fontBody, t.fgMuted);
        panel.add(Box.createVerticalGlue());
        panel.add(shop);
        panel.add(Box.createVerticalStrut(t.space3));
        panel.add(title);
        panel.add(Box.createVerticalStrut(t.space2));
        panel.add(sub);
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel vertical() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        return p;
    }

    private JLabel centeredLabel(String text, java.awt.Font font, Color fg) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(font);
        l.setForeground(fg);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        return l;
    }

    private static class MiniBoard extends JPanel {
        MiniBoard() {
            setOpaque(false);
            Dimension d = new Dimension(180, 180);
            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(d);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Tokens t = Theme.tokens();
            int w = getWidth(), h = getHeight();
            g2.setColor(Theme.currentSkin().board.bgTop);
            g2.fillRoundRect(0, 0, w, h, 12, 12);
            g2.setColor(t.boardLine);
            int n = 7, margin = 16;
            int cell = (Math.min(w, h) - 2 * margin) / (n - 1);
            int ox = (w - cell * (n - 1)) / 2;
            int oy = (h - cell * (n - 1)) / 2;
            for (int i = 0; i < n; i++) {
                g2.drawLine(ox, oy + i * cell, ox + cell * (n - 1), oy + i * cell);
                g2.drawLine(ox + i * cell, oy, ox + i * cell, oy + cell * (n - 1));
            }
            int dia = (int) (cell * 0.9);
            int[][] stones = {{2, 3, 1}, {3, 3, 0}, {3, 4, 1}, {4, 3, 0}, {4, 4, 1}};
            for (int[] s : stones) {
                int cx = ox + s[0] * cell, cy = oy + s[1] * cell;
                int x = cx - dia / 2, y = cy - dia / 2;
                g2.setColor(s[2] == 1 ? new Color(0x202020) : new Color(0xF0F0F0));
                g2.fillOval(x, y, dia, dia);
                g2.setColor(new Color(0, 0, 0, 80));
                g2.drawOval(x, y, dia - 1, dia - 1);
            }
            g2.dispose();
        }
    }

    private static class ShopPlaceholder extends JPanel {
        ShopPlaceholder() {
            setOpaque(false);
            Dimension d = new Dimension(220, 140);
            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(d);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Tokens t = Theme.tokens();
            int w = getWidth(), h = getHeight();
            g2.setColor(t.bgCard);
            g2.fillRoundRect(0, 0, w, h, 12, 12);
            g2.setColor(t.borderLight);
            g2.drawRoundRect(0, 0, w - 1, h - 1, 12, 12);
            g2.setColor(t.warning);
            g2.fillOval(w / 2 - 22, h / 2 - 30, 44, 44);
            g2.setColor(t.fgOnAccent);
            g2.setFont(t.fontTitle);
            String coin = "金";
            int sw = g2.getFontMetrics().stringWidth(coin);
            g2.drawString(coin, w / 2 - sw / 2, h / 2 - 30 + 30);
            g2.setColor(t.fgDefault);
            g2.setFont(t.fontBody);
            String tip = "皮肤 / 道具 / 会员";
            int tw = g2.getFontMetrics().stringWidth(tip);
            g2.drawString(tip, w / 2 - tw / 2, h / 2 + 36);
            g2.dispose();
        }
    }
}
