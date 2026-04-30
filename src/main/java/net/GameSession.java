package net;

import java.util.function.Consumer;

public interface GameSession {
    String myColor();
    String opponentName();
    int    initialTimeSeconds();

    void start();
    void makeMove(int col, int row);
    void giveup();
    void claimWin();
    void leave();
    void subscribe(Consumer<GameSessionEvent> handler);

    enum EventType { MY_TURN, OPP_MOVE, RESULT_WIN, RESULT_LOSE, OPP_LEFT, CHAT }

    class GameSessionEvent {
        public final EventType type;
        public final int col, row;
        public final String chatSender, chatText;
        public GameSessionEvent(EventType t) { this(t, 0, 0, null, null); }
        public GameSessionEvent(EventType t, int col, int row, String s, String tx) {
            this.type = t; this.col = col; this.row = row;
            this.chatSender = s; this.chatText = tx;
        }
    }
}
