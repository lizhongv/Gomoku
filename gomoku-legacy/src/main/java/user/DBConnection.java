// 数据库连接工厂
// 优先级：环境变量 > 工作目录下 db.properties > 内置默认值
package user;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBConnection {
	private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/fiveChess";
	private static final String DEFAULT_USER = "root";
	private static final String DEFAULT_PASSWORD = "12345678";
	private static final Properties CONFIG = loadConfig();

	private Connection conn = null;

	public DBConnection() {
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			conn = DriverManager.getConnection(getUrl(), getUser(), getPassword());
		} catch (ClassNotFoundException e) {
			System.err.println("[DBConnection] MySQL 驱动缺失，DB 操作不可用：" + e.getMessage());
		} catch (SQLException e) {
			System.err.println("[DBConnection] 无法连接 MySQL：" + e.getMessage());
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e1) {
					// ignore
				}
			}
		}
	}

	public Connection getConn() {
		return conn;
	}

	public static String getUrl() {
		return resolve("FIVE_DB_URL", "db.url", DEFAULT_URL);
	}

	public static String getUser() {
		return resolve("FIVE_DB_USER", "db.user", DEFAULT_USER);
	}

	public static String getPassword() {
		return resolve("FIVE_DB_PASSWORD", "db.password", DEFAULT_PASSWORD);
	}

	private static String resolve(String env, String prop, String fallback) {
		String v = System.getenv(env);
		if (v != null && !v.isEmpty()) {
			return v;
		}
		v = CONFIG.getProperty(prop);
		if (v != null && !v.isEmpty()) {
			return v;
		}
		return fallback;
	}

	private static Properties loadConfig() {
		Properties p = new Properties();
		// 1. 工作目录（开发 / 脚本运行）
		if (tryLoad(p, new java.io.File("db.properties"))) return p;
		// 2. 用户目录（jpackage 安装后的常驻位置）
		String home = System.getProperty("user.home");
		if (home != null) {
			tryLoad(p, new java.io.File(home, ".gomoku/db.properties"));
		}
		return p;
	}

	private static boolean tryLoad(Properties p, java.io.File f) {
		if (!f.isFile()) return false;
		try (FileInputStream in = new FileInputStream(f)) {
			p.load(in);
			return true;
		} catch (IOException ignored) {
			return false;
		}
	}
}
