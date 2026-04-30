package feature.skin;

import java.util.HashSet;
import java.util.Set;
import profile.Profile;
import ui.EventBus;
import ui.Theme;
import ui.design.Skin;
import ui.events.GameEndEvent;
import ui.events.SkinChangedEvent;

public final class SkinManager {
    private static String trialSkinId;
    private static long trialUntilMillis;
    private static javax.swing.Timer trialTimer;
    private static boolean gameEndSubscribed;

    private SkinManager() {}

    public static void init() {
        Profile p = Profile.get();
        Skin s = SkinPack.byId(p.skinId);
        Theme.set(s);
        if (!gameEndSubscribed) {
            EventBus.subscribe(GameEndEvent.class, e -> endTrial());
            gameEndSubscribed = true;
        }
    }

    public static Skin current() { return Theme.currentSkin(); }

    public static void setCurrent(String skinId) {
        endTrial();
        Skin s = SkinPack.byId(skinId);
        Theme.set(s);
        Profile p = Profile.get();
        p.skinId = skinId;
        p.save();
        EventBus.publish(new SkinChangedEvent(s));
    }

    public static boolean isUnlocked(String skinId) {
        Profile p = Profile.get();
        if (p.isVip()) return true;
        Set<String> unlocked = parse(p.unlockedSkinIds);
        Skin s = SkinPack.byId(skinId);
        if (!s.premium) return true;
        return unlocked.contains(skinId);
    }

    public static void unlock(String skinId) {
        Profile p = Profile.get();
        Set<String> set = parse(p.unlockedSkinIds);
        set.add(skinId);
        p.unlockedSkinIds = String.join(",", set);
        p.save();
    }

    public static void startTrial(String skinId, long durationMillis) {
        endTrial();
        Skin s = SkinPack.byId(skinId);
        trialSkinId = skinId;
        trialUntilMillis = System.currentTimeMillis() + durationMillis;
        Theme.set(s);
        EventBus.publish(new SkinChangedEvent(s));
        trialTimer = new javax.swing.Timer(60_000, e -> {
            if (System.currentTimeMillis() >= trialUntilMillis) endTrial();
        });
        trialTimer.start();
    }

    public static void endTrial() {
        if (trialSkinId == null) return;
        trialSkinId = null;
        trialUntilMillis = 0;
        if (trialTimer != null) {
            trialTimer.stop();
            trialTimer = null;
        }
        Profile p = Profile.get();
        Skin restore = SkinPack.byId(p.skinId);
        if (!restore.id.equals(Theme.currentSkin().id)) {
            Theme.set(restore);
            EventBus.publish(new SkinChangedEvent(restore));
        }
    }

    public static boolean isInTrial() { return trialSkinId != null; }
    public static long trialRemainingMillis() {
        if (trialSkinId == null) return 0;
        long r = trialUntilMillis - System.currentTimeMillis();
        return r < 0 ? 0 : r;
    }

    private static Set<String> parse(String csv) {
        Set<String> out = new HashSet<>();
        if (csv == null) return out;
        for (String s : csv.split(",")) {
            String t = s.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }
}
