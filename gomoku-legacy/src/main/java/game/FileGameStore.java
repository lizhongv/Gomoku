package game;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public final class FileGameStore {
    private static final String FILE_NAME = "server-games.csv";
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final Object LOCK = new Object();

    private FileGameStore() {}

    public static boolean addGame(Game game) {
        synchronized (LOCK) {
            File f = file();
            boolean newFile = !f.isFile();
            try (PrintWriter out = new PrintWriter(new FileWriter(f, true))) {
                if (newFile) {
                    out.println("# date,playerBlack,playerWhite,winner,manualFileName");
                }
                String date = DATE_FMT.format(game.getDate() == null ? new Date() : game.getDate());
                out.printf("%s,%s,%s,%s,%s%n",
                        escape(date), escape(game.getbUser()), escape(game.getwUer()),
                        escape(game.getWinner()), escape(game.getFileName()));
                return true;
            } catch (IOException e) {
                return false;
            }
        }
    }

    public static List<Game> getGames(String userName) {
        synchronized (LOCK) {
            List<Game> out = new ArrayList<>();
            File f = file();
            if (!f.isFile()) return out;
            try (BufferedReader r = new BufferedReader(new FileReader(f))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    String[] cols = line.split(",", -1);
                    if (cols.length < 5) continue;
                    String dateStr = unescape(cols[0]);
                    String b = unescape(cols[1]);
                    String w = unescape(cols[2]);
                    String winner = unescape(cols[3]);
                    String fname = unescape(cols[4]);
                    if (!userName.equals(b) && !userName.equals(w)) continue;
                    Date d;
                    try { d = DATE_FMT.parse(dateStr); } catch (Exception e) { d = new Date(); }
                    out.add(new Game(b, w, new java.sql.Date(d.getTime()), winner, fname));
                }
            } catch (IOException ignored) {}
            return out;
        }
    }

    public static int countWins(String winner) {
        synchronized (LOCK) {
            int n = 0;
            File f = file();
            if (!f.isFile()) return 0;
            try (BufferedReader r = new BufferedReader(new FileReader(f))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    String[] cols = line.split(",", -1);
                    if (cols.length < 5) continue;
                    if (winner.equals(unescape(cols[3]))) n++;
                }
            } catch (IOException ignored) {}
            return n;
        }
    }

    public static boolean removeAllForUser(String user) {
        synchronized (LOCK) {
            File f = file();
            if (!f.isFile()) return true;
            List<String> kept = new ArrayList<>();
            try (BufferedReader r = new BufferedReader(new FileReader(f))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (line.isEmpty() || line.startsWith("#")) { kept.add(line); continue; }
                    String[] cols = line.split(",", -1);
                    if (cols.length < 5) { kept.add(line); continue; }
                    String b = unescape(cols[1]);
                    String w = unescape(cols[2]);
                    if (!user.equals(b) && !user.equals(w)) kept.add(line);
                }
            } catch (IOException e) { return false; }
            try (PrintWriter out = new PrintWriter(new FileWriter(f, false))) {
                for (String l : kept) out.println(l);
                return true;
            } catch (IOException e) { return false; }
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace(",", "\\c").replace("\n", "\\n");
    }

    private static String unescape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(i + 1);
                if (n == 'c') { sb.append(','); i++; continue; }
                if (n == 'n') { sb.append('\n'); i++; continue; }
                if (n == '\\') { sb.append('\\'); i++; continue; }
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static File file() {
        return new File(config.AppConfig.get().userDataDir(), FILE_NAME);
    }
}
