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
        Profile p = Profile.get();
        Object[][] items = {
            {"悔棋券",    "↶", "AI 对局中撤回最近一手", 500,  "undo",  String.valueOf(p.undoTickets) + " 张"},
            {"提示卡",    "💡", "高亮一个推荐落子位置",   300,  "hint",  String.valueOf(p.hintTickets) + " 张"},
            {"VIP 体验",  "👑", "7 天 VIP（金币双倍 + 全皮肤）", 1000, "vip7", p.isVip() ? "VIP 中" : "未持有"},
            {"AI 加速",   "⚡", "AI 思考更快，开/关切换",  200,  "fast",  p.aiFastMode ? "已开启" : "未开启"}
        };
        for (Object[] item : items) {
            String name = (String) item[0];
            String emo  = (String) item[1];
            String desc = (String) item[2];
            int    price= (Integer) item[3];
            String code = (String) item[4];
            String stat = (String) item[5];
            GCard c = new GCard(true);
            c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
            c.setPreferredSize(new Dimension(220, 280));
            JLabel emoji = new JLabel(emo, SwingConstants.CENTER);
            emoji.setFont(t.fontDisplay);
            emoji.setAlignmentX(Component.CENTER_ALIGNMENT);
            JLabel nLbl = new JLabel(name);
            nLbl.setFont(t.fontHeading);
            nLbl.setForeground(t.fgDefault);
            nLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
            JLabel descLbl = new JLabel("<html><div style='text-align:center;'>" + desc + "</div></html>");
            descLbl.setFont(t.fontCaption);
            descLbl.setForeground(t.fgMuted);
            descLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
            JLabel statLbl = new JLabel("当前: " + stat);
            statLbl.setFont(t.fontCaption);
            statLbl.setForeground(t.fgDefault);
            statLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
            JPanel chipWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            chipWrap.setOpaque(false);
            chipWrap.add(new GChip(price + " 金币", t.warning, t.fgOnAccent));
            chipWrap.setAlignmentX(Component.CENTER_ALIGNMENT);
            String btnLabel;
            if ("fast".equals(code)) btnLabel = p.aiFastMode ? "关闭加速" : (price + " 💰 启用");
            else if ("vip7".equals(code) && p.isVip()) btnLabel = "续期 7 天";
            else btnLabel = price + " 💰 购买";
            GButton buy = GButton.primary(btnLabel);
            buy.setAlignmentX(Component.CENTER_ALIGNMENT);
            boolean canAfford = CoinManager.balance() >= price;
            if (!canAfford && !("fast".equals(code) && p.aiFastMode)) buy.setEnabled(false);
            buy.addActionListener(e -> handleItemPurchase(code, price));
            c.add(Box.createVerticalGlue());
            c.add(emoji);
            c.add(Box.createVerticalStrut(t.space1));
            c.add(nLbl);
            c.add(Box.createVerticalStrut(t.space1));
            c.add(descLbl);
            c.add(Box.createVerticalStrut(t.space2));
            c.add(statLbl);
            c.add(Box.createVerticalStrut(t.space1));
            c.add(chipWrap);
            c.add(Box.createVerticalStrut(t.space2));
            c.add(buy);
            c.add(Box.createVerticalGlue());
            panel.add(c);
        }
        return panel;
    }

    private void handleItemPurchase(String code, int price) {
        Profile p = Profile.get();
        if ("fast".equals(code) && p.aiFastMode) {
            p.aiFastMode = false;
            p.save();
            ui.component.GToast.success(rootPanel, "已关闭 AI 加速");
            refresh();
            return;
        }
        if (!CoinManager.spend(price, "购买道具 " + code)) {
            ui.component.GToast.error(rootPanel, "金币不足");
            return;
        }
        switch (code) {
            case "undo":
                p.undoTickets += 1;
                ui.component.GToast.success(rootPanel, "已购入 1 张悔棋券");
                break;
            case "hint":
                p.hintTickets += 1;
                ui.component.GToast.success(rootPanel, "已购入 1 张提示卡");
                break;
            case "vip7":
                long base = Math.max(p.vipUntilMillis, System.currentTimeMillis());
                p.vipUntilMillis = base + 7L * 24L * 3600L * 1000L;
                ui.component.GToast.success(rootPanel, "VIP 已开通 7 天");
                break;
            case "fast":
                p.aiFastMode = true;
                ui.component.GToast.success(rootPanel, "AI 加速已启用");
                break;
        }
        p.save();
        refresh();
    }

    private JPanel buildVipTab() {
        Tokens t = Theme.tokens();
        JPanel root = new JPanel(new BorderLayout(0, t.space3));
        root.setOpaque(false);
        Profile p = Profile.get();

        JPanel hero = new JPanel(new FlowLayout(FlowLayout.CENTER, t.space2, 0));
        hero.setOpaque(false);
        JLabel crown = new JLabel("👑");
        crown.setFont(t.fontDisplay);
        JLabel hLabel = new JLabel(p.isVip() ? "VIP 会员中" : "VIP 会员");
        hLabel.setFont(t.fontDisplay);
        hLabel.setForeground(t.fgDefault);
        hero.add(crown);
        hero.add(hLabel);

        JPanel benefits = new JPanel(new FlowLayout(FlowLayout.CENTER));
        benefits.setOpaque(false);
        JLabel ben = new JLabel("<html><div style='text-align:center;'>"
                + "✓ 全部皮肤免费使用 &nbsp; ✓ 胜利金币双倍 &nbsp; ✓ 无购买冷却"
                + "<br>当前状态: " + (p.isVip()
                        ? "剩余 " + (p.vipRemainingMillis() / (24L*3600L*1000L)) + " 天"
                        : "未开通")
                + "</div></html>");
        ben.setFont(t.fontBody);
        ben.setForeground(t.fgMuted);
        benefits.add(ben);

        JPanel plans = new JPanel(new FlowLayout(FlowLayout.CENTER, t.space4, t.space2));
        plans.setOpaque(false);
        Object[][] tiers = {
            {"月卡",  30,   3000},
            {"季卡",  90,   8000},
            {"年卡",  365, 25000}
        };
        for (Object[] tier : tiers) {
            String name = (String) tier[0];
            int days = (Integer) tier[1];
            int price = (Integer) tier[2];
            GCard c = new GCard(true);
            c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
            c.setPreferredSize(new Dimension(180, 220));
            JLabel nm = new JLabel(name);
            nm.setFont(t.fontTitle);
            nm.setForeground(t.fgDefault);
            nm.setAlignmentX(Component.CENTER_ALIGNMENT);
            JLabel dy = new JLabel(days + " 天");
            dy.setFont(t.fontBody);
            dy.setForeground(t.fgMuted);
            dy.setAlignmentX(Component.CENTER_ALIGNMENT);
            GChip pc = new GChip(price + " 金币", t.warning, t.fgOnAccent);
            JPanel pcw = new JPanel(new FlowLayout(FlowLayout.CENTER));
            pcw.setOpaque(false);
            pcw.add(pc);
            pcw.setAlignmentX(Component.CENTER_ALIGNMENT);
            GButton buy = GButton.primary("购买");
            buy.setAlignmentX(Component.CENTER_ALIGNMENT);
            buy.setEnabled(CoinManager.balance() >= price);
            buy.addActionListener(e -> {
                if (!CoinManager.spend(price, "VIP " + name)) {
                    ui.component.GToast.error(rootPanel, "金币不足");
                    return;
                }
                Profile pp = Profile.get();
                long base = Math.max(pp.vipUntilMillis, System.currentTimeMillis());
                pp.vipUntilMillis = base + days * 24L * 3600L * 1000L;
                pp.save();
                ui.component.GToast.success(rootPanel, "已开通 " + name + " - VIP 续至 "
                        + new java.text.SimpleDateFormat("yyyy-MM-dd")
                                .format(new java.util.Date(pp.vipUntilMillis)));
                refresh();
            });
            c.add(Box.createVerticalGlue());
            c.add(nm);
            c.add(Box.createVerticalStrut(t.space2));
            c.add(dy);
            c.add(Box.createVerticalStrut(t.space2));
            c.add(pcw);
            c.add(Box.createVerticalStrut(t.space3));
            c.add(buy);
            c.add(Box.createVerticalGlue());
            plans.add(c);
        }

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(hero);
        top.add(benefits);

        root.add(top, BorderLayout.NORTH);
        root.add(plans, BorderLayout.CENTER);
        return root;
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
