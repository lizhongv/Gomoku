package feature.shop;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import profile.Profile;
import ui.EventBus;
import ui.events.CoinsChangedEvent;

public final class CoinManager {
    private CoinManager() {}

    public static int balance() {
        return Profile.get().coins;
    }

    public static void add(int amount, String reason) {
        if (amount <= 0) return;
        Profile p = Profile.get();
        p.coins += amount;
        p.save();
        EventBus.publish(new CoinsChangedEvent(p.coins, amount, reason));
    }

    public static boolean spend(int amount, String reason) {
        if (amount <= 0) return true;
        Profile p = Profile.get();
        if (p.coins < amount) return false;
        p.coins -= amount;
        p.save();
        EventBus.publish(new CoinsChangedEvent(p.coins, -amount, reason));
        return true;
    }

    public static class CheckinResult {
        public final boolean newToday;
        public final int consecutiveDays;
        public final int reward;

        public CheckinResult(boolean newToday, int consecutiveDays, int reward) {
            this.newToday = newToday;
            this.consecutiveDays = consecutiveDays;
            this.reward = reward;
        }
    }

    public static CheckinResult checkinIfDue() {
        Profile p = Profile.get();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String today = LocalDate.now().format(fmt);
        if (today.equals(p.lastCheckinDate)) {
            return new CheckinResult(false, p.consecutiveCheckinDays, 0);
        }
        String yesterday = LocalDate.now().minusDays(1).format(fmt);
        if (yesterday.equals(p.lastCheckinDate)) {
            p.consecutiveCheckinDays++;
        } else {
            p.consecutiveCheckinDays = 1;
        }
        p.lastCheckinDate = today;
        int reward;
        int days = p.consecutiveCheckinDays;
        if (days > 0 && days % 21 == 0) reward = 100;
        else if (days > 0 && days % 14 == 0) reward = 100;
        else if (days > 0 && days % 7 == 0) reward = 50;
        else reward = 10;
        p.save();
        add(reward, "签到奖励 (连续 " + days + " 天)");
        return new CheckinResult(true, days, reward);
    }
}
