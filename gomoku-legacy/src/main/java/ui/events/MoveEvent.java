package ui.events;

import ui.Event;

public final class MoveEvent implements Event {
    public final int col, row;
    public final boolean self;

    public MoveEvent(int col, int row, boolean self) {
        this.col = col;
        this.row = row;
        this.self = self;
    }
}
