package app;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import com.formdev.flatlaf.FlatLightLaf;
import feature.rank.RankSubscriber;
import feature.shop.CoinEarnSubscriber;
import feature.shop.CoinManager;
import feature.skin.SkinManager;
import feature.sound.SoundManager;
import feature.sound.SoundSubscriber;
import profile.Profile;
import ui.component.GToast;
import ui.scene.HomeScene;
import ui.scene.Router;

public class Main {
    public static void main(String[] args) {
        FlatLightLaf.setup();
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
            });
        });
    }
}
