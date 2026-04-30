package feature.rank;

import profile.Profile;
import ui.EventBus;
import ui.events.GameEndEvent;
import ui.events.GameStartEvent;
import ui.events.RankUpEvent;

public final class RankSubscriber {
    private RankSubscriber() {}

    public static void install() {
        EventBus.subscribe(GameEndEvent.class, e -> {
            Profile p = Profile.get();
            Tier oldTier = Tier.of(p.elo);
            RankCalculator.GameType type;
            if (e.mode == GameStartEvent.Mode.ONLINE) type = RankCalculator.GameType.ONLINE;
            else if (e.aiDifficultyElo <= 800) type = RankCalculator.GameType.AI_EASY;
            else if (e.aiDifficultyElo >= 1600) type = RankCalculator.GameType.AI_HARD;
            else type = RankCalculator.GameType.AI_NORMAL;
            int delta = RankCalculator.calcDelta(p.elo, type, e.youWon);
            p.elo += delta;
            if (p.elo > p.peakElo) p.peakElo = p.elo;
            if (e.youWon) {
                if (e.mode == GameStartEvent.Mode.ONLINE) p.onlineWins++;
                else p.aiWins++;
                p.weeklyWins++;
            } else {
                if (e.mode == GameStartEvent.Mode.ONLINE) p.onlineLosses++;
                else p.aiLosses++;
                p.weeklyLosses++;
            }
            p.save();
            Tier newTier = Tier.of(p.elo);
            if (newTier != oldTier && p.elo > oldTier.max) {
                EventBus.publish(new RankUpEvent(oldTier.displayName, newTier.displayName));
            }
        });
    }
}
