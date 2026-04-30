package ui.scene;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import ui.events.CoinsChangedEvent;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import feature.shop.CoinManager;
import feature.skin.SkinManager;
import feature.skin.SkinPack;
import profile.Profile;
import ui.EventBus;
import ui.Theme;
import ui.component.BrandHeader;
import ui.component.GButton;
import ui.component.GCard;
import ui.component.GChip;
import ui.design.PieceStyle;
import ui.design.Skin;
import ui.design.Tokens;
import ui.scene.dialog.PurchaseDialog;

public class ShopScene implements Scene {
    private final Router router;
    private final JPanel rootPanel;
    private JLabel coinsLabel;
    private JLabel checkinLabel;
    private JPanel skinTab;
    private JPanel itemTab;
    private JPanel vipTab;
    private final Consumer<CoinsChangedEvent> coinsListener;

    public ShopScene(Router router) {
        this.router = router;
        this.rootPanel = new JPanel(new BorderLayout());
        this.coinsListener = e -> SwingUtilities.invokeLater(this::refresh);
        build();
    }

    private void build() {
        Tokens t = Theme.tokens();
        rootPanel.removeAll();
        rootPanel.setBackground(t.bgPrimary);

        JPanel north = new JPanel(new BorderLayout(0, t.space2));
        north.setOpaque(false);
        BrandHeader header = new BrandHeader();
        header.setCurrentUser(Profile.get().nickname, null);
        north.add(header, BorderLayout.NORTH);
        north.add(buildCoinsBar(), BorderLayout.SOUTH);
        rootPanel.add(north, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, t.space2));
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(t.space3, t.space5, t.space5, t.space5));

        ui.component.GTab tabs = new ui.component.GTab("皮肤", "道具", "会员");
        CardLayout cards = new CardLayout();
        JPanel cardHolder = new JPanel(cards);
        cardHolder.setOpaque(false);

        skinTab = buildSkinTab();
        itemTab = buildItemTab();
        vipTab = buildVipTab();

        cardHolder.add(skinTab, "skin");
        cardHolder.add(itemTab, "item");
        cardHolder.add(vipTab, "vip");
        cards.show(cardHolder, "skin");

        tabs.onSelect(idx -> {
            if (idx == 0) cards.show(cardHolder, "skin");
            else if (idx == 1) cards.show(cardHolder, "item");
            else cards.show(cardHolder, "vip");
        });

        center.add(tabs, BorderLayout.NORTH);
        center.add(cardHolder, BorderLayout.CENTER);
        rootPanel.add(center, BorderLayout.CENTER);

        rootPanel.revalidate();
        rootPanel.repaint();
    }

    private JPanel buildCoinsBar() {
        Tokens t = Theme.tokens();
        GCard bar = new GCard(false);
        bar.setLayout(new BorderLayout(t.space3, 0));
        bar.setBorder(BorderFactory.createEmptyBorder(t.space3, t.space5, t.space3, t.space5));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, t.space3, 0));
        left.setOpaque(false);
        JLabel icon = new JLabel("💰");
        icon.setFont(t.fontTitle);
        coinsLabel = new JLabel(CoinManager.balance() + " 金币");
        coinsLabel.setFont(t.fontTitle);
        coinsLabel.setForeground(t.fgDefault);
        checkinLabel = new JLabel("连续签到 " + Profile.get().consecutiveCheckinDays + " 天");
        checkinLabel.setFont(t.fontBody);
        checkinLabel.setForeground(t.fgMuted);
        left.add(icon);
        left.add(coinsLabel);
        left.add(checkinLabel);

        GButton back = GButton.ghost("← 返回");
        back.addActionListener(e -> router.pop());

        bar.add(left, BorderLayout.WEST);
        bar.add(back, BorderLayout.EAST);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setBorder(BorderFactory.createEmptyBorder(0, t.space5, 0, t.space5));
        wrap.add(bar, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel buildSkinTab() {
        Tokens t = Theme.tokens();
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, t.space4, t.space4));
        panel.setOpaque(false);
        for (Skin s : SkinPack.ALL) {
            panel.add(buildSkinCard(s));
        }
        return panel;
    }

    private GCard buildSkinCard(Skin skin) {
        Tokens t = Theme.tokens();
        GCard card = new GCard(true);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(200, 260));

        SkinPreview preview = new SkinPreview(skin);
        preview.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel name = new JLabel(skin.displayName);
        name.setFont(t.fontHeading);
        name.setForeground(t.fgDefault);
        name.setAlignmentX(Component.CENTER_ALIGNMENT);

        String priceText = skin.premium ? skin.priceCoins + " 金币" : "免费";
        Color chipBg = skin.premium ? t.warning : t.success;
        GChip price = new GChip(priceText, chipBg, t.fgOnAccent);
        JPanel chipWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        chipWrap.setOpaque(false);
        chipWrap.add(price);
        chipWrap.setAlignmentX(Component.CENTER_ALIGNMENT);

        JComponent action = buildSkinAction(skin);
        action.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(preview);
        card.add(Box.createVerticalStrut(t.space2));
        card.add(name);
        card.add(Box.createVerticalStrut(t.space1));
        card.add(chipWrap);
        card.add(Box.createVerticalStrut(t.space2));
        card.add(action);
        return card;
    }

    private JComponent buildSkinAction(Skin skin) {
        Skin curr = SkinManager.current();
        boolean isCurrent = curr.id.equals(skin.id);
        boolean isUnlocked = SkinManager.isUnlocked(skin.id);

        if (isCurrent) {
            GButton b = GButton.ghost("使用中");
            b.setEnabled(false);
            return b;
        }
        if (isUnlocked) {
            GButton b = GButton.primary("使用");
            b.addActionListener(e -> {
                SkinManager.setCurrent(skin.id);
                refresh();
            });
            return b;
        }
        GButton b = GButton.secondary(skin.priceCoins + " 💰 购买");
        b.addActionListener(e -> {
            PurchaseDialog d = new PurchaseDialog(SwingUtilities.getWindowAncestor(rootPanel), skin);
            d.setVisible(true);
            refresh();
        });
        return b;
    }

    private JPanel buildItemTab() {
        Tokens t = Theme.tokens();
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, t.space4, t.space4));
        panel.setOpaque(false);
        String[] items = {"悔棋券", "提示卡", "VIP 体验", "棋盘格调"};
        for (String name : items) {
            GCard c = new GCard(true);
            c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
            c.setPreferredSize(new Dimension(200, 260));
            JLabel emoji = new JLabel("🚧", SwingConstants.CENTER);
            emoji.setFont(t.fontDisplay);
            emoji.setAlignmentX(Component.CENTER_ALIGNMENT);
            JLabel n = new JLabel(name);
            n.setFont(t.fontHeading);
            n.setForeground(t.fgDefault);
            n.setAlignmentX(Component.CENTER_ALIGNMENT);
            JLabel sub = new JLabel("敬请期待");
            sub.setFont(t.fontBody);
            sub.setForeground(t.fgMuted);
            sub.setAlignmentX(Component.CENTER_ALIGNMENT);
            c.add(Box.createVerticalGlue());
            c.add(emoji);
            c.add(Box.createVerticalStrut(t.space2));
            c.add(n);
            c.add(Box.createVerticalStrut(t.space1));
            c.add(sub);
            c.add(Box.createVerticalGlue());
            panel.add(c);
        }
        return panel;
    }

    private JPanel buildVipTab() {
        Tokens t = Theme.tokens();
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, t.space4, t.space4));
        panel.setOpaque(false);
        GCard c = new GCard(true);
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        c.setPreferredSize(new Dimension(640, 320));
        JLabel emoji = new JLabel("👑", SwingConstants.CENTER);
        emoji.setFont(t.fontDisplay);
        emoji.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel n = new JLabel("VIP 会员");
        n.setFont(t.fontDisplay);
        n.setForeground(t.fgDefault);
        n.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel sub = new JLabel("VIP 会员功能即将上线");
        sub.setFont(t.fontBody);
        sub.setForeground(t.fgMuted);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);
        c.add(Box.createVerticalGlue());
        c.add(emoji);
        c.add(Box.createVerticalStrut(t.space3));
        c.add(n);
        c.add(Box.createVerticalStrut(t.space2));
        c.add(sub);
        c.add(Box.createVerticalGlue());
        panel.add(c);
        return panel;
    }

    private void refresh() {
        build();
    }

    @Override public JComponent root() { return rootPanel; }
    @Override public String title() { return "商店"; }

    @Override
    public void onEnter() {
        EventBus.subscribe(CoinsChangedEvent.class, coinsListener);
        refresh();
    }

    @Override
    public void onLeave() {
        EventBus.unsubscribe(CoinsChangedEvent.class, coinsListener);
    }

    private static class SkinPreview extends JPanel {
        private final Skin skin;

        SkinPreview(Skin skin) {
            this.skin = skin;
            setOpaque(false);
            setPreferredSize(new Dimension(160, 100));
            setMaximumSize(new Dimension(180, 100));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            g2.setColor(skin.board.bgTop);
            g2.fillRoundRect(0, 0, w, h, 8, 8);
            g2.setColor(skin.tokens.boardLine);
            int n = 4;
            int margin = 14;
            int cell = (Math.min(w, h) - 2 * margin) / (n - 1);
            int ox = (w - cell * (n - 1)) / 2;
            int oy = (h - cell * (n - 1)) / 2;
            for (int i = 0; i < n; i++) {
                g2.drawLine(ox, oy + i * cell, ox + cell * (n - 1), oy + i * cell);
                g2.drawLine(ox + i * cell, oy, ox + i * cell, oy + cell * (n - 1));
            }
            int diameter = (int) (cell * 0.85);
            int[][] stones = {{0, 0, 1}, {1, 1, 0}, {2, 1, 1}, {1, 2, 0}};
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
