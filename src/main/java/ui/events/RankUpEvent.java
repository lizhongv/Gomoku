package ui.events;

import ui.Event;

public final class RankUpEvent implements Event {
    public final String fromTier, toTier;

    public RankUpEvent(String fromTier, String toTier) {
        this.fromTier = fromTier;
        this.toTier = toTier;
    }
}
