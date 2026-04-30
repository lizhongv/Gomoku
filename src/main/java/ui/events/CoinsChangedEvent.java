package ui.events;

import ui.Event;

public final class CoinsChangedEvent implements Event {
    public final int newBalance, delta;
    public final String reason;

    public CoinsChangedEvent(int newBalance, int delta, String reason) {
        this.newBalance = newBalance;
        this.delta = delta;
        this.reason = reason;
    }
}
