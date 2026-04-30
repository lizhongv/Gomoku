package app;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import com.formdev.flatlaf.FlatLightLaf;
import feature.skin.SkinManager;
import profile.Profile;
import ui.scene.Router;
import ui.scene.Scene;

public class Main {
    public static void main(String[] args) {
        FlatLightLaf.setup();
        Profile.get();
        SkinManager.init();

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Gomoku 五子棋");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            Router router = new Router(frame);
            frame.setContentPane(router.cardHolder());
            router.push(new Scene() {
                @Override public JComponent root() { return new JPanel(); }
                @Override public String title() { return "Gomoku 五子棋"; }
            });
            // TODO: 4.2 改成 router.push(new HomeScene(router));
            frame.setSize(1100, 760);
            frame.setMinimumSize(new java.awt.Dimension(820, 600));
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
