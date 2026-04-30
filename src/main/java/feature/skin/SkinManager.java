package feature.skin;

import java.util.HashSet;
import java.util.Set;
import profile.Profile;
import ui.EventBus;
import ui.Theme;
import ui.design.Skin;
import ui.events.SkinChangedEvent;

public final class SkinManager {
    private SkinManager() {}

    public static void init() {
        Profile p = Profile.get();
        Skin s = SkinPack.byId(p.skinId);
        Theme.set(s);
    }

    public static Skin current() { return Theme.currentSkin(); }

    public static void setCurrent(String skinId) {
        Skin s = SkinPack.byId(skinId);
        Theme.set(s);
        Profile p = Profile.get();
        p.skinId = skinId;
        p.save();
        EventBus.publish(new SkinChangedEvent(s));
    }

    public static boolean isUnlocked(String skinId) {
        Profile p = Profile.get();
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
