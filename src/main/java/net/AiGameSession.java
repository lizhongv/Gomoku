package net;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import ai.Evaluate;

public class AiGameSession implements GameSession {
    private static final int N = 15;
    private final List<Consumer<GameSessionEvent>> handlers = new ArrayList<>();
    private String myColor;
    private final int seconds;
    private final int[][] board = new int[N][N];
    private int chessCount;
    private boolean finished;
    private long pendingGen;
    private SwingWorker<int[], Void> currentWorker;

    public AiGameSession(String myColor, int aiDepth, int aiSamples) {
        this.myColor = (myColor != null) ? myColor : "black";
        this.seconds = 300;
    }

    @Override public String myColor() { return myColor; }
    @Override public String opponentName() { return "AI"; }
    @Override public int initialTimeSeconds() { return seconds; }

    private int myCode() { return "black".equals(myColor) ? 1 : 2; }
    private int aiCode() { return "black".equals(myColor) ? 2 : 1; }

    @Override public void start() {
        if ("white".equals(myColor)) {
            triggerAiMove();
        } else {
            emit(new GameSessionEvent(EventType.MY_TURN));
        }
    }

    @Override public void makeMove(int col, int row) {
        if (finished) return;
        if (col < 0 || col >= N || row < 0 || row >= N) return;
        if (board[col][row] != 0) return;
        board[col][row] = myCode();
        chessCount++;
        if (isWin(col, row, myCode())) {
            finished = true;
            emit(new GameSessionEvent(EventType.RESULT_WIN));
            return;
        }
        if (chessCount >= N * N) {
            finished = true;
            emit(new GameSessionEvent(EventType.RESULT_WIN));
            return;
        }
        triggerAiMove();
    }

    @Override public void giveup() {
        if (finished) return;
        finished = true;
        emit(new GameSessionEvent(EventType.RESULT_LOSE));
    }

    @Override public void claimWin() {}

    @Override public void leave() {
        finished = true;
        pendingGen++;
        if (currentWorker != null) currentWorker.cancel(true);
    }

    @Override public void subscribe(Consumer<GameSessionEvent> handler) {
        if (handler != null) handlers.add(handler);
    }

    private void emit(GameSessionEvent ev) {
        SwingUtilities.invokeLater(() -> {
            for (Consumer<GameSessionEvent> h : new ArrayList<>(handlers)) h.accept(ev);
        });
    }

    private int[][] snapshot() {
        int[][] s = new int[N][N];
        for (int i = 0; i < N; i++) System.arraycopy(board[i], 0, s[i], 0, N);
        return s;
    }

    private void triggerAiMove() {
        final int aiColor = aiCode();
        final int[][] snap = snapshot();
        final long gen = ++pendingGen;
        currentWorker = new SwingWorker<int[], Void>() {
            @Override
            protected int[] doInBackground() {
                return new Evaluate(snap, aiColor).getTheBestPosition();
            }

            @Override
            protected void done() {
                if (gen != pendingGen || finished) return;
                try {
                    int[] pos = get();
                    if (pos == null) return;
                    int col = pos[0], row = pos[1];
                    if (col < 0 || col >= N || row < 0 || row >= N) return;
                    if (board[col][row] != 0) return;
                    board[col][row] = aiColor;
                    chessCount++;
                    if (isWin(col, row, aiColor)) {
                        finished = true;
                        emit(new GameSessionEvent(EventType.OPP_MOVE, col, row, null, null));
                        emit(new GameSessionEvent(EventType.RESULT_LOSE));
                        return;
                    }
                    emit(new GameSessionEvent(EventType.OPP_MOVE, col, row, null, null));
                    emit(new GameSessionEvent(EventType.MY_TURN));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        currentWorker.execute();
    }

    private boolean isWin(int col, int row, int color) {
        return countLine(col, row, color, 1, 0) >= 5
            || countLine(col, row, color, 0, 1) >= 5
            || countLine(col, row, color, 1, 1) >= 5
            || countLine(col, row, color, 1, -1) >= 5;
    }

    private int countLine(int col, int row, int color, int dx, int dy) {
        int count = 1;
        for (int x = col + dx, y = row + dy; x >= 0 && x < N && y >= 0 && y < N; x += dx, y += dy) {
            if (board[x][y] == color) count++; else break;
        }
        for (int x = col - dx, y = row - dy; x >= 0 && x < N && y >= 0 && y < N; x -= dx, y -= dy) {
            if (board[x][y] == color) count++; else break;
        }
        return count;
    }
}
