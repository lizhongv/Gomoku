package ui.scene;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import game.Game;
import game.Manual;
import net.AiGameSession;
import net.Communication;
import net.GameClient;
import net.OnlineGameSession;
import profile.Profile;
import ui.scene.dialog.CloudManualsDialog;
import ui.Theme;
import ui.component.BrandHeader;
import ui.component.ChatPanel;
import ui.component.GAvatar;
import ui.component.GButton;
import ui.component.GCard;
import ui.component.GChip;
import ui.component.GDialog;
import ui.component.GTextField;
import ui.component.GToast;
import ui.component.StatusBar;
import ui.design.Tokens;

public class LobbyScene implements Scene, GameClient {
    private final Router router;
    private final Communication comm;
    private final JPanel rootPanel;
    private final DefaultListModel<String> userListModel = new DefaultListModel<>();
    private JList<String> userList;
    private JLabel profileText;
    private GChip rankChip;
    private StatusBar statusBar;
    private ChatPanel chatPanel;
    private CloudManualsDialog cloudDialog;

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
        root.add(buildSouth(), BorderLayout.SOUTH);
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

        userList = new JList<>(userListModel);
        userList.setCellRenderer(new UserCellRenderer());
        userList.setBackground(t.bgCard);
        userList.setBorder(BorderFactory.createEmptyBorder(t.space2, t.space2, t.space2, t.space2));
        userList.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) inviteSelected();
            }
        });
        JScrollPane sp = new JScrollPane(userList);
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
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            switch (label) {
                case "本地棋谱":
                    b.addActionListener(e -> router.push(new LocalManualScene(router)));
                    break;
                case "邀请选中":
                    b.addActionListener(e -> inviteSelected());
                    break;
                case "聊天":
                    b.addActionListener(e -> doChat());
                    break;
                case "退出登录":
                    b.addActionListener(e -> doLogout());
                    break;
                case "人机对战":
                    b.addActionListener(e -> startAiGame());
                    break;
                case "云端棋谱":
                    b.addActionListener(e -> openCloudManuals());
                    break;
                default:
                    b.addActionListener(e -> GToast.info(rootPanel, label + "：功能后续 Phase 实现"));
            }
            card.add(b);
            if (i < labels.length - 1) card.add(Box.createVerticalStrut(t.space2));
        }

        JPanel padded = new JPanel(new BorderLayout());
        padded.setOpaque(false);
        padded.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, t.space5));
        padded.add(card, BorderLayout.NORTH);
        padded.setPreferredSize(new Dimension(220, 10));
        return padded;
    }

    private JPanel buildSouth() {
        Tokens t = Theme.tokens();
        JPanel south = new JPanel(new BorderLayout(0, t.space2));
        south.setOpaque(false);
        south.setBorder(BorderFactory.createEmptyBorder(0, t.space5, 0, t.space5));

        chatPanel = new ChatPanel();
        chatPanel.setPreferredSize(new Dimension(10, 100));
        south.add(chatPanel, BorderLayout.CENTER);

        statusBar = new StatusBar();
        statusBar.setOffline("未连接");
        south.add(statusBar, BorderLayout.SOUTH);
        return south;
    }

    private void inviteSelected() {
        String target = userList == null ? null : userList.getSelectedValue();
        if (target == null) { GToast.info(rootPanel, "请先选择玩家"); return; }
        String name = stripStateSuffix(target);
        if (name.equals(comm.getMyName())) { GToast.info(rootPanel, "不能邀请自己"); return; }
        showInviteDialog(name);
    }

    private void showInviteDialog(String target) {
        Window owner = SwingUtilities.getWindowAncestor(rootPanel);
        GDialog d = new GDialog(owner, "邀请对局", true);
        Tokens t = Theme.tokens();
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, t.space2, t.space2));
        panel.setOpaque(false);
        panel.add(new JLabel("用时（秒）："));
        GTextField timeField = new GTextField(8);
        timeField.setText("300");
        panel.add(timeField);
        d.getContentPane().add(panel, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, t.space2, 0));
        buttons.setOpaque(false);
        GButton ok = GButton.primary("邀请");
        ok.addActionListener(e -> {
            String txt = timeField.getText().trim();
            try {
                int sec = Integer.parseInt(txt);
                if (sec < 30 || sec > 3600) throw new NumberFormatException();
                comm.join(target, String.valueOf(sec));
                GToast.info(rootPanel, "已发送邀请给 " + target);
                d.dispose();
            } catch (NumberFormatException ex) {
                GToast.error(d, "请输入 30-3600 之间的整数");
            }
        });
        GButton cancel = GButton.ghost("取消");
        cancel.addActionListener(e -> d.dispose());
        buttons.add(ok);
        buttons.add(cancel);
        d.getContentPane().add(buttons, BorderLayout.SOUTH);
        d.setSize(new Dimension(360, 200));
        d.setLocationRelativeTo(rootPanel);
        d.setVisible(true);
    }

    private void doChat() {
        String text = JOptionPane.showInputDialog(rootPanel, "请输入消息：", "聊天",
                JOptionPane.PLAIN_MESSAGE);
        if (text == null) return;
        text = text.trim();
        if (text.isEmpty()) return;
        comm.talk(text);
        chatPanel.appendChat(comm.getMyName() == null ? Profile.get().nickname : comm.getMyName(),
                text, true);
    }

    private void doLogout() {
        try { comm.disConnect(); } catch (Exception ignored) {}
        router.pop();
    }

    private void startAiGame() {
        Object[] difficulties = {"简单 (深度 1)", "标准 (深度 3)", "困难 (深度 5)", "取消"};
        int choice = JOptionPane.showOptionDialog(
                SwingUtilities.getWindowAncestor(rootPanel),
                "请选择 AI 难度（你执黑先手）",
                "人机对战",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, difficulties, difficulties[1]);
        if (choice < 0 || choice == 3) return;
        int[] depths  = {1, 3, 5};
        int[] samples = {5, 10, 15};
        AiGameSession session = new AiGameSession("black", depths[choice], samples[choice]);
        router.push(new GameScene(router, session));
    }

    private void openCloudManuals() {
        Window owner = SwingUtilities.getWindowAncestor(rootPanel);
        cloudDialog = new CloudManualsDialog(owner, g -> {
            if (g != null) comm.getManual(g.getFileName());
        });
        comm.getGames(comm.getMyName());
        cloudDialog.setVisible(true);
    }

    private static String stripStateSuffix(String name) {
        if (name == null) return "";
        int idx = name.indexOf(':');
        return (idx > 0) ? name.substring(0, idx).trim() : name.trim();
    }

    private void runOnEdt(Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) r.run();
        else SwingUtilities.invokeLater(r);
    }

    private void replaceUserListItem(String name, String state) {
        for (int i = 0; i < userListModel.size(); i++) {
            String item = userListModel.get(i);
            if (stripStateSuffix(item).equals(name)) {
                userListModel.set(i, name + ": " + (state == null ? "" : state));
                return;
            }
        }
        userListModel.addElement(name + ": " + (state == null ? "" : state));
    }

    @Override public JComponent root() { return rootPanel; }
    @Override public String title() { return "大厅"; }

    @Override public void onEnter() {
        if (profileText != null) profileText.setText(buildProfileText());
        if (rankChip != null) rankChip.setText(Profile.get().levelTitle());
        if (comm != null) {
            comm.setClient(this);
            String me = comm.getMyName();
            if (me != null) statusBar.setOnline("已连接 - " + me);
            else statusBar.setOnline("已连接");
        }
    }

    // ====== GameClient 回调（在 ReceiveThread 触发，全部 invokeLater 到 EDT） ======

    @Override public void onLoggedIn(String myName) {
        runOnEdt(() -> statusBar.setOnline("已连接 - " + myName));
    }

    @Override public void onUserList(String name, String state) {
        runOnEdt(() -> replaceUserListItem(name, state == null ? "新手" : state));
    }

    @Override public void onUserStateChange(String name, String state) {
        runOnEdt(() -> replaceUserListItem(name, state == null ? "新手" : state));
    }

    @Override public void onUserLeft(String name) {
        runOnEdt(() -> {
            for (int i = 0; i < userListModel.size(); i++) {
                if (stripStateSuffix(userListModel.get(i)).equals(name)) {
                    userListModel.remove(i);
                    return;
                }
            }
        });
    }

    @Override public void onInvite(String fromName, int seconds) {
        runOnEdt(() -> showInvitePrompt(fromName, seconds));
    }

    private void showInvitePrompt(String fromName, int seconds) {
        Window owner = SwingUtilities.getWindowAncestor(rootPanel);
        Object[] opts = {"接受", "拒绝"};
        int choice = JOptionPane.showOptionDialog(owner,
                "玩家 " + fromName + " 邀请对局，用时 " + seconds + " 秒",
                "对局邀请", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, opts, opts[0]);
        if (choice == 0) comm.agree(fromName);
        else comm.refuse(fromName);
    }

    @Override public void onInviteRefused(String fromName) {
        runOnEdt(() -> GToast.error(rootPanel, "对方拒绝了邀请：" + fromName));
    }

    @Override public void onColor(String color, String oppName) {
        runOnEdt(() -> {
            OnlineGameSession session = new OnlineGameSession(comm, oppName, 300);
            session.onColor(color, oppName);
            router.push(new GameScene(router, session));
        });
    }

    @Override public void onOpponentMove(int col, int row) { /* 已交给 GameScene */ }
    @Override public void onResult(boolean youWon) { /* 已交给 GameScene */ }

    @Override public void onChat(String sender, String text) {
        runOnEdt(() -> chatPanel.appendChat(sender == null ? "对手" : sender,
                text == null ? "" : text,
                sender != null && sender.equals(comm.getMyName())));
    }

    @Override public void onGameList(ArrayList<Game> games) {
        runOnEdt(() -> {
            if (cloudDialog != null) cloudDialog.setData(games);
        });
    }

    @Override public void onManual(Manual manual) {
        runOnEdt(() -> {
            if (cloudDialog != null) cloudDialog.dispose();
            cloudDialog = null;
            if (manual == null) { GToast.error(rootPanel, "棋谱获取失败"); return; }
            router.push(new ReviewScene(router, manual));
        });
    }

    @Override public void onDisconnected() {
        runOnEdt(() -> {
            statusBar.setError("连接已断开");
            GToast.error(rootPanel, "连接已断开");
        });
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
