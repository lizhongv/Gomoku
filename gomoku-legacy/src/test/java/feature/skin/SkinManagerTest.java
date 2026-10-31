package feature.skin;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import profile.Profile;
import ui.EventBus;
import ui.events.SkinChangedEvent;

public class SkinManagerTest {
    static int passed = 0, failed = 0;

    public static void main(String[] args) throws Exception {
        File tmp = new File(System.getProperty("java.io.tmpdir"),
                            "gomoku-skin-test-" + System.currentTimeMillis());
        tmp.mkdirs();
        System.setProperty("user.home", tmp.getAbsolutePath());

        resetProfile();
        EventBus.reset();
        testDefaultSkinIsClassic();

        resetProfile();
        EventBus.reset();
        testSetCurrentPersistsToProfile();

        resetProfile();
        EventBus.reset();
        testSetCurrentPublishesEvent();

        resetProfile();
        EventBus.reset();
        testFreeSkinAlwaysUnlocked();

        resetProfile();
        EventBus.reset();
        testPremiumSkinUnlockFlow();

        System.out.println("结果: passed=" + passed + " failed=" + failed);
        System.exit(failed == 0 ? 0 : 1);
    }

    static void testDefaultSkinIsClassic() {
        SkinManager.init();
        eq("默认皮肤是 classic", "classic", SkinManager.current().id);
    }

    static void testSetCurrentPersistsToProfile() throws Exception {
        SkinManager.init();
        SkinManager.setCurrent("darkwood");
        boolean inMem = "darkwood".equals(Profile.get().skinId);
        clearSingleton();
        Profile reloaded = Profile.load();
        boolean onDisk = "darkwood".equals(reloaded.skinId);
        ok("setCurrent 持久化到 Profile（内存+磁盘）", inMem && onDisk);
    }

    static void testSetCurrentPublishesEvent() throws Exception {
        SkinManager.init();
        AtomicReference<String> got = new AtomicReference<>(null);
        EventBus.subscribe(SkinChangedEvent.class, e -> got.set(e.newSkin.id));
        SkinManager.setCurrent("jade");
        SwingUtilities.invokeAndWait(() -> {});
        ok("收到 SkinChangedEvent newSkin.id=jade", "jade".equals(got.get()));
    }

    static void testFreeSkinAlwaysUnlocked() {
        SkinManager.init();
        Profile.get().unlockedSkinIds = "";
        Profile.get().save();
        ok("免费 classic 永远解锁", SkinManager.isUnlocked("classic"));
    }

    static void testPremiumSkinUnlockFlow() {
        SkinManager.init();
        Profile.get().unlockedSkinIds = "classic";
        Profile.get().save();
        boolean before = SkinManager.isUnlocked("jade");
        SkinManager.unlock("jade");
        boolean after = SkinManager.isUnlocked("jade");
        ok("unlock 流程：解锁前 false → 解锁后 true", !before && after);
    }

    static void resetProfile() throws Exception {
        clearSingleton();
        File pf = new File(System.getProperty("user.home"), ".gomoku/profile.properties");
        if (pf.exists()) pf.delete();
    }

    static void clearSingleton() throws Exception {
        java.lang.reflect.Field f = Profile.class.getDeclaredField("instance");
        f.setAccessible(true);
        f.set(null, null);
    }

    static void eq(String name, String expected, String actual) {
        if (expected.equals(actual)) {
            passed++;
            System.out.println("  ✓ " + name);
        } else {
            failed++;
            System.out.println("  ✗ " + name + " expected=[" + expected + "] actual=[" + actual + "]");
        }
    }

    static void ok(String name, boolean cond) {
        if (cond) { passed++; System.out.println("  ✓ " + name); }
        else      { failed++; System.out.println("  ✗ " + name); }
    }
}
