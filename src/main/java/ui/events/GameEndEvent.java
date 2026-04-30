package ui.events;

import game.Manual;
import ui.Event;

public final class GameEndEvent implements Event {
    public final boolean youWon;
    public final Manual manual;
    public final GameStartEvent.Mode mode;
    public final int aiDifficultyElo;

    public GameEndEvent(boolean youWon, Manual manual, GameStartEvent.Mode mode, int aiDifficultyElo) {
        this.youWon = youWon;
        this.manual = manual;
        this.mode = mode;
        this.aiDifficultyElo = aiDifficultyElo;
    }
}
