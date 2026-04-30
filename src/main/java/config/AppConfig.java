// 全局配置：单例，启动时加载一次
// 加载顺序：环境变量 > ./config.properties > ~/.gomoku/config.properties > 内置默认
package config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public final class AppConfig {
	private static final AppConfig INSTANCE = new AppConfig();

	public static AppConfig get() { return INSTANCE; }

	private final Properties props = new Properties();

	private AppConfig() {
		// 1. 工作目录（开发场景）
		tryLoad(new File("config.properties"));
		// 2. 用户目录（jpackage 安装后的常驻位置）
		String home = System.getProperty("user.home");
		if (home != null) {
			tryLoad(new File(home, ".gomoku/config.properties"));
		}
	}

	private void tryLoad(File f) {
		if (!f.isFile()) return;
		try (FileInputStream in = new FileInputStream(f)) {
			Properties p = new Properties();
			p.load(in);
			// 工作目录优先：只填那些当前还没设的 key
			for (String k : p.stringPropertyNames()) {
				if (!props.containsKey(k)) props.setProperty(k, p.getProperty(k));
			}
		} catch (IOException ignored) {}
	}

	private String resolve(String envKey, String propKey, String fallback) {
		if (envKey != null) {
			String v = System.getenv(envKey);
			if (v != null && !v.isEmpty()) return v;
		}
		if (propKey != null) {
			String v = props.getProperty(propKey);
			if (v != null && !v.isEmpty()) return v;
		}
		return fallback;
	}

	public String getString(String envKey, String propKey, String fallback) {
		return resolve(envKey, propKey, fallback);
	}

	public int getInt(String envKey, String propKey, int fallback) {
		try {
			String v = resolve(envKey, propKey, null);
			return v == null ? fallback : Integer.parseInt(v.trim());
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	public boolean getBool(String envKey, String propKey, boolean fallback) {
		String v = resolve(envKey, propKey, null);
		return v == null ? fallback : Boolean.parseBoolean(v.trim());
	}

	// === 各模块配置访问器（集中所有可调键） ===

	// 数据库
	public String dbUrl()      { return getString("FIVE_DB_URL", "db.url", "jdbc:mysql://localhost:3306/fiveChess"); }
	public String dbUser()     { return getString("FIVE_DB_USER", "db.user", "root"); }
	public String dbPassword() { return getString("FIVE_DB_PASSWORD", "db.password", "12345678"); }

	// 网络
	public int    serverPort()  { return getInt(   "FIVE_SERVER_PORT", "server.port", 4801); }
	public String serverHost()  { return getString("FIVE_SERVER_HOST", "server.host", "127.0.0.1"); }
	public boolean tlsEnabled() { return getBool(  "FIVE_TLS",         "tls.enabled", false); }

	// 棋盘
	public int boardRows() { return getInt(null, "game.boardRows", 14); } // 0..14 → 15 路
	public int boardCols() { return getInt(null, "game.boardCols", 14); }
	public int defaultGameTimeSec()    { return getInt(null, "game.defaultTimeSec", 300); }
	public int inviteTimeoutSec()      { return getInt(null, "game.inviteTimeoutSec", 100); }

	// AI
	public int aiSearchDepth()    { return getInt(null, "ai.depth",        3); }
	public int aiSampleNumber()   { return getInt(null, "ai.samples",     10); }
	public int aiMinThinkMs()     { return getInt(null, "ai.thinkDelayMs", 300); }

	// UI
	public String uiTheme() { return getString(null, "ui.theme", "light"); } // light / dark
	public boolean uiSound() { return getBool(null, "ui.sound", true); }

	// 资源/数据目录
	public File userDataDir() {
		String home = System.getProperty("user.home");
		File dir = new File(home == null ? "." : home, ".gomoku");
		if (!dir.exists()) dir.mkdirs();
		return dir;
	}
	public File avatarDir() {
		File d = new File(userDataDir(), "avatars");
		if (!d.exists()) d.mkdirs();
		return d;
	}
	public File manualDir() {
		File d = new File(userDataDir(), "manuals");
		if (!d.exists()) d.mkdirs();
		return d;
	}

	/** 写回某个键到用户目录的 config.properties（用于"主题切换"等运行时持久化）。 */
	public synchronized void persist(String key, String value) {
		props.setProperty(key, value);
		File f = new File(userDataDir(), "config.properties");
		Properties existing = new Properties();
		if (f.isFile()) {
			try (FileInputStream in = new FileInputStream(f)) { existing.load(in); }
			catch (IOException ignored) {}
		}
		existing.setProperty(key, value);
		try (java.io.FileOutputStream out = new java.io.FileOutputStream(f)) {
			existing.store(out, "Gomoku user config");
		} catch (IOException ignored) {}
	}
}
