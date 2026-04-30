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
import javax.swing.SwingUtilities;
import feature.emote.Emote;
import feature.emote.EmoteParser;
import game.Manual;
import game.ManualDao;
import net.GameSession;
import profile.Profile;
import ui.EventBus;
import ui.Theme;
import ui.events.GameEndEvent;
import ui.events.GameStartEvent;
import ui.events.MoveEvent;
import ui.component.BrandHeader;
import ui.component.ChatPanel;
import ui.component.EmoteBar;
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
    @SuppressWarnings("unused")
    private Manual completedManual;
    private javax.swing.Timer countdown;
    private int meSeconds;
    private int opSeconds;
    private boolean myTurn;

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
            EventBus.publish(new MoveEvent(c, r, true));
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
                String trimmed = text.trim();
                session.sendChat(trimmed);
                chat.appendChat(Profile.get().nickname, trimmed, true);
                GToast.info(rootPanel, "消息已发送");
            }
        });
        side.add(chatBtn);
        side.add(Box.createVerticalStrut(t.space2));

        GButton emojiBtn = GButton.secondary("表情");
        emojiBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        emojiBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        emojiBtn.addActionListener(e -> showEmotePicker());
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
        side.add(Box.createVerticalGlue());

        final JPanel sideWrap = new JPanel(new BorderLayout());
        sideWrap.setOpaque(false);
        sideWrap.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, t.space5));
        sideWrap.setMinimumSize(new Dimension(180, 0));
        sideWrap.setPreferredSize(new Dimension(240, 10));
        sideWrap.add(side, BorderLayout.CENTER);
        rootPanel.add(sideWrap, BorderLayout.EAST);

        chat = new ChatPanel();
        chat.setMinimumSize(new Dimension(0, 80));
        chat.setPreferredSize(new Dimension(10, 130));

        rootPanel.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) {
                int w = rootPanel.getWidth();
                int h = rootPanel.getHeight();
                int sideW = Math.max(180, Math.min(320, (int) (w * 0.22)));
                sideWrap.setPreferredSize(new Dimension(sideW, 10));
                int chatH = Math.max(80, Math.min(180, (int) (h * 0.18)));
                chat.setPreferredSize(new Dimension(10, chatH));
                rootPanel.revalidate();
            }
        });
        JPanel chatWrap = new JPanel(new BorderLayout());
        chatWrap.setOpaque(false);
        chatWrap.setBorder(BorderFactory.createEmptyBorder(0, t.space5, t.space3, t.space5));
        chatWrap.add(chat, BorderLayout.CENTER);
        rootPanel.add(chatWrap, BorderLayout.SOUTH);

        session.subscribe(this::onSessionEvent);
    }

    private static String safe(String s) { return (s == null || s.isEmpty()) ? "对手" : s; }

    private void showEmotePicker() {
        java.awt.Window owner = SwingUtilities.getWindowAncestor(rootPanel);
        GDialog d = new GDialog(owner, "选择表情", false);
        EmoteBar bar = new EmoteBar(emote -> sendEmote(emote, d));
        d.getContentPane().setLayout(new BorderLayout());
        d.getContentPane().add(bar, BorderLayout.CENTER);
        d.setSize(new Dimension(420, 200));
        d.setLocationRelativeTo(rootPanel);
        d.setVisible(true);
    }

    private void sendEmote(Emote emote, GDialog d) {
        if (emote == null) return;
        String wrapped = EmoteParser.wrap(emote);
        session.sendChat(wrapped);
        chat.appendChat(Profile.get().nickname, wrapped, true);
        d.dispose();
    }

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
                myTurn = true;
                ensureCountdown();
                break;
            case OPP_MOVE:
                resolveColor();
                board.placeChess(ev.col, ev.row, !iAmBlack);
                board.setInteractive(false);
                turnChip.setText("对手回合");
                meCard.setActive(false);
                opCard.setActive(true);
                myTurn = false;
                ensureCountdown();
                EventBus.publish(new MoveEvent(ev.col, ev.row, false));
                break;
            case RESULT_WIN:
                ended = true;
                stopCountdown();
                board.setInteractive(false);
                turnChip.setText("您赢了");
                GToast.success(rootPanel, "您赢了！");
                handleResult(true, "胜利", "恭喜您赢了！");
                break;
            case RESULT_LOSE:
                ended = true;
                stopCountdown();
                board.setInteractive(false);
                turnChip.setText("您输了");
                GToast.error(rootPanel, "您输了");
                handleResult(false, "失败", "您输了，再接再厉。");
                break;
            case OPP_LEFT:
                ended = true;
                stopCountdown();
                board.setInteractive(false);
                turnChip.setText("对手已离开");
                GToast.error(rootPanel, "对手已离开");
                showResultWithReview("提示", "对手已离开。", null);
                break;
            case CHAT:
                chat.appendChat(ev.chatSender == null ? "对手" : ev.chatSender,
                                ev.chatText == null ? "" : ev.chatText, false);
                break;
            default:
                break;
        }
    }

    private void ensureCountdown() {
        if (countdown != null) return;
        meSeconds = session.initialTimeSeconds();
        opSeconds = session.initialTimeSeconds();
        meCard.setTimer(meSeconds);
        opCard.setTimer(opSeconds);
        countdown = new javax.swing.Timer(1000, e -> tickCountdown());
        countdown.start();
    }

    private void stopCountdown() {
        if (countdown != null) {
            countdown.stop();
            countdown = null;
        }
    }

    private void tickCountdown() {
        if (ended) { stopCountdown(); return; }
        if (myTurn) {
            meSeconds = Math.max(0, meSeconds - 1);
            meCard.setTimer(meSeconds);
            if (meSeconds == 0) {
                ended = true;
                stopCountdown();
                board.setInteractive(false);
                GToast.error(rootPanel, "时间到，自动认输");
                session.giveup();
            }
        } else {
            opSeconds = Math.max(0, opSeconds - 1);
            opCard.setTimer(opSeconds);
        }
    }

    private void handleResult(boolean youWon, String title, String msg) {
        Manual manual = session.buildManual();
        completedManual = manual;
        if (manual != null) {
            try { new ManualDao().addManual(manual); } catch (Exception ignored) {}
        }
        GameStartEvent.Mode mode = (session instanceof net.AiGameSession)
                ? GameStartEvent.Mode.AI : GameStartEvent.Mode.ONLINE;
        int aiElo = 0;
        if (session instanceof net.AiGameSession) {
            int d = ((net.AiGameSession) session).aiDepth();
            aiElo = (d == 1) ? 800 : (d == 5 ? 1600 : 1200);
        }
        EventBus.publish(new GameEndEvent(youWon, manual, mode, aiElo));
        showResultWithReview(title, msg, manual);
    }

    private void showResultWithReview(String title, String msg, Manual manual) {
        final GDialog d = new GDialog(null, title, true);
        d.getContentPane().setLayout(new BorderLayout());
        javax.swing.JLabel lbl = new javax.swing.JLabel(msg, javax.swing.SwingConstants.CENTER);
        lbl.setFont(Theme.tokens().fontBody);
        lbl.setForeground(Theme.tokens().fgDefault);
        d.getContentPane().add(lbl, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, Theme.tokens().space2, 0));
        buttons.setOpaque(false);
        if (manual != null) {
            GButton review = GButton.primary("查看复盘");
            review.addActionListener(e -> {
                d.dispose();
                router.replace(new ReviewScene(router, manual));
            });
            buttons.add(review);
        }
        GButton back = GButton.ghost("返回大厅");
        back.addActionListener(e -> {
            d.dispose();
            router.pop();
        });
        buttons.add(back);
        d.getContentPane().add(buttons, BorderLayout.SOUTH);
        d.setSize(new Dimension(380, 200));
        d.setLocationRelativeTo(rootPanel);
        d.setVisible(true);
    }

    @Override public JComponent root() { return rootPanel; }
    @Override public String title() { return "对局"; }
    @Override public void onEnter() {
        GameStartEvent.Mode mode = (session instanceof net.AiGameSession)
                ? GameStartEvent.Mode.AI : GameStartEvent.Mode.ONLINE;
        EventBus.publish(new GameStartEvent(mode, null, session.opponentName()));
        session.start();
    }
    @Override public void onLeave() {
        stopCountdown();
        if (!ended) session.leave();
    }
}
