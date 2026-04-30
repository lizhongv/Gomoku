package ui.scene;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import net.GameSession;
import profile.Profile;
import ui.Theme;
import ui.component.BrandHeader;
import ui.component.ChatPanel;
import ui.component.GBoard;
import ui.component.GButton;
import ui.component.GCard;
import ui.component.GChip;
import ui.component.GDialog;
import ui.component.GToast;
import ui.component.PlayerCard;
import ui.design.Tokens;

public class GameScene implements Scene {
    private final Router router;
    private final GameSession session;
    private final JPanel rootPanel;
    private final GBoard board;
    private ChatPanel chat;
    private GChip turnChip;
    private PlayerCard meCard;
    private PlayerCard opCard;
    private boolean iAmBlack;
    private boolean colorKnown;
    private boolean ended;

    public GameScene(Router router, GameSession session) {
        this.router = router;
        this.session = session;

        Tokens t = Theme.tokens();
        rootPanel = new JPanel(new BorderLayout(t.space3, t.space3));
        rootPanel.setBackground(t.bgPrimary);
        rootPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        BrandHeader header = new BrandHeader();
        header.setCurrentUser(Profile.get().nickname, null);
        rootPanel.add(header, BorderLayout.NORTH);

        board = new GBoard();
        board.setInteractive(false);
        board.onClick((c, r) -> {
            if (ended) return;
            if (!colorKnown) return;
            session.makeMove(c, r);
            board.placeChess(c, r, iAmBlack);
            board.setInteractive(false);
            turnChip.setText("对手回合");
        });
        JPanel boardWrap = new JPanel(new BorderLayout());
        boardWrap.setOpaque(false);
        boardWrap.setBorder(BorderFactory.createEmptyBorder(0, t.space5, 0, 0));
        boardWrap.add(board, BorderLayout.CENTER);
        rootPanel.add(boardWrap, BorderLayout.CENTER);

        opCard = new PlayerCard(safe(session.opponentName()), false, "对手");
        meCard = new PlayerCard(Profile.get().nickname, true, Profile.get().levelTitle());
        meCard.setActive(true);
        turnChip = new GChip("等待开始", t.accent, t.fgOnAccent);
        turnChip.setAlignmentX(Component.CENTER_ALIGNMENT);

        GCard side = new GCard(true);
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        opCard.setAlignmentX(Component.CENTER_ALIGNMENT);
        meCard.setAlignmentX(Component.CENTER_ALIGNMENT);
        side.add(opCard);
        side.add(Box.createVerticalStrut(t.space2));
        side.add(meCard);
        side.add(Box.createVerticalStrut(t.space3));
        JPanel chipWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        chipWrap.setOpaque(false);
        chipWrap.add(turnChip);
        chipWrap.setAlignmentX(Component.CENTER_ALIGNMENT);
        side.add(chipWrap);
        side.add(Box.createVerticalStrut(t.space3));

        GButton chatBtn = GButton.secondary("发送消息");
        chatBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        chatBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        chatBtn.addActionListener(e -> {
            String text = JOptionPane.showInputDialog(rootPanel, "请输入消息：");
            if (text != null && !text.trim().isEmpty()) {
                chat.appendChat(Profile.get().nickname, text.trim(), true);
                GToast.info(rootPanel, "消息已发送");
            }
        });
        side.add(chatBtn);
        side.add(Box.createVerticalStrut(t.space2));

        GButton emojiBtn = GButton.secondary("表情");
        emojiBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        emojiBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        emojiBtn.addActionListener(e -> GToast.info(rootPanel, "表情：Phase 7 实现"));
        side.add(emojiBtn);
        side.add(Box.createVerticalStrut(t.space2));

        GButton giveupBtn = GButton.secondary("认输");
        giveupBtn.setForeground(t.danger);
        giveupBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        giveupBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        giveupBtn.addActionListener(e -> {
            if (ended) return;
            int r = confirm(rootPanel, "确定要认输吗？", "认输");
            if (r == 0) session.giveup();
        });
        side.add(giveupBtn);
        side.add(Box.createVerticalStrut(t.space2));

        GButton backBtn = GButton.ghost("返回");
        backBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        backBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        backBtn.addActionListener(e -> {
            if (!ended) session.leave();
            router.pop();
        });
        side.add(backBtn);

        JPanel sideWrap = new JPanel(new BorderLayout());
        sideWrap.setOpaque(false);
        sideWrap.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, t.space5));
        sideWrap.setPreferredSize(new Dimension(240, 10));
        sideWrap.add(side, BorderLayout.NORTH);
        rootPanel.add(sideWrap, BorderLayout.EAST);

        chat = new ChatPanel();
        chat.setPreferredSize(new Dimension(10, 130));
        JPanel chatWrap = new JPanel(new BorderLayout());
        chatWrap.setOpaque(false);
        chatWrap.setBorder(BorderFactory.createEmptyBorder(0, t.space5, t.space3, t.space5));
        chatWrap.add(chat, BorderLayout.CENTER);
        rootPanel.add(chatWrap, BorderLayout.SOUTH);

        session.subscribe(this::onSessionEvent);
    }

    private static String safe(String s) { return (s == null || s.isEmpty()) ? "对手" : s; }

    private int confirm(Component parent, String msg, String title) {
        return JOptionPane.showConfirmDialog(parent, msg, title, JOptionPane.OK_CANCEL_OPTION);
    }

    private void resolveColor() {
        if (colorKnown) return;
        String c = session.myColor();
        if (c == null) return;
        iAmBlack = "black".equals(c);
        colorKnown = true;
        meCard.setName(Profile.get().nickname);
        meCard.setIsBlack(iAmBlack);
        opCard.setIsBlack(!iAmBlack);
        opCard.setName(safe(session.opponentName()));
    }

    private void onSessionEvent(GameSession.GameSessionEvent ev) {
        if (ev == null) return;
        switch (ev.type) {
            case MY_TURN:
                resolveColor();
                if (colorKnown) {
                    board.setHoverIsBlack(iAmBlack);
                    board.setInteractive(true);
                }
                turnChip.setText("您的回合");
                meCard.setActive(true);
                opCard.setActive(false);
                break;
            case OPP_MOVE:
                resolveColor();
                board.placeChess(ev.col, ev.row, !iAmBlack);
                board.setInteractive(false);
                turnChip.setText("对手回合");
                meCard.setActive(false);
                opCard.setActive(true);
                break;
            case RESULT_WIN:
                ended = true;
                board.setInteractive(false);
                turnChip.setText("您赢了");
                Profile p = Profile.get();
                if (session instanceof net.AiGameSession) p.aiWins++; else p.onlineWins++;
                p.save();
                GToast.success(rootPanel, "您赢了！");
                showResultDialog("胜利", "恭喜您赢了！");
                break;
            case RESULT_LOSE:
                ended = true;
                board.setInteractive(false);
                turnChip.setText("您输了");
                Profile pl = Profile.get();
                if (session instanceof net.AiGameSession) pl.aiLosses++; else pl.onlineLosses++;
                pl.save();
                GToast.error(rootPanel, "您输了");
                showResultDialog("失败", "您输了，再接再厉。");
                break;
            case OPP_LEFT:
                ended = true;
                board.setInteractive(false);
                turnChip.setText("对手已离开");
                GToast.error(rootPanel, "对手已离开");
                showResultDialog("提示", "对手已离开。");
                break;
            case CHAT:
                chat.appendChat(ev.chatSender == null ? "对手" : ev.chatSender,
                                ev.chatText == null ? "" : ev.chatText, false);
                break;
            default:
                break;
        }
    }

    private void showResultDialog(String title, String msg) {
        GDialog d = new GDialog(null, title, true) {{
            content.add(new javax.swing.JLabel(msg, javax.swing.SwingConstants.CENTER), BorderLayout.CENTER);
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
            buttons.setOpaque(false);
            GButton ok = GButton.primary("返回大厅");
            ok.addActionListener(e -> dispose());
            buttons.add(ok);
            content.add(buttons, BorderLayout.SOUTH);
            setSize(new Dimension(360, 180));
            setLocationRelativeTo(rootPanel);
        }};
        d.setVisible(true);
        router.pop();
    }

    @Override public JComponent root() { return rootPanel; }
    @Override public String title() { return "对局"; }
    @Override public void onEnter() { session.start(); }
    @Override public void onLeave() { if (!ended) session.leave(); }
}
