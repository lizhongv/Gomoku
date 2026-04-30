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
