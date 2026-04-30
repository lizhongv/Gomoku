package app;

import java.awt.Font;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import com.formdev.flatlaf.FlatLightLaf;
import feature.rank.RankSubscriber;
import feature.shop.CoinEarnSubscriber;
import feature.shop.CoinManager;
import feature.skin.SkinManager;
import feature.sound.SoundManager;
import feature.sound.SoundSubscriber;
import profile.Profile;
import ui.component.GToast;
import ui.design.Fonts;
import ui.scene.HomeScene;
import ui.scene.Router;

public class Main {
    public static void main(String[] args) {
        Font uiFont = Fonts.sans(13, Font.PLAIN);
        UIManager.put("defaultFont", new FontUIResource(uiFont));
        FlatLightLaf.setup();
        forceFontFamilyForAllFontKeys(uiFont);

        // EDT 吞 RuntimeException 会让按钮看似"无反应" — 在 sun.awt.exception.handler 兜底打印
        Thread.setDefaultUncaughtExceptionHandler((th, ex) -> {
            System.err.println("[UNCAUGHT] thread=" + th.getName());
            ex.printStackTrace();
        });
        System.setProperty("sun.awt.exception.handler", "app.Main$EdtCrashHandler");

        Profile.get();
        SkinManager.init();
        SoundManager.preload();
        SoundSubscriber.install();
        RankSubscriber.install();
        CoinEarnSubscriber.install();

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Gomoku 五子棋");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            Router router = new Router(frame);
            frame.setContentPane(router.cardHolder());
            frame.setJMenuBar(buildMenuBar(frame, router));
            router.push(new HomeScene(router));
            frame.setSize(1100, 760);
            frame.setMinimumSize(new java.awt.Dimension(820, 600));
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            SwingUtilities.invokeLater(() -> {
                CoinManager.CheckinResult r = CoinManager.checkinIfDue();
                if (r.newToday) {
                    GToast.success(frame,
                            "签到成功！连续 " + r.consecutiveDays + " 天，奖励 " + r.reward + " 金币");
                }
                if (!Profile.get().onboardingDone) {
                    new ui.scene.dialog.OnboardingDialog(frame).setVisible(true);
                }
            });
        });
    }

    private static javax.swing.JMenuBar buildMenuBar(JFrame frame, ui.scene.Router router) {
        javax.swing.JMenuBar bar = new javax.swing.JMenuBar();

        javax.swing.JMenu fileMenu = new javax.swing.JMenu("文件");
        javax.swing.JMenuItem mExit = new javax.swing.JMenuItem("退出");
        mExit.addActionListener(e -> System.exit(0));
        fileMenu.add(mExit);

        javax.swing.JMenu viewMenu = new javax.swing.JMenu("视图");
        javax.swing.JMenuItem mHome = new javax.swing.JMenuItem("返回主页");
        mHome.addActionListener(e -> router.replace(new ui.scene.HomeScene(router)));
        javax.swing.JMenuItem mShop = new javax.swing.JMenuItem("商店");
        mShop.addActionListener(e -> router.push(new ui.scene.ShopScene(router)));
        javax.swing.JMenuItem mSettings = new javax.swing.JMenuItem("设置");
        mSettings.addActionListener(e -> router.push(new ui.scene.SettingsScene(router)));
        viewMenu.add(mHome);
        viewMenu.addSeparator();
        viewMenu.add(mShop);
        viewMenu.add(mSettings);

        javax.swing.JMenu helpMenu = new javax.swing.JMenu("帮助");
        javax.swing.JMenuItem mRules = new javax.swing.JMenuItem("游戏规则");
        mRules.addActionListener(e -> showRulesDialog(frame));
        javax.swing.JMenuItem mGuide = new javax.swing.JMenuItem("新手引导");
        mGuide.addActionListener(e -> new ui.scene.dialog.OnboardingDialog(frame).setVisible(true));
        javax.swing.JMenuItem mAbout = new javax.swing.JMenuItem("关于");
        mAbout.addActionListener(e -> new ui.scene.dialog.AboutDialog(frame).setVisible(true));
        helpMenu.add(mRules);
        helpMenu.add(mGuide);
        helpMenu.add(mAbout);

        bar.add(fileMenu);
        bar.add(viewMenu);
        bar.add(helpMenu);
        return bar;
    }

    private static void showRulesDialog(JFrame owner) {
        javax.swing.JEditorPane pane = new javax.swing.JEditorPane();
        pane.setContentType("text/html");
        pane.setEditable(false);
        pane.setText("<html><body style='font-family:Microsoft YaHei UI;'>"
                + "<h3>对局目标</h3>"
                + "<p>在 15×15 棋盘上，先在横、竖、斜任一方向连成五子者胜。</p>"
                + "<h3>基本规则</h3>"
                + "<ul><li>黑棋先手，黑白双方轮流落子，每次仅落一颗。</li>"
                + "<li>已落下的棋子不可移动或拿取。</li>"
                + "<li>本作采用「无禁手」规则，黑白方权利对等。</li></ul>"
                + "</body></html>");
        javax.swing.JOptionPane.showMessageDialog(owner,
                new javax.swing.JScrollPane(pane), "五子棋规则",
                javax.swing.JOptionPane.PLAIN_MESSAGE);
    }

    /** 旧 sun.awt.exception.handler 协议要求 public 类有 public 无参构造 + handle(Throwable) 方法。 */
    public static class EdtCrashHandler {
        public EdtCrashHandler() {}
        public void handle(Throwable ex) {
            System.err.println("[EDT-UNCAUGHT]");
            ex.printStackTrace();
        }
    }

    private static void forceFontFamilyForAllFontKeys(Font uiFont) {
        String family = uiFont.getFamily();
        java.util.List<Object> keys = new java.util.ArrayList<>();
        java.util.Enumeration<Object> en = UIManager.getDefaults().keys();
        while (en.hasMoreElements()) keys.add(en.nextElement());
        for (Object k : keys) {
            Object v = UIManager.get(k);
            if (v instanceof FontUIResource) {
                FontUIResource old = (FontUIResource) v;
                UIManager.put(k, new FontUIResource(family, old.getStyle(), old.getSize()));
            } else if (v instanceof Font) {
                Font old = (Font) v;
                UIManager.put(k, new FontUIResource(family, old.getStyle(), old.getSize()));
            }
        }
    }
}
