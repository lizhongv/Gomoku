package ui.events;

import ui.Event;

public final class GameStartEvent implements Event {
    public enum Mode { ONLINE, AI }
    public final Mode mode;
    public final String myColor;
    public final String opponentName;

    public GameStartEvent(Mode mode, String myColor, String opponentName) {
        this.mode = mode;
        this.myColor = myColor;
        this.opponentName = opponentName;
    }
}
