// 本地用户档案：昵称、AI 战绩、等级、签到等
// 持久化到 ~/.gomoku/profile.properties（简单 KV）
package profile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import config.AppConfig;

public class Profile {
	public String nickname = "玩家";
	public int    aiWins = 0;
	public int    aiLosses = 0;
	public int    onlineWins = 0;
	public int    onlineLosses = 0;
	public long   firstUseMillis = 0;
	public boolean onboardingDone = false;

	// === 段位（阶段一本地 ELO）===
	public int  elo = 1200;
	public int  peakElo = 1200;
	public int  weeklyWins = 0;
	public int  weeklyLosses = 0;
	public long weekStartMillis = 0;

	// === 皮肤 / 商店 ===
	public String skinId = "classic";
	public String unlockedSkinIds = "classic";

	// === 金币 / 签到 ===
	public int  coins = 0;
	public String lastCheckinDate = "";
	public int  consecutiveCheckinDays = 0;

	public int totalGames() { return aiWins + aiLosses + onlineWins + onlineLosses; }

	public int winRate() {
		int t = aiWins + aiLosses + onlineWins + onlineLosses;
		if (t == 0) return 0;
		return (aiWins + onlineWins) * 100 / t;
	}

	/** 简单的等级算法：按总胜场数升级。 */
	public int level() {
		int wins = aiWins + onlineWins;
		if (wins < 5) return 1;
		if (wins < 15) return 2;
		if (wins < 30) return 3;
		if (wins < 60) return 4;
		if (wins < 100) return 5;
		return 6 + (wins - 100) / 50;
	}

	public String levelTitle() {
		switch (Math.min(level(), 6)) {
			case 1: return "新手";
			case 2: return "学徒";
			case 3: return "棋客";
			case 4: return "棋士";
			case 5: return "高手";
			default: return "大师";
		}
	}

	private static File file() {
		return new File(AppConfig.get().userDataDir(), "profile.properties");
	}

	public static Profile load() {
		Profile p = new Profile();
		File f = file();
		if (!f.isFile()) {
			p.firstUseMillis = System.currentTimeMillis();
			p.save();
			return p;
		}
		Properties props = new Properties();
		try (FileInputStream in = new FileInputStream(f)) {
			props.load(in);
		} catch (IOException ignored) { return p; }

		p.nickname        = props.getProperty("nickname", "玩家");
		p.aiWins          = parseInt(props, "aiWins", 0);
		p.aiLosses        = parseInt(props, "aiLosses", 0);
		p.onlineWins      = parseInt(props, "onlineWins", 0);
		p.onlineLosses    = parseInt(props, "onlineLosses", 0);
		p.firstUseMillis  = Long.parseLong(props.getProperty("firstUseMillis",
				String.valueOf(System.currentTimeMillis())));
		p.onboardingDone  = "true".equalsIgnoreCase(props.getProperty("onboardingDone", "false"));
		p.elo                    = parseInt(props, "elo", 1200);
		p.peakElo                = parseInt(props, "peakElo", 1200);
		p.weeklyWins             = parseInt(props, "weeklyWins", 0);
		p.weeklyLosses           = parseInt(props, "weeklyLosses", 0);
		p.weekStartMillis        = parseLong(props, "weekStartMillis", 0);
		p.skinId                 = props.getProperty("skinId", "classic");
		p.unlockedSkinIds        = props.getProperty("unlockedSkinIds", "classic");
		p.coins                  = parseInt(props, "coins", 0);
		p.lastCheckinDate        = props.getProperty("lastCheckinDate", "");
		p.consecutiveCheckinDays = parseInt(props, "consecutiveCheckinDays", 0);
		return p;
	}

	private static int parseInt(Properties p, String k, int dft) {
		try { return Integer.parseInt(p.getProperty(k, String.valueOf(dft))); }
		catch (NumberFormatException e) { return dft; }
	}

	private static long parseLong(Properties p, String k, long dft) {
		try { return Long.parseLong(p.getProperty(k, String.valueOf(dft))); }
		catch (NumberFormatException e) { return dft; }
	}

	public synchronized void save() {
		Properties props = new Properties();
		props.setProperty("nickname", nickname);
		props.setProperty("aiWins", String.valueOf(aiWins));
		props.setProperty("aiLosses", String.valueOf(aiLosses));
		props.setProperty("onlineWins", String.valueOf(onlineWins));
		props.setProperty("onlineLosses", String.valueOf(onlineLosses));
		props.setProperty("firstUseMillis", String.valueOf(firstUseMillis));
		props.setProperty("onboardingDone", String.valueOf(onboardingDone));
		props.setProperty("elo", String.valueOf(elo));
		props.setProperty("peakElo", String.valueOf(peakElo));
		props.setProperty("weeklyWins", String.valueOf(weeklyWins));
		props.setProperty("weeklyLosses", String.valueOf(weeklyLosses));
		props.setProperty("weekStartMillis", String.valueOf(weekStartMillis));
		props.setProperty("skinId", skinId == null ? "classic" : skinId);
		props.setProperty("unlockedSkinIds", unlockedSkinIds == null ? "classic" : unlockedSkinIds);
		props.setProperty("coins", String.valueOf(coins));
		props.setProperty("lastCheckinDate", lastCheckinDate == null ? "" : lastCheckinDate);
		props.setProperty("consecutiveCheckinDays", String.valueOf(consecutiveCheckinDays));
		try (FileOutputStream out = new FileOutputStream(file())) {
			props.store(out, "Gomoku player profile");
		} catch (IOException ignored) {}
	}

	// === 单例 ===
	private static Profile instance;
	public static synchronized Profile get() {
		if (instance == null) instance = load();
		return instance;
	}
}
