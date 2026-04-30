package ui.scene.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Window;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import feature.shop.CoinManager;
import feature.skin.SkinManager;
import ui.Theme;
import ui.component.GButton;
import ui.component.GChip;
import ui.component.GDialog;
import ui.component.GToast;
import ui.design.PieceStyle;
import ui.design.Skin;
import ui.design.Tokens;

public class PurchaseDialog extends GDialog {

    public PurchaseDialog(Window owner, Skin skin) {
        super(owner, "购买皮肤", true);
        Tokens t = Theme.tokens();

        JPanel preview = new SkinBigPreview(skin);
        preview.setPreferredSize(new Dimension(200, 200));
        JPanel previewWrap = new JPanel(new FlowLayout(FlowLayout.CENTER));
        previewWrap.setOpaque(false);
        previewWrap.add(preview);

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));

        JLabel name = new JLabel(skin.displayName);
        name.setFont(t.fontTitle);
        name.setForeground(t.fgDefault);
        name.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel desc = new JLabel("解锁后可在所有对局中使用");
        desc.setFont(t.fontBody);
        desc.setForeground(t.fgMuted);
        desc.setAlignmentX(Component.CENTER_ALIGNMENT);

        GChip price = new GChip(skin.priceCoins + " 金币", t.warning, t.fgOnAccent);
        JPanel chipWrap = new JPanel(new FlowLayout(FlowLayout.CENTER));
        chipWrap.setOpaque(false);
        chipWrap.add(price);
        chipWrap.setAlignmentX(Component.CENTER_ALIGNMENT);

        info.add(name);
        info.add(Box.createVerticalStrut(t.space2));
        info.add(desc);
        info.add(Box.createVerticalStrut(t.space2));
        info.add(chipWrap);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, t.space2, 0));
        buttons.setOpaque(false);

        boolean canAfford = CoinManager.balance() >= skin.priceCoins;
        GButton buyBtn = GButton.primary("🪙 " + skin.priceCoins + " 金币购买");
        buyBtn.setEnabled(canAfford);
        buyBtn.addActionListener(e -> {
            if (CoinManager.spend(skin.priceCoins, "购买 " + skin.id)) {
                SkinManager.unlock(skin.id);
                SkinManager.setCurrent(skin.id);
                GToast.success(owner, "购买成功！皮肤已切换");
                dispose();
            } else {
                GToast.error(owner, "金币不足");
            }
        });

        GButton trialBtn = GButton.secondary("试用一局（占位）");
        trialBtn.addActionListener(e -> {
            SkinManager.unlock(skin.id);
            SkinManager.setCurrent(skin.id);
            GToast.info(owner, "已试用");
            dispose();
        });

        GButton cancelBtn = GButton.ghost("取消");
        cancelBtn.addActionListener(e -> dispose());

        buttons.add(buyBtn);
        buttons.add(trialBtn);
        buttons.add(cancelBtn);

        JPanel center = new JPanel(new BorderLayout(0, t.space2));
        center.setOpaque(false);
        center.add(previewWrap, BorderLayout.NORTH);
        center.add(info, BorderLayout.CENTER);

        content.add(center, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);

        setSize(360, 420);
        setLocationRelativeTo(owner);
    }

    private static class SkinBigPreview extends JPanel {
        private final Skin skin;

        SkinBigPreview(Skin skin) {
            this.skin = skin;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder());
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            g2.setColor(skin.board.bgTop);
            g2.fillRoundRect(0, 0, w, h, 12, 12);
            g2.setColor(skin.tokens.boardLine);
            int n = 5;
            int margin = 24;
            int cell = (Math.min(w, h) - 2 * margin) / (n - 1);
            int ox = (w - cell * (n - 1)) / 2;
            int oy = (h - cell * (n - 1)) / 2;
            for (int i = 0; i < n; i++) {
                g2.drawLine(ox, oy + i * cell, ox + cell * (n - 1), oy + i * cell);
                g2.drawLine(ox + i * cell, oy, ox + i * cell, oy + cell * (n - 1));
            }
            int diameter = (int) (cell * 0.85);
            int[][] stones = {{1, 1, 1}, {2, 2, 0}, {3, 1, 1}, {2, 3, 0}, {1, 2, 1}};
            PieceStyle ps = skin.pieces;
            for (int[] sx : stones) {
                int cc = sx[0], rr = sx[1];
                boolean black = sx[2] == 1;
                int cx = ox + cc * cell;
                int cy = oy + rr * cell;
                int x = cx - diameter / 2;
                int y = cy - diameter / 2;
                Color hi = black ? ps.blackHighlight : ps.whiteHighlight;
                Color base = black ? ps.blackBase : ps.whiteBase;
                float fx = x + diameter * 0.32f;
                float fy = y + diameter * 0.32f;
                float radius = diameter * 0.7f;
                g2.setPaint(new RadialGradientPaint(fx, fy, radius,
                        new float[]{0f, 1f}, new Color[]{hi, base}));
                g2.fillOval(x, y, diameter, diameter);
            }
            g2.dispose();
        }
    }
}
