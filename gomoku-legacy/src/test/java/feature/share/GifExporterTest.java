package feature.share;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Date;
import feature.skin.SkinManager;
import game.Manual;

public class GifExporterTest {
    static int passed = 0, failed = 0;

    public static void main(String[] args) throws Exception {
        File tmp = new File(System.getProperty("java.io.tmpdir"),
                "gomoku-gif-test-" + System.currentTimeMillis());
        tmp.mkdirs();
        System.setProperty("user.home", tmp.getAbsolutePath());

        SkinManager.init();

        int[][] chess = new int[][]{{7, 7}, {8, 8}, {7, 8}, {8, 7}, {6, 7}};
        Manual m = new Manual("alice", "bob", new Date(System.currentTimeMillis()), "alice", chess.length, chess);

        ExportOpts opts = new ExportOpts();
        opts.skinId = "classic";
        opts.maxFrames = chess.length;
        opts.scale = 200;
        opts.frameDelayMs = 100;

        File out = GifExporter.export(m, opts, null);

        boolean exists = out.exists() && out.length() > 100;
        boolean header = false;
        try (FileInputStream in = new FileInputStream(out)) {
            byte[] head = new byte[6];
            int read = in.read(head);
            header = (read == 6) && new String(head).equals("GIF89a");
        }
        ok("GIF 生成且头部为 GIF89a", exists && header);
        out.delete();

        System.out.println("结果: passed=" + passed + " failed=" + failed);
        System.exit(failed == 0 ? 0 : 1);
    }

    static void ok(String name, boolean cond) {
        if (cond) { passed++; System.out.println("  ✓ " + name); }
        else      { failed++; System.out.println("  ✗ " + name); }
    }
}
