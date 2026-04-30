package net;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;
import game.Game;
import game.Manual;

public class OnlineGameSession implements GameSession, GameClient {
    private final Communication comm;
    private final List<Consumer<GameSessionEvent>> handlers = new ArrayList<>();
    private String myColor;
    private String opponentName;
    private final int seconds;

    public OnlineGameSession(Communication comm, String opponentName, int seconds) {
        this.comm = comm;
        this.opponentName = opponentName;
        this.seconds = seconds;
        comm.setClient(this);
    }

    @Override public String myColor() { return myColor; }
    @Override public String opponentName() { return opponentName; }
    @Override public int initialTimeSeconds() { return seconds; }

    @Override public void start() {
        // 邀请已在 Lobby 阶段完成；此处仅等待 server 的 GUESSCOLOR 回调
    }

    @Override public void makeMove(int col, int row) { comm.go(col, row); }
    @Override public void giveup() { comm.giveup(); }
    @Override public void claimWin() { comm.wins(); }
    @Override public void leave() { comm.disConnect(); }

    @Override public void subscribe(Consumer<GameSessionEvent> handler) {
        if (handler != null) handlers.add(handler);
    }

    private void emit(GameSessionEvent ev) {
        SwingUtilities.invokeLater(() -> {
            for (Consumer<GameSessionEvent> h : new ArrayList<>(handlers)) h.accept(ev);
        });
    }

    @Override public void onLoggedIn(String myName) {}
    @Override public void onUserList(String name, String state) {}
    @Override public void onUserStateChange(String name, String state) {}

    @Override public void onUserLeft(String name) {
        if (name != null && name.equals(opponentName)) {
            emit(new GameSessionEvent(EventType.OPP_LEFT));
        }
    }

    @Override public void onInvite(String fromName, int seconds) {}
    @Override public void onInviteRefused(String fromName) {}

    @Override public void onColor(String color, String oppName) {
        this.myColor = color;
        if (oppName != null && !oppName.isEmpty()) this.opponentName = oppName;
        if ("black".equals(color)) {
            emit(new GameSessionEvent(EventType.MY_TURN));
        }
    }

    @Override public void onOpponentMove(int col, int row) {
        emit(new GameSessionEvent(EventType.OPP_MOVE, col, row, null, null));
        emit(new GameSessionEvent(EventType.MY_TURN));
    }

    @Override public void onResult(boolean youWon) {
        emit(new GameSessionEvent(youWon ? EventType.RESULT_WIN : EventType.RESULT_LOSE));
    }

    @Override public void onChat(String sender, String text) {
        emit(new GameSessionEvent(EventType.CHAT, 0, 0, sender, text));
    }

    @Override public void onGameList(ArrayList<Game> games) {}
    @Override public void onManual(Manual manual) {}
    @Override public void onDisconnected() {
        emit(new GameSessionEvent(EventType.OPP_LEFT));
    }
}
