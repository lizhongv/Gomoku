package feature.shop;

import ui.EventBus;
import ui.events.GameEndEvent;
import ui.events.GameStartEvent;

public final class CoinEarnSubscriber {
    private CoinEarnSubscriber() {}

    public static void install() {
        EventBus.subscribe(GameEndEvent.class, e -> {
            if (!e.youWon) return;
            int reward;
            String reason;
            if (e.mode == GameStartEvent.Mode.ONLINE) {
                reward = 10;
                reason = "联机胜利";
            } else {
                reward = 5;
                reason = "AI 胜利";
            }
            CoinManager.add(reward, reason);
        });
    }
}
