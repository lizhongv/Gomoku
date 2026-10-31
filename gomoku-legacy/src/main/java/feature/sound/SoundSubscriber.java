package feature.sound;

import ui.EventBus;
import ui.events.GameEndEvent;
import ui.events.MoveEvent;
import ui.events.RankUpEvent;

public final class SoundSubscriber {
    private SoundSubscriber() {}

    public static void install() {
        EventBus.subscribe(MoveEvent.class, e -> SoundManager.play(SoundId.PLACE));
        EventBus.subscribe(GameEndEvent.class,
                e -> SoundManager.play(e.youWon ? SoundId.WIN : SoundId.LOSE));
        EventBus.subscribe(RankUpEvent.class, e -> SoundManager.play(SoundId.WIN));
    }
}
