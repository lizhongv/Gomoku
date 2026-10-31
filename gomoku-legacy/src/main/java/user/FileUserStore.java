package user;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public final class FileUserStore {
    private static final String FILE_NAME = "server-users.properties";
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd");
    private static final Object LOCK = new Object();

    private FileUserStore() {}

    public static synchronized boolean addUser(User user) {
        synchronized (LOCK) {
            Properties p = load();
            if (p.containsKey(user.getUserName())) return false;
            String hashed = PasswordHash.hash(user.getPassWord());
            String email = user.getEmail() == null ? "" : user.getEmail();
            int level = user.getLevel();
            Date d = user.getRegDate() == null ? new Date() : user.getRegDate();
            p.setProperty(user.getUserName(),
                    encode(hashed, email, level, DATE_FMT.format(d)));
            return save(p);
        }
    }

    public static synchronized User getUser(String userName, String passWord) {
        synchronized (LOCK) {
            Properties p = load();
            String raw = p.getProperty(userName);
            if (raw == null) return null;
            String[] parts = raw.split("\\|", -1);
            if (parts.length < 4) return null;
            String stored = parts[0];
            if (!PasswordHash.verify(passWord, stored)) return null;
            String email = parts[1];
            int level = parseInt(parts[2], 1);
            Date d;
            try { d = DATE_FMT.parse(parts[3]); } catch (Exception e) { d = new Date(); }
            if (!PasswordHash.isHashed(stored)) {
                p.setProperty(userName, encode(PasswordHash.hash(passWord), email, level, parts[3]));
                save(p);
            }
            return new User(userName, passWord, email, level, d);
        }
    }

    public static synchronized boolean removeUser(String userName) {
        synchronized (LOCK) {
            Properties p = load();
            Object removed = p.remove(userName);
            if (removed == null) return false;
            return save(p);
        }
    }

    private static String encode(String pwdHash, String email, int level, String dateStr) {
        return pwdHash + "|" + (email == null ? "" : email) + "|" + level + "|" + dateStr;
    }

    private static int parseInt(String s, int fallback) {
        try { return Integer.parseInt(s); } catch (Exception e) { return fallback; }
    }

    private static File file() {
        File dir = config.AppConfig.get().userDataDir();
        return new File(dir, FILE_NAME);
    }

    private static Properties load() {
        Properties p = new Properties();
        File f = file();
        if (!f.isFile()) return p;
        try (FileInputStream in = new FileInputStream(f)) {
            p.load(in);
        } catch (IOException ignored) {}
        return p;
    }

    private static boolean save(Properties p) {
        File f = file();
        try (FileOutputStream out = new FileOutputStream(f)) {
            p.store(out, "Gomoku server users (passwordHash|email|level|regDate)");
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
