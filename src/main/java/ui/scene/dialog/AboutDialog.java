package ui.scene.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Window;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import ui.Theme;
import ui.component.GAvatar;
import ui.component.GButton;
import ui.component.GDialog;
import ui.design.Tokens;

public class AboutDialog extends GDialog {

    public AboutDialog(Window owner) {
        super(owner, "关于 Gomoku", true);
        Tokens t = Theme.tokens();

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        GAvatar logo = new GAvatar(72, true);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel version = new JLabel("v1.0.0", SwingConstants.CENTER);
        version.setFont(t.fontTitle);
        version.setForeground(t.fgDefault);
        version.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel desc = new JLabel(
                "<html><div style='text-align:center;'>"
                + "经典策略对战<br>"
                + "局域网联机 / 人机对战 / 实时复盘"
                + "</div></html>",
                SwingConstants.CENTER);
        desc.setFont(t.fontBody);
        desc.setForeground(t.fgMuted);
        desc.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel credits = new JLabel(
                "<html><div style='width:360px;'>"
                + "<b>依赖项：</b><br>"
                + "&bull; FlatLaf — 现代化 Swing 主题<br>"
                + "&bull; animated-gif-lib — GIF 编码<br>"
                + "&bull; MySQL Connector/J — 数据库驱动<br>"
                + "<br>"
                + "源代码：github.com/lizhongv/Gomoku"
                + "</div></html>");
        credits.setFont(t.fontCaption);
        credits.setForeground(t.fgMuted);
        credits.setAlignmentX(Component.CENTER_ALIGNMENT);

        center.add(logo);
        center.add(Box.createVerticalStrut(t.space3));
        center.add(version);
        center.add(Box.createVerticalStrut(t.space2));
        center.add(desc);
        center.add(Box.createVerticalStrut(t.space3));
        center.add(credits);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER));
        south.setOpaque(false);
        GButton ok = GButton.primary("好的");
        ok.addActionListener(e -> dispose());
        south.add(ok);

        content.add(center, BorderLayout.CENTER);
        content.add(south, BorderLayout.SOUTH);

        setSize(440, 400);
        setLocationRelativeTo(owner);
    }
}
