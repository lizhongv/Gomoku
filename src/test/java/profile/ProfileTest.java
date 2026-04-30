// ProfileTest：save/load 往返 + 字段升级兼容
// 注：项目无 JUnit 依赖，使用纯 main + 断言风格（同 E2eTest）
package profile;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Properties;

public class ProfileTest {
    static int passed = 0, failed = 0;

    public static void main(String[] args) throws Exception {
        File tmp = new File(System.getProperty("java.io.tmpdir"),
                            "gomoku-test-" + System.currentTimeMillis());
        tmp.mkdirs();
        System.setProperty("user.home", tmp.getAbsolutePath());
        File old = new File(tmp, ".gomoku/profile.properties");
        if (old.exists()) old.delete();

        testRoundTrip();
        testLoadOldProfileFillsDefaults();

        System.out.println("结果: passed=" + passed + " failed=" + failed);
        System.exit(failed == 0 ? 0 : 1);
    }

    static void testRoundTrip() throws Exception {
        resetSingleton();
        Profile p = Profile.get();   // 初始化空档案
        p.nickname = "测试玩家";
        p.elo = 1450;
        p.peakElo = 1500;
        p.coins = 99;
        p.skinId = "darkwood";
        p.unlockedSkinIds = "classic,darkwood";
        p.consecutiveCheckinDays = 5;
        p.avatarPath = "/tmp/avatar.png";
        p.fontFamily = "microsoft_yahei";
        p.aiDefaultDepth = 5;
        p.inviteSeconds = 600;
        p.vipUntilMillis = 9999999999L;
        p.undoTickets = 3;
        p.hintTickets = 7;
        p.aiFastMode = true;
        p.save();

        resetSingleton();
        Profile loaded = Profile.load();
        eq("nickname round-trip", "测试玩家", loaded.nickname);
        eq("elo round-trip", "1450", String.valueOf(loaded.elo));
        eq("peakElo round-trip", "1500", String.valueOf(loaded.peakElo));
        eq("coins round-trip", "99", String.valueOf(loaded.coins));
        eq("skinId round-trip", "darkwood", loaded.skinId);
        eq("unlockedSkins round-trip", "classic,darkwood", loaded.unlockedSkinIds);
        eq("checkin days", "5", String.valueOf(loaded.consecutiveCheckinDays));
        eq("avatarPath round-trip", "/tmp/avatar.png", loaded.avatarPath);
        eq("fontFamily round-trip", "microsoft_yahei", loaded.fontFamily);
        eq("aiDefaultDepth round-trip", "5", String.valueOf(loaded.aiDefaultDepth));
        eq("inviteSeconds round-trip", "600", String.valueOf(loaded.inviteSeconds));
        eq("vipUntilMillis round-trip", "9999999999", String.valueOf(loaded.vipUntilMillis));
        eq("undoTickets round-trip", "3", String.valueOf(loaded.undoTickets));
        eq("hintTickets round-trip", "7", String.valueOf(loaded.hintTickets));
        eq("aiFastMode round-trip", "true", String.valueOf(loaded.aiFastMode));
    }

    static void testLoadOldProfileFillsDefaults() throws Exception {
        File f = new File(System.getProperty("user.home"), ".gomoku/profile.properties");
        f.getParentFile().mkdirs();
        if (f.exists()) f.delete();
        Properties oldProps = new Properties();
        oldProps.setProperty("nickname", "老用户");
        oldProps.setProperty("aiWins", "10");
        try (FileOutputStream out = new FileOutputStream(f)) {
            oldProps.store(out, "old test profile");
        }
        resetSingleton();
        Profile loaded = Profile.load();
        eq("旧档案 nickname 保留", "老用户", loaded.nickname);
        eq("旧档案 aiWins 保留", "10", String.valueOf(loaded.aiWins));
        eq("新字段默认 elo=1200", "1200", String.valueOf(loaded.elo));
        eq("新字段默认 coins=0", "0", String.valueOf(loaded.coins));
        eq("新字段默认 skinId=classic", "classic", loaded.skinId);
        eq("新字段默认 avatarPath=空", "", loaded.avatarPath);
        eq("新字段默认 fontFamily=default", "default", loaded.fontFamily);
        eq("新字段默认 aiDefaultDepth=3", "3", String.valueOf(loaded.aiDefaultDepth));
        eq("新字段默认 inviteSeconds=300", "300", String.valueOf(loaded.inviteSeconds));
        eq("新字段默认 vipUntilMillis=0", "0", String.valueOf(loaded.vipUntilMillis));
        eq("新字段默认 undoTickets=0", "0", String.valueOf(loaded.undoTickets));
        eq("新字段默认 hintTickets=0", "0", String.valueOf(loaded.hintTickets));
        eq("新字段默认 aiFastMode=false", "false", String.valueOf(loaded.aiFastMode));
    }

    static void resetSingleton() throws Exception {
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
}
