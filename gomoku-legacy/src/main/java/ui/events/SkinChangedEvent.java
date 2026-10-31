package ui.events;

import ui.Event;
import ui.design.Skin;

public final class SkinChangedEvent implements Event {
    public final Skin newSkin;
    public SkinChangedEvent(Skin newSkin) { this.newSkin = newSkin; }
}
