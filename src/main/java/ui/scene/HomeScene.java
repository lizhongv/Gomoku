package ui.scene;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import profile.Profile;
import ui.Theme;
import ui.component.GAvatar;
import ui.component.GButton;
import ui.component.GCard;
import ui.component.GToast;
import ui.design.Tokens;
import ui.scene.dialog.LoginDialog;

public class HomeScene implements Scene {
    private final Router router;
    private final JPanel rootPanel;
    private JLabel profileLine;

    public HomeScene(Router router) {
        this.router = router;
        this.rootPanel = build();
    }

    private JPanel build() {
        Tokens t = Theme.tokens();
        JPanel root = new JPanel(new BorderLayout(0, t.space5));
        root.setBackground(t.bgPrimary);
        root.setBorder(BorderFactory.createEmptyBorder(t.space6, t.space6, t.space5, t.space6));

        root.add(buildBrand(), BorderLayout.NORTH);
        root.add(buildCenter(), BorderLayout.CENTER);
        root.add(buildSouth(), BorderLayout.SOUTH);
        return root;
    }

    private JPanel buildBrand() {
        Tokens t = Theme.tokens();
        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));

        GAvatar logo = new GAvatar(96, true);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("Gomoku 五子棋", SwingConstants.CENTER);
        title.setFont(t.fontDisplay);
        title.setForeground(t.fgDefault);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("经典策略对战 · 局域网联机 · 人机对战", SwingConstants.CENTER);
        sub.setFont(t.fontBody);
        sub.setForeground(t.fgMuted);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        brand.add(logo);
        brand.add(Box.createVerticalStrut(t.space3));
        brand.add(title);
        brand.add(Box.createVerticalStrut(t.space2));
        brand.add(sub);
        return brand;
    }

    private JPanel buildCenter() {
        Tokens t = Theme.tokens();
        JPanel grid = new JPanel(new GridLayout(1, 2, t.space5, 0));
        grid.setOpaque(false);

        Color onlineFrom = t.accent;
        Color onlineTo = t.accent.darker();
        GradientCard onlineCard = new GradientCard(onlineFrom, onlineTo,
            "联机对战", "邀请好友 · 观战 · 棋谱共享");
        onlineCard.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { onOnlineClicked(); }
        });

        Color aiFrom = new Color(0x1976D2);
        Color aiTo = new Color(0x0D47A1);
        GradientCard aiCard = new GradientCard(aiFrom, aiTo,
            "人机对战", "本地 AI · 多种难度 · 即开即玩");
        aiCard.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                GToast.info(rootPanel, "人机对战：进 GameScene（Phase 5 实现）");
            }
        });

        grid.add(onlineCard);
        grid.add(aiCard);
        return grid;
    }

    private void onOnlineClicked() {
        Profile p = Profile.get();
        boolean loggedIn = p.nickname != null
            && !p.nickname.isEmpty()
            && !"玩家".equals(p.nickname);
        if (loggedIn) {
            GToast.info(rootPanel, "联机功能：进 LobbyScene");
        } else {
            new LoginDialog(SwingUtilities.getWindowAncestor(root()),
                comm -> router.push(new LobbyScene(router, comm))).setVisible(true);
        }
    }

    private JPanel buildSouth() {
        Tokens t = Theme.tokens();
        JPanel south = new JPanel(new BorderLayout(0, t.space3));
        south.setOpaque(false);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, t.space3, 0));
        actions.setOpaque(false);
        GButton bShop    = GButton.ghost("商店");
        GButton bManual  = GButton.ghost("本地棋谱");
        GButton bSetting = GButton.ghost("设置");
        GButton bAbout   = GButton.ghost("关于");
        bShop.addActionListener(e    -> router.push(new ShopScene(router)));
        bManual.addActionListener(e  -> GToast.info(rootPanel, "本地棋谱：功能后续 Phase 实现"));
        bSetting.addActionListener(e -> GToast.info(rootPanel, "设置：功能后续 Phase 实现"));
        bAbout.addActionListener(e   -> GToast.info(rootPanel, "关于：功能后续 Phase 实现"));
        actions.add(bShop);
        actions.add(bManual);
        actions.add(bSetting);
        actions.add(bAbout);

        GCard profileCard = new GCard(false);
        profileCard.setLayout(new BorderLayout(t.space3, 0));
        GAvatar mini = new GAvatar(40, false);
        profileCard.add(mini, BorderLayout.WEST);
        profileLine = new JLabel(buildProfileText());
        profileLine.setFont(t.fontBody);
        profileLine.setForeground(t.fgDefault);
        profileCard.add(profileLine, BorderLayout.CENTER);

        south.add(actions, BorderLayout.NORTH);
        south.add(profileCard, BorderLayout.SOUTH);
        return south;
    }

    private String buildProfileText() {
        Profile p = Profile.get();
        return "欢迎回来，" + p.nickname + "！您已对局 " + p.totalGames()
            + " 次，" + p.levelTitle() + "(Lv." + p.level() + ")";
    }

    @Override public JComponent root() { return rootPanel; }
    @Override public String title() { return "首页"; }
    @Override public void onEnter() {
        if (profileLine != null) profileLine.setText(buildProfileText());
    }

    private static class GradientCard extends GCard {
        private final Color from, to;
        private final String headline, subline;

        GradientCard(Color from, Color to, String headline, String subline) {
            super(true);
            this.from = from;
            this.to = to;
            this.headline = headline;
            this.subline = subline;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setOpaque(false);
            buildBody();
        }

        private void buildBody() {
            Tokens t = Theme.tokens();
            JPanel inner = new JPanel();
            inner.setOpaque(false);
            inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));

            JLabel h = new JLabel(headline);
            h.setFont(t.fontDisplay);
            h.setForeground(t.fgOnAccent);
            h.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel s = new JLabel(subline);
            s.setFont(t.fontBody);
            s.setForeground(t.fgOnAccent);
            s.setAlignmentX(Component.LEFT_ALIGNMENT);

            inner.add(Box.createVerticalGlue());
            inner.add(h);
            inner.add(Box.createVerticalStrut(t.space2));
            inner.add(s);
            inner.add(Box.createVerticalGlue());
            add(inner, BorderLayout.CENTER);
            setPreferredSize(new Dimension(360, 220));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Tokens t = Theme.tokens();
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            int r = t.radiusLg;
            int rectH = h - 3;
            Color shadow = t.boardShadow;
            int sa = shadow.getAlpha();
            for (int i = 1; i <= 3; i++) {
                int alpha = Math.max(0, sa / (i + 2));
                g2.setColor(new Color(shadow.getRed(), shadow.getGreen(), shadow.getBlue(), alpha));
                g2.fillRoundRect(0, i, w, rectH, r, r);
            }
            GradientPaint gp = new GradientPaint(0, 0, from, w, rectH, to);
            g2.setPaint(gp);
            g2.fillRoundRect(0, 0, w, rectH, r, r);
            g2.dispose();
        }
    }
}
