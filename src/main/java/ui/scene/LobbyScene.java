package ui.scene;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import net.Communication;
import profile.Profile;
import ui.Theme;
import ui.component.BrandHeader;
import ui.component.GAvatar;
import ui.component.GButton;
import ui.component.GCard;
import ui.component.GChip;
import ui.component.GToast;
import ui.component.StatusBar;
import ui.design.Tokens;

public class LobbyScene implements Scene {
    @SuppressWarnings("unused")
    private final Router router;
    @SuppressWarnings("unused")
    private final Communication comm;
    private final JPanel rootPanel;
    private final DefaultListModel<String> userListModel = new DefaultListModel<>();
    private JLabel profileText;
    private GChip rankChip;
    private StatusBar statusBar;

    public LobbyScene(Router router, Communication comm) {
        this.router = router;
        this.comm = comm;
        this.rootPanel = build();
    }

    private JPanel build() {
        Tokens t = Theme.tokens();
        JPanel root = new JPanel(new BorderLayout(t.space4, t.space4));
        root.setBackground(t.bgPrimary);
        root.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        root.add(buildNorth(), BorderLayout.NORTH);
        root.add(buildCenter(), BorderLayout.CENTER);
        root.add(buildEast(), BorderLayout.EAST);
        statusBar = new StatusBar();
        statusBar.setOffline("未连接");
        root.add(statusBar, BorderLayout.SOUTH);
        return root;
    }

    private JPanel buildNorth() {
        Tokens t = Theme.tokens();
        JPanel north = new JPanel(new BorderLayout(0, t.space3));
        north.setOpaque(false);

        BrandHeader header = new BrandHeader();
        header.setCurrentUser(Profile.get().nickname, null);
        north.add(header, BorderLayout.NORTH);

        GCard profileCard = new GCard(true);
        profileCard.setLayout(new BorderLayout(t.space3, 0));

        GAvatar avatar = new GAvatar(56, false);
        profileCard.add(avatar, BorderLayout.WEST);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        JPanel firstRow = new JPanel(new FlowLayout(FlowLayout.LEFT, t.space2, 0));
        firstRow.setOpaque(false);
        JLabel nick = new JLabel(Profile.get().nickname);
        nick.setFont(t.fontHeading);
        nick.setForeground(t.fgDefault);
        rankChip = new GChip(Profile.get().levelTitle(), t.accent, t.fgOnAccent);
        firstRow.add(nick);
        firstRow.add(rankChip);

        profileText = new JLabel(buildProfileText());
        profileText.setFont(t.fontBody);
        profileText.setForeground(t.fgMuted);
        profileText.setAlignmentX(Component.LEFT_ALIGNMENT);

        center.add(firstRow);
        center.add(Box.createVerticalStrut(t.space1));
        center.add(profileText);
        profileCard.add(center, BorderLayout.CENTER);

        GButton change = GButton.secondary("更换头像");
        change.addActionListener(e -> GToast.info(rootPanel, "更换头像：功能后续 Phase 实现"));
        JPanel east = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        east.setOpaque(false);
        east.add(change);
        profileCard.add(east, BorderLayout.EAST);

        JPanel padded = new JPanel(new BorderLayout());
        padded.setOpaque(false);
        padded.setBorder(BorderFactory.createEmptyBorder(0, t.space5, 0, t.space5));
        padded.add(profileCard, BorderLayout.CENTER);
        north.add(padded, BorderLayout.CENTER);
        return north;
    }

    private String buildProfileText() {
        Profile p = Profile.get();
        int t = p.totalGames();
        int wr = p.winRate();
        return "对局 " + t + " 次 / 胜率 " + wr + "%";
    }

    private JPanel buildCenter() {
        Tokens t = Theme.tokens();
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setBorder(BorderFactory.createEmptyBorder(0, t.space5, 0, 0));

        JList<String> list = new JList<>(userListModel);
        list.setCellRenderer(new UserCellRenderer());
        list.setBackground(t.bgCard);
        list.setBorder(BorderFactory.createEmptyBorder(t.space2, t.space2, t.space2, t.space2));
        JScrollPane sp = new JScrollPane(list);
        sp.setBorder(BorderFactory.createLineBorder(t.borderLight));
        sp.getViewport().setBackground(t.bgCard);
        wrap.add(sp, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel buildEast() {
        Tokens t = Theme.tokens();
        GCard card = new GCard(true);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        String[] labels = {"邀请选中", "聊天", "人机对战", "云端棋谱", "本地棋谱", "退出登录"};
        for (int i = 0; i < labels.length; i++) {
            String label = labels[i];
            GButton b = (i == 0) ? GButton.primary(label) : GButton.secondary(label);
            b.setAlignmentX(Component.CENTER_ALIGNMENT);
            b.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 40));
            if ("本地棋谱".equals(label)) {
                b.addActionListener(e -> router.push(new LocalManualScene(router)));
            } else {
                b.addActionListener(e -> GToast.info(rootPanel, label + "：功能后续 Phase 实现"));
            }
            card.add(b);
            if (i < labels.length - 1) card.add(Box.createVerticalStrut(t.space2));
        }

        JPanel padded = new JPanel(new BorderLayout());
        padded.setOpaque(false);
        padded.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, t.space5));
        padded.add(card, BorderLayout.NORTH);
        padded.setPreferredSize(new java.awt.Dimension(220, 10));
        return padded;
    }

    @Override public JComponent root() { return rootPanel; }
    @Override public String title() { return "大厅"; }
    @Override public void onEnter() {
        if (profileText != null) profileText.setText(buildProfileText());
        if (rankChip != null) rankChip.setText(Profile.get().levelTitle());
    }

    private static class UserCellRenderer extends JPanel implements ListCellRenderer<String> {
        private final GAvatar avatar = new GAvatar(28, false);
        private final JLabel nameLabel = new JLabel();
        private final GChip rank;
        UserCellRenderer() {
            super(new FlowLayout(FlowLayout.LEFT, Theme.tokens().space2, 2));
            Tokens t = Theme.tokens();
            setOpaque(true);
            nameLabel.setFont(t.fontBody);
            rank = new GChip("新手", t.accent, t.fgOnAccent);
            add(avatar);
            add(nameLabel);
            add(rank);
        }
        @Override
        public Component getListCellRendererComponent(JList<? extends String> list, String value,
                                                       int index, boolean isSelected, boolean cellHasFocus) {
            Tokens t = Theme.tokens();
            String name = value == null ? "" : value;
            String rankText = "新手";
            int colon = name.indexOf(':');
            if (colon > 0) {
                rankText = name.substring(colon + 1).trim();
                name = name.substring(0, colon).trim();
            }
            nameLabel.setText(name);
            rank.setText(rankText);
            Color bg = isSelected ? new Color(t.accent.getRed(), t.accent.getGreen(), t.accent.getBlue(), 40) : t.bgCard;
            setBackground(bg);
            nameLabel.setForeground(t.fgDefault);
            return this;
        }
    }
}
