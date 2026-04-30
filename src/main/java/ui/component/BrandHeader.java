package ui.component;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import ui.Theme;
import ui.ThemeAware;
import ui.design.Tokens;

public class BrandHeader extends JPanel implements ThemeAware {
    private final GAvatar logoAvatar;
    private final JLabel logoText;
    private final GAvatar userAvatar;
    private final JLabel userText;

    public BrandHeader() {
        super(new BorderLayout());
        Tokens t = Theme.tokens();
        setBackground(t.bgToolbar);
        setBorder(BorderFactory.createEmptyBorder(t.space3, t.space3, t.space3, t.space3));

        logoAvatar = new GAvatar(28, true);
        logoText = new JLabel("Gomoku 五子棋");
        logoText.setFont(t.fontHeading);
        logoText.setForeground(t.fgDefault);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, t.space2, 0));
        left.setOpaque(false);
        left.add(logoAvatar);
        left.add(logoText);

        userAvatar = new GAvatar(28, false);
        userText = new JLabel("");
        userText.setFont(t.fontBody);
        userText.setForeground(t.fgDefault);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, t.space2, 0));
        right.setOpaque(false);
        right.add(userText);
        right.add(userAvatar);

        add(left, BorderLayout.WEST);
        add(right, BorderLayout.EAST);
    }

    public void setCurrentUser(String name, BufferedImage avatar) {
        userText.setText(name == null ? "" : name);
        userAvatar.setImage(avatar);
    }

    @Override
    public void onSkinChanged() {
        Tokens t = Theme.tokens();
        setBackground(t.bgToolbar);
        setBorder(BorderFactory.createEmptyBorder(t.space3, t.space3, t.space3, t.space3));
        logoText.setFont(t.fontHeading);
        logoText.setForeground(t.fgDefault);
        userText.setFont(t.fontBody);
        userText.setForeground(t.fgDefault);
        repaint();
    }
}
