// 存储后端选择：默认文件存储，避免依赖 MySQL；FIVE_DB_TYPE=mysql 时走 DB
package user;

import java.sql.Connection;
import java.sql.DriverManager;

public final class StorageMode {
    public enum Mode { FILES, MYSQL }

    private static volatile Mode resolved;

    private StorageMode() {}

    public static Mode current() {
        Mode m = resolved;
        if (m != null) return m;
        synchronized (StorageMode.class) {
            if (resolved != null) return resolved;
            resolved = resolve();
            return resolved;
        }
    }

    public static boolean useFiles() { return current() == Mode.FILES; }

    private static Mode resolve() {
        String env = System.getenv("FIVE_DB_TYPE");
        String confKey = System.getProperty("five.db.type");
        String pick = env;
        if (pick == null || pick.isEmpty()) pick = confKey;
        if (pick != null) pick = pick.trim().toLowerCase();
        if ("mysql".equals(pick)) return Mode.MYSQL;
        if ("files".equals(pick) || "file".equals(pick) || "memory".equals(pick)) return Mode.FILES;
        // 自动探测：若 MySQL 可达则用，否则文件兜底
        if (canConnectMysql()) {
            log("MySQL 已就绪，使用 DB 存储");
            return Mode.MYSQL;
        }
        log("MySQL 不可达（或未安装），已切换到本地文件存储");
        return Mode.FILES;
    }

    private static boolean canConnectMysql() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (Throwable t) {
            return false;
        }
        try (Connection c = DriverManager.getConnection(
                DBConnection.getUrl(), DBConnection.getUser(), DBConnection.getPassword())) {
            return c != null && !c.isClosed();
        } catch (Throwable t) {
            return false;
        }
    }

    private static void log(String s) {
        System.out.println("[StorageMode] " + s);
    }
}
